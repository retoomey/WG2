package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logging;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.renderers.TileRenderer.Tile;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array1DfloatAsNodes;

/** A tile that displays a section of a LatLonGridProduct using quads
 * Based originally on the worldwind surfaceTile class
 * @author Robert Toomey
 *
 */
public class LatLonGridTile extends TileRenderer.Tile {

    private static Log log = LogFactory.getLog(LatLonGridTile.class);
    protected int counter = 0;
    protected int mySize = 0;
    protected static boolean toggle = false;
    /** Is tile the highest resolution level? */
    protected boolean atMaxLevel = false;
    /** The sector this tile represents */
    protected Sector mySector;
    /** The vertex points to render */
    protected Array1Dfloat verts = null;
    /** The colors for the vertexes */
    protected Array1Dfloat colors = null;
    /** The row number in the level, used for reference */
    private final int myRow;
    /** The col number in the level, used for reference */
    private final int myCol;
    private int myLevelNumber;
    /** tileCreated and workerlock need to sync together */
    protected Object myWorkerLock = new Object();
    protected backgroundTile myWorker;
    // Hold the subtiles within us for now, until we figure out our cache system
    protected LatLonGridTile[] mySubtile;

    @Override
    public void makeTheTile(DrawContext dc, Product aProduct, WdssiiJobMonitor m) {
        //m.beginTask("LatLonTile:", IProgressMonitor.UNKNOWN);
        //setTileCreating();  set by worker..

        LatLonGrid aWF = (LatLonGrid) aProduct.getRawDataType();
        ColorMap aColorMap = aProduct.getColorMap();

        // Get the max size of the tile.  FIXME: this is wasteful
        final int DENSITY_X = LatLonGridRenderer.DENSITY_X;
        final int DENSITY_Y = LatLonGridRenderer.DENSITY_Y;
        final int pointsPerQuad = 4;
        int vertCounter = DENSITY_X * DENSITY_Y * 3 * pointsPerQuad;
        int colorCounter = DENSITY_X * DENSITY_Y * 1 * pointsPerQuad;  // one float per color

        Globe g = dc.getGlobe();

        verts = new Array1DfloatAsNodes(vertCounter, 0.0f);
        colors = new Array1DfloatAsNodes(colorCounter, 0.0f);

        Angle minLat = mySector.getMinLatitude();
        Angle maxLat = mySector.getMaxLatitude();
        Angle minLong = mySector.getMinLongitude();
        Angle maxLong = mySector.getMaxLongitude();

        //LatLonGrid aWF = latLonProduct.getLatLonGrid();
        double height = aWF.getLocation().getHeightKms() + 10.0;

        Location currentLocation = new Location(0, 0, 0);
        Location c1 = new Location(0, 0, 0);
        Location c2 = new Location(0, 0, 0);
        Location c3 = new Location(0, 0, 0);

        // March so that it's ordered Sector for united states
        double deltaLat = (minLat.degrees - maxLat.degrees) / DENSITY_Y;
        double deltaLon = (maxLong.degrees - minLong.degrees) / DENSITY_X;

        double currentLat;
        double currentLon;

        // THIS IS WHAT STOPS US FROM DIVIDING
        // This is the max level if the density of our 'scan' is >= the density of the
        // actual data.  Since our tiles split aligned to the data, eventually this will
        // be true.  Not sure this is 'good' enough or if it works
        double deltaWFLat = aWF.getDeltaLat();
        if (deltaWFLat >= deltaLat) {	// If the density of windfield lat greater or equal to our sample density
            // AND same for longitude
            if (aWF.getDeltaLon() >= deltaLon) {
                atMaxLevel = true; // Don't divide after this tile
                //System.out.println("REACHED MAX LEVEL FOR TILE");
            }
        }

        ColorMapFloatOutput out = new ColorMapFloatOutput();

        // We match left to right, top to bottom for north hemisphere
        currentLon = minLong.degrees;
        currentLat = maxLat.degrees;

        // From 'top' to 'bottom' in latitude
        int idy = 0;
        int idx = 0;
        for (int y = 0; y < DENSITY_Y; y++) {

            // For x direction left to right, update longitude
            currentLon = minLong.degrees; // start long
            for (int x = 0; x < DENSITY_X; x++) {

                currentLocation.init(currentLat, currentLon, height);
                float value = aWF.getValue(currentLocation);

                /** For 'smoothing' the data... might look better */
                c1.init(currentLat + deltaLat, currentLon, height);
                float v1 = aWF.getValue(c1);
                c2.init(currentLat + deltaLat, currentLon + deltaLon, height);
                float v2 = aWF.getValue(c2);
                c3.init(currentLat, currentLon + deltaLon, height);
                float v3 = aWF.getValue(c3);
                boolean g1 = DataType.isRealDataValue(v1);
                boolean g2 = DataType.isRealDataValue(v2);
                boolean g3 = DataType.isRealDataValue(v3);

                boolean good = DataType.isRealDataValue(value);

                if (good && g1 && g2 && g3) { // If good value, add a quad


                    Vec4 point = g.computePointFromPosition(
                            Angle.fromDegrees(currentLat),
                            Angle.fromDegrees(currentLon),
                            height);
                    verts.set(idx++, (float) point.x);
                    verts.set(idx++, (float) point.y);
                    verts.set(idx++, (float) point.z);
                    aColorMap.fillColor(out, value);
                    idy = out.putUnsignedBytes(colors, idy);

                    point = g.computePointFromPosition(
                            Angle.fromDegrees(currentLat + deltaLat),
                            Angle.fromDegrees(currentLon),
                            height);

                    verts.set(idx++, (float) point.x);
                    verts.set(idx++, (float) point.y);
                    verts.set(idx++, (float) point.z);
                    aColorMap.fillColor(out, v1);
                    idy = out.putUnsignedBytes(colors, idy);
                    point = g.computePointFromPosition(
                            Angle.fromDegrees(currentLat + deltaLat),
                            Angle.fromDegrees(currentLon + deltaLon),
                            height);

                    verts.set(idx++, (float) point.x);
                    verts.set(idx++, (float) point.y);
                    verts.set(idx++, (float) point.z);
                    aColorMap.fillColor(out, v2);

                    idy = out.putUnsignedBytes(colors, idy);
                    point = g.computePointFromPosition(
                            Angle.fromDegrees(currentLat),
                            Angle.fromDegrees(currentLon + deltaLon),
                            height);

                    verts.set(idx++, (float) point.x);
                    verts.set(idx++, (float) point.y);
                    verts.set(idx++, (float) point.z);
                    aColorMap.fillColor(out, v3);

                    idy = out.putUnsignedBytes(colors, idy);
                }
                currentLon += deltaLon;
            }
            currentLat += deltaLat;
        }
        setTileCreated();
    }

