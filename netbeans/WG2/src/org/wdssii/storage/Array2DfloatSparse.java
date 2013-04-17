package org.wdssii.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* ALPHA code....needs some work.
 * A 2D sparse float array in RAM
 * 
 * This is my first attempt at reducing the massive memory usage of the 
 * java display, in particular CONUS.
 * First attempt at a decent speed sparse array, each dimension
 * requires log(n) lookup...or O(2logN) in this case.
 * FIXME: Clean up, reduce node size to save more memory..etc...
 * FIXME: shouldn't have netcdf in here...
 * 
 * @author Robert Toomey
 */
public class Array2DfloatSparse extends DataStorage implements Array2D<Float> {

    private final static Logger LOG = LoggerFactory.getLogger(Array2DfloatSparse.class);
    private static int counter = 0;
    private int myX;
    private int myY;
    private float myBackground;

    @Override
    public void beginRowOrdered() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void endRowOrdered() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Each 'node' is sorted in its dimension by the 'myIndex' value,
     * this makes lookup for each dimension a binary search
     * @author Robert Toomey
     * FIXME: make separate types for data vs dimension stuff to reduce memory footprint
     *
     */
    private static class ArrayNode {

        public int myIndex;  						// Index in this dimension, such as 'x'
        // These two are really a 'union' FIXME: make separate classes.
        public ArrayList<ArrayNode> myNextDimension;// Next dimension, such as 'y'
        public float value;
    }
    private ArrayList<ArrayNode> myXData;

    /** A sparse array
     */
    public Array2DfloatSparse(int x, int y, float backgroundValue,
            ucar.ma2.Array data, // Array of data values
            ucar.ma2.Array xvalues,
            ucar.ma2.Array yvalues,
            ucar.ma2.Array counts,
            ucar.ma2.Index index,
            int dataCount // number of data values
            ) {
        myX = x;
        myY = y;
        myBackground = backgroundValue;

        // First dimension....
        myXData = new ArrayList<ArrayNode>();
        ArrayNode dummyX = new ArrayNode();
        ArrayNode dummyY = new ArrayNode();

        int actualData = 0;
        for (int i = 0; i < dataCount; ++i) {
            index.set(i);
            // The data value
            float value = data.getFloat(index);
            // The (x,y) where it's at
            short xat = xvalues.getShort(index);
            short yat = yvalues.getShort(index);

            int count = (counts == null) ? 1 : counts.getInt(index);
            for (int j = 0; j < count; ++j, ++yat) {
                if (yat == y) {
                    ++xat;
                    yat = 0;
                }

                dummyX.myIndex = xat;
                dummyY.myIndex = yat;
                //ArrayNode found = 
                getXNode(dummyX, dummyY, value);

                // See if 'x' exists yet, it will have the column
                //set(xat, yat, value);


                //	values[x][y] = value;
                actualData++;
            }
        }
        LOG.warn("Actual data count was " + actualData);
        // Verify the X creation...
        LOG.warn("Created Sparse X of total size " + myXData.size());

        DataManager.getInstance().dataCreated(this, actualData);

        for (int i = 0; i < myXData.size(); i++) {
            //ArrayNode n = myXData.get(i);
            //LOG.warn("VALUE OF X: "+n.myIndex);
        }
    }

    private ArrayNode getXNode(ArrayNode x, ArrayNode y, float value) {
        ArrayNode found = null;
        ArrayNode foundY = null;

        // Binary search myXData for index...
        // first 'dimension' of data
        int index = Collections.binarySearch(myXData, x, new Comparator<ArrayNode>() {

            @Override
            public int compare(ArrayNode arg1, ArrayNode arg2) {
                int i1 = arg1.myIndex;
                int i2 = arg2.myIndex;
                if (i1 < i2) {
                    return -1;
                }
                if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        });
        //int radialIndex = (index < 0) ? -(index + 1) : index;
        if (index < 0) {
            if (counter < 20) {
                LOG.warn("Creating a node for " + x.myIndex);
                counter++;
            }
            // Not found, so create it...
            index = -(index + 1);
            found = new ArrayNode();  // sloppy
            found.myIndex = x.myIndex;
            found.myNextDimension = new ArrayList<ArrayNode>();
            myXData.add(index, found);  // auto sorts
        } else {
            found = myXData.get(index);
        }

        // Ok...now we have a 'found' node, we hunt in the 2nd dimension... (y) in this case
        // Binary search myXData for index...
        // first 'dimension' of data
        int indexY = Collections.binarySearch(found.myNextDimension, y, new Comparator<ArrayNode>() {

            @Override
            public int compare(ArrayNode arg1, ArrayNode arg2) {
                int i1 = arg1.myIndex;
                int i2 = arg2.myIndex;
                if (i1 < i2) {
                    return -1;
                }
                if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        });

        if (indexY < 0) {
            // Not found, so create it...
            indexY = -(indexY + 1);
            foundY = new ArrayNode();  // sloppy
            foundY.myIndex = y.myIndex;

            // 'terminal' mode we set value, not dimension...
            //found.myNextDimension = new ArrayList<ArrayNode>();
            foundY.value = value;

            found.myNextDimension.add(indexY, foundY);  // auto sorts
        } else {
            foundY = found.myNextDimension.get(indexY);
        }

        return foundY;
    }

    private ArrayNode searchNode(int xv, int yv) {
        // SLOWWWWWW....we need our own binarySearch function that doesn't require a node to search nodes....
        ArrayNode x = new ArrayNode();
        x.myIndex = xv;
        ArrayNode y = new ArrayNode();
        y.myIndex = yv;

        ArrayNode found = null;
        ArrayNode foundY = null;

        // Binary search myXData for index...
        // first 'dimension' of data
        int index = Collections.binarySearch(myXData, x, new Comparator<ArrayNode>() {

            @Override
            public int compare(ArrayNode arg1, ArrayNode arg2) {
                int i1 = arg1.myIndex;
                int i2 = arg2.myIndex;
                if (i1 < i2) {
                    return -1;
                }
                if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        });
        //int radialIndex = (index < 0) ? -(index + 1) : index;
        if (index < 0) {

            // Not found, so return background or not found, whatever....
            return null;
        }
        found = myXData.get(index);

        // Ok...now we have a 'found' node, we hunt in the 2nd dimension... (y) in this case
        // Binary search myXData for index...
        // first 'dimension' of data
        int indexY = Collections.binarySearch(found.myNextDimension, y, new Comparator<ArrayNode>() {

            @Override
            public int compare(ArrayNode arg1, ArrayNode arg2) {
                int i1 = arg1.myIndex;
                int i2 = arg2.myIndex;
                if (i1 < i2) {
                    return -1;
                }
                if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        });

        if (indexY < 0) {
            // Not found, so create it...
            return null;
        }
        foundY = found.myNextDimension.get(indexY);

        return foundY;
    }

    @Override
    public Float get(int x, int y) {

        // Need a fairly fast lookup...giving up speed for ram.
        ArrayNode n = searchNode(x, y);
        if (n != null) {
            return n.value;
        }
        return myBackground;
    }

    @Override
    public void set(int x, int y, Float value) {
        // FIXME: don't actually do anything yet....
        LOG.error("Can't set value in sparse array (read only implementation)");
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
    public Array1D<Float> getCol(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array1D<Float> getRow(int i) {
        // TODO Auto-generated method stub
        return null;
    }
}
