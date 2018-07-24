package org.wdssii.gui.worldwind;

import org.wdssii.gui.worldwind.products.WWWindFieldRenderer;
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
import javax.media.opengl.GL2;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.WindField;
import org.wdssii.datatypes.WindField.WindFieldDataPoint;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.worldwind.WWTileRenderer.Tile;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DfloatAsNodes;

/** A tile that displays a section of a WindFieldProduct
 * Based originally on the worldwind surfaceTile class
 * 
 * @author Robert Toomey
 *
 */
public class WindFieldTile extends WWTileRenderer.Tile {

    private final static Logger LOG = LoggerFactory.getLogger(WindFieldTile.class);
    protected int counter = 0;
    protected boolean tileCreated = false;
    protected boolean atMaxLevel = false;
    /** The sector this tile represents */
    protected Sector mySector;
    /** The vertex points to render */
    protected Array1D<Float> verts = null;
    /** The colors for the vertexes */
    protected Array1D<Float> colors = null;
    /** The row number in the level, used for reference */
    private final int myRow;
    /** The col number in the level, used for reference */
    private final int myCol;
    private int myLevelNumber;
    /** tileCreated and workerlock need to sync together */
    protected Object myWorkerLock = new Object();
    protected backgroundTile myWorker;
    // Hold the subtiles within us for now, until we figure out our cache system
    protected WindFieldTile[] mySubtile;

    public class backgroundTile extends WdssiiJob {

        public DrawContext dc;
        public Product aProduct;

