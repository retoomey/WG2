package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.ChartViewChart;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.views.ChartView;
import org.wdssii.gui.worldwind.WorldwindUtil;

/**
 * A collection of editable points in a line. Constant or increasing time. This
 * might merge with LLHArea. I'm refactoring/cleaning up some of the
 * LLHAreaSlice, etc..mess. Keeping things separate until proven they work.
 *
 * This should really be the LLHArea....
 *
 */
public class LLHAreaSet extends LLHArea {

    private final static Logger LOG = LoggerFactory.getLogger(LLHAreaSet.class);
    public int currentHeightMeters = (int) LLHArea.DEFAULT_HEIGHT_METERS;
    public int currentBottomMeters = 0;
    public int myNumRows = 50;
    public int myNumCols = 100;
    private String myChartKey;
    
    /**
     * Change to pass onto the LLHArea. All fields common to LLHArea are here
     */
    public static class LLHAreaSetMemento extends LLHArea.LLHAreaMemento {

        public static final String TOP_HEIGHT = "topheightmeters";
        public static final String BOTTOM_HEIGHT = "bottommeters";
        public static final String GRID_ROWS = "gridrows";
        public static final String GRID_COLS = "gridcols";
        public static final String POINTS = "points";
        public static final String RENDERER = "renderer";

        // Create a new default property LLHAreaSetMemento
        public LLHAreaSetMemento(LLHAreaSet a) {
            super(a);  // this will go away
            initProperty(TOP_HEIGHT, 10000);
            initProperty(BOTTOM_HEIGHT, 0);
            initProperty(GRID_ROWS, 50);
            initProperty(GRID_COLS, 100);
            // Experimental...
            initProperty(POINTS, new ArrayList<LatLon>());
            initProperty(RENDERER, "");
        }
    }

    /**
     * Get the memento for this class
     */
    @Override
    public LLHAreaSetMemento getMemento() {
        LLHAreaSetMemento m = new LLHAreaSetMemento(this);
        // hack into old way bleh...so bad.  always set so thrash...
        m.setProperty(LLHAreaSetMemento.TOP_HEIGHT, currentHeightMeters);
        m.setMaxHeight(currentHeightMeters);
        m.setProperty(LLHAreaSetMemento.BOTTOM_HEIGHT, currentBottomMeters);
        m.setMinHeight(currentBottomMeters);
        m.setProperty(LLHAreaSetMemento.GRID_ROWS, myNumRows);
        m.setProperty(LLHAreaSetMemento.GRID_COLS, myNumCols);
        m.setProperty(LLHAreaSetMemento.RENDERER, myChartKey);
        
        // Stuff points into property copy each time could be bad later for >> N
        m.setProperty(LLHAreaSetMemento.POINTS, this.getArrayListCopyOfLocations());
        return m;
    }

