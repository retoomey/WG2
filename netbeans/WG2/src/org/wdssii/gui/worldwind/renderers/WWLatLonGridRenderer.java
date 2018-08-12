package org.wdssii.gui.worldwind.renderers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Stack;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.Location;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.worldwind.GLWorldWW;
import org.wdssii.gui.worldwind.LatLonGridTile;
import org.wdssii.gui.worldwind.WWTileRenderer;

/**
 * The lat lon grid renderer creates tiles on demand for rendering large scale
 * lat lon grids.  It uses the world wind classes extensively, so will not work
 * inside of just a GLWorld object
 *
 * @author Robert Toomey
 *
 */
public class WWLatLonGridRenderer extends WWTileRenderer {

    @SuppressWarnings("unused")
    private final static Logger LOG = LoggerFactory.getLogger(WWLatLonGridRenderer.class);

    // Resolution settings....
    // I tweaked these for CONUS us images...
    // @TODO Could possibly look at a combination of data resolution and area covered
    // and create a formula that would work for 'any' latlongrid.  Any latlongrid
    // larger than a CONUS in area size will probably look blocky with these settings
    /**
     * Tile size in x, power of 2. Should match Y to be square
     */
    public final static int DENSITY_X = 64;
    /**
     * Tile size in y, power of 2. Should match X to be square
     */
    public final static int DENSITY_Y = 64;
    /**
     * Number of top X tiles. TOP_X*DENSITY_X = Resolution of CONUS zoomed out
     */
    public final static int TOP_X = 8;  // 4 * 64 = 256 across.  Good enough for most displays at distance
    public final static int TOP_Y = 8;
    private boolean mySetUpLevels = false;

    public void lazyInit() {
        this.createTopLevelTiles();
    }

    public WWLatLonGridRenderer() {
        super(true);
    }
    // Experimental.... create a single tile generator job for any tiles needing to be generated...
    // Use a stack so that newest requested tiles get generated first...
    // Note: this means tiles get rendered even if they are never drawn again if you move around a lot...
    // might be a way to cancel a background render if tile hasn't been 'drawn' recently...
    // We could probably have a limited size stack and toss off the oldest ones....
    public final Object myStackLock = new Object();
    public Stack<Tile> myBackgroundTiles = new Stack<Tile>();
    public backgroundRender myWorker = null;

    public class backgroundRender extends WdssiiJob {

        private DrawContext myDc;
        private Product myProduct;
        public boolean isDone = false;

        public backgroundRender(String name, DrawContext aDc, Product aP) {
            super(name);
            myDc = aDc;
            myProduct = aP;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {

            // Go based on stack size RIGHT THIS MOMENT..
            // Note...another thread might add more tiles before we are done..
            Tile currentTile;
            int size;
            // Unknown because more tiles may add while we're running
            monitor.beginTask("Tiles", WdssiiJobMonitor.UNKNOWN);
            while (true) {

                // Get the size and top tile from the current stack...
                synchronized (myStackLock) {
                    size = myBackgroundTiles.size();
                    // Always start the most recent tile....
                    monitor.subTask("Tiles left " + size);
                    if (size > 0) {
                        currentTile = myBackgroundTiles.pop();

                        // Mark tile as creating within stack lock, so that it won't
                        // get shoved into stack again in other thread.
                        currentTile.setTileCreating();
                    } else {
                        // Stack is empty, finish this job....
                        isDone = true;
                        return WdssiiJobStatus.OK_STATUS;
                    }
                }

                // Work on a tile without holding onto stack...
                // Note we aren't synching to stack anymore...
                // Hey wait...we're out of stack so the tile will just get shoved back while we're working on it
                // so it can double call it...right?
                currentTile.makeTheTile(myDc, myProduct, monitor);
                monitor.worked(1);

            }
            //return Status.OK_STATUS;
        }
    }

