package org.wdssii.gui;

import java.util.ArrayList;
import java.util.Date;

import org.wdssii.gui.commands.DataCommand;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.Index;
import org.wdssii.index.IndexCollection;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.IndexWatcher;
import org.wdssii.index.HistoricalIndex.Direction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Maintains a persistent IndexCollection for the GUI.  This is the
 * list of sources
 * 
 * Note: Made the design decision to not make SourceManager a subclass of
 * IndexCollection.  This allows restricting access to the global IndexCollection
 * (We force coders to user the SourceCommand objects, thus enforcing jobs/threads/gui updates)
 * 
 * @author Robert Toomey
 *  
 */
public class SourceManager implements Singleton {

    private static Log log = LogFactory.getLog(SourceManager.class);

    /** Listener linking new records to the GUI */
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
    /** Our index collection that corresponds to the current sources in the GUI */
    private IndexCollection myIndexCollection = new SourceManagerCollection();

    /** The root class for all source commands.
     * Only SourceCommands have access to private methods of SourceManager.
     * These are 'helper' objects of SourceManager
     * To call any combination of methods here you should create a subclass of SourceCommand:
     * SourceCommand c = new SourceDeleteCommand("KTLX");
     * CommandManager.getInstance().execute(c);
     * It's ok to create a SourceCommand within the execute of another command.
     */
    public static abstract class SourceCommand extends DataCommand {

        /** The index key for affected index, if any */
        public String myIndexName = null;

        /** Get the index key for this command */
        public String getIndexName() {
            return myIndexName;
        }

        /** Set the index key for this command */
        public void setIndexName(String s) {
            myIndexName = s;
        }

        /** Do we have a valid index or selected source? */
        protected boolean validIndexNameOrSelected() {
            String name = getIndexName();
            if (name == null) {
                name = SourceManager.getInstance().getIndexCollection().getSelectedIndexKey();
            }
            if (name != null) {
                setIndexName(name);
            }
            return (name != null);
        }

        // Private method access (for subclasses) ----------------------------
        /** Remove an index */
        protected void removeIndexKey(String name) {
            SourceManager.getInstance().getIndexCollection().removeIndexKey(name);
        }

        /** Add an index */
        protected String add(String shortName, String path, boolean realtime) {
            String success = SourceManager.getInstance().getIndexCollection().add(shortName, path, realtime);
            return success;
        }

        /** Called before a connect, so that we can update GUI to show connecting statuses */
        protected boolean aboutToConnect(String keyName) {
            boolean success = SourceManager.getInstance().getIndexCollection().aboutToConnect(keyName);
            return success;
        }

        /** Add to an index.  This can take some time so should be called in a worker thread */
        protected boolean connect(String keyName) {
            boolean success = SourceManager.getInstance().getIndexCollection().connect(keyName);
            return success;
        }

        protected void disconnect(String keyName) {
            SourceManager.getInstance().getIndexCollection().disconnect(keyName);
        }
    }

    /** Only SourceManager and SourceCommands are allowed to access this directly */
    private IndexCollection getIndexCollection() {
        return myIndexCollection;
    }
    private static SourceManager instance = null;
    private SourceRecordLog mySourceRecordLog = new SourceRecordLog();

    private SourceManager() {
        // Exists only to defeat instantiation.
    }

    public static SourceManager getInstance() {
        if (instance == null) {
            Index.setGUIMode(true);
            instance = new SourceManager();

            //String userdir = System.getProperty("user.dir");
            //System.out.println("*************>>>>>>>>>>"+userdir);
            //String javaversion = System.getProperty("java.specification.version");
            //System.out.println(">>>>>>>>>>>>>>>>>>JAVA VERSION "+javaversion);
        }

        return instance;
    }

    /**
     * Do the init work of the singleton here
     */
    @Override
    public void singletonManagerCallback() {
        // FIXME: This is just for testing, shouldn't do this really....

        // Offline mode for worldwind...need to be able to toggle it...when off network, we 'hang'
        //NetworkStatus s = WorldWind.getNetworkStatus();  // FIXME: Gonna need our own network status I think that uses RCP jobs to check..
        //s.setOfflineMode(true);

        CommandManager c = CommandManager.getInstance();
        boolean connect = true;
        c.executeCommand(new SourceAddCommand("CONUS", "file:/E:/CONUS/code_index.xml?p=xml", false, false, connect), false);
        c.executeCommand(new SourceAddCommand("KTLX", "file:/E:/KTLX-large/radar_data.xml?p=xml", false, false, connect), false);
        c.executeCommand(new SourceAddCommand("Wind", "file:/E:/WindData/code_index.xml", false, false, connect), false);


        c.executeCommand(new SourceAddCommand("KTLX-ARCHIVE", "http://tensor.protect.nssl/data/KTLX-large/radar_data.xml?p=xml", false, false, connect), false);

        //instance.connect("CONUS", "xml:E:/CONUS/code_index.xml", false);
        //instance.connect("KTLX", "xml:E:/KTLX-large/radar_data.xml", false);
        //instance.connect("Wind", "xml:E:/WindData/code_index.xml", false);	
    }

