package org.wdssii.gui.sources;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.views.SourcesURLLoadDialog;
import org.wdssii.index.*;

/**
 * An 'Index' source is our NSSL xml format that stores a collection of
 * time-based references to a collection of urls/files. It is basically a
 * meta-data file for organizing a collection of data.
 *
 * @author Robert Toomey
 */
public class IndexSource extends Source implements HistoryListener {

	private static Logger log = LoggerFactory.getLogger(IndexSource.class);

	/** Zero is special case for keeping all records and never deleting,
	 * this should be used only for set archives otherwise memory will fill
	 * up.  FIXME: Might actually disable autoupdate for this case
	 */
	public static int HISTORY_ARCHIVE = 0;

	/**
	 * The small helper object for making this source
	 */
	public static class Factory extends SourceFactory {

		/**
		 * @return true if we handle this file type
		 */
		@Override
		public boolean canHandleFileType(File f) {

			boolean canHandle;
			String t = f.getName().toLowerCase();
			// FIXME: Many get this from all of the Wdssii Index objects
			canHandle = ((t.endsWith(".xml")) || 
				     (t.endsWith(".xml.gz")) ||
				     (f.isDirectory())); // will use fam to watch for fml files
			return canHandle;
		}

		/**
		 * @return type information on what files we can handle
		 */
		@Override
		public Set<String> getHandledFileDescriptions() {
			TreeSet<String> d = new TreeSet<String>();
			d.add("W2 Index");   // Wdssii index
			return d;
		}

		@Override
		public GUIPlugInPanel createParamsGUI(SourcesURLLoadDialog d) {
			return new IndexSourceParamsGUI(d);
		}

		/**
		 * Return true if we can create a source from this URL
		 */
		@Override
		public boolean canCreateFromURL(URL aURL) {
			boolean canCreate = false;
			if (aURL != null) {
				boolean isIndex = IndexFactory.checkURLForIndex(aURL);
				if (isIndex) {
					canCreate = true;
				}
			}
			return canCreate;
		}

		@Override
		public String getDialogDescription() {
			return "WDSSII XML Index";
		}
	}
	/**
	 * The historical index within this source, this does all the low-level
	 * hard work
	 */
	private HistoricalIndex myIndex = null;
	/**
	 * The GUI for this Feature
	 */
	private SourceGUI myControls;
	protected boolean myRealtime;
	protected String myPath;
	protected int myHistory;
	protected boolean myLastReadSuccess = false;
	protected volatile boolean myConnecting = false;
	protected volatile boolean myConnected = false;

	public IndexSource(String niceName, URL aURL, int historyValue) {
		super(niceName, aURL);
		myPath = aURL.toString();
		myRealtime = true;
		myHistory = historyValue;
	}

	@Override
	public synchronized boolean isConnecting() {
		return myConnecting;
	}

	public synchronized void setConnecting(boolean flag) {
		myConnecting = flag;
	}

	@Override
	public synchronized boolean isConnected() {
		return myConnected;
	}

	@Override
	public boolean isRealtime() {
		return myRealtime;
	}

	public synchronized String getIndexLocation() {
		return myPath;
	}

	public synchronized String getPath() {
		return myPath;
	}

	@Override
	public synchronized void disconnect() {
		if (myIndex != null) {
			myConnected = false;
			myIndex.aboutToDispose();
			myIndex = null;
		}
	}

	/**
	 * Used by GUI to set status to 'connecting' and update GUI before
	 * starting connection
	 */
	@Override
	public synchronized boolean aboutToConnect(boolean start) {
		setConnecting(start);
		return true;
	}

	/**
	 * Create a new historical index around path, or null on failure. Note:
	 * the gui calls this in a separate thread, so synchronize if needed.
	 * The GUI knows not to access this IndexWatcher until connect
	 * completes.
	 */
	@Override
	public synchronized boolean connect() {

		//setConnecting(true);  If we do this here, GUI doesn't get chance to update
		if (!isConnecting()) {  // If called by someone other than GUI, just turn it on now.
			setConnecting(true);
		}

		boolean success = false;

		if (myIndex == null) {

			try {
				myIndex = new HistoricalIndex(myPath, myHistory);
			} catch (Exception e) {
				log.error("Index could not be created: '" + myPath + "'");
				myIndex = null;
			}
			if (myIndex != null) {
				myIndex.addHistoryListener(this);
				success = true;
			}
		} else {
			success = true;
		}
		//  setConnecting(false);  GUI will set..hummm
		myConnected = success;

		return success;
	}

	/**
	 * Directly set the index we use
	 */
	public void setIndex(HistoricalIndex i) {
		myIndex = i;
	}

	/**
	 * Return current historical index, only valid on proper connection
	 */
	public HistoricalIndex getIndex() {
		// Assume index not ready for use if we're still connecting...
		// log.debug("GET INDEX " + myIndex + " " + myConnecting + ", " + !myConnected);
		if (myConnecting || !myConnected) {
			return null;
		}
		return myIndex; // CAN be null on failure or not connected
	}

	public boolean wasLastReadSuccessful() {
		if (myIndex != null) {
			myLastReadSuccess = myIndex.getIndex().getLastReadSuccess();
		}
		return myLastReadSuccess;
	}

	@Override
	public void recordAdded(IndexRecord rec) {
		rec.setSourceName(getKey());
		//myCollection.handleAddedRecord(rec);
	}

	@Override
	public void recordDeleted(IndexRecord rec) {
		// TODO Auto-generated method stub
		//System.out.println("WG got deleted record event");
	}

	@Override
	public boolean setupSourceGUI(JComponent source, JComponent source2) {

		// FIXME: general FeatureFactory..move code up into Feature
		boolean success = false;
		// if (myFactory != null) {

		if (myControls == null) {
			//myControls = myFactory.createGUI(myLLHArea, source);
			myControls = new IndexSourceGUI(this);
		}

		// Set the layout and add our controls
		if (myControls != null) {
			myControls.activateGUI(source, source2);
			updateGUI();
			success = true;
		}
		//  }

		/**
		 * Fill in with default stuff if GUI failed or doesn't exist
		 */
		if (!success) {
			return super.setupSourceGUI(source, source2);
		}
                return false;
	}

	@Override
	public void updateGUI() {
		if (myControls != null) {
			myControls.updateGUI();
		}
	}

	@Override
	public String getSourceDescription() {
		String description;
		int aSize = 0;
		int maxSize = 0;
		int totalSize = 0;
		String location = "";

		HistoricalIndex i = getIndex();
		if (i != null) {
			aSize = i.getCurrentHistorySize();
			maxSize = i.getMaxHistorySize();
			totalSize = i.getTotalHistorySize();
			location = getPath();
		}
		// Update the content description for selected index
		// String shortName = getVisibleName();
		String content = String.format("%s (%d/%d [%d lifetime])",
			location,
			aSize,
			maxSize,
			totalSize);
		description = content;
		return description;
	}

	/**
	 * Get the shown type name for this source The GUI uses this to show
	 * what type we think we are
	 */
	@Override
	public String getShownTypeName() {
		String name = "";
		HistoricalIndex i = getIndex();
		if (i != null) {
                     Index anIndex = i.getIndex();
		     if (anIndex != null){
			    name= anIndex.getClass().getSimpleName();
		     }
		}
		if (name.isEmpty()){
			return "W2 Index";
		}else{
		    return "W2 Index("+name+")";
		}
	}
}
