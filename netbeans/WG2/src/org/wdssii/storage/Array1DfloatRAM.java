package org.wdssii.storage;

import java.nio.FloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full 1D float array in RAM.  Not recommended since for massive radar data
 * this will be a RAM pig, though on high RAM algorithm machines it's
 * not an issue.
 * 
 * @author Robert Toomey
 *
 */
public class Array1DfloatRAM implements Array1Dfloat {

    private static Logger log = LoggerFactory.getLogger(Array1DfloatRAM.class);
    private int mySize;
    private float myBackground;
    private float[] myArray;
    private boolean myValid = false;

    public Array1DfloatRAM(int aSize, float backgroundValue) {
        mySize = aSize;
        myBackground = backgroundValue;

        try {
            myArray = new float[mySize];
            myValid = true;
        } catch (OutOfMemoryError mem) {
            log.warn("Array1D storage not enough heap space for float[" + mySize + "]");
        }
    }

    @Override
    public float get(int x) {
        if (myValid) {
            return myArray[x];
        }
        return myBackground;
    }

    @Override
    public void set(int x, float value) {
        if (myValid) {
            myArray[x] = value;
        }
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public FloatBuffer getRawBuffer() {
        // TODO Auto-generated method stub
        // We should really store the ram float as a FloatBuffer
        return null;
    }

    @Override
    public Object getBufferLock() {
        // TODO Auto-generated method stub
        return null;
    }
}