    /** Background create tile information.... */
    public class backgroundTile extends WdssiiJob {

        public DrawContext dc;
        public Product aProduct;

        public backgroundTile(DrawContext aDc, Product rec) {
            super("3D LatLonTile Generation");
            dc = aDc;
            aProduct = rec;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            monitor.beginTask("LatLonTile:", WdssiiJobMonitor.UNKNOWN);
            makeTheTile(dc, aProduct, monitor);
            monitor.done();
            return WdssiiJobStatus.OK_STATUS;
        }
    }

    public LatLonGridTile(Sector sector, int levelNumber, int row, int col) {
        mySector = sector;
        myRow = row;
        myCol = col;
        myLevelNumber = levelNumber;
        log.info("---created level " + myLevelNumber + ", " + myRow + ", " + myCol);
    }

    public String tileKey(Object data) {
        String key = toString();
        if (data instanceof Product) {
            Product product = (Product) (data);
            key = product.getCacheKey() + ", " + myLevelNumber + ", " + myRow + ", " + myCol;
        }
        return key;
    }

    /** Create tile does the work of creating what is shown for the given data 
     */
    public void createTile(DrawContext dc, Product data) {
        if (myWorker != null) {
            return;
        }
        myWorker = new backgroundTile(dc, data);
        //myWorker.start();
        //PlatformUI.getWorkbench().getProgressService().showInDialog(null, myWorker);
        myWorker.schedule();
    }

    /** Using a beginBatch/endBatch.  This will only work when rendering a group of common tiles..
     * if we eventually 'mix' tile types it could fail
     * @param dc
     * @return
     */
    public static boolean beginBatch(GL gl) {
        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                | GL.GL_COLOR_BUFFER_BIT
                | GL.GL_ENABLE_BIT
                | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT);

