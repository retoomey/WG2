package org.wdssii.storage;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The data manager will handle:
 * Loading/Offloading data from disk to ram...
 * Keep track of total data size...
 * Other things as developed...
 * 
 * This works at a raw data level, not DataType or Products or anything, at least for now
 * the purpose is to allow access to massive numbers of floats.
 * 
 * @author Robert Toomey
 * 
 */
public class DataManager {

    /** The subdirectory we use to offload any data nodes from RAM */
    public final static String tempNodes = "datanodes";
    private static DataManager instance = null;
    private static Log log = LogFactory.getLog(DataManager.class);
    private String myDiskLocation;
    private File myTempDir = null;
    private int myPurgeCount = 0;
    /** Number of nodes we try to hold in RAM (RAM cache size)
     * Note: This size is true for 2D data tiles, not for the GL rendered nodes (not yet at least)
     */
    private int myRAMCacheNodeMaxCount = 500;
    private final int mySizePerNode = 1000000;  // Size in floats
    // LRU reference cache.  These two structures work together, so the synchronize lock is for both
    private Object myCacheLock = new Object();
    TreeMap<String, DataNode> myTileRAMCache = new TreeMap<String, DataNode>();
    ArrayList<DataNode> myLRUStack = new ArrayList<DataNode>();  //0, 1, 2, ... LRU product (at end of list)

    private DataManager() {
        // Exists only to defeat instantiation.
        // FIXME: make GUI able to change this....
        myDiskLocation = System.getProperty("java.io.tmpdir");
        try {
            myTempDir = createTempDir();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("OS temporary directory is: " + myDiskLocation);
        log.info("Using root temp directory:" + myTempDir.getAbsolutePath());
        System.setProperty("java.io.tmpdir", myTempDir.getAbsolutePath());
        log.info("DataManager temp is " + myTempDir.getAbsolutePath());
        // We create a 'datacache' array...

    }

    public String getTempDirName(String subname) {
        File dir = getTempDir(subname);
        return dir.getAbsolutePath();
    }

    public File getTempDir(String subname) {
        String path = getRootTempDir();
        File temporaryDir = new File(path, subname);
        if (!temporaryDir.exists()) {
            log.info("Creating temp directory " + temporaryDir.getAbsolutePath());
            temporaryDir.delete();
            temporaryDir.mkdir();
            temporaryDir.deleteOnExit();
        }
        return temporaryDir;
    }

    public String getRootTempDir() {
        return myTempDir.getAbsolutePath();
    }

    /** FIXME: I'm going to use this dir as 'root' for the entire display,
     * even tricking others into using it
     * @return
     * @throws IOException
     */
    public static File createTempDir() throws IOException {
        final File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
        File newTempDir;
        final int maxAttempts = 9;
        int attemptCount = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss"); // Note: hour is UTC time
        java.util.Date date = new java.util.Date();

        do {
            attemptCount++;
            if (attemptCount > maxAttempts) {
                throw new IOException(
                        "The highly improbable has occurred! Failed to "
                        + "create a unique temporary directory after "
                        + maxAttempts + " attempts.");
            }
            // Use date as part of temp directory for debugging.
            String extra = (attemptCount == 1) ? "" : Integer.toString(attemptCount);
            String dirName = "WG2-" + dateFormat.format(date) + extra;
            log.info("Trying to create directory " + dirName);
            // The name of the 'root' directory just as a random number
            //String dirName = "WG2-"+UUID.randomUUID().toString();
            newTempDir = new File(sysTempDir, dirName);
        } while (newTempDir.exists());

        if (newTempDir.mkdirs()) {
            log.info("Created temp dir of name " + newTempDir.getAbsolutePath());
            return newTempDir;
        }

        throw new IOException(
                "Failed to create temp dir named "
                + newTempDir.getAbsolutePath());
    }

    /** Get the recommended size in floats of a tile. A tile is allowed to fudge this size
     * somewhat, but you should try to stick to it.
     * This is not a 'dimension' but raw memory, since
     * we are used for different dimensional data structures.
     * FIXME: be able to set in GUI.  This would force a purge of all current tiles,
     * including any disk storage, which would in turn require purging of all products, etc.
     * Would be a big deal, so changing this other than startup probably not a good idea.
     * @return tile length
     */
    public int getRecommendedNodeSize() {
        return mySizePerNode;
    }

    public int getMaxMemoryInBytes() {
        return (getRecommendedNodeSize() * 4) * myRAMCacheNodeMaxCount;
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
            int RAMsizeBytes = instance.getMaxMemoryInBytes();
            float inGB = (RAMsizeBytes / 1024.0f / 1024.0f / 1024.0f);
            log.info("DataManager initialized, max RAM allowed is currently " + inGB + " GB");
        }
        return instance;
    }

