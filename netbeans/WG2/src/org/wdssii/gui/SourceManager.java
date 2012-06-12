package org.wdssii.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.index.Index;

/**
 *
 * This class is slowly going away since I've expanded 'Sources' to include non-Index
 * based sources.
 * 
 * @author Robert Toomey
 *
 */
public class SourceManager implements Singleton {

	private static Logger log = LoggerFactory.getLogger(SourceManager.class);

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

	public SourceRecordLog getSourceRecordLog() {
		return mySourceRecordLog;
	}
}