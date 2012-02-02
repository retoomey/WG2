package org.wdssii.storage;

import java.nio.FloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full 2D float array in RAM.  Not recommended since for massive radar data
 * this will be a RAM pig, though on high RAM algorithm machines it's
 * not an issue.
 * 
 * Also implements two wrapper classes for accessing an entire row/col
 * as an Array1Dfloat
 * 
 * @author Robert Toomey
 *
 */
public class Array2DfloatRAM implements Array2Dfloat {

    private static Logger log = LoggerFactory.getLogger(Array2DfloatRAM.class);
    private int myX;
    private int myY;
    private float myBackground;
    private float[][] myArray;
    private boolean myValid = false;

    /** Not to be implemented directly, this class wraps a column in the 2D float array */
    private static class Array1DfloatRAMCOLUMN implements Array1Dfloat {

        private Array2DfloatRAM my2DArray;
        private int myColumn;

        public Array1DfloatRAMCOLUMN(Array2DfloatRAM ram, int col) {
            my2DArray = ram;
            myColumn = col;
        }

        @Override
        public float get(int x) {
            if (my2DArray.myValid) {
                return my2DArray.myArray[x][myColumn];
            }
            return 0;
        }

        @Override
        public void set(int x, float value) {
            if (my2DArray.myValid) {
                my2DArray.myArray[x][myColumn] = value;
            }
        }

        @Override
        public int size() {
            if (my2DArray.myValid) {
                return my2DArray.getY();
            }
            return 0;
        }

        @Override
        public FloatBuffer getRawBuffer() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getBufferLock() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /** Not to be implemented directly, this class wraps a column in the 2D float array */
    private static class Array1DfloatRAMROW implements Array1Dfloat {

        private Array2DfloatRAM my2DArray;
        private int myRow;

        public Array1DfloatRAMROW(Array2DfloatRAM ram, int row) {
            my2DArray = ram;
            myRow = row;
        }

        @Override
        public float get(int x) {
            if (my2DArray.myValid) {
                return my2DArray.myArray[myRow][x];
            }
            return 0;
        }

        @Override
        public void set(int x, float value) {
            if (my2DArray.myValid) {
                my2DArray.myArray[myRow][x] = value;
            }
        }

        @Override
        public int size() {
            if (my2DArray.myValid) {
                return my2DArray.getX();
            }
            return 0;
        }

        @Override
        public FloatBuffer getRawBuffer() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getBufferLock() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public Array2DfloatRAM(int x, int y, float backgroundValue) {
        myX = x;
        myY = y;
        myBackground = backgroundValue;

        try {
            myArray = new float[x][y];
            myValid = true;
        } catch (OutOfMemoryError mem) {
            log.warn("Array2D storage not enough heap space for float[" + x + "][" + y + "] array");
        }
    }

    @Override
    public float get(int x, int y) {
        if (myValid) {
            return myArray[x][y];
        }
        return myBackground;
    }

    @Override
    public void set(int x, int y, float value) {
        if (myValid) {
            myArray[x][y] = value;
        }
    }

    @Override
    public int getX() {
        return myX;
    }

    @Override
    public int getY() {
        return myY;
    }

    @Override
    public int size() {
        return myX * myY;
    }

    @Override
    public Array1Dfloat getCol(int i) {
        // Note the only memory here is the object, not the array
        return new Array1DfloatRAMCOLUMN(this, i);
    }

    @Override
    public Array1Dfloat getRow(int i) {
        // Note the only memory here is the object, not the array
        return new Array1DfloatRAMROW(this, i);
    }
}