    /** Get a tile from the DataManager
     * @param key the 'key' of tile...
     * @return null or the found tile
     */
    public DataNode getTile(String key, int firstSize, float background) {

        DataNode theTile = null;
        if (key != null) {

            synchronized (myCacheLock) { // Wait until the get is safe...
                theTile = myTileRAMCache.get(key);  // Safe to get
            }
            // Product not in cache, create it and add it to cache
            if (theTile == null) {

                // We can allow other threads to do stuff while we create a new tile
                // Create a new data tile and get it into ram.
                theTile = new DataNode(key, firstSize, background);  // FIXME: does the tile need the key?
                boolean success = theTile.loadNodeIntoRAM();
                if (!success) {
                    log.error("Failed to load tile " + key);
                    // FIXME: try to get more memory?
                }
                // First, check cache size and trim to maxsize -1 before adding new product
                // Problem with this is if cache size can change on the fly we need to trim
                // to the new lower size actually.  This only works with new cache == old
                trimCache(myRAMCacheNodeMaxCount - 1);
                synchronized (myCacheLock) {
                    myTileRAMCache.put(key, theTile);
                    myLRUStack.add(theTile);
                }
                // Tile already found in cache.  Raise it in the LRU to top
            } else {
                //System.out.println("Product is IN cache: "+productCacheKey);
                // Move item to top of LRU cache...
                //log.info("Tile restore "+key);
                synchronized (myCacheLock) {
                    myLRUStack.remove(theTile);  // Move product from inside stack to 'top'
                    myLRUStack.add(theTile);
                }
            }

        }
        return (theTile);
    }

    /** Trim cache down to the MIN_CACHE_SIZE */
    private void trimCache(int toSize) {

        // Don't trim less than zero
        if (toSize < 0) {
            toSize = 0;
        }
        try {
            synchronized (myCacheLock) {  // We'll lock the entire purge cycle.  Might be able
                // to lock a single tile purge...
                this.myPurgeCount++;
                //log.info("START PURGE-----------------------"+myPurgeCount);
                while (true) {
                    // Drop oldest from stack until we've got space...
                    //log.info("size "+myLRUStack.size() + " ("+toSize+")");			
                    if (myLRUStack.size() > toSize) {
                        //DataNode oldest = myLRUStack.get(0); // Oldest
                        DataNode oldest = myLRUStack.remove(0);
                        if (oldest != null) {
                            //		log.info("PURGE: "+oldest.key());
                            oldest.purgeNodeFromRAM();
                            myTileRAMCache.remove(oldest.key());
                        }
                        //myLRUStack.remove(0);
                    } else {
                        break;
                    }
                }
                //log.info("END PURGE--------------------------"+myPurgeCount);
            }
        } catch (Exception e) {
            log.error("Exception purging cache element " + e.toString());
            e.printStackTrace();
        }
    }

    public void dataCreated(DataStorage storage, int memoryGuess) {
        //myCurrentData.put(storage, memoryGuess);
    }

    public void printData() {

        //Iterator<Entry<DataStorage, Integer>> i = myCurrentData.entrySet().iterator();
        //	while(i.hasNext()){
        //	Entry<DataStorage, Integer> item = i.next();
        //	log.info("DATA OBJECT "+item.getKey()+" size: "+item.getValue());
        //}
        synchronized (myCacheLock) {
            log.info("cache size is " + myLRUStack.size());
            Iterator<DataNode> i = myLRUStack.iterator();
            while (i.hasNext()) {
                DataNode t = i.next();
                log.info("Tile key:" + t.key());
            }
        }
        //log.info(arg0)
    }
}
