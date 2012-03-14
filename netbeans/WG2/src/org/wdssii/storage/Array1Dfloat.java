package org.wdssii.storage;

import java.nio.FloatBuffer;

/** Array1Dfloat hides the internals of storage of a 1D float array.
 * This way we can store it sparse, full, off to disk, etc...
 * 
 * FIXME: add iterators so that sparse data can be accessed
 * without scanning an entire grid...
 *  * 
 * @author Robert Toomey
 */
public interface Array1Dfloat {

    /** Begin a batch (>1) of set/get.  This allows array to optimize caching
     */
    void begin();
    
    /** End a batch (>1) of set/get.  This allows array to optimize caching
     */
    void end();
    
    /** Get a value from given x and y */
    float get(int x);

    /** Set a value given an x and y */
    void set(int x, float value);

    /** Return the full size of the array */
    int size();

    /** Get the lock object for using the raw buffer */
    Object getBufferLock();

    /** Get a FloatBuffer of the data if possible, or return null */
    FloatBuffer getRawBuffer();
}