    @Override
    public void setFromMemento(LLHArea.LLHAreaMemento l) {

        if (l instanceof LLHAreaSet.LLHAreaSetMemento) {
            LLHAreaSet.LLHAreaSetMemento ls = (LLHAreaSet.LLHAreaSetMemento) (l);
            setFromMemento(ls);
        }
        // Call super afterwards since we hacked altitudes
        super.setFromMemento(l);

        @SuppressWarnings("unchecked")
        ArrayList<LatLon> list = ((ArrayList<LatLon>) l.getPropertyValue(LLHAreaSetMemento.POINTS));
        if (list != null) {
            this.setLocations(list);
            // FeatureMemento fm = (FeatureMemento)(m); // Check it
            //	FeatureChangeCommand c = new FeatureChangeCommand(this, fm);
            //	CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /**
     * Not overridden
     */
    protected void setFromMemento(LLHAreaSet.LLHAreaSetMemento l) {
        // Big mess right now will become more like other features 
        // FIXME: This will go away
        Integer v = ((Integer) l.getPropertyValue(LLHAreaSetMemento.TOP_HEIGHT));
        if (v != null) {
            l.setMaxHeight(v.intValue());
            currentHeightMeters = (int) l.getMaxHeight();
        }
        v = ((Integer) l.getPropertyValue(LLHAreaSetMemento.BOTTOM_HEIGHT));
        if (v != null) {
            l.setMinHeight(v.intValue());
            currentBottomMeters = (int) l.getMinHeight();
        }
        v = ((Integer) l.getPropertyValue(LLHAreaSetMemento.GRID_ROWS));
        if (v != null) {
            myNumRows = v.intValue();
        }
        v = ((Integer) l.getPropertyValue(LLHAreaSetMemento.GRID_COLS));
        if (v != null) {
            myNumCols = v.intValue();
        }
         String k = ((String) l.getPropertyValue(LLHAreaSetMemento.RENDERER));
        if (k != null){
            myChartKey = k;
        } 

    }

    /**
     * Number of rows...this is really X resolution...
     */
    public int getNumRows() {
        return myNumRows;
    }

    /**
     * Number of cols...this is really Y or height resolution
     */
    public int getNumCols() {
        return myNumCols;
    }

    public LLHAreaSet(LLHAreaFeature f) {
        super(f);
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//
    protected Vec4 computeReferenceCenter(DrawContext dc) {
        Extent extent = this.getExtent(dc);
        return extent != null ? extent.getCenter() : null;
    }

    /**
     * Get our chart object, if any that we use to fill in our stuff
     */
    public ChartViewChart get3DRendererChart() {
        ChartView v = getChartView();
        if (v != null){
            return v.getChart();
        }
        return null;
    }

    public void setChartView(ChartView v0) {
        // We don't hold the object in case name changes or it is destroyed
        myChartKey = v0.getKey();
    }

    public ChartView getChartView() {
        ArrayList<ChartView> list = ChartView.getList();
        for (ChartView c : list) {
            if (c.getKey().equals(myChartKey)){
                return c;
            }
        }
        // if not found and there's a list, use the first one....
        if (!list.isEmpty()){
            ChartView fallBack = list.get(0);
            myChartKey = fallBack.getKey();
            return fallBack;            
        }
        return null;
    }

    /**
     * This is what we have to modify, replace...right here.....
     *
     * @param dc
     * @param drawStyle
     * @param locations
     * @param edgeFlags
     */
    @Override
    protected void doRenderGeometry(DrawContext dc, String drawStyle, List<LatLon> locations, List<Boolean> edgeFlags) {
        if (locations.isEmpty()) {
            return;
        }

        // Get the true altitudes for sampling purposes
        // vertical is for rendering only...getAltitudes(dc.getVerticalExaggeration());
        double[] altitudes = this.getAltitudes();

        // We were syncing to my current grid here...
        //myCurrentGrid.bottomHeight = altitudes[0];
        //myCurrentGrid.topHeight = altitudes[1];
        List<LatLon> list = this.getLocations();
        GL gl = dc.getGL();

        if (drawStyle.equals("fill")) {

            if (!list.isEmpty()) {
                // This is the default draw
                Globe globe = dc.getGlobe();
                double vert = dc.getVerticalExaggeration();
                gl.glPushAttrib(GL.GL_LINE_BIT | GL.GL_LIGHTING_BIT | GL.GL_ENABLE_BIT);
                gl.glDisable(GL.GL_LIGHTING);
                gl.glDisable(GL.GL_DEPTH_TEST);
                gl.glLineWidth(5);

                boolean box = dc.isPickingMode();
                box = true;
                if (!dc.isPickingMode()) { // Pick mode uses a unique color to pick
                    gl.glColor4f(1.0f, 1.0f, 1.0f, .20f);
                }

                double botHeight = altitudes[0] * vert;
                double topHeight = altitudes[1] * vert;

                // quad strip connecting dots in order for 'grabbing'
                if (dc.isPickingMode()) {

                    if (list.size() > 1) {
                        gl.glBegin(GL.GL_QUAD_STRIP);
                        for (LatLon l : list) {
                            // Calculate vec from position
                            Vec4 bot = globe.computePointFromPosition(l.latitude, l.longitude, botHeight);
                            Vec4 top = globe.computePointFromPosition(l.latitude, l.longitude, topHeight);
                            gl.glVertex3d(bot.x, bot.y, bot.z);
                            gl.glVertex3d(top.x, top.y, top.z);
                        }
                        gl.glEnd();
                    } else {
                        // Size of 1.  Draw a box so user can 'hit' line...
                        // Calculate vec from position
                        LatLon l = list.get(0);
                        Vec4 bot = globe.computePointFromPosition(l.latitude, l.longitude, botHeight);
                        LLHAreaControlPoint.CircleMarker.billboard(dc, bot);
                    }
                } else {
                    // Connect the dots with a line strip...
                    if (list.size() > 1) {
                        gl.glBegin(GL.GL_LINE_STRIP);
                        for (LatLon l : list) {
                            Vec4 bot = globe.computePointFromPosition(l.latitude, l.longitude, botHeight);
                            gl.glVertex3d(bot.x, bot.y, bot.z);
                        }
                        gl.glEnd();
                    } else {
                        // Draw special box in case of single point so always visible
                        LatLon l = list.get(0);
                        Vec4 bot = globe.computePointFromPosition(l.latitude, l.longitude, botHeight);
                        LLHAreaControlPoint.CircleMarker.billboard(dc, bot);
                    }

                    // Draw 'heights' of sticks as well since we only draw bot points
                    gl.glBegin(GL.GL_LINES);
                    for (LatLon l : list) {
                        Vec4 bot = globe.computePointFromPosition(l.latitude, l.longitude, botHeight);
                        Vec4 top = globe.computePointFromPosition(l.latitude, l.longitude, topHeight);
                        gl.glVertex3d(bot.x, bot.y, bot.z);
                        gl.glVertex3d(top.x, top.y, top.z);
                    }
                    gl.glEnd();
                }


                gl.glPopAttrib();
            }
            // Pass render to chart object....
            ChartViewChart c = get3DRendererChart();
            if (c != null) {
                // Ok if chart exists...use it to draw.....

                // Shouldn't this code be in the renderer???
                gl.glPushAttrib(GL.GL_POLYGON_BIT);
                gl.glEnable(GL.GL_CULL_FACE);
                gl.glFrontFace(GL.GL_CCW);

                c.drawChartInLLHArea(dc, locations, altitudes, edgeFlags);
                //this.drawVSlice(dc, locations, edgeFlags);

                gl.glPopAttrib();
            }
        }
    }

    /**
     * Get a key that represents the GIS location of this slice
     */
    public String getGISKey() {
        
        // Add location and altitude...
        List<LatLon> locations = getLocationList();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < locations.size(); i++) {
            LatLon l = locations.get(i);
            buf.append(l.getLatitude()+":");
            buf.append(l.getLongitude()+":");
        }
        //newKey = newKey + myCurrentGrid.bottomHeight;
        //newKey = newKey + myCurrentGrid.topHeight;
        double[] altitudes = this.getAltitudes();
        buf.append(altitudes[0]);
        buf.append(altitudes[1]);
        String newKey = buf.toString();
        return newKey;
    }

    /**
     * Get a volume key for this slice, either virtual or nonvirtual volume
     */
    public String getVolumeKey(String follow, boolean useVirtual) {
        // Add the key of the volume....
        String newKey = "";
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, useVirtual);
        newKey += volume.getKey();	// java 6 StringBuilder is internally used...
        return newKey;
    }

    /**
     * Get a unique key representing all states. Used by charts and 3d slice to
     * tell unique state for recalculation.
     *
     * @param virtual -- shouldn't pass these params probably.
     * @param useFilters
     * @return
     */
    public String getKey(String follow, boolean virtual, FilterList list, boolean useFilters) {
        String newKey = getGISKey();
        return newKey;
    }

    public double getBottomHeightKms() {
        double[] altitudes = this.getAltitudes();
        return altitudes[0];
    }

    public double getTopHeightKms() {
        return upperAltitude;
    }

    /**
     * Update the current grid that is the GIS location of the slice
     */
    @Override
    public void updateCurrentGrid() {
    }

    /**
     * Our version of computePointFromPosition that doesn't make new objects and
     * do tons of checks. Meant to be called from the generate function only
     * where we have already done all the safety checks. Since this is called a
     * zillion times during rendering any speed improvement here helps.
     *
     * @param dc
     * @param latitude
     * @param longitude
     * @param elevation
     * @param terrainConformant
     * @return
     */
    protected Vec4 computePoint(Globe globe, Angle latitude, Angle longitude, double elevation,
            boolean terrainConformant) {

        return globe.computePointFromPosition(latitude, longitude, elevation);
    }

    /**
     * The default location for a newly created LLHArea
     */
    @Override
    protected List<LatLon> getDefaultLocations(WorldWindow wwd, Object params) {

        // Taken from worldwind...we'll need to figure out how we want the vslice/isosurface to work...
        Position position = WorldwindUtil.getNewShapePosition(wwd);
        Angle heading = WorldwindUtil.getNewShapeHeading(wwd, true);

        /**
         * Create based on viewport.
         */
        Globe globe = wwd.getModel().getGlobe();
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
        transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));
        double sizeInMeters = DEFAULT_LENGTH_METERS;
        double widthOver2 = sizeInMeters / 2.0;
        double heightOver2 = sizeInMeters / 2.0;

        int count = 2;
        if (params instanceof Integer) {
            Integer c = (Integer) (params);
            count = c.intValue();
        }
        Vec4[] points;

        switch (count) {
            case 1:
                points = new Vec4[]{
                    //new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
                    new Vec4(0.0, 0.0, 0.0).transformBy4(transform) // lower left (as if looking down, to sw)
                };
                break;
            default:
            case 2:
                points = new Vec4[]{
                    new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
                    // new Vec4(widthOver2,  -heightOver2, 0.0).transformBy4(transform), // lower right
                    new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
                // new Vec4(-widthOver2,  heightOver2, 0.0).transformBy4(transform)  // upper left
                };
        }

        /**
         * Convert from vector model coordinates to LatLon
         */
        LatLon[] locations = new LatLon[points.length];
        for (int i = 0;
                i < locations.length;
                i++) {
            locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
        }

        return Arrays.asList(locations);
    }
}