        public backgroundTile(DrawContext aDc, Product rec) {
            super("3D WindField Generation");
            dc = aDc;
            aProduct = rec;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            monitor.beginTask("WindFieldTile:", WdssiiJobMonitor.UNKNOWN);

            //WindFieldProduct myWindFieldProduct = (WindFieldProduct)(data);
            WindField aWF = (WindField) aProduct.getRawDataType();

            final int DENSITY_X = WWWindFieldRenderer.DENSITY_X;
            final int DENSITY_Y = WWWindFieldRenderer.DENSITY_Y;
            // Think polar coordinates for these (shape of a kite)
            final double arrowDelta = 0.3926; // Angle in radians 'out' from arrow shaft
            final double arrowPercent = .60;  // Distance towards base point (100% = center, 0% = arrow head)
            final double arrowLength = 0.70; // Percent of box
            final int pointsPerArrow = 6;

            //verts = BufferUtil.newDoubleBuffer(DENSITY_X*DENSITY_Y*3*pointsPerArrow);
            //colors = BufferUtil.newDoubleBuffer(DENSITY_X*DENSITY_Y*4*pointsPerArrow);

            //verts = OpenGLUtils.newDoubleBuffer(DENSITY_X*DENSITY_Y*3*pointsPerArrow);
            //colors = OpenGLUtils.newDoubleBuffer(DENSITY_X*DENSITY_Y*4*pointsPerArrow);

            verts = new Array1DfloatAsNodes(DENSITY_X * DENSITY_Y * 3 * pointsPerArrow, 0.0f);
            colors = new Array1DfloatAsNodes(DENSITY_X * DENSITY_Y * 4 * pointsPerArrow, 0.0f);

            Globe g = dc.getGlobe();

            Angle minLat = mySector.getMinLatitude();
            Angle maxLat = mySector.getMaxLatitude();
            Angle minLong = mySector.getMinLongitude();
            Angle maxLong = mySector.getMaxLongitude();

            ColorMap aColorMap = aProduct.getColorMap();
            double height = aWF.getLocation().getHeightKms() + 10.0;

            Location currentLocation = new Location(0, 0, 0);

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

            WindFieldDataPoint output = new WindFieldDataPoint();
            int vertCount = 0;
            int colorCount = 0;
            ColorMapFloatOutput c = new ColorMapFloatOutput();

            // We match left to right, top to bottom for north hemisphere
            currentLon = minLong.degrees;
            currentLat = maxLat.degrees;

            double halfLat = deltaLat / 2.0d;
            double halfLon = deltaLon / 2.0d;

            // From 'top' to 'bottom' in latitude
            for (int y = 0; y < DENSITY_Y; y++) {

                // For x direction left to right, update longitude
                currentLon = minLong.degrees; // start long
                for (int x = 0; x < DENSITY_X; x++) {
                    currentLocation.init(currentLat, currentLon, 0);
                    boolean good = aWF.getValue(currentLocation, output);
                    if (good) { // If good value, add an arrow for it...

                        // FIXME: check for missing data values...

                        float value = (float) (Math.sqrt(output.u * output.u + output.v * output.v));
                        aColorMap.fillColor(c, value);
                        //output.u = 100.0f;
                        //output.v = 10.0f;
                        //c[0] = 1.0;
                        //c[1] = 1.0;
                        //c[2] = 1.0;

                        // Test for windfield ..force each direction
						/*	switch(counter){
                        case 0:
                        output.u = 0.0f; output.v = 100.f; // North?
                        break;
                        case 1:
                        output.u = -100.0f; output.v = 0.0f; // West 
                        break;
                        case 2:
                        output.u = 0.0f; output.v = -100.f; // South
                        break;
                        case 3: output.u = 100.0f; output.v = 0.0f; // east
                        break;
                        }
                        counter++; if (counter > 3){ counter = 0; }
                         */

                        // Make an arrow head, using the 'square' size
                        // u is east, v is north
                        // can always fix it later if wrong
                        //double angle = Math.atan2(-output.v, -output.u);
                        double angle = Math.atan2(output.v, output.u);
                        //System.out.println("atans2 "+output.u+", "+output.v+" --> "+angle);
                        // Base of arrow, centered within the Sector
                        double centerLat = currentLat + halfLat;
                        double centerLon = currentLon + halfLon;

                        // Center of circle (base of arrow)
                        Vec4 point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat + (halfLat * Math.sin(angle) * arrowLength)),
                                Angle.fromDegrees(centerLon - (halfLon * Math.cos(angle) * arrowLength)),
                                height);

                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);

                        // Head of arrow (in the inset circle of Sector)
                        point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat - (halfLat * Math.sin(angle) * arrowLength)),
                                Angle.fromDegrees(centerLon + (halfLon * Math.cos(angle) * arrowLength)),
                                height);
                        //System.out.println("sin(angle) = "+Math.sin(angle));
                        //System.out.println("cos(angle) = "+Math.cos(angle));
                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);

                        // 'Right' arrow head (another line from head to right side )
                        // Head of arrow (in the inset circle of Sector)
                        point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat - (halfLat * Math.sin(angle) * arrowLength)),
                                Angle.fromDegrees(centerLon + (halfLon * Math.cos(angle) * arrowLength)),
                                height);

                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);
                        point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat - (halfLat * Math.sin(angle + arrowDelta) * arrowLength * arrowPercent)), // .66 means arrow 1/3 size of whole arrow (.66 towards base point)
                                Angle.fromDegrees(centerLon + (halfLon * Math.cos(angle + arrowDelta) * arrowLength * arrowPercent)),
                                height);

                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);

                        // Head of arrow (in the inset circle of Sector)
                        point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat - (halfLat * Math.sin(angle) * arrowLength)),
                                Angle.fromDegrees(centerLon + (halfLon * Math.cos(angle) * arrowLength)),
                                height);

                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);
                        point = g.computePointFromPosition(
                                Angle.fromDegrees(centerLat - (halfLat * Math.sin(angle - arrowDelta) * arrowLength * arrowPercent)), // .66 means arrow 1/3 size of whole arrow (.66 towards base point)
                                Angle.fromDegrees(centerLon + (halfLon * Math.cos(angle - arrowDelta) * arrowLength * arrowPercent)),
                                height);

                        colorCount = c.putUnsignedBytes(colors, colorCount);

                        verts.set(vertCount++, (float) point.x);
                        verts.set(vertCount++, (float) point.y);
                        verts.set(vertCount++, (float) point.z);
                    }
                    currentLon += deltaLon;
                }
                currentLat += deltaLat;
            }
            tileCreated = true;

            return WdssiiJobStatus.OK_STATUS;
        }
    }

    public WindFieldTile(Sector sector, int levelNumber, int row, int col) {
        mySector = sector;
        myRow = row;
        myCol = col;
        myLevelNumber = levelNumber;
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
     * */
    public void createTile(DrawContext dc, Product data) {

        if (myWorker != null) {
            return;
        }
        myWorker = new backgroundTile(dc, data);
        myWorker.schedule();
    }

    /** Using a beginBatch/endBatch.  This will only work when rendering a group of common tiles..
     * if we eventually 'mix' tile types it could fail
     * @param dc
     * @return
     */
    public static boolean beginBatch1(GL glold) {
    	final GL2 gl = glold.getGL().getGL2();

        // Set up
        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL2.GL_LIGHTING_BIT
                | GL.GL_COLOR_BUFFER_BIT
                | GL2.GL_ENABLE_BIT // FIXME: too many attributes
                | GL2.GL_TEXTURE_BIT | GL2.GL_TRANSFORM_BIT
                | GL2.GL_VIEWPORT_BIT | GL2.GL_CURRENT_BIT);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D); // no textures
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_FLAT); // FIXME: push attrib
        gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT
                | GL2.GL_CLIENT_PIXEL_STORE_BIT);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        gl.glLineWidth(2.0f);

        return true;
    }

    public static void endBatch1(GL glold) {
    	final GL2 gl = glold.getGL().getGL2();

        gl.glLineWidth(1.0f);
        gl.glPopClientAttrib();
        gl.glPopAttrib();
    }

    public boolean isTileReadyToDraw() {
        return tileCreated;
    }

    @Override
    public void generateTile(DrawContext dc, Product latlon) {
        if (!tileCreated) {
            // createTile will get called over and over again while worker thread is running...
            // Reason for lock is that worker thread could finish and set tileCreated = true right here, thus we need sync
            LOG.info("CREATING TILE>>SHOULD PAUSE");
            createTile(dc, latlon);
        }
    }

    /** Tiles should create themselves, allowing different tile types to be used in different areas.
     * FIXME: thread these in separate task if needed */
    @Override
    public void drawTile(DrawContext dc, Product windfield, boolean readoutMode) {

        if (tileCreated) {
            GL glold = dc.getGL();
        	final GL2 gl = glold.getGL2();

            try {
                Object lock1 = verts.getBufferLock();
                Object lock2 = colors.getBufferLock();

                // Nested locks considered dangerous, the order HAS to be
                // the same for anyone locking.  Our convention is verts then colors.
                synchronized (lock1) {
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
                            gl.glDrawArrays(GL.GL_LINES, start_vertex,
                                    run_vertices);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Exeception while drawing tile :" + e.toString());
            }
        }
    }

    /** Given an ArrayList, add this tile or the subtiles depending upon visibility and camera */
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
            WindFieldTile[] subTiles = getSubTiles(null, dc);

            if (subTiles == null) {
                // Fall back to parent tile.
                list.add(this);
            } else {

                // Recursively add children tiles
                for (WindFieldTile child : subTiles) {
                    if (child.isTileVisible(dc)) {
                        child.addTileOrDescendants(dc, splitScale, p, list);
                    }
                }
            }

        }
    }

    /** Create the divided subtiles for this tile */
    public WindFieldTile[] getSubTiles(Level nextLevel, DrawContext dc) {
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

        // Create four subtiles
        if (mySubtile == null) {
            mySubtile = new WindFieldTile[4];
        }

        if (mySubtile[0] == null) {
            mySubtile[0] = new WindFieldTile(new Sector(p0, p1, t0, t1), myLevelNumber + 1, 2 * row, 2 * col);
        }

        if (mySubtile[1] == null) {
            mySubtile[1] = new WindFieldTile(new Sector(p0, p1, t1, t2), myLevelNumber + 1, 2 * row, 2 * col + 1);
        }

        if (mySubtile[2] == null) {
            mySubtile[2] = new WindFieldTile(new Sector(p1, p2, t0, t1), myLevelNumber + 1, 2 * row + 1, 2 * col);
        }

        if (mySubtile[3] == null) {
            mySubtile[3] = new WindFieldTile(new Sector(p1, p2, t1, t2), myLevelNumber + 1, 2 * row + 1, 2 * col + 1);
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

        return Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector());
        // return dc.getGlobe().computeBoundingCylinder(dc.getVerticalExaggeration(), this.getSector());
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