    /**
     * Create the largest area covering tile. We sync this to the full lat lon
     * grid of the data product, instead of to the entire planet (as in google
     * earth, worldwind). This prevents data 'jitter' on tile changes.
     */
    private void createTopLevelTiles() {

        /**
         * Create the 'top' level tiles. Currently hand set, this should
         * probably depend upon the physical size of the LatLonGrid
         */
        LatLonGrid p = getLatLonGrid();
        if (p != null) {

            Location loc = p.getLocation();
            double maxLat = loc.getLatitude();
            double minLon = loc.getLongitude();
            double minLat = maxLat - (p.getDeltaLat() * p.getNumLat());
            double maxLon = minLon + (p.getDeltaLon() * p.getNumLon());
            double deltaLat = (maxLat - minLat) / TOP_Y;
            double deltaLon = (maxLon - minLon) / TOP_X;

            ArrayList<Tile> list = new ArrayList<Tile>(TOP_X * TOP_Y);
            double lat = maxLat;
            for (int y = 0; y < TOP_Y; y++) {
                double lon = minLon;
                for (int x = 0; x < TOP_X; x++) {
                    LatLonGridTile aWFTile = new LatLonGridTile(new Sector(
                            Angle.fromDegrees(lat - deltaLat),
                            Angle.fromDegrees(lat),
                            Angle.fromDegrees(lon),
                            Angle.fromDegrees(lon + deltaLon)),
                            0, x, y);
                    list.add(aWFTile);
                    lon += deltaLon;
                }
                lat -= deltaLat;
            }
            setTopLevelTiles(list);
        }
    }

    private LatLonGrid getLatLonGrid() {
        Product p = getProduct();
        if (p != null) {
            DataType dt = p.getRawDataType();
            if (dt instanceof LatLonGrid) {
                return (LatLonGrid) (dt);
            }
        }
        return null;
    }

    public void readoutTileAtPoint(Point p, Rectangle view, DrawContext dc) {
    }

    public void queueUnmadeTiles(ArrayList<Tile> tiles, DrawContext dc) {
        // Render the current visible and loaded tileset
        if (tiles.size() >= 1) {
            Product p = getProduct();
            // Push any uncreated tiles onto the to create job stack....
            // We sync access to Stack since the worker thread is popping tiles off it to generate...
            if (p != null) {  // Otherwise we'll have to do it later....
                synchronized (myStackLock) {
                    int counter = 0;
                    int counter2 = 0;
                    for (Tile tile : tiles) {
                        if (tile.isTileEmpty()) {
                            if (!myBackgroundTiles.contains(tile)) {
                                myBackgroundTiles.push(tile);  // Gonna need limited stack so stuff falls off bottom?
                                counter++;
                            }
                        } else {
                            counter2++;
                        }
                    }
                    if (myBackgroundTiles.size() > 0) {
                        if ((myWorker == null) || (myWorker.isDone)) {
                            myWorker = new backgroundRender("LatLonGridRenderer", dc, p);
                            myWorker.schedule();
                        }
                    }
                    //System.out.println("TILES: CREATED:"+counter2+" NOT:"+counter+" LEFT: "+myBackgroundTiles.size());
                }
            }
        }
    }

    @Override
    public void draw(GLWorld w, FeatureMemento m) {

        if (getProduct() == null) {
            return;
        }

        if (mySetUpLevels == false) {
            lazyInit();
            mySetUpLevels = true;
        }

        // Hack back to old....
        DrawContext dc = null;
        if (w instanceof GLWorldWW) {
            GLWorldWW ww = (GLWorldWW) (w);
            dc = ww.getDC();
        }
        // assemble tiles.  This gets/creates tile OBJECTS only..that would
        // draw at this time.  
        Product p = getProduct();
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        ArrayList<Tile> list = getTopLevelTiles();
        for (Tile tile : list) {
            tile.addTileOrDescendants(dc, getSplitScale(), p, tiles);
        }

        // Render the current visible and loaded tileset
        if (tiles.size() >= 1) {

            // Queue up any unmade tiles...
            queueUnmadeTiles(tiles, dc);

            LatLonGridTile.beginBatch(dc.getGL());
            for (Tile tile : tiles) {
                //tile.generateTile(dc, p); // spawn creation job if needed
                tile.drawTile(dc, p, false);
            }
            LatLonGridTile.endBatch(dc.getGL());
        }
    }

