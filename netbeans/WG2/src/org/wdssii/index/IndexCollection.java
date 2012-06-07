package org.wdssii.index;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An IndexCollection manages a collection of named Indexes for you.
 * Each index is wrapped with an IndexWatcher that is the actual listener.
 * Create a subclass to handle added records
 * 
 * You can connect/disconnect, add and remove indexes from the collection.
 * SourceManager in the GUI uses this for maintaining the list of sources in the GUI,
 * but you can see it for anything you want.
 * 
 * @author Robert Toomey
 *
 */
public abstract class IndexCollection {

    /** Log for this class */
    private static Logger log = LoggerFactory.getLogger(IndexCollection.class);

    /** Do something when a new record comes in.  You need to implement this for
     * autoupdates
     * FIXME: add more info other than record.
     */
    public abstract void handleAddedRecord(IndexRecord rec);
    /** Default number of records to keep per product */
    public static final int myDefaultHistorySize = 5000;
    /** Internal counter for each added unique index */
    private int myIndexCounter = 1;
    /** The 'selected' index in the list */
    private String mySelectIndex = null;
    /** A map from an internal key to the IndexWatcher for it */
    private ConcurrentHashMap<String, IndexWatcher> myIndexSet = new ConcurrentHashMap<String, IndexWatcher>();
    /** The short names are labels such as 'KTLX' that can be changed.  For the GUI,
     * this allows things to be renamed without breaking.
     */
    private ConcurrentHashMap<String, String> myIndexShortNames = new ConcurrentHashMap<String, String>();

    public boolean canAdd(String shortName, String path) {
        // Check that the filtered path isn't already in our list...
        // This is slow, can't do it until 'connect'
        String filterPath = pathFilter(path, ".protect.nssl");

        // FIXME: the real path and the typed one can be different...how to keep someone
        // from doing 'tensor.protect.nssl' and 'tensor' at same time?
        if (havePath(filterPath)) {
            return false;
        }
        return true;
    }

    /** Add a new index to the collection
     * @param shortName  A convenient short name to reference by, such as 'KTLX'
     * @param path		 The path to the index ..."webindex://...."
     * @param realtime 	 Is this realtime or not?  FIXME: Do we need this?
     * @return index key on successful add, or null
     */
    public String add(String shortName, String path, boolean realtime) {

        String success = null;

        // Check that the filtered path isn't already in our list...
        // This is slow, can't do it until 'connect'
        String filterPath = pathFilter(path, ".protect.nssl");

        // Crap..the real path and the typed one can be different...how to keep someone
        // from doing 'tensor.protect.nssl' and 'tensor' at same time?
        if (havePath(filterPath)) {
            return success;
        }

        // Create a new internal name, which is unique for every added index.
        String keyName = "i" + myIndexCounter++;

        IndexWatcher current = myIndexSet.get(keyName);
        if (current == null) {

            // Add IndexInformation wrapper for this index
            //current = new IndexWatcher(this, keyName, webindexPathHelper(path, ".protect.nssl"),
            //		realtime);
            current = new IndexWatcher(this, keyName, filterPath, realtime);
            myIndexSet.put(keyName, current);
            myIndexShortNames.put(keyName, shortName); // Notice, duplicate short names allowed
            log.warn("ADDED " + keyName + " " + shortName + " " + filterPath);
            success = keyName;

        } else {
        }
        return success;
    }

    /** Disconnect source */
    public void disconnect(String keyName) {
        IndexWatcher current = myIndexSet.get(keyName);
        if (current != null) {
            current.disconnect();
        }
    }

    /** Called before background job */
    public boolean aboutToConnect(String keyName, boolean start) {
        boolean success = false;
        IndexWatcher current = myIndexSet.get(keyName);
        if (current != null) {
            success = current.aboutToConnect(start);
        }
        return success;
    }

    /** Called in a background job */
    public boolean connect(String keyName) {
        boolean success = false;
        IndexWatcher current = myIndexSet.get(keyName);
        if (current != null) {
            success = current.connect();
        }
        return success;
    }

    /** Filter path and clean it up.  Don't do any operation here that hangs, such
     * as nslookup... that should be done inside the connection which is done as a job
     * @param path
     * @param domain
     * @return
     */
    public String pathFilter(String path, String domain) {
        String outPath = path;
        //Pattern p = Pattern.compile("^webindex:http://([^:/]*):?([0-9]*)(/.*)");
        Pattern p = Pattern.compile("^http://([^:/]*):?([0-9]*)(/.*)");
        Matcher m = p.matcher(path);
        if (m.find()) {
            String host = m.group(1);
            if (host.indexOf(".") == -1) {
                host = host + domain;
                //outPath = "webindex:http://" + host;
                outPath = "http://" + host;
                if (m.group(2).length() > 0) {
                    outPath += ":" + m.group(2);
                }
                outPath += m.group(3);
            }

            // Ok if no '.' in the host name, assume it's not a fully qualified prefix and
            // add ours...
            // FIXME: will need more advanced domain name stuff since other people will have
            // sources that aren't internal to NSSL (in other words multiple domains possible,
            // so which one to use?)  We can't use nslookup here it will hang.

            // InetAddress ina;
			/*
            try {
            // See if the host in the webindex is found...
            ina = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
            // If not found, try adding protect.nssl to the name
            try {
            host = host + domain;
            ina = InetAddress.getByName(host);
            // Success. Replace the host in the path with the full path
            outPath = "webindex:http://" + host;
            if (m.group(2).length() > 0) {
            outPath += ":" + m.group(2);
            }
            outPath += m.group(3);
            } catch (UnknownHostException e2) {
            log.warn("Can't find machine URL named " + m.group(1));
            }
            }*/

        }
        return outPath;
    }

