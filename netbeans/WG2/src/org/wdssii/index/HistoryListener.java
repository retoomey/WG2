package org.wdssii.index;

/** 
 * A history listener listens to a HistorialIndex for adding/deletion of records
 * 
 * @author Robert Toomey
 *
 */
public interface HistoryListener {

    /** Called when a record is added to the history */
    public void recordAdded(IndexRecord rec);

    /** Called when a record is deleted from history (oldest record when full) */
    public void recordDeleted(IndexRecord rec);
}