package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.GeometryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.VolumeSlice3DOutput;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.worldwind.WorldwindUtil;

/**
 * A copy of worldwind Polygon, with modifications to allow us to render the vslice
 * Would be nice to subclass polygon, but worldwind currently has locked down too much of the renderering
 * code.  The only thing we really want from Polygon is all the nice GUI control interface stuff.
 * Hummm...maybe we can override polygon and simply replace the full doRender
 * FIXME:  merge back to polygon subclass if able.
 */
public class LLHAreaSlice extends LLHArea {

	private static Logger log = LoggerFactory.getLogger(LLHAreaSlice.class);

	/** Change to pass onto the LLHArea.  All fields common to
	LLHArea are here
	 */
	public static class LLHAreaSliceMemento extends LLHAreaMemento {

		private double leftLatDegrees;
		private boolean useLeftLatDegrees = false;
		private double leftLonDegrees;
		private boolean useLeftLonDegrees = false;
		private double rightLatDegrees;
		private boolean useRightLatDegrees = false;
		private double rightLonDegrees;
		private boolean useRightLonDegrees = false;

		public LLHAreaSliceMemento(LLHAreaSlice a) {
			super(a);
			final LatLon left = a.getLeftLocation();
			final LatLon right = a.getRightLocation();
			leftLatDegrees = left.getLatitude().degrees;
			leftLonDegrees = left.getLongitude().degrees;
			rightLatDegrees = right.getLatitude().degrees;
			rightLonDegrees = right.getLongitude().degrees;
		}

		public double getLeftLatDegrees() {
			return leftLatDegrees;
		}

		public void setLeftLatDegrees(double lld) {
			useLeftLatDegrees = true;
			leftLatDegrees = lld;
		}

		public double getLeftLonDegrees() {
			return leftLonDegrees;
		}

		public void setLeftLonDegrees(double lld) {
			useLeftLonDegrees = true;
			leftLonDegrees = lld;
		}

		public double getRightLatDegrees() {
			return rightLatDegrees;
		}

		public void setRightLatDegrees(double rld) {
			useRightLatDegrees = true;
			rightLatDegrees = rld;
		}

		public double getRightLonDegrees() {
			return rightLonDegrees;
		}