    /**
     * Attempt to connect to an index, returns true on success
     * 
     * @param shortName
     *            the short name of the index
     * @param path
     *            the url path of the index
     * @param realtime
     *            if realtime, connect an index listener to it
     * @return success of connection to index
     */
    /*public boolean connect(String shortName, String path, boolean realtime) {
    boolean changed = false;
    boolean success = false;
    
    // This 'adds' to the list of index (not the same as connect)
    IndexWatcher current = add(shortName, path, realtime);
    try {
    success = current.connect();
    if (!success) {
    log.warn("Couldn't connect to index '" + shortName + "' " + path);
    } else {
    log.info("Connected to '" + shortName + "' " + path);
    log.info("There are " + getIndexCount() + "index(es)");
    
    }
    changed = true;
    } catch (IllegalArgumentException e) {
    log.error("Could not connect to source " + shortName);
    } catch (DataUnavailableException e) {
    log.error("Data UNAVAILABLE!");
    }
    if (changed) {
    CommandManager.getInstance().executeCommand(
    new SourceConnectCommand(), false);
    }
    return true;
    }*/
    //public void connect(String indexKey){
//
    //}
    /** A disconnect is basically a delete, but where we keep the index for future connection */
    public void disconnect(String indexName) {
        //	IndexInformation current = myIndexSet.get(indexName);
        //if (current != null) {
        // FIXME: Actually disconnect the source to stop autoupdate...
        // We're just removing all stuff right now other than the listing
        //CommandManager.getInstance().executeCommand(
        //		new SourceDisconnectCommand(indexName), false); // Source disconnected
        //}
    }

    public static HistoricalIndex getIndexByName(String name) {
        return getInstance().getIndexCollection().getIndexByNameI(name);
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

    // Temp functions
    // Given an index key, record and direction, try to get the next record
    // Shouldn't these be inside the index class?
    protected IndexRecord getPreviousLowestRecord(String indexKey,
            IndexRecord current) {
        HistoricalIndex theIndex = getIndexByName(indexKey);
        IndexRecord newRecord = null;

        if (theIndex != null) {
            // newRecord = theIndex.getNextRecord(current, direction);
            // ----- THIS might be new direction in index...but then again, we
            // need to
            // find 'base' by comparing the subtype string. Something like that
            // should
            // be overridable by Product.

            IndexRecord iter = current;
            IndexRecord previous;
            boolean found = false;
            String subtype;

            subtype = current.getSubType();
            // System.out.println("Current subtype is "+subtype);
            if (subtype == null) {
                return null;
            }

            String currsubtype = subtype;
            String prevsubtype = null;

            // The 'base' logic. FIXME: this is flawed.
            // Hunt previous records until we get a subtype above us
            do {
                previous = theIndex.getNextRecord(iter,
                        Direction.PreviousSubType);
                if (previous != null) {
                    prevsubtype = previous.getSubType();
                    // System.out.println("Comparing "+prevsubtype+" to "+subtype);
                    // System.out.format("----->%d",
                    // prevsubtype.compareTo(subtype));
                    if (prevsubtype.compareTo(subtype) >= 0) { // THIS SHOULD BE
                        // BY PRODUCT
                        break;
                    }
                }

                found = true;
                if (prevsubtype != null) {
                    subtype = prevsubtype;
                }
                iter = previous;
            } while (found);

            if (found && (!currsubtype.equals(subtype))) {
                newRecord = iter;
            }
            // ----- END NEW DIRECTION?

            //
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

    /*  This will shift subtype
    public IndexRecord getRecordLatestUpToDate(String indexKey,
    String dataType, Date time) {
    Index theIndex = getIndexByName(indexKey);
    IndexRecord newRecord = null;
    
    if (theIndex != null) {
    
    // First try record matching datatype, subtype and time...
    newRecord = theIndex.getLatestUpToDate(dataType, time);
    
    // Make sure source name set in record
    if (newRecord != null) {
    newRecord.setSourceName(getIndexName(theIndex));
    }
    }
    return newRecord;
    }
     */
    /** Going to modify the builder/index so that we can 'open' a single file and have it go into a 'static' index */
    public void build1FromLocation() {
        //Date toUse = new Date(); // use 'now'  Humm notice with this the true date is INSIDE the object, not outside like normal
        //IndexRecord r = new IndexRecord(toUse, null, null);
        //BuilderFactory.createDataType(rec);
        //	testrcp.Application test ;
        //	IndexRecord r = new IndexRecord();
    }
    /*
    public void disconnectSelectedSource() {
    System.out.println("Current sourceName is " + mySelectIndex);
    disconnect(mySelectIndex);
    }*/

    // Index collection access  -----------------------------
    public String getNiceShortName(String toDelete) {
        return myIndexCollection.getNiceShortName(toDelete);
    }

    public String getIndexName(HistoricalIndex anIndex) {
        return myIndexCollection.getIndexName(anIndex);
    }

    public String getSelectedIndexKey() {
        return myIndexCollection.getSelectedIndexKey();
    }

    public ArrayList<IndexWatcher> getIndexList() {
        return myIndexCollection.getIndexList();
    }

    public IndexWatcher getIndexWatcher(String s) {
        return myIndexCollection.getIndexWatcher(s);
    }

    public void selectIndexKey(String key) {
        myIndexCollection.selectIndexKey(key);
    }
}