    public float drawReadout(GLWorld w, Point aPoint, Rectangle view, float missingData) {

        // Same as draw, but we really should only draw the tile and/or descendants 
        // under the mouse point....
        // FIXME: smarter draw to speed up...find tile under point and only render it...

        // Hack back to old....
        DrawContext dc = null;
        if (w instanceof GLWorldWW) {
            GLWorldWW ww = (GLWorldWW) (w);
            dc = ww.getDC();
        }
        if (getProduct() == null) {
            return missingData;
        }

        if (mySetUpLevels == false) {
            lazyInit();
            mySetUpLevels = true;
        }

        // assemble tiles.  This gets/creates tile OBJECTS only..that would
        // draw at this time.  
        Product p = getProduct();
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        ArrayList<Tile> list = getTopLevelTiles();
        for (Tile tile : list) {
            tile.addTileOrDescendants(dc, getSplitScale(), p, tiles);
        }

        // Render the current visible and loaded tileset
        float value = missingData;
        if (tiles.size() >= 1) {

            Position pos = getPosition(dc, aPoint, dc.getView(), dc.getGlobe());

            // Queue up any unmade tiles...
            queueUnmadeTiles(tiles, dc);

            tiles.get(0);

            LatLonGridTile.beginBatch(dc.getGL());

            /* Draw the readout as image so we can debug mouse readout 
             for (Tile tile : tiles) {

             // Attempt to draw only tiles with surface mouse point within
             // the tile sector...
             if (tile.positionInTile(dc, pos)) {
             tile.drawTile(dc, p, true);
             }
             }
             * */

            LatLonGridTile.beginReadout(aPoint, view, dc);
            int countHits = 0;
            for (Tile tile : tiles) {

                // Attempt to draw only tiles with surface mouse point within
                // the tile sector...
                if (tile.positionInTile(dc, pos)) {
                    tile.drawTile(dc, p, true);
                    countHits++;
                }
            }
            value = LatLonGridTile.endReadout(aPoint, view, dc);
            if (countHits == 0) { // Outside tiles..
                value = DataType.DataUnavailable;
            }
            LatLonGridTile.endBatch(dc.getGL());
        }
        return value;
    }

    /**
     * Return position from mouse
     */
    public Position getPosition(DrawContext dc, Point p, View view, Globe globe) {

        Position pos = null;
        Line ray = view.computeRayFromScreenPoint(p.getX(), p.getY());

        if (ray != null) {
            // Intersect mouse with round earth ball surface...
            Intersection[] intersections = globe.intersect(ray, 0);
            if (intersections == null) {
                return null;
            }

            Vec4 point = nearestIntersectionPoint(ray, intersections);
            if (point != null) {
                pos = globe.computePositionFromPoint(point);
            }
        }
        return pos;

    }

    public static Vec4 nearestIntersectionPoint(Line line, Intersection[] intersections) {
        Vec4 intersectionPoint = null;

        // Find the nearest intersection that's in front of the ray origin.
        double nearestDistance = Double.MAX_VALUE;
        for (Intersection intersection : intersections) {
            // Ignore any intersections behind the line origin.
            if (!isPointBehindLineOrigin(line, intersection.getIntersectionPoint())) {
                double d = intersection.getIntersectionPoint().distanceTo3(line.getOrigin());
                if (d < nearestDistance) {
                    intersectionPoint = intersection.getIntersectionPoint();
                    nearestDistance = d;
                }
            }
        }

        return intersectionPoint;
    }

    public static boolean isPointBehindLineOrigin(Line line, Vec4 point) {
        double dot = point.subtract3(line.getOrigin()).dot3(line.getDirection());
        return dot < 0.0;
    }

    /**
     * Get the readout for this product
     */
    /*
    @Override
    public float getReadoutValue(Point p, Rectangle view, GLWorld w) {

        float value = DataType.MissingData;
        if (p != null) {
            value = drawReadout(w, p, view, DataType.MissingData);
        }
        return value;
    }*/
}
