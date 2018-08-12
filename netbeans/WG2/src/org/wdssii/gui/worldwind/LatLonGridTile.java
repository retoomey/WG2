package org.wdssii.gui.worldwind;

import org.wdssii.gui.renderers.QuadStripRenderer;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logging;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.datatypes.LatLonGrid.LatLonGridQuery;
import org.wdssii.geom.Location;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.worldwind.WWTileRenderer.Tile;
import org.wdssii.gui.worldwind.renderers.WWLatLonGridRenderer;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.GrowList;

/**
 * A tile that displays a section of a LatLonGridProduct using quads Based
 * originally on the worldwind surfaceTile class
 *
 * @author Robert Toomey
 *
 */
public class LatLonGridTile extends WWTileRenderer.Tile {

    private final static Logger LOG = LoggerFactory.getLogger(LatLonGridTile.class);
    protected int counter = 0;
    protected int mySize = 0;
    protected static boolean toggle = false;
    public final static boolean BATCHED_TILES = true;
    /**
     * Is tile the highest resolution level?
     */
    protected boolean atMaxLevel = false;
    /**
     * The sector this tile represents
     */
    protected Sector mySector;
    protected QuadStripRenderer myQuadRenderer = new QuadStripRenderer();
    /**
     * The row number in the level, used for reference
     */
    private final int myRow;
    /**
     * The col number in the level, used for reference
     */
    private final int myCol;
    private int myLevelNumber;
    /**
     * tileCreated and workerlock need to sync together
     */
    protected Object myWorkerLock = new Object();
    protected backgroundTile myWorker;
    // Hold the subtiles within us for now, until we figure out our cache system
    protected LatLonGridTile[] mySubtile;
    /**
     * How much to shift a grid 'up' to avoid 3d hitting the terrain...humm,
     * There should be something in data set to tell if it's flat or not...
     * FIXME: Problem is some LLG should be where they say they are, others like
     * Satellite...where do they belong in 3d space?
     */
    private double FLAT_UP_ADJUST = 10.0d;

