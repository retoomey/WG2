package org.wdssii.gui;

import java.net.URL;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.index.HistoricalIndex.Direction;
import org.wdssii.index.*;

/**
 * Maintains a persistent IndexCollection for the GUI. This is the list of
 * sources
 *
 * This class is slowly going away since I've expanded 'Sources' to include non-Index
 * based sources.
 * 
 * @author Robert Toomey
 *
 */
public class SourceManager implements Singleton {

	private static Logger log = LoggerFactory.getLogger(SourceManager.class);
	/**
	 * The manual index for local files
	 */
	private ManualLoadIndex myManualIndex;

	/**
	 * Listener linking new records to the GUI
	 */
	public static class SourceManagerCollection extends IndexCollection {

		@Override
		public void handleAddedRecord(IndexRecord rec) {
			// Prints FREAK the console here
			// System.out.println("**********************Source manager got a record to dispatch to display!");
			// Add record to the record log
			SourceManager.getInstance().getSourceRecordLog().add(rec);

			CommandManager.getInstance().handleRecord(rec);
		}
	}
	/**
	 * Our index collection that corresponds to the current sources in the
	 * GUI
	 */
	private IndexCollection myIndexCollection = new SourceManagerCollection();

	private static SourceManager instance = null;
	private SourceRecordLog mySourceRecordLog = new SourceRecordLog();

	private SourceManager() {
		// Exists only to defeat instantiation.
	}

	public static SourceManager getInstance() {
		if (instance == null) {
			Index.setGUIMode(true);
			instance = new SourceManager();
		}

		return instance;
	}

	/**
	 * Do the init work of the singleton here
	 */
	@Override
	public void singletonManagerCallback() {
		try {
			CommandManager c = CommandManager.getInstance();
			boolean connect = true;
			c.executeCommand(new SourceAddCommand("KTLX-ARCHIVE", "http://tensor.protect.nssl/data/KTLX-large/radar_data.xml", false, connect), false);
		} catch (Exception e) {
			// Recover
		}
	}

	public static HistoricalIndex getIndexByName(String name) {
		SourceManager m = getInstance();
		Source s = SourceList.theSources.getSource(name);
		HistoricalIndex anIndex = null;
		if (s instanceof IndexSource) {
			anIndex = ((IndexSource) s).getIndex();
		}
		return anIndex;
	}

	// Given an index key, record and direction, try to get the next record
	protected IndexRecord getRecordFromInfo(String indexKey,
		IndexRecord current, Direction direction) {
		HistoricalIndex theIndex = getIndexByName(indexKey);
		IndexRecord newRecord = null;

		if (theIndex != null) {
			newRecord = theIndex.getNextRecord(current, direction);
			if (newRecord != null) {
				newRecord.setSourceName(myIndexCollection.getIndexName(theIndex));
			}
		}
		return newRecord;
	}

	// Humm if there's only one source manager,
	// why not have all public methods static??
	public static IndexRecord getRecord(String indexKey, IndexRecord current,
		Direction direction) {
		return (getInstance().getRecordFromInfo(indexKey, current, direction));
	}

	protected IndexRecord getPreviousLowestRecord(String indexKey,
		IndexRecord current) {
		HistoricalIndex theIndex = getIndexByName(indexKey);
		IndexRecord newRecord = null;

		if (theIndex != null) {
			newRecord = theIndex.getPreviousLowestRecord(current);
			if (newRecord != null) {
				newRecord.setSourceName(myIndexCollection.getIndexName(theIndex));
			}
		}
		return newRecord;
	}

	public static IndexRecord getPreviousLatestRecord(String indexKey,
		IndexRecord current) {
		return (getInstance().getPreviousLowestRecord(indexKey, current));
	}

	public SourceRecordLog getSourceRecordLog() {
		return mySourceRecordLog;
	}

	// Get latest record up to given time, but not greater (used for product
	// synchronization
	public IndexRecord getRecordLatestUpToDate(String indexKey,
		String dataType, String subType, Date time) {
		HistoricalIndex theIndex = getIndexByName(indexKey);
		IndexRecord newRecord = null;

		if (theIndex != null) {

			// First try record matching datatype, subtype and time...
			newRecord = theIndex.getLatestUpToDate(dataType, subType, time);

			// Make sure source name set in record
			if (newRecord != null) {
				newRecord.setSourceName(myIndexCollection.getIndexName(theIndex));
			}
		}
		return newRecord;
	}

	/**
	 * Add a local URL to our ManualLoadIndex
	 */
	public void addSingleURL(URL location, String product, String choice, Date time, String[] params) {
		if (myManualIndex != null) {
			myManualIndex.addRecordFromURL(location, product, choice, time, params);
		}
	}
}