        gl.glDisable(GL.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D); // no textures
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glShadeModel(GL.GL_FLAT);
//		gl.glShadeModel(GL.GL_SMOOTH);

        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                | GL.GL_CLIENT_PIXEL_STORE_BIT);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glLineWidth(2.0f);

        return true;
    }

    public static void endBatch(GL gl) {
        gl.glLineWidth(1.0f);
        gl.glPopClientAttrib();
        gl.glPopAttrib();
    }

    @Override
    public void generateTile(DrawContext dc, Product latlon) {
        if (!isTileCreated()) {
            // createTile will get called over and over again while worker thread is running...
            // Reason for lock is that worker thread could finish and set tileCreated = true right here, thus we need sync
            log.info("CREATING TILE>>SHOULD PAUSE");
            createTile(dc, latlon);
        }
    }

    /** Draw this tile */
    @Override
    public void drawTile(DrawContext dc, Product latlon) {

        if (isTileCreated()) {

            GL gl = dc.getGL();
            try {
                // Lock any tile we are drawing so data manager doesn't purge it on us
                Object lock1 = verts.getBufferLock();
                Object lock2 = colors.getBufferLock();
                synchronized (lock1) {  // This double lock is ok, because verts and colors are independent?
                    synchronized (lock2) {
                        FloatBuffer v = verts.getRawBuffer();
                        FloatBuffer c = colors.getRawBuffer();

                        gl.glVertexPointer(3, GL.GL_FLOAT, 0, v.rewind());
                        //gl.glColorPointer(4, GL.GL_FLOAT, 0, c.rewind());
                        gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());
                        int size = verts.size();
                        if (size > 1) {
                            int start_index = 0;
                            int end_index = size;
                            int run_indices = end_index - start_index;
                            int start_vertex = start_index / 3;
                            int run_vertices = run_indices / 3;
                            gl.glDrawArrays(GL.GL_QUADS, start_vertex,
                                    run_vertices);
                        }

                    }
                }

            } catch (Exception e) {
                log.error("Exception while drawing tile :" + e.toString());
            }
        }
    }

    /** Given an ArrayList, add this tile or the subtiles depending upon visibility and camera.
     * These are the tiles that would draw at this resolution if they WERE ALL READY TO DRAW. */
    @Override
    public void addTileOrDescendants(DrawContext dc, double splitScale, Product p, ArrayList<Tile> list) {

        // Show tile or subtiles only is we're visible...
        if (isTileVisible(dc)) {

            // If this tile is good enough, add it...
            if (meetsRenderCriteria(dc, splitScale)) {
                list.add(this);
                return;
            }

            // We're not good enough, try to split to subtiles...
            LatLonGridTile[] subTiles = getSubTiles(null, dc);

            if (subTiles == null) {
                // Fall back to parent tile.
                list.add(this);
            } else {

                // Recursively add children tiles
                for (LatLonGridTile child : subTiles) {
                    if (child.isTileVisible(dc)) {
                        child.addTileOrDescendants(dc, splitScale, p, list);
                    }
                }
            }

        }
    }

    /** This draws tiles at the highest resolution where they are generated */
    public void drawTileOrDescendants(DrawContext dc, double splitScale, Product p,
            ArrayList<Tile> list) {
        // The tiles actually _ready_ to draw for this camera/resolution (parent > children if children not ready)

        // Show tile or subtiles only is we're visible...
        if (isTileVisible(dc)) {

            // If this tile is good enough, add it...
            if (meetsRenderCriteria(dc, splitScale)) {
                if (isTileCreated()) {
                    list.add(this);
                }
                return;
            }

            // We're not good enough, try to split to subtiles...
            Tile[] subTiles = getSubTiles(null, dc);

            if (subTiles == null) {
                // Fall back to parent tile.
                if (isTileCreated()) {
                    list.add(this);
                }
            } else {

                // Recursively add children tiles
                for (Tile child : subTiles) {
                    if (child.isTileVisible(dc)) {
                        child.addTileOrDescendants(dc, splitScale, p, list);
                    }
                }
            }

        }
    }

    /** Create the divided subtiles for this tile.  We split a tile into four smaller tiles, unless we're at max level
     */
    public LatLonGridTile[] getSubTiles(Level nextLevel, DrawContext dc) {
        if (atMaxLevel) {
            return null;
        }

        Angle p0 = this.getSector().getMinLatitude();
        Angle p2 = this.getSector().getMaxLatitude();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = this.getSector().getMinLongitude();
        Angle t2 = this.getSector().getMaxLongitude();
        Angle t1 = Angle.midAngle(t0, t2);

        int row = myRow;
        int col = myCol;

        // Create four subtiles.  We will probably never need to actually delete the subtiles,
        // they aren't very big..it's the data that is large
        if (mySubtile == null) {
            mySubtile = new LatLonGridTile[4];
        }

        if (mySubtile[0] == null) {
            mySubtile[0] = new LatLonGridTile(new Sector(p0, p1, t0, t1), myLevelNumber + 1, 2 * row, 2 * col);
        }

        if (mySubtile[1] == null) {
            mySubtile[1] = new LatLonGridTile(new Sector(p0, p1, t1, t2), myLevelNumber + 1, 2 * row, 2 * col + 1);
        }

        if (mySubtile[2] == null) {
            mySubtile[2] = new LatLonGridTile(new Sector(p1, p2, t0, t1), myLevelNumber + 1, 2 * row + 1, 2 * col);
        }

        if (mySubtile[3] == null) {
            mySubtile[3] = new LatLonGridTile(new Sector(p1, p2, t1, t2), myLevelNumber + 1, 2 * row + 1, 2 * col + 1);
        }
        return mySubtile;
    }

    /** Return true if tile is visible in the given dc */
    @Override
    public boolean isTileVisible(DrawContext dc) {
        return getExtent(dc).intersects(
                dc.getView().getFrustumInModelCoordinates())
                && (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(getSector()));
    }

    public Extent getExtent(DrawContext dc) {
        if (dc == null) {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        //return dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
        return Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector());
    }

    public final Sector getSector() {
        return mySector;
    }

    public final int getLevelNumber() {
        return myLevelNumber;
    }

    // Move these from layer into tile..only tile type knows when to split
    public boolean meetsRenderCriteria(DrawContext dc, double splitScale) {
        return (atMaxLevel || !needToSplit(dc, getSector(), splitScale));
    }
}