    @Override
    public void makeTheTile(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
        //m.beginTask("LatLonTile:", IProgressMonitor.UNKNOWN);
        //setTileCreating();  set by worker..

        LatLonGrid llg = (LatLonGrid) aProduct.getRawDataType();
        FilterList aList = aProduct.getFilterList();
        Globe myGlobe = dc.getGlobe(); // FIXME: says may be null???

        // Get the max size of the tile.  FIXME: this is wasteful
        final int DENSITY_X = WWLatLonGridRenderer.DENSITY_X;
        final int DENSITY_Y = WWLatLonGridRenderer.DENSITY_Y;
        final int pointsPerQuad = 4;
        int vertCounter = DENSITY_X * DENSITY_Y * 3 * pointsPerQuad;
        int colorCounter = DENSITY_X * DENSITY_Y * 1 * pointsPerQuad;  // one float per color

        // Allocate is too big...bleh...
        myQuadRenderer.allocate(vertCounter, colorCounter * 4);
        myQuadRenderer.begin();
        Array1DOpenGL verts = myQuadRenderer.getVerts();
        Array1DOpenGL colors = myQuadRenderer.getColors();
        Array1DOpenGL readout = myQuadRenderer.getReadout();
        GrowList<Integer> myOffsets = myQuadRenderer.getOffsets();

        // Allow rendering while creating..it's ok as long as offsets
        // are set LAST
        myQuadRenderer.setCanDraw(true);


        Angle minLat = mySector.getMinLatitude();
        Angle maxLat = mySector.getMaxLatitude();
        Angle minLong = mySector.getMinLongitude();
        Angle maxLong = mySector.getMaxLongitude();
        double height = llg.getLocation().getHeightKms() + FLAT_UP_ADJUST;

        Location currentLocation = new Location(0, 0, 0);

        // March so that it's ordered Sector for united states
        double deltaLat = (maxLat.degrees - minLat.degrees) / (DENSITY_Y * 1.0d);
        double deltaLon = (maxLong.degrees - minLong.degrees) / (DENSITY_X * 1.0d);

        // THIS IS WHAT STOPS US FROM DIVIDING
        double deltaWFLat = Math.abs(llg.getDeltaLat());
        if (deltaWFLat >= Math.abs(deltaLat)) {
            // AND same for longitude
            if (Math.abs(llg.getDeltaLon()) >= Math.abs(deltaLon)) {
                atMaxLevel = true; // Don't divide after this tile
                //System.out.println("REACHED MAX LEVEL FOR TILE");
            }
        }

        int idx = 0;
        int idy = 0;
        int idREAD = 0;

        // The four locations of the quad of the data cell
        Location loc0 = new Location(0, 0, 0);
        Location loc1 = new Location(0, 0, 0);
        Location loc2 = new Location(0, 0, 0);
        Location loc3 = new Location(0, 0, 0);
        ColorMapFloatOutput out = new ColorMapFloatOutput();

        int numLats = DENSITY_Y;
        int numLons = DENSITY_X;

        LatLonGridQuery lq = new LatLonGridQuery();
        Vec4 point0, point1, point2 = null, point3 = null;
        boolean startQuadStrip;
        int updateIndex = 0;

        // Northwest corner...
        double startLat = maxLat.degrees;
        double startLon = minLong.degrees;
        double latDelta = deltaLat;
        double lonDelta = deltaLon;

        double curLat;
        double curLon;
        float[] point01 = new float[6];
        float[] point23 = new float[6];
        float[] temp;

        curLat = startLat;
        for (int y = 0; y <= numLats; y++) {
            monitor.subTask("Row " + y + "/" + numLats);
            monitor.worked(1);   // Do it first to ensure it's called

            // Move alone a single 'row' of grid..increasing lon..
            curLon = startLon;
            int lastJWithData = -2;
            for (int x = 0; x <= numLons; x++) {
                currentLocation.init(curLat, curLon, height);
                float value = llg.getValue(currentLocation);
                if (value == DataType.MissingData) {
                    // This new way we don't have to calculate anything
                    // with missing data.  Much better for long bursts of
                    // missing...
                } else {

                    // Calculate the two points closest 'bottom' to the radar center
                    // if last written then we have this cached from the 
                    // old two 'top' points...
                    if (lastJWithData == (x - 1)) {
                        // The previous 'top' is our bottom,
                        // we don't need a new strip in this case...
                        // v0  v2  v4  v6
                        // v1  v3  v5  v7
                        loc0 = loc2;
                        loc1 = loc3;
                        //point0 = point2;
                        //point1 = point3;
                        temp = point01;
                        point01 = point23;
                        point23 = temp;
                        startQuadStrip = false;
                    } else {
                        // Calculate the closet points to 'left' and 'bottom'
                        loc0.init(curLat, curLon, height);
                        loc1.init(curLat - latDelta, curLon, height);
                        point0 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc0.getLatitude()),
                                Angle.fromDegrees(loc0.getLongitude()),
                                loc0.getHeightKms() * 1000);
                        point01[0] = (float) point0.x;
                        point01[1] = (float) point0.y;
                        point01[2] = (float) point0.z;
                        point1 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc1.getLatitude()),
                                Angle.fromDegrees(loc1.getLongitude()),
                                loc1.getHeightKms() * 1000);
                        point01[3] = (float) point1.x;
                        point01[4] = (float) point1.y;
                        point01[5] = (float) point1.z;
                        startQuadStrip = true;
                    }
                    // Calculate the furthest two points 'right' of the quad  
                    loc2.init(curLat, curLon + lonDelta, height);
                    loc3.init(curLat - latDelta, curLon + lonDelta, height);
                    lastJWithData = x;

                    // Filler data value...
                    lq.inDataValue = value;
                    lq.outDataValue = value;
                    aList.fillColor(out, lq, false);

