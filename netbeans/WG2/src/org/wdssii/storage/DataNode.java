package org.wdssii.storage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** A data node, a 'small' amount of data, usually representing a part of a larger 2D, 3D array of float data.
 * These nodes are cached to disk/moved in and out of ram on demand in an LRU queue controlled by DataManager.
 * Currently nodes are created/filled when accessed with a set OR a get,
 * which could be improved by only creating empty tiles on a 'set' call that is not background.
 * 
 * Node instances don't correspond with the actual data.  Data may be written to disk and 
 * the node disposed and then recreated later.
 * 
 * FIXME: Add sparse ability?
 * 
 */
public class DataNode {

    private static Log log = LogFactory.getLog(DataManager.class);  // use datamanager log?
    /** The key representing this data tile.  Final */
    private final String myKey;
    /** Synchronization required for reading/writing/using */
    //private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Object myBufferLock = new Object();
    private ByteBuffer myDataByte;
    /** Background data for 'missing' data */
    private float myBackground = 0.0f;
    /** Is data currently in RAM? */
    private boolean myLoaded = false;
    private boolean myEverLoaded = false;
    /** The 'true' size of the data we store (as if not sparse) */
    private int mySize = 0;

    /** Create a data tile with given key name */
    public DataNode(String key, int firstSize, float background) {
        myKey = key;
        mySize = firstSize;
        myBackground = background;
    }

    public String key() {
        return myKey;
    }

    public void setBackground(float b) {
        myBackground = b;
    }

    /** The tile lock, allows synchronized READ access to the raw float buffer of the tile,
     * which in general is required only by openGL since it needs a vector.  Use get/set
     * routines for changing data otherwise */
    public Object getReadLock() {
        return myBufferLock;
        //return lock.readLock();
    }

    public Object getWriteLock() {
        return myBufferLock;
    }

    /** Get the raw buffer only if loaded.  GUI uses this to quickly render data
     * from a tile. 
     * You must use the getBufferLock method to synchronize around using this
     * buffer.  This is so that the data will be 'frozen' and not stolen out
     * from under you by the disk offloading thread.
     * synchronized(tile.getBufferLock()){
     *    FloatBuffer g = tile.getRawBuffer();
     *    ...do something like opengl glDrawArrays(..,..,g);
     * }
     * @return
     */
    public FloatBuffer getRawBuffer() {

        //lock.readLock().lock();
        FloatBuffer fb = null;
        if (myDataByte != null) {
            myDataByte.rewind();
            fb = myDataByte.asFloatBuffer();
        }
        return fb;

    }

    public void set(int index, float value) {

        synchronized (getWriteLock()) {
            //synchronized(myBufferLock){
            if (myLoaded && (index < mySize)) {
                try {
                    if (value == 0.0) {
                        value = -500000.0f;
                    } else if (value == myBackground) {
                        value = 0.0f;
                    }

                    myDataByte.asFloatBuffer().put(index, value);
                } catch (IndexOutOfBoundsException i) {
                    log.error("Tried to put v[" + index + "] = " + value);
                    log.error("Size is " + mySize);
                }
            } else {
                log.error("Can't set value on unloaded tile " + myKey + ", might be load on RAM");
                // FIXME: notify DataManager, try to get more RAM? 
            }
        }
    }

    /** Get only value if myInRam.  Check skipped here for speed.  Caller should
     * call load before using data... Hummmm.  Tile auto load?
     */
    public float get(int index) {

        synchronized (getReadLock()) {
            //synchronized(myBufferLock){
            if ((myLoaded) && (index < mySize)) {
                myDataByte.rewind();  // Probably not needed
                FloatBuffer fb = myDataByte.asFloatBuffer();

                float value = fb.get(index);

                // Map '0' to myBackground.  This prevents us having to
                // fill in the background for non-sparse which is slower than $%(@%@
                if (value == 0.0) {
                    value = myBackground;
                } else if (value == -500000.0f) {
                    value = 0.0f;
                }

                //return fb.get(index);
                return value;
            }
            return myBackground;
        }
    }

    /** Called by the data manager after creating us to load any old data */
    public boolean loadNodeIntoRAM() {
        synchronized (getWriteLock()) {
            boolean success = false;
            try {

                log.info("Allocation node " + this);

                myDataByte = ByteBuffer.allocateDirect(mySize * 4);
                myDataByte.order(ByteOrder.nativeOrder());

                myLoaded = true;
                myEverLoaded = true;
                success = true;

                readFromDisk();
                // Wow this is crazy slow...I _have_ to believe I'm
                // doing this wrong.  We'll map 0 to myBackground instead...
                //if (!readFromDisk()){
                // Do the initial background fill....for sparse we wouldn't need this... 
                //FloatBuffer fb = ((ByteBuffer)myDataByte.rewind()).asFloatBuffer();
                //while (fb.hasRemaining()){
                //	fb.put(myBackground);
                //}
                //myDataByte.rewind();
                //}
            } catch (OutOfMemoryError m) {
                myLoaded = false;
                log.error("Could not allocate " + mySize + " floats for Tile " + myKey);
            }
            return success;
        }
    }

    public boolean purgeNodeFromRAM() {
        log.info("purge from ram node " + this);

        boolean success = false;
        success = writeToDisk();
        synchronized (getWriteLock()) {
            myDataByte = null; // Delete from RAM
            myLoaded = false;  // Mark as unloaded
        }
        return success;
    }

    /** Offload tile to disk and purge ram usage of tile, called by DataManager before disposing tile */
    private boolean writeToDisk() {

        // We're _read_ing from the data and writing to disk....
        synchronized (getReadLock()) {
            boolean success = false;
            //log.info("Offload tile: "+myKey);
            if (myDataByte != null) {
                try {
                    // FIXME: do we need lock for the file (thread2 might be readFromDisk below)
                    String path = DataManager.getInstance().getTempDirName(DataManager.tempNodes);
                    path += "/" + key() + ".data";
                    FileOutputStream fout = new FileOutputStream(path);
                    FileChannel fc = fout.getChannel();
                    myDataByte.rewind();
                    fc.write(myDataByte);
                    fc.close();
                    success = true;
                } catch (FileNotFoundException e) {
                    log.error("Can't offload Tile to disk " + myKey + " " + e);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                log.error("offload to disk with null myDataByte? " + myEverLoaded);
            }
            return success;
        }
    }

    /** Restore data into RAM if we can */
    private boolean readFromDisk() {

        // We're reading from disk and _write_ing to the data
        synchronized (getWriteLock()) {
            boolean success = false;
            //log.info("Restore tile: "+myKey);
            try {
                String path = DataManager.getInstance().getTempDirName(DataManager.tempNodes);
                path += "/" + key() + ".data";
                FileInputStream fout = new FileInputStream(path);
                FileChannel fc = fout.getChannel();
                myDataByte.rewind();
                fc.read(myDataByte);
                myDataByte.rewind();
                fc.close();
                success = true;
            } catch (FileNotFoundException e) {
                // This is 'ok', may never have been written to disk...
                success = false;
            } catch (IOException e) {
                log.error("Disk error restoring tile " + myKey + " " + e);
            }
            return success;
        }
    }
}
