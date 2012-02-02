package org.wdssii.storage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.LRUCache;

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
    private static Logger log = LoggerFactory.getLogger(DataManager.class);
    private String myDiskLocation;
    private File myTempDir = null;
    
    /** Number of nodes we try to hold in RAM (RAM cache size)  The LRUCache
     will hold this many objects */
    private int myRAMCacheNodeMaxCount = 50;
    private final int mySizePerNode = 1000000;  // Size in floats
   
     /** The cache for DataNode objects */
    LRUCache<DataNode> myCache = new LRUCache<DataNode>();
    
    /** Number of bytes allocated by program */
    private long myAllocatedBytes = 0;
    
    /** Number of bytes deallocated by program */
    private long myDeallocatedBytes = 0;
    
    /** Number of bytes failed to allocate by program */
    private long myFailedAllocatedBytes = 0;
    
    private DataManager() {
        // Exists only to defeat instantiation.
        // FIXME: make GUI able to change this....
        myDiskLocation = System.getProperty("java.io.tmpdir");
        try {
            myTempDir = createTempDir();
            myCache.setMinCacheSize(50);
            myCache.setMaxCacheSize(200);
        } catch (IOException e){
        }

        log.info("OS temporary directory is: " + myDiskLocation);
        log.info("Using root temp directory:" + myTempDir.getAbsolutePath());
        System.setProperty("java.io.tmpdir", myTempDir.getAbsolutePath());
        log.info("DataManager temp is " + myTempDir.getAbsolutePath());
        // We create a 'datacache' array...

    }

    /** Using this function for all creation of ByteBuffers will allow
     * us to track the memory usage better...caller should call
     * deallocate below when the ByteBuffer is set to null
     * 
     * @return new ByteBuffer or null
     */
    public ByteBuffer allocate(int aSize, String who){
        ByteBuffer bb = ByteBuffer.allocateDirect(aSize);
        if (bb !=null){
            myAllocatedBytes += aSize;
        }else{
            myFailedAllocatedBytes += aSize;
        }
        return bb;
    }
    
    /** Anyone calling allocate above should call this to let us know
     * it's been nulled.  Doesn't mean JVM or native library has Garbage
     * collected it though...just counting for debugging purposes.
     * 
     * @param aSize
     * @param who 
     */
    public void deallocate(int aSize, String who){
        myAllocatedBytes -= aSize;
        myDeallocatedBytes += aSize;
    }
    
    public long getAllocatedBytes(){
        return myAllocatedBytes;
    }
    
    public long getDeallocatedBytes(){
        return myDeallocatedBytes;
    }
    
    public long getFailedAllocatedBytes(){
        return myFailedAllocatedBytes;
    }
    
    public int getNumberOfCachedItems(){
        return myCache.getCacheFilledSize();
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

            theTile = myCache.get(key);
            
            // Tile not in cache, create it and add it to cache
            if (theTile == null){
                
                theTile = new DataNode(key, firstSize, background);
                boolean success = ((theTile != null) && (theTile.loadNodeIntoRAM()));
                if (success){
                   // theTile.setCacheKey() constructor
                    
                    myCache.put(key, theTile);
                    // CommandManager.getInstance().cacheManagerNotify();
                }else{
                    log.error("Wasn't able to create/load a tile");
                }          
                // Tile already found in cache
            } 
        }
        return (theTile);
    }

    /** Trim cache down to the MIN_CACHE_SIZE */
    /*
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
        }
    }*/

    public void dataCreated(DataStorage storage, int memoryGuess) {
        //myCurrentData.put(storage, memoryGuess);
    }

    /*
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
     * 
     */
}