                    if (startQuadStrip) {
                        // Then we have to write the new bottom values...
                        updateIndex = idx;

                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);
                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);

                        idx = verts.set(idx, point01);
                        /*
                         verts.set(idx++, (float) point0.x);
                         verts.set(idx++, (float) point0.y);
                         verts.set(idx++, (float) point0.z);
                         verts.set(idx++, (float) point1.x);
                         verts.set(idx++, (float) point1.y);
                         verts.set(idx++, (float) point1.z);
                         * */
                    }

                    // Always write the 'top' of the strip
                    // Push back last two vertices of quad
                    point2 = myGlobe.computePointFromPosition(
                            Angle.fromDegrees(loc2.getLatitude()),
                            Angle.fromDegrees(loc2.getLongitude()),
                            loc2.getHeightKms() * 1000);
                    point23[0] = (float) point2.x;
                    point23[1] = (float) point2.y;
                    point23[2] = (float) point2.z;
                    point3 = myGlobe.computePointFromPosition(
                            Angle.fromDegrees(loc3.getLatitude()),
                            Angle.fromDegrees(loc3.getLongitude()),
                            loc3.getHeightKms() * 1000);
                    point23[3] = (float) point3.x;
                    point23[4] = (float) point3.y;
                    point23[5] = (float) point3.z;
                    readout.set(idREAD++, value);
                    idy = out.putUnsignedBytes(colors, idy);
                    readout.set(idREAD++, value);
                    idy = out.putUnsignedBytes(colors, idy);
                    idx = verts.set(idx, point23);

                    /*verts.set(idx++, (float) point2.x);
                     verts.set(idx++, (float) point2.y);
                     verts.set(idx++, (float) point2.z);
                     verts.set(idx++, (float) point3.x);
                     verts.set(idx++, (float) point3.y);
                     verts.set(idx++, (float) point3.z);
                     */
                    // Update the offsets last...
                    if (startQuadStrip) {
                        myOffsets.add(updateIndex);
                    }
                }
                curLon += lonDelta;
            }
            curLat -= latDelta;
        }

        myQuadRenderer.end();

        setTileCreated();
        AnimateManager.updateDuringRender();
    }

    public void setBatched(boolean flag) {
        myQuadRenderer.setBatched(flag);
    }

    /**
     * Background create tile information....
     */
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
            return WdssiiJobStatus.OK_STATUS;
        }
    }

    public LatLonGridTile(Sector sector, int levelNumber, int row, int col) {
        mySector = sector;
        myRow = row;
        myCol = col;
        myLevelNumber = levelNumber;
        LOG.info("---created level " + myLevelNumber + ", " + myRow + ", " + myCol);
    }

    public String tileKey(Object data) {
        String key = toString();
        if (data instanceof Product) {
            Product product = (Product) (data);
            key = product.getCacheKey() + ", " + myLevelNumber + ", " + myRow + ", " + myCol;
        }
        return key;
    }

    /**
     * Create tile does the work of creating what is shown for the given data
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

    public static void beginReadout(Point p, Rectangle view, DrawContext dc) {
        QuadStripRenderer.beginReadout(p, view, dc.getGL());
    }

    public static float endReadout(Point p, Rectangle view, DrawContext dc) {
        ByteBuffer data = QuadStripRenderer.endReadout(p, view, dc.getGL());
        //float f = QuadStripRenderer.byteBufferToFloat(data);
        float f = 0;
        return f;
    }

    /**
     * Using a beginBatch/endBatch. This will only work when rendering a group
     * of common tiles.. if we eventually 'mix' tile types it could fail
     *
     * @param dc
     * @return
     */
    public static boolean beginBatch(GL gl) {
        if (BATCHED_TILES) {
            QuadStripRenderer.beginBatch(gl);
        }
        return true;
    }

    public static void endBatch(GL gl) {
        if (BATCHED_TILES) {
            QuadStripRenderer.endBatch(gl);
        }
    }

    @Override
    public void generateTile(DrawContext dc, Product latlon) {
        if (!isTileCreated()) {
            // createTile will get called over and over again while worker thread is running...
            // Reason for lock is that worker thread could finish and set tileCreated = true right here, thus we need sync
            LOG.info("CREATING TILE>>SHOULD PAUSE");
            createTile(dc, latlon);
        }
    }

    /**
     * Draw this tile
     */
    @Override
    public void drawTile(DrawContext dc, Product latlon, boolean readoutMode) {

        if (isTileCreated()) {
            myQuadRenderer.drawData(dc.getGL(), readoutMode);
        }
    }

    /**
     * Given an ArrayList, add this tile or the subtiles depending upon
     * visibility and camera. These are the tiles that would draw at this
     * resolution if they WERE ALL READY TO DRAW.
     */
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
                LOG.debug("add back parent " + this);
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

    /**
     * Create the divided subtiles for this tile. We split a tile into four
     * smaller tiles, unless we're at max level
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

    /**
     * Does this position hit our tile?
     */
    @Override
    public boolean positionInTile(DrawContext dc, Position p) {
        boolean contains = false;
        if (p != null) {
            Sector s = getSector();
            // Too close to edge of tile fails...so we pad sector a bit to
            // ensure tile hit...
            final int DENSITY_X = WWLatLonGridRenderer.DENSITY_X;
            final int DENSITY_Y = WWLatLonGridRenderer.DENSITY_Y;
            // Add a border of 2 of the 'pixels' of tile
            double deltaLat = (s.getDeltaLatDegrees() / DENSITY_X) * 2;
            double deltaLon = (s.getDeltaLonDegrees() / DENSITY_Y) * 2;

            double minLat = s.getMinLatitude().degrees - deltaLat;
            double maxLat = s.getMaxLatitude().degrees + deltaLat;
            double minLon = s.getMinLongitude().degrees - deltaLon;
            double maxLon = s.getMaxLongitude().degrees + deltaLon;

            Sector s2 = Sector.fromDegrees(minLat, maxLat, minLon, maxLon);

            contains = s2.contains(p);
            // if (contains) {
            //     LOG.debug("Sector " + s);
            //     LOG.debug("Point " + p);
            //     LOG.debug("Inside " + s.contains(p));
            // }
        }
        return contains;
    }

    /**
     * Return true if tile is visible in the given dc
     */
    @Override
    public boolean isTileVisible(DrawContext dc) {
       // Extent anExtent = getExtent(dc);
        // LOG.debug("Extent: " + anExtent);
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
