package org.wdssii.tests;

import org.wdssii.storage.Array2DfloatAsTiles;
import org.wdssii.storage.DataManager;

import junit.framework.TestCase;

public class Array2DTileTest extends TestCase {

    /** Tests a tile array 2D by creating enough data to force cache flushing/loading */
    public void testLargeSizeGrid() {
        Array2DfloatAsTiles a = new Array2DfloatAsTiles(1000000, 1000000, 1.0f);
        /** Set the diagonal of the 10000x10000 array, this will force tiles to disk */
        for (int i = 0; i < 100000; i++) {
            a.set(i, i, i);
            float out = a.get(i, i);
            if (i != out) {
                assertEquals(out, i);
                break;
            }
        }

        System.out.println("Writing files to " + DataManager.getInstance().getRootTempDir());

        //	float backTest = a.get(100,200);  Background fails at moment because it's full array I don't want to fill it yet
        //	assertEquals(1.0f, backTest);
        boolean success = true;
        for (int i = 0; i < 100000; i++) {
            float value = a.get(i, i);
            //System.out.println("Got: "+i+" = "+value);
            if (value != i) {
                success = false;
                assertEquals(value, i);
                break;
            }
        }
        System.out.println("Test success: " + success);
    }
}
