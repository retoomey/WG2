package org.wdssii.gui;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.core.Singleton;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.IndexSourceAddParams;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.Index;

/**
 *
 * This class is slowly going away since I've expanded 'Sources' to include
 * non-Index based sources.
 *
 * @author Robert Toomey
 * @deprecated
 */
public class SourceManager implements Singleton {

    private final static Logger LOG = LoggerFactory.getLogger(SourceManager.class);
    private static SourceManager instance = null;
    private SourceRecordLog mySourceRecordLog = new SourceRecordLog();

    private SourceManager() {
        // Exists only to defeat instantiation.
    }

    public static Singleton create() {
        instance = new SourceManager();
        return instance;
    }

    public static SourceManager getInstance() {
        if (instance == null) {
            LOG.debug("SourceManager must be created by SingletonManager");
        }
        return instance;
    }

    /**
     * Do the init work of the singleton here
     */
    @Override
    public void singletonManagerCallback() {
        try {
            Index.setGUIMode(true);
            instance = new SourceManager();
           // CommandManager c = CommandManager.getInstance();
           // boolean connect = true;
           // IndexSourceAddParams p = new IndexSourceAddParams("KTLX-ARCHIVE", "http://tensor.protect.nssl/data/KTLX-large/radar_data.xml", false, connect, HistoricalIndex.HISTORY_ARCHIVE);
           // c.executeCommand(new SourceAddCommand(p), false);
        } catch (Exception e) {
            // Recover
        }
    }

    public SourceRecordLog getSourceRecordLog() {
        return mySourceRecordLog;
    }
}