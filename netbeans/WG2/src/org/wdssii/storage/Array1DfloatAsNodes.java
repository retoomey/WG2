package org.wdssii.storage;

import java.nio.FloatBuffer;

import org.wdssii.storage.DataNode;

/** Stores 1D arrays as nodes for the DataManager.
 * 
 * Currently stores entire array into a 'single' node.  It will have to
 * fit entirely into RAM.
 * We will have to break up a large array into multiple nodes.
 * 
 * The GUI uses this to store a GL draw data...
 * FIXME: need a count of total nodes function for looping in opengl...
 * 
 * @author Robert Toomey
 *
 */
public class Array1DfloatAsNodes extends DataStorage implements Array1Dfloat {

    private int mySize;
    private float myBackground;
    /** The 'root' for all nodes for this data structure */
    private final String myTileRoot;
    /** Every time we create one, we give it a unique number */
    private static int counter = 1;

    // Initializer block...Shared by all constructors, called before constructors
    {
        // Unique id for Array
        counter++;
        myTileRoot = "Array1D" + counter;
    }

    public Array1DfloatAsNodes(int aSize, float backgroundValue) {
        mySize = aSize;
        myBackground = backgroundValue;

        // Node size based off DataManager node size.
        // Currently we just stick it all in a single node. 
        // FIXME: should probably 'break' up our data into multiple nodes
        // So if our size = 10*recommended we make 10 nodes...
        //int tileSize = DataManager.getInstance().getRecommendedNodeSize();
        // That's it.  Nodes will be created on demand as needed during set/get...
    }

    /** Return the raw float buffer for this array.  Currently only one.  Used
     * by GUI to get render buffer for GL data.  If in a different thread then
     * DataManager you need to call synchronize(getBufferLock()){
     * around your access of the FloatBuffer to keep DataManager from 
     * swapping/deleting stuff out from under you
     *
     */
    @Override
    public FloatBuffer getRawBuffer() {
        final String key = myTileRoot + "s" + mySize;
        DataNode tile = DataManager.getInstance().getTile(key, mySize, myBackground);
        return tile.getRawBuffer();
    }

    @Override
    public float get(int x) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with set "inline"
        final String key = myTileRoot + "s" + mySize;
        DataNode tile = DataManager.getInstance().getTile(key, mySize, myBackground);
        return tile.get(x);
    }

    @Override
    public void set(int x, float value) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with get "inline"
        final String key = myTileRoot + "s" + mySize;
        DataNode tile = DataManager.getInstance().getTile(key, mySize, myBackground);
        tile.set(x, value);
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public Object getBufferLock() {
        final String key = myTileRoot + "s" + mySize;
        DataNode tile = DataManager.getInstance().getTile(key, mySize, myBackground);
        return tile.getReadLock();
    }
}
