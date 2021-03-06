package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.RadialUtil;
import org.wdssii.geom.Location;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;
import org.wdssii.gui.features.PolarGridFeature.PolarGridMemento;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.storage.GrowList;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 *
 * @author Robert Toomey
 *
 *         Render a PolarGrid, a collection of ranged circles around a center
 *         point FIXME: worried about polygonData lock being possibly called out
 *         of order from workerlock...hummm..check this.
 *
 */
public class PolarGridRenderer extends Feature3DRenderer {

	private final static Logger LOG = LoggerFactory.getLogger(PolarGridRenderer.class);
	/**
	 * Lock for changing offsets/polygonData
	 */
	private final Object drawLock = new Object(); // Lock 2 (second)
	/**
	 * Offset array, one per Polygon
	 *
	 * I'm allowing appending to this object by one thread while reading from
	 * another.
	 */
	private GrowList<Integer> myOffsets;
	/**
	 * Verts for the map. Single for now (monster maps may fail)
	 */
	protected Array1D<Float> polygonData;
	private GrowList<V3> myLabelPoints;
	private GrowList<String> myLabelStrings;
	/**
	 * Current memento settings
	 */
	private PolarGridMemento current;

	public PolarGridRenderer() {
	}

	/**
	 * The worker job if we are threading
	 */
	private BackgroundPolarGridMaker myWorker = null;
	private final Object workerLock = new Object(); // Lock 1 (first)
	private boolean myCreated = false;

	/**
	 * Do we have valid offsets/polygonData?
	 */
	public boolean isCreated() {
		return myCreated;
	}

	/**
	 * Set when we have valid offsets/polygonData.
	 */
	public void setIsCreated(boolean flag) {
		myCreated = flag;
	}

	@Override
	public void pick(GLWorld w, Point p, FeatureMemento m) {
	}

	@Override
	public void preRender(GLWorld w, FeatureMemento m) {
	}

	/**
	 * Job for creating in the background any rendering
	 */
	public static class BackgroundPolarGridMaker extends WdssiiJob {

		public GLWorld w;
		private PolarGridMemento m;
		private PolarGridRenderer myPolarGridRenderer;

		public BackgroundPolarGridMaker(String jobName, GLWorld aW, PolarGridRenderer r, PolarGridMemento p) {
			super(jobName);
			w = aW;
			myPolarGridRenderer = r;
			m = p;
		}

		@Override
		public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
			return create(w, m, myPolarGridRenderer, monitor);
		}

