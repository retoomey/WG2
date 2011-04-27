package org.wdssii.storage;

/** Array2Dfloat hides the internals of storage of a 2D float array.
 * This way we can store it sparse, full, off to disk, etc...
 * 
 * FIXME: add iterators so that sparse data can be accessed
 * without scanning an entire grid...
 * 
 * 
 * @author Robert Toomey
 */
public interface Array2Dfloat {

    /** Get a value from given x and y */
    float get(int x, int y);

    /** Set a value given an x and y */
    void set(int x, int y, float value);

    /** Get the 'x' dimension of the array */
    int getX();

    /** Get the 'y' dimension of the array */
    int getY();

    /** Return the full size of the array */
    int size();

    /** Return an efficient 1D float access to given col of 2Dfloat,
     * this method 'may' copy, but probably shouldn't.  Return null
     * if you can't implement this */
    Array1Dfloat getCol(int i);

    /** Return an efficient 1D float access to given col of 2Dfloat,
     * this method 'may' copy, but probably shouldn't.  Return null
     * if you can't implement this */
    Array1Dfloat getRow(int i);
}
