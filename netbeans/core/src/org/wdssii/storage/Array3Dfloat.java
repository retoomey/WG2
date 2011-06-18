package org.wdssii.storage;

/** Array3Dfloat hides the internals of storage of a 3D float array.
 * This way we can store it sparse, full, off to disk, etc...
 * 
 * FIXME: add iterators so that sparse data can be accessed
 * without scanning an entire grid...
 * 
 * 
 * @author Robert Toomey
 */
public interface Array3Dfloat {
 /** Get a value from given x and y */
    float get(int x, int y, int z);

    /** Set a value given an x and y */
    void set(int x, int y, int z, float value);

    /** Get the 'x' dimension of the array */
    int getX();

    /** Get the 'y' dimension of the array */
    int getY();
    
    /** Get the 'z' dimension of the array */
    int getZ();

    /** Return the full size of the array */
    int size();    
}
