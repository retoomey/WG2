package org.wdssii.gui;

import java.util.ArrayList;

import org.wdssii.index.IndexRecord;

/* Abstract model for source record logger.  This can exist independent of a view that shows it
it can also be modified in a different thread than the swt UI thread
 * @author Robert Toomey
 * FIXME: this probably needs synchronize in it
 */
public class SourceRecordLog {

    /** Counter of total records received to log */
    private int myCounter = 0;
    private boolean myRememberNullRecords = false;
    /** Max number of records we will hold in the log */
    public int myMaxSize = 500;

    // Storage for the filtered records
    // FIXME: Maybe this should be handled by the sourcemanager thread
    public static class SourceRecordLogData {
        // FIXME: use get/set?

        public String indexName; // Humm or the index key so if name changes we
        // stay synced
        public String dataType;
        public String timeStamp;
        public int number;  // order counter
    }
    protected ArrayList<SourceRecordLogData> myRecordList = new ArrayList<SourceRecordLogData>();

    public int size() {
        return myRecordList.size();
    }

    public int getCounter() {
        return myCounter;
    }

    public int getMaxHistorySize() {
        return myMaxSize;
    }

    public SourceRecordLogData get(int index) {
        SourceRecordLogData d = null;
        int size = myRecordList.size();
        // Invert the log (newest is 0 for showing)
        int reverse = size - index - 1;
        if (reverse >= 0) { // if too large, bigger negative
            d = myRecordList.get(reverse);
        }
        return d;
    }

    /*
     * public void add(SourceRecordLogData data){ myRecordList.add(data); }
     */
    public void add(IndexRecord rec) {

        if (!myRememberNullRecords && (rec == null)) {
            return;
        }

        myCounter++;
        SourceRecordLogData newSourceRecordData = new SourceRecordLogData();
        if (rec != null) {
            newSourceRecordData.indexName = rec.getSourceName();
            newSourceRecordData.dataType = rec.getDataType();
            newSourceRecordData.timeStamp = rec.getTimeStamp();
        } else {
            newSourceRecordData.indexName = "NULL"; // FIXME: setters?
            newSourceRecordData.dataType = "????";
            newSourceRecordData.timeStamp = "????";
        }
        newSourceRecordData.number = myCounter;

        // Trim the records to the maximum size (this deletes only one)
        if (myRecordList.size() == myMaxSize) {
            myRecordList.remove(0);
        }
        myRecordList.add(newSourceRecordData);

    }
}
