package org.wdssii.gui.volumes;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.ScreenSizeDetailLevel;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.LLHAreaCommand;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.VolumeSlice3DOutput;
import org.wdssii.gui.products.VolumeSliceInput;

import java.util.*;

/**
 * A copy of worldwind Polygon, with modifications to allow us to render the vslice
 * Would be nice to subclass polygon, but worldwind currently has locked down too much of the renderering
 * code.  The only thing we really want from Polygon is all the nice GUI control interface stuff.
 * Hummm...maybe we can override polygon and simply replace the full doRender
 * FIXME:  merge back to polygon subclass if able.
 */
public class LLHAreaSlice extends LLHArea {

    /** The resolution of the vslice */
    public static final int myNumRows = 100;  //50
    public static final int myNumCols = 100; //100
    private VolumeSliceInput myCurrentGrid = null;
    private List<LatLon> locations = new ArrayList<LatLon>();
    private int subdivisions = 1;  // power of 2 breakdown of side..
    private VSliceRenderer myRenderer = new VSliceRenderer(this);
    private ProductVolume myVolumeProduct = null;
    private VolumeSlice3DOutput myGeometry = new VolumeSlice3DOutput();
    private double myAltitude0 = 0;
    private double myAltitude1 = 100;
    private String myCacheKey = "";

    // A counter increased whenever vslice data changes.  Used
    // by charts to determine if data has changed
    //  private int myIterationCount = 0;
    public int getNumRows() {
        return myNumRows;
    }

    public int getNumCols() {
        return myNumCols;
    }

    /** Called by chart to get the current grid */
    public VolumeSliceInput getGrid() {
        return myCurrentGrid;
    }

    // public int getIterationCount(){ return myIterationCount; }
    public double getRangeKms() {
        // Get the range of this vslice in Kms...
        LatLon l1 = locations.get(0);
        LatLon l2 = locations.get(1);
        // FIXME: cleaner way of this?....fetch radius of current globe..
        double radius = CommandManager.getInstance().getEarthBall().getWwd().getModel().getGlobe().getRadius();
        double length = LatLon.greatCircleDistance(l1, l2).radians * radius;
        return length;
    }

    public double getHeightKms() {
        // Get the range of this vslice in Kms...
        // double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
        double bottomHeight = myAltitude0;  // Remembered from last draw...FIXME.
        double topHeight = myAltitude1;
        return topHeight - bottomHeight;
    }

    public LLHAreaSlice(AirspaceAttributes attributes) {
        super(attributes);
        this.makeDefaultDetailLevels();
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(myProductFollow, getUseVirtualVolume());
        myVolumeProduct = volume;
    }

    public LLHAreaSlice() {
        this.makeDefaultDetailLevels();
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(myProductFollow, getUseVirtualVolume());
        myVolumeProduct = volume;
    }

