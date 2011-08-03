package org.wdssii.gui.products;

import java.util.ArrayList;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;

/** A general tile renderer that handles tiling off one of our
 * dynamic 'Product' classes.  Based off the WorldWind classes but simplified
 * for our non-static data.
 * 
 * @author Robert Toomey
 *
 */
public class TileRenderer extends ProductRenderer {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog(TileRenderer.class);

    public TileRenderer(boolean asBackgroundJob){
        super(asBackgroundJob);
    }
    
    /** Root class of all tiles we can render */
    public static class Tile {

        private final int TILE_CREATED = 1;
        private final int TILE_CREATING = 2;
        private final int TILE_EMPTY = 0;
        private Object myTileCreateLock = new Object();
        private int tileCreated = TILE_EMPTY;

        public void addTileOrDescendants(DrawContext dc, double splitScale, Product p, ArrayList<Tile> t) {
        }

        public void generateTile(DrawContext dc, Product p) {
        }

        public void makeTheTile(DrawContext dc, Product p, WdssiiJobMonitor m) {
        }

        public void drawTile(DrawContext dc, Product p) {
        }

        public boolean isTileVisible(DrawContext dc) {
            return false;
        }

        /** Has the tile been completely created?  Is the opengl data for tile completely ready? */
        public boolean isTileCreated() {
            synchronized (myTileCreateLock) {
                return (tileCreated == TILE_CREATED);
            }
        }

        /** Is the tile empty and not being created? */
        public boolean isTileEmpty() {
            synchronized (myTileCreateLock) {
                return (tileCreated == TILE_EMPTY);
            }
        }

        /** Set tile created.  It's finished */
        public void setTileCreated() {
            synchronized (myTileCreateLock) {
                tileCreated = TILE_CREATED;
            }
        }

        /** Set tile as being created by worker thread.  It's not complete yet */
        public void setTileCreating() {
            synchronized (myTileCreateLock) {
                tileCreated = TILE_CREATING;
            }
        }

        /**
         * Does this tile need to split into four?
         * 
         * FIXME: this function needs work.  
         * @param dc
         * @param sector
         * @param splitScale
         * @return
         */
        public boolean needToSplit(DrawContext dc, Sector sector, double splitScale) {
            Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), 1.0);
            Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe(), 1.0);

            View view = dc.getView();
            double d1 = view.getEyePoint().distanceTo3(corners[0]);
            double d2 = view.getEyePoint().distanceTo3(corners[1]);
            double d3 = view.getEyePoint().distanceTo3(corners[2]);
            double d4 = view.getEyePoint().distanceTo3(corners[3]);
            double d5 = view.getEyePoint().distanceTo3(centerPoint);

            double minDistance = d1;
            if (d2 < minDistance) {
                minDistance = d2;
            }
            if (d3 < minDistance) {
                minDistance = d3;
            }
            if (d4 < minDistance) {
                minDistance = d4;
            }
            if (d5 < minDistance) {
                minDistance = d5;
            }

            double cellSize = (Math.PI * sector.getDeltaLatRadians() * dc.getGlobe().getRadius()) / 20; // TODO

            return !(Math.log10(cellSize) <= (Math.log10(minDistance) - splitScale));
        }
    }
    /** The scale factor for splitting */
    private double mySplitScale = 0.99;
    /** The original 'top' level tiles.  The tiles when zoomed out all the way */
    private ArrayList<Tile> myTopLevelTiles = null;

    /** Set the split scale */
    protected void setSplitScale(double splitScale) {
        mySplitScale = splitScale;
    }

    /** Get the split scale */
    protected double getSplitScale() {
        return mySplitScale;
    }

    public ArrayList<Tile> getTopLevelTiles() {
        return myTopLevelTiles;
    }

    /** Set the top level tile set. The zoomed out all the way tiles */
    public void setTopLevelTiles(ArrayList<Tile> list) {
        myTopLevelTiles = list;
    }

    @Override
    public void draw(DrawContext dc) {
    }
}
