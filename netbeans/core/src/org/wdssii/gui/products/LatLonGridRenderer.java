package org.wdssii.gui.products;

import java.util.ArrayList;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.geom.Location;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;

/** The lat lon grid renderer creates tiles on demand for rendering large scale lat lon grids
 * 
 * @author Robert Toomey
 *
 */
public class LatLonGridRenderer extends TileRenderer {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog(LatLonGridRenderer.class);

    @Override
    public void initToProduct(DrawContext dc, Product aProduct) {
        super.initToProduct(dc, aProduct);

        // Set resolution based on product info??  With lazy load not sure how...
    }
    // Resolution settings....
    // I tweaked these for CONUS us images...
    // @TODO Could possibly look at a combination of data resolution and area covered
    // and create a formula that would work for 'any' latlongrid.  Any latlongrid
    // larger than a CONUS in area size will probably look blocky with these settings
    /** Tile size in x, power of 2.  Should match Y to be square */
    public final static int DENSITY_X = 64;
    /** Tile size in y, power of 2.  Should match X to be square */
    public final static int DENSITY_Y = 64;
    /** Number of top X tiles.  TOP_X*DENSITY_X = Resolution of CONUS zoomed out */
    public final static int TOP_X = 8;  // 4 * 64 = 256 across.  Good enough for most displays at distance
    public final static int TOP_Y = 8;
    private boolean mySetUpLevels = false;

    public void lazyInit() {
        this.createTopLevelTiles();
    }

    public LatLonGridRenderer() {
    }
    // Experimental.... create a single tile generator job for any tiles needing to be generated...
    // Use a stack so that newest requested tiles get generated first...
    // Note: this means tiles get rendered even if they are never drawn again if you move around a lot...
    // might be a way to cancel a background render if tile hasn't been 'drawn' recently...
    // We could probably have a limited size stack and toss off the oldest ones....
    public Object myStackLock = new Object();
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
            Tile currentTile = null;
            int size = 0;
            // Unknown because more tiles may add while we're running
            monitor.beginTask("Tiles", WdssiiJobMonitor.UNKNOWN); 
            while (true) {

                // Get the size and top tile from the current stack...
                synchronized (myStackLock) {
                    size = myBackgroundTiles.size();
                    // Always start the most recent tile....
                    monitor.subTask("Tiles left "+size);
                    if (size > 0) {
                        currentTile = myBackgroundTiles.pop();

                        // Mark tile as creating within stack lock, so that it won't
                        // get shoved into stack again in other thread.
                        currentTile.setTileCreating();
                    } else {
                        // Stack is empty, finish this job....
                        isDone = true;
                        monitor.done();
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

    /** Create the largest area covering tile.  We sync this to the full lat lon grid of the data product, instead of
     * to the entire planet (as in google earth, worldwind).  This prevents data 'jitter' on tile changes.
     */
    private void createTopLevelTiles() {

        /**Create the 'top' level tiles.  Currently hand set, this should probably depend upon the physical size
         * of the LatLonGrid
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

    @Override
    public void draw(DrawContext dc) {

        if (getProduct() == null) {
            return;
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

        //Draw tiles or descendants...

        // Render the current visible and loaded tileset
        if (tiles.size() >= 1) {

            // Push any uncreated tiles onto the to create job stack....
            // We sync access to Stack since the worker thread is popping tiles off it to generate...
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

            LatLonGridTile.beginBatch(dc.getGL());
            for (Tile tile : tiles) {
                //tile.generateTile(dc, p); // spawn creation job if needed
                tile.drawTile(dc, p);
            }
            LatLonGridTile.endBatch(dc.getGL());
        }

    }
}
