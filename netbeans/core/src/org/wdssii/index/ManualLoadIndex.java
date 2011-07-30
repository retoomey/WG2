package org.wdssii.index;

import java.net.URL;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;

/**
 * ManualLoadIndex is a dynamic index that stores records based on
 * sending the actual URL location of the file.  For example, the
 * GUI uses this to allow the user to open a local file on disk...that
 * file is then added to this index.  All opened data files/paths are typically
 * added to a single ManualLoadIndex
 * 
 * @author Robert Toomey
 */
public class ManualLoadIndex extends Index {

    private static Log log = LogFactory.getLog(TestIndex.class);

    @Override
    public Index newInstance(URL path, URL full, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {
        return new ManualLoadIndex(path, listeners);
    }

    /** meant for prototype factory use only. */
    public ManualLoadIndex() {
        super(null, null);
    }

    @Override
    public void update() {
    }

    public ManualLoadIndex(URL aURL, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        super(Index.getParent(aURL), listeners);
        
    }

    /** Add a record for a given URL location.  This will allow the user
     * to load the product contained within by clicking on it.
     * 
     */
    public void addRecordFromURL(URL theURL, String product, String choice, Date d,
            String[] params){
    
        String timeString = IndexRecord.getStringFromDate(d) + " ";
       
        // Create the selections for this file...
        String[] selections = new String[3];
        selections[0] = timeString;
        selections[1] = product;
        selections[2] = choice;
        
        // Create a single params for file...
        String[][] aparams = new String[1][];
        aparams[0] = params;
    
        IndexRecord rec = new IndexRecord(d, selections, aparams);
        rec.setDataLocationURL(theURL);
        addRecord(rec);
    }
}