		/**
		 * Do the work of generating the OpenGL stuff
		 */
		public WdssiiJobStatus create(GLWorld w, PolarGridMemento p, PolarGridRenderer r, WdssiiJobMonitor monitor) {

			int idx = 0;

			Integer range = p.get(PolarGridMemento.RING_RANGE, 1000);
			Integer numCircles = p.get(PolarGridMemento.RING_COUNT, 20);
			float ringApart = range / 1000.0f;

			if (numCircles < 1) {
				numCircles = 1;
			} // HAVE to have at least one

			V2 center = null;
			center = p.get(PolarGridMemento.CENTER, center);
			Location ourCenter = new Location(center.x, center.y, 0);

			// if (ringApart > myMaxR) {
			// numCircles = 1;
			// } else {
			// numCircles = (int) (0.5 + ((float) myMaxR) / ringApart);
			// }
			Double elev = p.get(PolarGridMemento.ELEV_DEGREES, 50.0d);
			final double sinElevAngle = Math.sin(Math.toRadians(elev));
			final double cosElevAngle = Math.cos(Math.toRadians(elev));
			//final int numSegments = 10;

			Location l = new Location(35, 35, 10);
			//double circleHeightKms = 0;

			final int maxDegree = 360; // Make sure no remainder for numSegs...
			final int degStep = 6; // density of rings
			final int numSegs = maxDegree / degStep;

			// Where in circle the labels are drawn.
			int labelDegree = 90;

			final int numSpokes = 6;
			final int spokeStep = 360 / numSpokes;

			// Allocate memory...
			Array1D<Float> workPolygons = new Array1DfloatAsNodes((numCircles * (numSegs + 1) * 3) // Number of circle
																									// points
					+ (numSpokes * (numCircles + 1) * 3), // Number of
															// spoke
															// points
					0.0f);
			GrowList<Integer> workOffsets = new GrowList<Integer>();
			GrowList<V3> workLabelPoints = new GrowList<V3>();
			GrowList<String> workLabelStrings = new GrowList<String>();
			workOffsets.add(0); // Just one for now

			float rangeKMS = ringApart;

			// -----------------------------------------------------------------------
			// Ring pass...
			for (int c = 0; c < numCircles; c++) {

				for (int d = 0; d <= maxDegree; d += degStep) {
					double sinAzimuthRAD = Math.sin(Math.toRadians(d));
					double cosAzimuthRAD = Math.cos(Math.toRadians(d));
					RadialUtil.getAzRanElLocation(l, ourCenter, sinAzimuthRAD, cosAzimuthRAD, rangeKMS, sinElevAngle,
							cosElevAngle);
					V3 point = w.projectLLH(l.latDegrees(), l.lonDegrees(), l.getHeightKms() * 1000.0);
					workPolygons.set(idx++, (float) point.x);
					workPolygons.set(idx++, (float) point.y);
					workPolygons.set(idx++, (float) point.z);
				}
				workOffsets.add(idx);
				rangeKMS += ringApart;
				// Update our renderer once a ring...abort if we're stale
				if (!r.updateData(this, workOffsets, workPolygons, workLabelPoints, workLabelStrings, false)) {
					return WdssiiJobStatus.OK_STATUS;
				}
				;
			}

			// -----------------------------------------------------------------------
			// Spoke pass...

			// Calculate the absolute center, shared by each spoke....
			// Vec4 cpoint = myGlobe.computePointFromPosition(
			// Angle.fromDegrees(ourCenter.getLatitude()),
			// Angle.fromDegrees(ourCenter.getLongitude()),
			// ourCenter.getHeightKms() * 1000.0);

			V3 cpoint = w.projectLLH(ourCenter.latDegrees(), ourCenter.lonDegrees(),
					ourCenter.getHeightKms() * 1000.0);
			// For each step in spoke degrees....
			int spokeAngle = 0;
			for (int d = 0; d < numSpokes; d += 1) {
				double sinAzimuthRAD = Math.sin(Math.toRadians(spokeAngle));
				double cosAzimuthRAD = Math.cos(Math.toRadians(spokeAngle));

				// Add center... note we have to have at least ONE circle
				// to complete the linestrip
				workPolygons.set(idx++, (float) cpoint.x);
				workPolygons.set(idx++, (float) cpoint.y);
				workPolygons.set(idx++, (float) cpoint.z);
				rangeKMS = ringApart;
				for (int c = 0; c < numCircles; c++) {
					RadialUtil.getAzRanElLocation(l, ourCenter, sinAzimuthRAD, cosAzimuthRAD, rangeKMS, sinElevAngle,
							cosElevAngle);
					// Vec4 point = myGlobe.computePointFromPosition(
					// Angle.fromDegrees(l.getLatitude()),
					// Angle.fromDegrees(l.getLongitude()),
					// l.getHeightKms() * 1000.0);
					V3 point = w.projectLLH(l.latDegrees(), l.lonDegrees(), l.getHeightKms() * 1000.0);
					workPolygons.set(idx++, (float) point.x);
					workPolygons.set(idx++, (float) point.y);
					workPolygons.set(idx++, (float) point.z);
					rangeKMS += ringApart;
				}
				workOffsets.add(idx);
				spokeAngle += spokeStep;
				// Update our renderer once a spoke...abort if we're stale
				if (!r.updateData(this, workOffsets, workPolygons, workLabelPoints, workLabelStrings, false)) {
					return WdssiiJobStatus.OK_STATUS;
				}
			}

			// -----------------------------------------------------------------------
			// Label pass
			// Draw priority with our rectangles is outside first
			rangeKMS = ringApart;
			for (int c = numCircles; c > 0; c--) {
				double sinAzimuthRAD = Math.sin(Math.toRadians(labelDegree));
				double cosAzimuthRAD = Math.cos(Math.toRadians(labelDegree));
				RadialUtil.getAzRanElLocation(l, ourCenter, sinAzimuthRAD, cosAzimuthRAD, rangeKMS, sinElevAngle,
						cosElevAngle);

				V3 point = w.projectLLH(l.latDegrees(), l.lonDegrees(), l.getHeightKms() * 1000.0);

				// Add STRING first, draw thread will use size of points to
				// render,
				// so we need to make sure string is available
				// FIXME: need more intelligent labeling for low values...
				workLabelStrings.add(String.format("%d Km", (int) (rangeKMS)));
				workLabelPoints.add(point);
				rangeKMS += ringApart;
			}

			// Final update...
			r.updateData(this, workOffsets, workPolygons, workLabelPoints, workLabelStrings, true);
			return WdssiiJobStatus.OK_STATUS;
		}
	}

	/**
	 * Tell if this changes requires a new background job. Some changes, like line
	 * thickness are done by the renderer on the fly
	 */
	public boolean changeNeedsUpdate(PolarGridMemento new1, PolarGridMemento old) {
		boolean needsUpdate = false;
		if (current == null) { // No settings yet...definitely update
			return true;
		}
		// If rings change we need a new worker...
		Integer newRing = new1.get(PolarGridMemento.RING_COUNT, 20);
		Integer oldRing = old.get(PolarGridMemento.RING_COUNT, 20);
		if (newRing != oldRing) {
			needsUpdate = true;
		}
		Integer newRange = new1.get(PolarGridMemento.RING_RANGE, 1000);
		Integer oldRange = old.get(PolarGridMemento.RING_RANGE, 1000);
		if (newRange != oldRange) {
			needsUpdate = true;
		}

		// Follow the top selected RadialSet product. Could make this a setting
		// If following top, we should hide our visibility when there isn't a
		// top
		// selected RadialSet...
		String useKey = ProductManager.TOP_PRODUCT;
		ProductFeature tph = ProductManager.getInstance().getProductFeature(useKey);
		Product prod = null;
		if (tph != null) {
			prod = tph.getProduct();
			if (prod != null) {
				DataType d = prod.getRawDataType();
				if (d instanceof PPIRadialSet) {
					PPIRadialSet radial = (PPIRadialSet) (d);
					Location l = radial.getLocation();
					double latDegs = l.latDegrees();
					double lonDegs = l.lonDegrees();
					// Why am I mixing worldwind LatLon with wdssii location?
					V2 ll = null;
					ll = old.get(PolarGridMemento.CENTER, ll);
					double latOldDegs = ll.x;
					double lonOldDegs = ll.y;

					// Round off if V2 is float for example causes bad match
					// ((latOldDegs != latDegs) || (lonOldDegs != lonDegs))
					if ((Math.abs(latOldDegs - latDegs) > 0.01) || // close enough
							(Math.abs(lonOldDegs - lonDegs) > 0.01)) {
						LOG.error("DEGREE CHANGE " + latOldDegs + ", " + latDegs + ", " + lonOldDegs + ", " + lonDegs);

						needsUpdate = true;
						V2 aLatLon = new V2(latDegs, lonDegs);
						new1.setProperty(PolarGridMemento.CENTER, aLatLon);
					}

					Double oldElev = old.get(PolarGridMemento.ELEV_DEGREES, 50.0d);
					double newElev = radial.getFixedAngleDegs();
					if (oldElev != newElev) {
						needsUpdate = true;
						new1.setProperty(PolarGridMemento.ELEV_DEGREES, newElev);
					}
				}
			}
		}
		return needsUpdate;
	}

	/**
	 * Draw the product in the current dc. FIXME: Shared code with map renderer
	 * right now.....do we merge some of these classes or make util functions?
	 */
	@Override
	public void draw(GLWorld w, FeatureMemento mf) {

		PolarGridMemento m = (PolarGridMemento) (mf);
		// Regenerate if memento is different...
		if (changeNeedsUpdate(m, current)) {
			// Have to make copy of information for background job
			current = new PolarGridMemento(m); // Keep new settings...
			PolarGridMemento aCopy = new PolarGridMemento(m);

			// Change out workers only in workerLock
			synchronized (workerLock) {
				if (myWorker != null) {
					myWorker.cancel(); // doesn't matter really
				}
				myWorker = new BackgroundPolarGridMaker("Job", w, this, aCopy);
				myWorker.schedule();
			}

		}

		synchronized (drawLock) {
			if (isCreated() && (polygonData != null)) {
				final GL glold = w.gl;
				final GL2 gl = glold.getGL().getGL2();
				Color line = m.get(PolarGridMemento.LINE_COLOR, Color.WHITE);
				final float r = line.getRed() / 255.0f;
				final float g = line.getGreen() / 255.0f;
				final float b = line.getBlue() / 255.0f;
				final float a = line.getAlpha() / 255.0f;
				boolean attribsPushed = false;
				boolean statePushed = false;
				try {
					Object lock1 = polygonData.getBufferLock();
					synchronized (lock1) {

						gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL2.GL_LIGHTING_BIT | GL.GL_COLOR_BUFFER_BIT
								| GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_TRANSFORM_BIT | GL2.GL_VIEWPORT_BIT
								| GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT);
						attribsPushed = true;

						gl.glDisable(GL2.GL_LIGHTING);
						gl.glDisable(GL.GL_TEXTURE_2D);
						gl.glDisable(GL.GL_DEPTH_TEST);
						gl.glShadeModel(GL2.GL_FLAT);
						gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT | GL2.GL_CLIENT_PIXEL_STORE_BIT);
						gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
						// gl.glEnableClientState(GL.GL_COLOR_ARRAY);

						// Smooth?
						gl.glEnable(GL.GL_LINE_SMOOTH);
						gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
						gl.glEnable(GL.GL_BLEND);
						gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

						statePushed = true;
						FloatBuffer z = polygonData.getRawBuffer();
						gl.glColor4f(r, g, b, a);
						Integer t = m.get(PolarGridMemento.LINE_THICKNESS, 1);
						gl.glLineWidth(t);
						GLUtil.renderArrays(w.gl, z, myOffsets, GL.GL_LINE_STRIP);

					}
				} finally {
					if (statePushed) {
						gl.glPopClientAttrib();
					}
					if (attribsPushed) {
						gl.glPopAttrib();
					}
				}

				// Pre non-opengl first...
				TextRenderer aText = null;
				Font font = new Font("Arial", Font.PLAIN, 14);
				if (aText == null) {
					aText = new TextRenderer(font, true, true);
				}
				// Globe myGlobe = dc.getGlobe();
				Rectangle2DIntersector i = new Rectangle2DIntersector();
				// Get the iterator for the object updated LAST
				Iterator<V3> points = myLabelPoints.iterator();
				Iterator<String> strings = myLabelStrings.iterator();
				// final View myView = dc.getView();

				GLUtil.pushOrtho2D(w);
				aText.begin3DRendering();
				while (strings.hasNext()) {
					final V3 v = points.next();
					final String l = strings.next();
					// final V2 s = myView.project(v);
					final V2 s = w.project(v);
					Rectangle2D bounds = aText.getBounds(l);
					bounds.setRect(bounds.getX() + s.x, bounds.getY() + s.y, bounds.getWidth(), bounds.getHeight());
					if (!i.intersectsAdd(bounds)) {
						GLUtil.cheezyOutline(aText, l, Color.WHITE, Color.BLACK, (int) s.x, (int) s.y);
					}
				}
				aText.end3DRendering();
				GLUtil.popOrtho2D(w.gl);
			}
		}
	}

	/**
	 * Simple rectangle collection intersection. Not optimized, this could be better
	 * with an interval binary search tree....
	 */
	public static class Rectangle2DIntersector {

		ArrayList<Rectangle2D> myRects = new ArrayList<Rectangle2D>();

		/**
		 * Return if intersects. Add to collection
		 */
		public boolean intersectsAdd(Rectangle2D newOne) {
			boolean hits = false;
			for (Rectangle2D r : myRects) { // O(N)
				if (r.intersects(newOne)) {
					hits = true;
					break;
				}
			}
			// Only add if it DOESN'T intersect.
			if (!hits) {
				myRects.add(newOne);
			}
			return hits;
		}
	}

	/**
	 * Pick an object in the current dc at point
	 */
	public void doPick(GLWorld w, java.awt.Point pickPoint) {
	}

	/**
	 * Update our data to the data of a worker. Note because of threads for brief
	 * time periods more than one worker might be going. (Fast changing of
	 * settings). The worker will stop on false
	 */
	public boolean updateData(BackgroundPolarGridMaker worker, GrowList<Integer> off, Array1D<Float> poly,
			GrowList<V3> points, GrowList<String> labels, boolean done) {

		// WorkerLock --> drawLock. Never switch order
		boolean keepWorking;
		synchronized (workerLock) {
			// See if worker changed..if so
			if (worker == myWorker) {

				synchronized (drawLock) {
					myOffsets = off;
					polygonData = poly;
					myLabelPoints = points;
					myLabelStrings = labels;
					setIsCreated(true);
				}
				keepWorking = true;
			} else {
				// Old worker, stop...
				keepWorking = false;
			}
		}
		FeatureList.getFeatureList().updateOnMinTime();
		return keepWorking;
	}
	
	
    @Override
    public FeatureRenderer.Level getDrawRank(){
        return FeatureRenderer.Level.LINE3D;  // Draw over others
    }
}
