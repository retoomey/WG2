package org.wdssii.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.SourceManager;

/** IndexCollection doesn't directly connects to indexes. The IndexWatcher
 * class does.  This hides the connect/reconnect among other
 * things.
 * Connect can take some time, so it is called from a different thread,
 * this means we have to use sync methods
 * 
 * There is one IndexWatcher per Index.
 * 
 * @author Robert Toomey
 */
public class IndexWatcher implements HistoryListener {

    private static Log log = LogFactory.getLog(SourceManager.class);
    protected IndexCollection myCollection;
    protected String myName;
    private HistoricalIndex myIndex = null;
    protected boolean myRealtime;
    protected String myPath;
    protected boolean myLastReadSuccess = false;
    protected volatile boolean myConnecting = false;
    protected volatile boolean myConnected = false;

    public IndexWatcher(IndexCollection c, String nameToUse, String indexPath,
            boolean realtime) {
        myCollection = c;
        myName = nameToUse;
        myPath = indexPath;
        myRealtime = realtime;
    }

    public boolean isConnecting() {
        return myConnecting;
    }

    public void setConnecting(boolean flag) {
        myConnecting = flag;
    }

    public boolean isConnected() {
        return myConnected;
    }

    public String getKeyName() {
        return myName;
    }

    public boolean getRealtime() {
        return myRealtime;
    }

    public synchronized String getIndexLocation() {
        return myPath;
    }

    public synchronized String getPath() {
        return myPath;
    }

    public synchronized void setPath(String newPath) {
        myPath = newPath;
    }

    public synchronized void disconnect() {
        if (myIndex != null) {
            myConnected = false;
            myIndex = null;
        }
    }

    /** Used by GUI to set status to 'connecting' and update GUI before starting connection */
    public synchronized boolean aboutToConnect() {
        setConnecting(true);
        return true;
    }

    /** Create a new historical index around path, or null on failure. 
     * Note: the gui calls this in a separate thread, so synchronize if needed.
     * The GUI knows not to access this IndexWatcher until connect completes. */
    public synchronized boolean connect() {

        //setConnecting(true);  If we do this here, GUI doesn't get chance to update
        if (!isConnecting()) {  // If called by someone other than GUI, just turn it on now.
            setConnecting(true);
        }

        boolean success = false;

        if (myIndex == null) {

            try {
                myIndex = new HistoricalIndex(myPath, IndexCollection.myDefaultHistorySize);
            } catch (Exception e) {
                // If index fails, we'll try to recover
                log.error("Index could not be created: '" + myPath + "'");
                myIndex = null;
            }
            if (myIndex != null) {
                Index anIndex = myIndex.getIndex();
                if (anIndex != null) { // Update path with actual located one (this is bugged in wdssiijava)
                    // Bug: xml:C --> becomes C --> 'No protocol C' error
                    //myPath = anIndex.getIndexLocation();
                }
                myIndex.addHistoryListener(this);
                success = true;
            }
        } else {
            // Warn on connecting to already connected? Or autoreconnect?
            success = true;
        }
        // DataUnavailableException e)
        setConnecting(false);
        myConnected = success;

        return success;
    }

    /** Return current historical index, only valid on proper connection */
    public HistoricalIndex getIndex() {
        // Assume index not ready for use if we're still connecting...
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
        rec.setSourceName(myName);
        myCollection.handleAddedRecord(rec);
    }

    @Override
    public void recordDeleted(IndexRecord rec) {
        // TODO Auto-generated method stub
        //System.out.println("WG got deleted record event");
    }

    @Override
    public String toString() {
        return myName;
    }

    public boolean isRealtime() {
        return myRealtime;
    }
}