    /** Return true if path exists in our index collection */
    public boolean havePath(String path) {
        // Find in collection this path, if any
        boolean found = false;

        Iterator<String> i = myIndexSet.keySet().iterator();
        while (i.hasNext()) {
            IndexWatcher current = myIndexSet.get(i.next());
            String p = current.getPath();
            if ((p != null) && (p.equals(path))) {
                found = true;
                break;
            }
        }
        return found;
    }

    /** Remove a given index key if there */
    public void removeIndexKey(String indexKey) {
        if (!indexKey.equals("iManualFiles")) {
            IndexWatcher current = myIndexSet.get(indexKey);
            if (current != null) {
                myIndexSet.remove(indexKey);
            }

            // If we have a selection, and it's the one being deleted, clear it.
            if ((mySelectIndex != null) && (mySelectIndex.equals(indexKey))) {
                mySelectIndex = null;
            }
        }
    }

    /** Get number of indexes we are holding onto */
    public int getIndexCount() {
        if (myIndexSet != null) {
            return myIndexSet.size();
        }
        return 0;
    }

    /** Does this index exist?  Uses an index key */
    public boolean indexExists(String indexKey) {
        if (myIndexSet.containsKey(indexKey)) {
            return true;
        }
        return false;
    }

    /** Get the IndexWatcher for a given index key */
    public IndexWatcher getIndexWatcher(String indexKey) {
        IndexWatcher current = null;
        if (myIndexSet.containsKey(indexKey)) {
            current = myIndexSet.get(indexKey);
        }
        return current;
    }

    /** A COPY of our sorted list of the index information by nice name. */
    public ArrayList<IndexWatcher> getIndexList() {
        ArrayList<IndexWatcher> theIndexes = new ArrayList<IndexWatcher>();

        Iterator<String> i = myIndexSet.keySet().iterator();
        while (i.hasNext()) {
            IndexWatcher current = myIndexSet.get(i.next());
            theIndexes.add(current);
        }

        // Sort by the nice name
        Collections.sort(theIndexes, new Comparator<IndexWatcher>() {

            @Override
            public int compare(IndexWatcher arg0, IndexWatcher arg1) {
                String nice0 = getNiceShortName(arg0.myName);
                String nice1 = getNiceShortName(arg1.myName);
                return (nice0.compareTo(nice1));
            }
        });
        return theIndexes;
    }

    /*
     * getIndexByName Get the Index for a given index key, if any
     * 
     */
    public HistoricalIndex getIndexByNameI(String name) {
        HistoricalIndex anIndex = null;
        if (name != null) {
            if (myIndexSet.containsKey(name)) {
                IndexWatcher current = myIndexSet.get(name);
                anIndex = current.getIndex();
            }
        }
        return anIndex;
    }

    /** Get the name for a given index, if any */
    public String getIndexName(HistoricalIndex findMe) {
        for (String o : myIndexSet.keySet()) {
            IndexWatcher current = myIndexSet.get(o);
            HistoricalIndex currentIndex = current.getIndex();
            if (currentIndex != null) {
                if (currentIndex.equals(findMe)) {
                    return o;
                }
            }
        }
        return "Failed";
    }

    /** Get the current short name displayed in all gui items (user changable), Nice means return
     * a string always so we don't have to check for null in the code everywhere */
    public String getNiceShortName(String keyName) {
        String shortName = myIndexShortNames.get(keyName);
        if (shortName == null) {
            if (keyName != null) {
                shortName = "?" + keyName;
            } else {
                shortName = "?";
            }
        }
        return (shortName);
    }

    /** Select given index key */
    public void selectIndexKey(String sourceName) {
        mySelectIndex = sourceName;
    }

    /** Get the current selected key */
    public String getSelectedIndexKey() {
        return mySelectIndex;
    }

    /** This checks the host in realtime files and then adds a domain suffix
     * if the host isn't found.  Some machines aren't set up to add a suffix properly.
     * I should probably modify bookmark files to have the full qualified path
     * 
     * This can be slow...should be done in worker thread...
     * @param path
     * @return the fully qualified path
     */
    public String webindexPathHelper(String path, String domain) {
        String outPath = path;
        //Pattern p = Pattern.compile("^webindex:http://([^:/]*):?([0-9]*)(/.*)");
        Pattern p = Pattern.compile("^http://([^:/]*):?([0-9]*)(/.*)");
        Matcher m = p.matcher(path);
        if (m.find()) {
            String host = m.group(1);
            // InetAddress ina;
            try {
                // See if the host in the webindex is found...
				/* ina = */ InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                // If not found, try adding protect.nssl to the name
                try {
                    host = host + domain;
                    /* ina = */ InetAddress.getByName(host);
                    // Success. Replace the host in the path with the full path
                    //outPath = "webindex:http://" + host;
                    outPath = "http://" + host;
                    if (m.group(2).length() > 0) {
                        outPath += ":" + m.group(2);
                    }
                    outPath += m.group(3);
                } catch (UnknownHostException e2) {
                    log.warn("Can't find machine URL named " + m.group(1));
                }
            }

        }
        return outPath;
    }
}
