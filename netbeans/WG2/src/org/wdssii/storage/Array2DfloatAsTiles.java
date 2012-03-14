package org.wdssii.storage;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *   Breaks a 2D array down into a grid where each DataManager node is a square
 * with sides ~= sqrt(nodesize). This works well for data such as LatLonGrids
 * since the user is usually over a subsection of the LatLonGrid (think graph
 * paper). Well, ok..in the **GUI** they are.
 *
 *   For doing an algorithm, it might be more efficient to store nodes as 'rows'
 * of data instead of squares, since less node swapping will occur. (You're
 * likely to iterate over the grid this way..) In other words, store the data in
 * the way you're most likely to access it to maximize cache hits.
 *
 *   @author Robert Toomey
 *
 */
public class Array2DfloatAsTiles extends DataStorage implements Array2Dfloat {

    private static Logger log = LoggerFactory.getLogger(Array2DfloatAsTiles.class);
    private int myX;
    private int myY;
    private float myBackground;
    /**
     *  The 'root' for all tiles for this data structure
     */
    private final String myTileRoot;
    /**
     *  The 'side' of a 2D tile, which we will set to sqrt of the DataManager
     * tile size
     */
    private final int mySide;
    /**
     *  The 'side' squared
     */
    private final int mySideSquared;
    /**
     *  Every time we create one, we give it a unique number
     */
    private static int counter = 0;
    private ArrayList<DataNode> myWorkingTiles = null;
    private int myCurrentOrderedRow = 0;
    private static final Object myCounterSync = new Object();
    
    // Initializer block...Shared by all constructors, called before constructors
    {
        // Unique id for Array
        synchronized(myCounterSync){
            counter++;
            myTileRoot = "Array2D" + counter;
        }
       
        // Tile size based off DataManager node size
        int tileSize = DataManager.getInstance().getRecommendedNodeSize();
        mySide = (int) Math.floor(Math.sqrt(tileSize));
        mySideSquared = mySide * mySide;
    }

    @Override
    public void beginRowOrdered() {

        // Row order should be looping like this:
        // for (y=0...y++)
        // for (x=0...x++)
        // Note it will still work if you violate order, but it's optimized
        // to do a single row of tiles at a time...
        myCurrentOrderedRow = 0;
        myWorkingTiles = preloadTilesForRow(myCurrentOrderedRow);

    }

    private ArrayList<DataNode> preloadTilesForRow(int tileRow) {
        // So we preload the tiles for a single row...when y increases, we
        // buffer the next row of tiles and put back the old ones...
        int tilesPerRow = (myX / mySide) + 1;  // +1 so 375/100 ==> 3 +1 = 4 tiles
        ArrayList<DataNode> theTiles = new ArrayList<DataNode>();
        for (int x = 0; x < tilesPerRow; x++) {
            final String key = myTileRoot + "x" + x + "y" + tileRow;
            DataNode tile = DataManager.getInstance().popTile(key, mySideSquared, myBackground);
            theTiles.add(tile);
        }
        return theTiles;
    }

    private void unloadTilesForRow() {
        if (myWorkingTiles != null) {
            Iterator<DataNode> i = myWorkingTiles.iterator();
            while (i.hasNext()) {
                DataNode tile = i.next();
                DataManager.getInstance().pushTile(tile.getCacheKey(), tile);
            }
            myWorkingTiles = null;
        }
    }

    @Override
    public void endRowOrdered() {
        unloadTilesForRow();
    }

    /**
     *  Not to be implemented directly, this class wraps a column in the 2D float
     * array. This acts like a general class right now, but could be optimized
     * later so I'm keeping it internal.
     */
    private static class Array1DfloatTileCol implements Array1Dfloat {

        private Array2DfloatAsTiles my2DArray;
        private int myColumn;

        public Array1DfloatTileCol(Array2DfloatAsTiles data, int col) {
            my2DArray = data;
            myColumn = col;
        }

        @Override
        public float get(int x) {
            return my2DArray.get(myColumn, x);
        }

        @Override
        public void set(int x, float value) {
            my2DArray.set(myColumn, x, value);
        }

        @Override
        public int size() {
            return my2DArray.getY();
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

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }
    }

    /**
     *  Not to be implemented directly, this class wraps a column in the 2D float
     * array
     */
    private static class Array1DfloatTileRow implements Array1Dfloat {

        private Array2DfloatAsTiles my2DArray;
        private int myRow;

        public Array1DfloatTileRow(Array2DfloatAsTiles data, int row) {
            my2DArray = data;
            myRow = row;
        }

        @Override
        public float get(int x) {
            return my2DArray.get(x, myRow);
        }

        @Override
        public void set(int x, float value) {
            my2DArray.set(x, myRow, value);
        }

        @Override
        public int size() {
            return my2DArray.getX();
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

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }
    }

    public Array2DfloatAsTiles(int x, int y, float backgroundValue) {
        myX = x;
        myY = y;
        myBackground = backgroundValue;
        // That's it.  Tiles will be created on demand as needed during set/get...
    }

    @Override
    public float get(int x, int y) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with set "inline"
        final int tileX = x / mySide;
        final int tileY = y / mySide;
        final String key = myTileRoot + "x" + tileX + "y" + tileY;
        DataNode tile = DataManager.getInstance().getTile(key, mySideSquared, myBackground);
        final int localX = x - (mySide * tileX);
        final int localY = y - (mySide * tileY);
        final int at = (localY * mySide) + localX;  // 'x' order
        return tile.get(at);
    }

    @Override
    public void set(int x, int y, float value) {

        int tileX = x / mySide;
        int tileY = y / mySide;
        if (myWorkingTiles != null) {

            // Cache a row of tiles...
            if (tileY != this.myCurrentOrderedRow) {
                unloadTilesForRow();
                myCurrentOrderedRow = tileY;
                myWorkingTiles = this.preloadTilesForRow(tileY);
            }

            //String key = myTileRoot + "x" + tileX + "y" + tileY;

            // Attempt to get tile from preloaded ones....
            DataNode tile = myWorkingTiles.get(tileX);

            // Debug..compare key to actual tile...
            //if (!(tile.getCacheKey().equals(key))) {
            //    log.error("KEY FAILURE: " + key + "!= " + tile.getCacheKey());
            //}
            int localX = x - (mySide * tileX);
            int localY = y - (mySide * tileY);
            int at = (localY * mySide) + localX;  // 'x' order
            //  log.debug("SET " + key + " " + at + " == " + value + " " + myX + ", " + myY);
            tile.set(at, value);
        } else {

            // Slower than snails mating...

            // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
            // This code duplicates with get "inline"
            // int tileX = x / mySide;
            // int tileY = y / mySide;
            String key = myTileRoot + "x" + tileX + "y" + tileY;
            DataNode tile = DataManager.getInstance().getTile(key, mySideSquared, myBackground);
            int localX = x - (mySide * tileX);
            int localY = y - (mySide * tileY);
            int at = (localY * mySide) + localX;  // 'x' order
            //log.debug("SET " + key + " " + at + " == " + value + " " + myX + ", " + myY);
            tile.set(at, value);
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
    /**
     *  We treat a constant X value as a column
     */
    public Array1Dfloat getCol(int i) {
        return new Array1DfloatTileCol(this, i);
    }

    @Override
    /**
     *  We treat a constant Y value as a row
     */
    public Array1Dfloat getRow(int i) {
        return new Array1DfloatTileRow(this, i);
    }
}