		public void setRightLonDegrees(double rld) {
			useRightLonDegrees = true;
			rightLonDegrees = rld;
		}
	}
	/** The number of rows or altitudes of the VSlice */
	public static final int myNumRows = 50;  //50
	/** The number of cols or change in Lat/Lon */
	public static final int myNumCols = 100; //100
	/** Holder for the slice GIS 'state' */
	private VolumeSliceInput myCurrentGrid =
		new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
		0, 0, 0, 50);
	private LatLon myLeftLocation;
	private LatLon myRightLocation;
	/** How many times to 'split' the raw fill or outline of slice.  This
	 * isn't the same as the vslice resolution.
	 */
	private int subdivisions = 4;  // power of 2 breakdown of side..
	private VSliceRenderer myRenderer = new VSliceRenderer();
	private ProductVolume myVolumeProduct = null;
	private VolumeSlice3DOutput myGeometry = new VolumeSlice3DOutput();
	private String myCacheKey = "";

	/** Get the memento for this class */
	@Override
	public LLHAreaSliceMemento getMemento() {
		return new LLHAreaSliceMemento(this);
	}

	@Override
	public void setFromMemento(LLHAreaMemento l) {
		super.setFromMemento(l);
		if (l instanceof LLHAreaSliceMemento) {
			LLHAreaSliceMemento ls = (LLHAreaSliceMemento) (l);
			setFromMemento(ls);
		}
	}

	/** Not overridden */
	protected void setFromMemento(LLHAreaSliceMemento l) {

		boolean updateLocations = false;
		LatLon newLeft = myLeftLocation;
		LatLon newRight = myRightLocation;

		if (l.useLeftLatDegrees || l.useLeftLonDegrees) {
			double newLeftLat = myLeftLocation.latitude.degrees;
			double newLeftLon = myLeftLocation.longitude.degrees;
			if (l.useLeftLatDegrees) {
				newLeftLat = l.leftLatDegrees;
			}
			if (l.useLeftLonDegrees) {
				newLeftLon = l.leftLonDegrees;
			}
			newLeft = new LatLon(
				Angle.fromDegrees(newLeftLat),
				Angle.fromDegrees(newLeftLon));
			updateLocations = true;
		}

		if (l.useRightLatDegrees || l.useRightLonDegrees) {
			double newRightLat = myRightLocation.latitude.degrees;
			double newRightLon = myRightLocation.longitude.degrees;
			if (l.useRightLatDegrees) {
				newRightLat = l.rightLatDegrees;
			}
			if (l.useRightLonDegrees) {
				newRightLon = l.rightLonDegrees;
			}
			newRight = new LatLon(
				Angle.fromDegrees(newRightLat),
				Angle.fromDegrees(newRightLon));
			updateLocations = true;
		}

		if (updateLocations) {
			List<LatLon> newLocations = new ArrayList<LatLon>();
			newLocations.add(newLeft);
			newLocations.add(newRight);

			// Validate locations here in case user puts in some crazy
			// values...?  FIXME
			//System.out.println("SET to "+newLeft+", "+newRight);
			this.setLocations(newLocations);
		}
	}

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

	public double getRangeKms() {
		// FIXME: cleaner way of this?....fetch radius of current globe..
		double radius = CommandManager.getInstance().getEarthBall().getWwd().getModel().getGlobe().getRadius();
		double length = LatLon.greatCircleDistance(myLeftLocation, myRightLocation).radians * radius;
		return length;
	}

	public double getHeightKms() {
		// Get the range of this vslice in Kms...
		// double[] altitudes = this.getAltitudes(dc.getVerticalExaggeration());
		// double bottomHeight = myAltitude0;  // Remembered from last draw...FIXME.
		// double topHeight = myAltitude1;
		double bottomHeight = myCurrentGrid.bottomHeight;
		double topHeight = myCurrentGrid.topHeight;
		return topHeight - bottomHeight;
	}

	public LLHAreaSlice(LLHAreaFeature f) {
		super(f);
		ProductVolume volume = ProductManager.getCurrentVolumeProduct(myProductFollow, getUseVirtualVolume());
		myVolumeProduct = volume;
	}

	//**************************************************************//
	//********************  Geometry Rendering  ********************//
	//**************************************************************//
	protected Vec4 computeReferenceCenter(DrawContext dc) {
		Extent extent = this.getExtent(dc);
		return extent != null ? extent.getCenter() : null;
	}

	/** This is what we have to modify, replace...right here.....
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
		myCurrentGrid.bottomHeight = altitudes[0];
		myCurrentGrid.topHeight = altitudes[1];

		GL gl = dc.getGL();

		if (drawStyle.equals("fill")) {

			// Shouldn't this code be in the renderer???
			gl.glPushAttrib(GL.GL_POLYGON_BIT);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glFrontFace(GL.GL_CCW);

			this.drawVSlice(dc, locations, edgeFlags);

			gl.glPopAttrib();

		}
	}

	protected int computeCartesianPolygon(DrawContext dc, List<? extends LatLon> locations, List<Boolean> edgeFlags,
		Vec4[] points, Boolean[] edgeFlagArray, Matrix[] transform) {
		Globe globe = dc.getGlobe();

		// Allocate space to hold the list of locations and location vertices.
		int locationCount = locations.size();

		// Compute the cartesian points for each location.
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
		List<LatLon> locations = getLocationList();
		for (int i = 0; i < locations.size(); i++) {
			LatLon l = locations.get(i);
			newKey = newKey + l.getLatitude() + ":";
			newKey = newKey + l.getLongitude() + ":";
		}
		newKey = newKey + myCurrentGrid.bottomHeight;
		newKey = newKey + myCurrentGrid.topHeight;
		return newKey;
	}

	/** Get a pretty GIS label for rendering in a chart, for instance */
	public String getGISLabel() {
		LatLon left = getLeftLocation();
		LatLon right = getRightLocation();
		return getGISLabel(left.getLatitude().degrees,
			left.getLongitude().degrees,
			right.getLatitude().degrees,
			right.getLongitude().degrees);
	}

	public String getGISLabel(double startLat, double startLon, double endLat, double endLong) {
		String newKey = String.format(
			"(%5.2f, %5.2f)                        (%5.2f, %5.2f)",
			startLat, startLon, endLat, endLong);
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
		FilterList f = ProductManager.getInstance().getFilterList(getProductFollow());

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
	private VolumeSlice3DOutput getVSliceGeometry(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags) {
		String newKey = getNewCacheKey();
		// Add exaggeration to cache key so changing exaggeration will redraw it
		newKey += dc.getVerticalExaggeration();
		if (newKey.compareTo(myCacheKey) == 0) {
			return myGeometry;
		}

		// System.out.println("_------------>>> REGENERATE VSLICE!!!");
		myCacheKey = newKey;
		myGeometry.setHaveVSliceData(false);
		this.makeVSlice(dc, locations, edgeFlags, myGeometry);

		// Fire changed event?  Is this enough?
		CommandManager.getInstance().executeCommand(new FeatureCommand(), true);
		return myGeometry;
	}

	/** New routine, draw a vslice  */
	private void drawVSlice(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags) {
		VolumeSlice3DOutput geom = this.getVSliceGeometry(dc, locations, edgeFlags);

		myRenderer.drawVSlice(dc, geom);

	}

	protected void orderLocations(List<LatLon> input) {
		// VSlice only.  Two locations, the points on the bottom. 
		// Make sure the east one is right of the west one...
		LatLon l1 = input.get(0);
		LatLon l2 = input.get(1);
		LatLon leftBottom, rightBottom;
		if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
			leftBottom = l1;
			rightBottom = l2;
		} else {
			leftBottom = l2;
			rightBottom = l1;
		}
		myLeftLocation = leftBottom;
		myRightLocation = rightBottom;
	}

	public LatLon getLeftLocation() {
		return myLeftLocation;
	}

	public LatLon getRightLocation() {
		return myRightLocation;
	}

	public double getBottomHeightKms() {
		return myCurrentGrid.bottomHeight;
	}

	public double getTopHeightKms() {
		return upperAltitude;
	}

	private void makeVSlice(DrawContext dc, List<LatLon> locations, List<Boolean> edgeFlags,
		VolumeSlice3DOutput dest) {
		if (locations.isEmpty()) {
			return;
		}

		// For dynamic sizing outlines...I might need this code for 'smart' legend over vslice, so
		// I'm leaving it here for the moment --Robert Toomey
		GeometryBuilder gb = this.getGeometryBuilder();

		Vec4[] polyPoints = new Vec4[locations.size() + 1];
		Boolean[] polyEdgeFlags = new Boolean[locations.size() + 1];
		Matrix[] polyTransform = new Matrix[1];
		int polyCount = this.computeCartesianPolygon(dc, locations, edgeFlags, polyPoints, polyEdgeFlags,
			polyTransform);

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

		int fillIndexPos = 0;
		int outlineIndexPos = 0;
		int vertexPos = 0;

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
			double altitudes[] = new double[2];
			altitudes[0] = myCurrentGrid.bottomHeight;
			altitudes[1] = myCurrentGrid.topHeight;
			double vert = dc.getVerticalExaggeration();
			for (int p = 0; p < numPoints; p++) {
				int pindex = 3 * p;
				Vec4 vec = new Vec4(locationVerts[pindex], locationVerts[pindex + 1], locationVerts[pindex + 2]);
				vec = vec.transformBy4(polyTransform[0]);
				Position pos2 = globe.computePositionFromPoint(vec);

				for (int j = 0; j < 2; j++) {
					// vec = this.computePointFromPosition(dc, pos2.getLatitude(), pos2.getLongitude(), altitudes[j],
					//        terrainConformant[j]);
					vec = globe.computePointFromPosition(pos2.getLatitude(), pos2.getLongitude(), altitudes[j] * vert);

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

		// Volume VSlice color generation....
		ProductVolume volume = ProductManager.getCurrentVolumeProduct(getProductFollow(), getUseVirtualVolume());
		myVolumeProduct = volume;

		// Get the filter list and the record object
		FilterList aList = null;
		ProductFeature pf = ProductManager.getInstance().getTopProductFeature();
		if (pf != null) {
			aList = pf.getFList();
		}

		if (aList == null) {
			return;
		}
		aList.prepForVolume(volume);

		// Generate the 3D VSlice in the window, and the 2D slice for charting...
		myCurrentGrid.set(myNumRows, myNumCols, myCurrentGrid.startLat, myCurrentGrid.startLon,
			myCurrentGrid.endLat, myCurrentGrid.endLon,
			myCurrentGrid.bottomHeight, myCurrentGrid.topHeight);
		// Let the volume generate the 3D slice output
		// FIXME: Need to be able to change volumevalue
		myVolumeProduct.generateSlice3D(myCurrentGrid, dest, dc.getGlobe(), aList, true, dc.getVerticalExaggeration(), null);
	}

	/** Update the current grid that is the GIS location of the slice */
	@Override
	public void updateCurrentGrid() {

		// Generate the 3D VSlice in the window, and the 2D slice for charting...
		orderLocations(getLocationList());
		double startLat = myLeftLocation.getLatitude().getDegrees();
		double startLon = myLeftLocation.getLongitude().getDegrees();
		double endLat = myRightLocation.getLatitude().getDegrees();
		double endLon = myRightLocation.getLongitude().getDegrees();

		myCurrentGrid.startLat = startLat;
		myCurrentGrid.startLon = startLon;
		myCurrentGrid.endLat = endLat;
		myCurrentGrid.endLon = endLon;
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

	/** The default location for a newly created LLHArea */
	@Override
	protected List<LatLon> getDefaultLocations(WorldWindow wwd) {

		// Taken from worldwind...we'll need to figure out how we want the vslice/isosurface to work...
		Position position = WorldwindUtil.getNewShapePosition(wwd);
		Angle heading = WorldwindUtil.getNewShapeHeading(wwd, true);

		/** Create based on viewport. */
		Globe globe = wwd.getModel().getGlobe();
		Matrix transform = Matrix.IDENTITY;
		transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
		transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));
		double sizeInMeters = DEFAULT_LENGTH_METERS;
		double widthOver2 = sizeInMeters / 2.0;
		double heightOver2 = sizeInMeters / 2.0;

		Vec4[] points = new Vec4[]{
			new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
			// new Vec4(widthOver2,  -heightOver2, 0.0).transformBy4(transform), // lower right
			new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
		// new Vec4(-widthOver2,  heightOver2, 0.0).transformBy4(transform)  // upper left
		};

		/** Convert from vector model coordinates to LatLon */
		LatLon[] locations = new LatLon[points.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
		}
		return Arrays.asList(locations);
	}
}