    private void makeDefaultDetailLevels() {
        List<LLHAreaDetailLevel> levels = new ArrayList<LLHAreaDetailLevel>();
        double[] ramp = ScreenSizeDetailLevel.computeDefaultScreenSizeRamp(5);

        LLHAreaDetailLevel level;
        level = new LLHAreaScreenSizeDetailLevel(ramp[0], "Detail-Level-0");
        level.setValue(SUBDIVISIONS, 4);
        level.setValue(DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new LLHAreaScreenSizeDetailLevel(ramp[1], "Detail-Level-1");
        level.setValue(SUBDIVISIONS, 3);
        level.setValue(DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new LLHAreaScreenSizeDetailLevel(ramp[2], "Detail-Level-2");
        level.setValue(SUBDIVISIONS, 2);
        level.setValue(DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new LLHAreaScreenSizeDetailLevel(ramp[3], "Detail-Level-3");
        level.setValue(SUBDIVISIONS, 1);
        level.setValue(DISABLE_TERRAIN_CONFORMANCE, false);
        levels.add(level);

        level = new LLHAreaScreenSizeDetailLevel(ramp[4], "Detail-Level-4");
        level.setValue(SUBDIVISIONS, 0);
        level.setValue(DISABLE_TERRAIN_CONFORMANCE, true);
        levels.add(level);

        this.setDetailLevels(levels);
    }

    public List<LatLon> getLocations() {
        return Collections.unmodifiableList(this.locations);
    }

    public void setLocations(Iterable<? extends LatLon> locations) {
        this.locations.clear();
        this.addLocations(locations);
    }

    protected List<LatLon> getLocationList() {
        return this.locations;
    }

    protected void addLocations(Iterable<? extends LatLon> newLocations) {
        if (newLocations != null) {
            for (LatLon ll : newLocations) {
                if (ll != null) {
                    this.locations.add(ll);
                }
            }
        }
        this.setExtentOutOfDate();
    }

    @Override
    public Position getReferencePosition() {
        return this.computeReferencePosition(this.locations, this.getAltitudes());
    }

    @Override
    protected void doMoveTo(Position oldRef, Position newRef) {
        if (oldRef == null) {
            String message = "nullValue.OldRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (newRef == null) {
            String message = "nullValue.NewRefIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.doMoveTo(oldRef, newRef);

        int count = this.locations.size();
        LatLon[] newLocations = new LatLon[count];
        for (int i = 0; i < count; i++) {
            LatLon ll = this.locations.get(i);
            double distance = LatLon.greatCircleDistance(oldRef, ll).radians;
            double azimuth = LatLon.greatCircleAzimuth(oldRef, ll).radians;
            newLocations[i] = LatLon.greatCircleEndPosition(newRef, azimuth, distance);
        }
        this.setLocations(Arrays.asList(newLocations));
    }

    protected int getSubdivisions() {
        return this.subdivisions;
    }

    protected void setSubdivisions(int subdivisions) {
        if (subdivisions < 0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "subdivisions=" + subdivisions);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.subdivisions = subdivisions;
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//
    protected Vec4 computeReferenceCenter(DrawContext dc) {
        Extent extent = this.getExtent(dc);
        return extent != null ? extent.getCenter() : null;
    }

    @Override
    protected Extent doComputeExtent(DrawContext dc) {
        return this.computeBoundingCylinder(dc, this.locations);
    }

    @Override
    protected void doRenderGeometry(DrawContext dc, String drawStyle) {
        this.doRenderGeometry(dc, drawStyle, this.locations, null);
    }

    /** This is what we have to modify, replace...right here.....
     * 
     * @param dc
     * @param drawStyle
     * @param locations
     * @param edgeFlags
     */
    protected void doRenderGeometry(DrawContext dc, String drawStyle, List<LatLon> locations, List<Boolean> edgeFlags) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null) {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (locations == null) {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (locations.size() == 0) {
            return;
        }

        double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
        myAltitude0 = altitudes[0];  // Hack
        myAltitude1 = altitudes[1];

        boolean[] terrainConformant = this.isTerrainConforming();
        int currentDivisions = this.subdivisions;

        if (this.getAltitudeDatum()[0].equals(AVKey.ABOVE_GROUND_REFERENCE)
                || this.getAltitudeDatum()[1].equals(AVKey.ABOVE_GROUND_REFERENCE)) {
            this.adjustForGroundReference(dc, terrainConformant, altitudes);
        }

        if (this.isEnableLevelOfDetail()) {
            LLHAreaDetailLevel level = this.computeDetailLevel(dc);

            Object o = level.getValue(SUBDIVISIONS);
            if (o != null && o instanceof Integer) {
                currentDivisions = (Integer) o;
            }

            o = level.getValue(DISABLE_TERRAIN_CONFORMANCE);
            if (o != null && o instanceof Boolean && (Boolean) o) {
                terrainConformant[0] = terrainConformant[1] = false;
            }
        }

        Vec4 referenceCenter = this.computeReferenceCenter(dc);
        // GOOP this.setExpiryTime(this.nextExpiryTime(dc, terrainConformant));
        this.clearElevationMap();

        GL gl = dc.getGL();

        dc.getView().pushReferenceCenter(dc, referenceCenter);

        // Ok worldwind gets the geometry twice, which is silly since it's the same each time.
        // Just get geometry once per draw pass...
        dc.getView().popReferenceCenter(dc);

        if (Airspace.DRAW_STYLE_FILL.equals(drawStyle)) {
            if (!this.isAirspaceCollapsed()) {
                gl.glPushAttrib(GL.GL_POLYGON_BIT);
                gl.glEnable(GL.GL_CULL_FACE);
                gl.glFrontFace(GL.GL_CCW);
            }

            // The 'old' polygon fill we don't want... it's private we couldn't override it

            //this.drawPolygonFill(dc, locations, edgeFlags, altitudes, terrainConformant, enableCaps, subdivisions,
            //   referenceCenter);
            this.drawVSlice(dc, locations, edgeFlags, altitudes, terrainConformant, currentDivisions,
                    referenceCenter);

            if (!this.isAirspaceCollapsed()) {
                gl.glPopAttrib();
            }
        } else if (Airspace.DRAW_STYLE_OUTLINE.equals(drawStyle)) {
            //   this.drawPolygonOutline(dc, locations, edgeFlags, altitudes, terrainConformant, enableCaps, subdivisions,
            //       referenceCenter);
        }

        //  dc.getView().popReferenceCenter(dc);
    }

    protected void adjustForGroundReference(DrawContext dc, boolean[] terrainConformant, double[] altitudes) {
        LatLon groundRef = this.getGroundReference();

        if (groundRef == null && this.getLocationList().size() > 0) {
            groundRef = this.getLocationList().get(0);
        }

        this.adjustForGroundReference(dc, terrainConformant, altitudes, groundRef); // no-op if groudRef is null
    }

    protected int computeCartesianPolygon(DrawContext dc, List<? extends LatLon> locations, List<Boolean> edgeFlags,
            Vec4[] points, Boolean[] edgeFlagArray, Matrix[] transform) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getGL() == null) {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (locations == null) {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (points == null) {
            String message = "nullValue.LocationsIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (points.length < (1 + locations.size())) {
            String message = Logging.getMessage("generic.ArrayInvalidLength",
                    "points.length < " + (1 + locations.size()));
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (transform == null) {
            String message = "nullValue.TransformIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (transform.length < 1) {
            String message = Logging.getMessage("generic.ArrayInvalidLength",
                    "transform.length < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Globe globe = dc.getGlobe();

        // Allocate space to hold the list of locations and location vertices.
        int locationCount = locations.size();

        // Compute the cartesian points for each location.
        // System.out.println ("Location count is "+locationCount);
        for (int i = 0; i < locationCount; i++) {
            LatLon ll = locations.get(i);
            points[i] = globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(), 0.0);

            if (edgeFlagArray != null) {
                edgeFlagArray[i] = (edgeFlags != null) ? edgeFlags.get(i) : true;
            }
        }

        // Compute the average of the cartesian points.
        Vec4 centerPoint = Vec4.computeAveragePoint(Arrays.asList(points));

        // Test whether the polygon is closed. If it is not closed, repeat the first vertex.
        if (!points[0].equals(points[locationCount - 1])) {
            points[locationCount] = points[0];
            if (edgeFlagArray != null) {
                edgeFlagArray[locationCount] = edgeFlagArray[0];
            }

            locationCount++;
        }

        // Compute a transform that will map the cartesian points to a local coordinate system centered at the average
        // of the points and oriented with the globe surface.
        Position centerPos = globe.computePositionFromPoint(centerPoint);
        Matrix tx = globe.computeModelCoordinateOriginTransform(centerPos);
        Matrix txInv = tx.getInverse();
        // Map the cartesian points to a local coordinate space.
        for (int i = 0; i < locationCount; i++) {
            points[i] = points[i].transformBy4(txInv);
        }

        transform[0] = tx;

        return locationCount;
    }

    /** Get a key that represents the GIS location of this slice */
    public String getGISKey() {
        String newKey = "";

        // Add location and altitude...
        for (int i = 0; i < locations.size(); i++) {
            LatLon l = locations.get(i);
            newKey = newKey + l.getLatitude() + ":";
            newKey = newKey + l.getLongitude() + ":";
        }
        newKey = newKey + myAltitude0;
        newKey = newKey + myAltitude1;
        return newKey;
    }

    /** Get a volume key for this slice, either virtual or nonvirtual volume */
    public String getVolumeKey(String follow, boolean useVirtual) {
        // Add the key of the volume....
        String newKey = "";
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, useVirtual);
        newKey += volume.getKey();	// java 6 StringBuilder is internally used...
        return newKey;
    }

    /** Get a unique key representing all states.  Used by charts and 3d slice to tell
     * unique vslice.  Note the parameters are passed in because different things are
     * in different states...
     * 
     * @param virtual
     * @param useFilters
     * @return
     */
    public String getKey(String follow, boolean virtual, FilterList list, boolean useFilters) {
        // Start with GIS location key
        String newKey = getGISKey();

        // Add the key of the volume....
        newKey += getVolumeKey(follow, virtual);
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, virtual);
        newKey += volume.getKey();	// java 6 StringBuilder is internally used...

        // Add filter settings if wanted
        if (list != null) {
            newKey += list.getFilterKey(useFilters);
        }
        return newKey;
    }

    /** Cache key represented by the vslice we're looking at.  When this changes, we know
     * we need to render a new vslice.
     * Takes into account the size/shape of vslice, the volume, and the filters
     * @return
     * 
     * This is the current key used to tell if the 3D slice in the earth view needs to be
     * remade or not.....
     */
    private String getNewCacheKey() {

        // FIXME: this is wrong....should be the filters for the product we are following....this
        // is the top product.....
        FilterList f = CommandManager.getInstance().getFilterList(getProductFollow());
        /*  FilterList f = null;
        ProductHandlerList phl = CommandManager.getInstance().getProductOrderedSet();
        if (phl != null){
        ProductHandler tph = phl.getTopProductHandler();
        if (tph != null){
        f = tph.getFList();
        }
        } */

        // Add the key of the current filter list...
        String newKey = getKey(myProductFollow, getUseVirtualVolume(), f, true);
        return newKey;
    }

    /**
     * This gets the special fill-in geometry for the vslice, a multi-set of triangles with data value colors.
     *@author Robert Toomey
     *
     * @return
     */
    private VolumeSlice3DOutput getVSliceGeometry(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags,
            double[] altitudes, boolean[] terrainConformant,
            int currentSubdivisions) {
        String newKey = getNewCacheKey();
        if (newKey.compareTo(myCacheKey) == 0) {
            return myGeometry;
        }

        // System.out.println("_------------>>> REGENERATE VSLICE!!!");
        myCacheKey = newKey;
        this.makeVSlice(dc, locations, edgeFlags, altitudes, terrainConformant, currentSubdivisions, myGeometry);

        // Fire changed event?  Is this enough?
        CommandManager.getInstance().executeCommand(new LLHAreaCommand(), true);
        return myGeometry;
    }

    /** New routine, draw a vslice  */
    private void drawVSlice(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags,
            double[] altitudes, boolean[] terrainConformant, int currentDivisions,
            Vec4 referenceCenter) {
        VolumeSlice3DOutput geom = this.getVSliceGeometry(dc, locations, edgeFlags, altitudes, terrainConformant,
                currentDivisions);

        myRenderer.drawVSlice(dc, geom);

        // this.getRenderer().drawGeometry(dc, geom.getFillIndexGeometry(), geom.getVertexGeometry());
    }

    public LatLon getLeftLocation() {
        // VSlice only.  Two locations, the points on the bottom. Make sure the east one is right of the west one...
        LatLon l1 = locations.get(0);
        LatLon l2 = locations.get(1);
        LatLon leftBottom;
        //LatLon rightBottom;
        if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
            leftBottom = l1;
            //rightBottom = l2;
        } else {
            leftBottom = l2;
            // rightBottom = l1;
        }
        return leftBottom;
    }

    public LatLon getRightLocation() {
        // VSlice only.  Two locations, the points on the bottom. Make sure the east one is right of the west one...
        LatLon l1 = locations.get(0);
        LatLon l2 = locations.get(1);
        //  LatLon leftBottom;
        LatLon rightBottom;
        if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
            // leftBottom = l1;
            rightBottom = l2;
        } else {
            // leftBottom = l2;
            rightBottom = l1;
        }
        return rightBottom;
    }

    public double getBottomHeightKms() {
        return myAltitude0;
    }

    public double getTopHeightKms() {
        return myAltitude1;
    }

    private void makeVSlice(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags,
            double[] altitudes, boolean[] terrainConformant,
            int currentSubdivisions,
            VolumeSlice3DOutput dest) {
        if (locations.size() == 0) {
            return;
        }

        // VolumeProduct volume = ProductManager.getCurrentVolumeProduct(myUseVirtual);
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getProductFollow(), getUseVirtualVolume());


        myVolumeProduct = volume;
        //  dc.setVerticalExaggeration(10.0);

        // VSlice only.  Two locations, the points on the bottom. Make sure the east one is right of the west one...
        // FIXME: duplicate code with getLeftLocation/getRightLocation
        LatLon l1 = locations.get(0);
        LatLon l2 = locations.get(1);
        LatLon leftBottom;
        LatLon rightBottom;
        if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
            leftBottom = l1;
            rightBottom = l2;
        } else {
            leftBottom = l2;
            rightBottom = l1;
        }

        // Get the filter list and the record object
        // ArrayList<DataFilter> list = null;
        FilterList aList = null;
        ProductHandlerList phl = CommandManager.getInstance().getProductOrderedSet();
        if (phl != null) {
            ProductHandler tph = phl.getTopProductHandler();
            if (tph != null) {
                //		list = tph.getFilterList();
                aList = tph.getFList();
            }
        }
        // Prep each filter for the volume...
        //  if (list != null){
        // 	for(DataFilter d:list){
        // 		d.prepFilterForVolume(volume);
        // 	}
        //}
        if (aList == null){ return; }
        aList.prepForVolume(volume);

        // For dynamic sizing outlines...I might need this code for 'smart' legend over vslice, so
        // I'm leaving it here for the moment --Robert Toomey
        GeometryBuilder gb = this.getGeometryBuilder();

        Vec4[] polyPoints = new Vec4[locations.size() + 1];
        Boolean[] polyEdgeFlags = new Boolean[locations.size() + 1];
        Matrix[] polyTransform = new Matrix[1];
        int polyCount = this.computeCartesianPolygon(dc, locations, edgeFlags, polyPoints, polyEdgeFlags,
                polyTransform);

        // System.out.println("polyCount is "+polyCount);

        // Compute the winding order of the planar cartesian points. If the order is not counter-clockwise, then
        // reverse the locations and points ordering.
        //      int winding = gb.computePolygonWindingOrder2(0, polyCount, polyPoints);
        //      if (winding != GeometryBuilder.COUNTER_CLOCKWISE)
        //      {
        //          gb.reversePoints(0, polyCount, polyPoints);
        //          gb.reversePoints(0, polyCount, polyEdgeFlags);
        //      }

        // Copy from polyVertices into polyPoints?  why???
        float[] polyVertices = new float[3 * polyCount];
        for (int i = 0; i < polyCount; i++) {
            int index = 3 * i;
            polyVertices[index] = (float) polyPoints[i].x;
            polyVertices[index + 1] = (float) polyPoints[i].y;
            polyVertices[index + 2] = (float) polyPoints[i].z;
        }

        int fillIndexCount = 0;
        int outlineIndexCount = 0;
        int vertexCount = 0;

        // GeometryBuilder.IndexedTriangleArray ita = null;
        fillIndexCount += this.getEdgeFillIndexCount(polyCount, subdivisions);
        outlineIndexCount += this.getEdgeOutlineIndexCount(polyCount, subdivisions, polyEdgeFlags);
        vertexCount += this.getEdgeVertexCount(polyCount, subdivisions);

        int[] fillIndices = new int[fillIndexCount];
        int[] outlineIndices = new int[outlineIndexCount];
        float[] vertices = new float[3 * vertexCount];
        //  float[] normals = new float[3 * vertexCount];

        // System.out.println("Vertex count is "+vertexCount+", polycount "+polyCount);

        int fillIndexPos = 0;
        int outlineIndexPos = 0;
        int vertexPos = 0;
        // int colorPos = 0;

        // make edge
        gb.setOrientation(GeometryBuilder.OUTSIDE);

        int sectionFillIndexCount = this.getSectionFillIndexCount(subdivisions);
        int sectionVertexCount = this.getSectionVertexCount(subdivisions);

        for (int i = 0; i < polyCount - 1; i++) {
            boolean beginEdgeFlag = polyEdgeFlags[i];
            boolean endEdgeFlag = polyEdgeFlags[i + 1];

            // Make section fill indices....
            int count = gb.getSubdivisionPointsVertexCount(subdivisions);

            int index = fillIndexPos;
            int pos, nextPos;
            for (int fill = 0; fill < count - 1; fill++) {
                pos = vertexPos + 2 * fill;
                nextPos = vertexPos + 2 * (fill + 1);
                fillIndices[index++] = pos + 1;
                fillIndices[index++] = pos;
                fillIndices[index++] = nextPos + 1;
                fillIndices[index++] = nextPos + 1;
                fillIndices[index++] = pos;
                fillIndices[index++] = nextPos;
            }
            // End Make section fill indices     

            // Make the fill vertices
            int numPoints = gb.getSubdivisionPointsVertexCount(subdivisions);

            Globe globe = dc.getGlobe();
            int index1 = 3 * i;
            int index2 = 3 * (i + 1);

            float[] locationVerts = new float[3 * numPoints];
            gb.makeSubdivisionPoints(
                    polyVertices[index1], polyVertices[index1 + 1], polyVertices[index1 + 2],
                    polyVertices[index2], polyVertices[index2 + 1], polyVertices[index2 + 2],
                    subdivisions, locationVerts);

            for (int p = 0; p < numPoints; p++) {
                int pindex = 3 * p;
                Vec4 vec = new Vec4(locationVerts[pindex], locationVerts[pindex + 1], locationVerts[pindex + 2]);
                vec = vec.transformBy4(polyTransform[0]);
                Position pos2 = globe.computePositionFromPoint(vec);

                for (int j = 0; j < 2; j++) {
                    vec = this.computePointFromPosition(dc, pos2.getLatitude(), pos2.getLongitude(), altitudes[j],
                            terrainConformant[j]);

                    pindex = 2 * p + j;
                    pindex = 3 * (vertexPos + pindex);
                    vertices[pindex] = (float) (vec.x);
                    vertices[pindex + 1] = (float) (vec.y);
                    vertices[pindex + 2] = (float) (vec.z);


                }

            }
            // end make section vertices

            // Outline the polys..this is lines from one to the other...
            this.makeSectionOutlineIndices(subdivisions, vertexPos, outlineIndexPos, outlineIndices,
                    beginEdgeFlag, endEdgeFlag);


            // Due we need normals for a vslice?  Probably not..we don't really want data colors changing since
            // it's a key.  Now with isosurfaces we might...
            //    gb.makeIndexedTriangleArrayNormals(fillIndexPos, sectionFillIndexCount, fillIndices,
            //        vertexPos, sectionVertexCount, vertices, normals);

            fillIndexPos += sectionFillIndexCount;
            outlineIndexPos += this.getSectionOutlineIndexCount(subdivisions, beginEdgeFlag, endEdgeFlag);
            vertexPos += sectionVertexCount;
        }

        // end make edge

        dest.getFillIndexGeometry().setElementData(GL.GL_TRIANGLES, fillIndexCount, fillIndices);
        dest.getOutlineIndexGeometry().setElementData(GL.GL_LINES, outlineIndexCount, outlineIndices);
        dest.getVertexGeometry().setVertexData(vertexCount, vertices);
        //  dest.getVertexGeometry().setNormalData(vertexCount, normals);

        // Generate the 3D VSlice in the window, and the 2D slice for charting...
        double startLat = leftBottom.getLatitude().getDegrees();
        double startLon = leftBottom.getLongitude().getDegrees();
        double endLat = rightBottom.getLatitude().getDegrees();
        double endLon = rightBottom.getLongitude().getDegrees();

        // The input data
        myCurrentGrid = new VolumeSliceInput(myNumRows, myNumCols, startLat, startLon,
                endLat, endLon, altitudes[0], altitudes[1], dc.getGlobe(), 0); //++myIterationCount);

        // Let the volume generate the 3D slice output
        myVolumeProduct.generateSlice3D(myCurrentGrid, dest, dc.getGlobe(), aList, true);
    }

    /** Our version of computePointFromPosition that doesn't make new objects and do tons of checks.
     * Meant to be called from the generate function only where we have already done all the safety checks.
     * Since this is called a zillion times during rendering any speed improvement here helps.
     * @param dc
     * @param latitude
     * @param longitude
     * @param elevation
     * @param terrainConformant
     * @return
     */
    protected Vec4 computePoint(Globe globe, Angle latitude, Angle longitude, double elevation,
            boolean terrainConformant) {
        //   double newElevation = elevation;

        //   if (terrainConformant)
        //   {
        //        newElevation += this.computeElevationAt(dc, latitude, longitude);
        //   }

        return globe.computePointFromPosition(latitude, longitude, elevation);
    }

    private int getEdgeFillIndexCount(int count, int subdivisions) {
        return (count - 1) * this.getSectionFillIndexCount(subdivisions);
    }

    private int getEdgeOutlineIndexCount(int count, int subdivisions, Boolean[] edgeFlags) {
        int sum = 0;
        for (int i = 0; i < count - 1; i++) {
            sum += this.getSectionOutlineIndexCount(subdivisions, edgeFlags[i], edgeFlags[i + 1]);
        }

        return sum;
    }

    private int getEdgeVertexCount(int count, int subdivisions) {
        return (count - 1) * this.getSectionVertexCount(subdivisions);
    }

    private int getSectionFillIndexCount(int subdivisions) {
        GeometryBuilder gb = this.getGeometryBuilder();
        return 6 * (gb.getSubdivisionPointsVertexCount(subdivisions) - 1);
    }

    private int getSectionOutlineIndexCount(int subdivisions, boolean beginEdgeFlag, boolean endEdgeFlag) {
        GeometryBuilder gb = this.getGeometryBuilder();
        int count = 4 * (gb.getSubdivisionPointsVertexCount(subdivisions) - 1);
        if (beginEdgeFlag) {
            count += 2;
        }
        if (endEdgeFlag) {
            count += 2;
        }

        return count;
    }

    private int getSectionVertexCount(int subdivisions) {
        GeometryBuilder gb = this.getGeometryBuilder();
        return 2 * gb.getSubdivisionPointsVertexCount(subdivisions);
    }

    private void makeSectionOutlineIndices(int subdivisions, int vertexPos, int indexPos, int[] indices,
            boolean beginEdgeFlag, boolean endEdgeFlag) {
        GeometryBuilder gb = this.getGeometryBuilder();
        int count = gb.getSubdivisionPointsVertexCount(subdivisions);

        int index = indexPos;
        int pos, nextPos;

        if (beginEdgeFlag) {
            pos = vertexPos;
            indices[index++] = pos;
            indices[index++] = pos + 1;
        }

        for (int i = 0; i < count - 1; i++) {
            pos = vertexPos + 2 * i;
            nextPos = vertexPos + 2 * (i + 1);
            indices[index++] = pos;
            indices[index++] = nextPos;
            indices[index++] = pos + 1;
            indices[index++] = nextPos + 1;
        }

        if (endEdgeFlag) {
            pos = vertexPos + 2 * (count - 1);
            indices[index++] = pos;
            indices[index] = pos + 1;
        }
    }
    //**************************************************************//
    //********************  END Geometry Rendering  ****************//
    //**************************************************************//
/*
    @Override
    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
    super.doGetRestorableState(rs, context);
    
    if (this.locations != null)
    rs.addStateValueAsLatLonList(context, "locations", this.locations);
    }
    
    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
    super.doRestoreState(rs, context);
    
    ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
    if (locations != null)
    this.setLocations(locations);
    }
     */
}
