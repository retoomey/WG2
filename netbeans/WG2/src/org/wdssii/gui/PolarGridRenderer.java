package org.wdssii.gui;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.geom.Location;
import org.wdssii.gui.features.PolarGridFeature.PolarGridMemento;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.storage.GrowList;
import org.wdssii.util.RadialUtil;

/**
 *
 * @author Robert Toomey
 *
 * Render a PolarGrid, a collection of ranged circles around a center point
 *
 */
public class PolarGridRenderer {

	private static Logger log = LoggerFactory.getLogger(PolarGridRenderer.class);
	/**
	 * Lock for changing offsets/polygonData
	 */
	private final Object drawLock = new Object(); // Lock 2 (second)
	/**
	 * Offset array, one per Polygon
	 * 
	 * I'm allowing appending to this object by one thread while reading
	 * from another.  
	 */
	private ArrayList<Integer> myOffsets;
	/**
	 * Verts for the map. Single for now (monster maps may fail)
	 */
	protected Array1Dfloat polygonData;
	private GrowList<Vec4> myLabelPoints;
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
	/**
	 * Volatile because renderer job creates the data in one thread, but
	 * drawn in another. myCreated set to true by worker thread after the
	 * buffers are created (draw allowed)
	 */
	private volatile boolean myCreated = false;

	/**
	 * Do we have valid offsets/polygonData?
	 */
	public synchronized boolean isCreated() {
		return myCreated;
	}

	/**
	 * Set when we have valid offsets/polygonData.
	 */
	public synchronized void setIsCreated(boolean flag) {
		myCreated = flag;
	}

	/**
	 * Job for creating in the background any rendering
	 */
	public static class BackgroundPolarGridMaker extends WdssiiJob {

		public DrawContext dc;
		private PolarGridMemento m;
		private PolarGridRenderer myPolarGridRenderer;

		public BackgroundPolarGridMaker(String jobName, DrawContext aDc, PolarGridRenderer r, PolarGridMemento p) {
			super(jobName);
			dc = aDc;
			myPolarGridRenderer = r;
			m = p;
		}

		@Override
		public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
			return create(dc, m, myPolarGridRenderer, monitor);
		}

		/**
		 * Do the work of generating the OpenGL stuff
		 */
		public WdssiiJobStatus create(DrawContext dc, PolarGridMemento p,
			PolarGridRenderer r, WdssiiJobMonitor monitor) {

			Globe myGlobe = dc.getGlobe();
			int idx = 0;
			Location ourCenter = new Location(p.getCenter().getLatitude().degrees,
				p.getCenter().getLongitude().degrees, 0);

			//	int myMaxR = 500; // firstgate+(numgates*gatewidth)....for a radialset
			int ringApart = 50;
			//		int numCircles;
			int numCircles = p.getNumRings();

//			if (ringApart > myMaxR) {
//				numCircles = 1;
//			} else {
//				numCircles = (int) (0.5 + ((float) myMaxR) / ringApart);
//			}
			final double sinElevAngle = Math.sin(Math.toRadians(p.getElevDegrees()));
			final double cosElevAngle = Math.cos(Math.toRadians(p.getElevDegrees()));
			final int numSegments = 10;

			Location l = new Location(35, 35, 10);
			double circleHeightKms = 0;

			int maxDegree = 360; // Make sure no remainder for numSegs...
			int degStep = 6;
			int numSegs = maxDegree / degStep;
			int labelDegree = 90;

			// Allocate memory...
			Array1Dfloat workPolygons = new Array1DfloatAsNodes(numCircles * numSegs * 3, 0.0f);
			ArrayList<Integer> workOffsets = new ArrayList<Integer>();
			GrowList<Vec4> workLabelPoints = new GrowList<Vec4>();
			GrowList<String> workLabelStrings = new GrowList<String>();
			workOffsets.add(0);  // Just one for now

			int rangeMeters = ringApart;

			// Ring pass...
			for (int c = 0; c < numCircles; c++) {

				for (int d = 0; d < maxDegree; d += degStep) {
					double sinAzimuthRAD = Math.sin(Math.toRadians(d));
					double cosAzimuthRAD = Math.cos(Math.toRadians(d));
					RadialUtil.getAzRanElLocation(l, ourCenter,
						sinAzimuthRAD, cosAzimuthRAD, rangeMeters,
						sinElevAngle, cosElevAngle);
					Vec4 point = myGlobe.computePointFromPosition(
						Angle.fromDegrees(l.getLatitude()),
						Angle.fromDegrees(l.getLongitude()),
						l.getHeightKms() * 1000.0);
					workPolygons.set(idx++, (float) point.x);
					workPolygons.set(idx++, (float) point.y);
					workPolygons.set(idx++, (float) point.z);
				}
				workOffsets.add(idx);
				rangeMeters += ringApart;
				// Update our renderer once a ring...abort if we're stale
				if (!r.updateData(this, workOffsets, workPolygons, workLabelPoints, workLabelStrings, false)) {
					return WdssiiJobStatus.OK_STATUS;
				};
			}


			// Do labels with second pass.....
			// Draw priority with our rectangles is outside first 
			for (int c = numCircles; c > 0; c--) {
					double sinAzimuthRAD = Math.sin(Math.toRadians(labelDegree));
					double cosAzimuthRAD = Math.cos(Math.toRadians(labelDegree));
					RadialUtil.getAzRanElLocation(l, ourCenter,
						sinAzimuthRAD, cosAzimuthRAD, rangeMeters,
						sinElevAngle, cosElevAngle);
					Vec4 point = myGlobe.computePointFromPosition(
						Angle.fromDegrees(l.getLatitude()),
						Angle.fromDegrees(l.getLongitude()),
						l.getHeightKms() * 1000.0);

					// Add STRING first, draw thread will use size of points to render,
					// so we need to make sure string is available
						workLabelStrings.add(String.format("%d Km", rangeMeters));
						workLabelPoints.add(point);
			}


			// Final update...
			r.updateData(this, workOffsets, workPolygons, workLabelPoints, workLabelStrings, true);
			return WdssiiJobStatus.OK_STATUS;
		}
	}

	/**
	 * Tell if this changes requires a new background job. Some changes,
	 * like line thickness are done by the renderer on the fly
	 */
	public boolean changeNeedsUpdate(PolarGridMemento m1, PolarGridMemento m2) {
		boolean needsUpdate = false;
		if (current == null) { // No settings yet...definitely update
			return true;
		}
		// If rings change we need a new worker...
		if (m1.getNumRings() != m2.getNumRings()) {
			needsUpdate = true;
		}
		return needsUpdate;
	}

	/**
	 * Draw the product in the current dc. FIXME: Shared code with map
	 * renderer right now.....do we merge some of these classes or make util
	 * functions?
	 */
	public void draw(DrawContext dc, PolarGridMemento m) {

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
				myWorker = new BackgroundPolarGridMaker("Job", dc, this, aCopy);
				myWorker.schedule();
			}

		}

		synchronized (drawLock) {
			if (isCreated() && (polygonData != null)) {
				GL gl = dc.getGL();
				Color line = m.getLineColor();
				final float r = line.getRed() / 255.0f;
				final float g = line.getGreen() / 255.0f;
				final float b = line.getBlue() / 255.0f;
				final float a = line.getAlpha() / 255.0f;
				boolean attribsPushed = false;
				boolean statePushed = false;
				try {
					Object lock1 = polygonData.getBufferLock();
					synchronized (lock1) {

						gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
							| GL.GL_COLOR_BUFFER_BIT
							| GL.GL_ENABLE_BIT
							| GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
							| GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT
							| GL.GL_LINE_BIT);
						attribsPushed = true;

						gl.glDisable(GL.GL_LIGHTING);
						gl.glDisable(GL.GL_TEXTURE_2D);
						gl.glDisable(GL.GL_DEPTH_TEST);
						gl.glShadeModel(GL.GL_FLAT);
						gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
							| GL.GL_CLIENT_PIXEL_STORE_BIT);
						gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
						// gl.glEnableClientState(GL.GL_COLOR_ARRAY);
						statePushed = true;
						FloatBuffer z = polygonData.getRawBuffer();

						// Only render if there is data to render
						if ((z != null) && (z.capacity() > 0)) {
							gl.glColor4f(r, g, b, a);
							gl.glLineWidth(m.getLineThickness());
							gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());
							int size = myOffsets.size();
							if (size > 1) {
								for (int i = 0; i < size - 1; i++) {
									int start_index = myOffsets.get(i);
									int end_index = myOffsets.get(i + 1);
									int run_indices = end_index - start_index;
									int start_vertex = start_index / 3;
									int run_vertices = run_indices / 3;
									gl.glDrawArrays(GL.GL_LINE_LOOP, start_vertex,
										run_vertices);
								}
							}

						}

					}
				} finally {
					if (statePushed) {
						gl.glPopClientAttrib();
					}
					if (attribsPushed) {
						gl.glPopAttrib();
					}
				}

				boolean projectionPushed = false;
				boolean modelviewPushed = false;
				try {

					// System.out.println("Drawing color key layer");
					gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
						| GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
						| GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
						| GL.GL_CURRENT_BIT);
					attribsPushed = true;
					gl.glEnable(GL.GL_BLEND);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
					gl.glDisable(GL.GL_DEPTH_TEST);

					View myView = dc.getView();
					// Standard ortho projection
					int aViewWidth = myView.getViewport().width;
					int aViewHeight = myView.getViewport().height;

					gl.glMatrixMode(GL.GL_PROJECTION);
					gl.glPushMatrix();
					projectionPushed = true;
					gl.glLoadIdentity();
					gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1);  // TopLeft
					gl.glMatrixMode(GL.GL_MODELVIEW);
					gl.glPushMatrix();
					modelviewPushed = true;
					gl.glLoadIdentity();

					gl.glDisable(GL.GL_TEXTURE_2D); // no textures
					gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
					TextRenderer aText = null;
					Font font = new Font("Arial", Font.PLAIN, 14);
					if (aText == null) {
						aText = new TextRenderer(font, true, true);
					}
					aText.begin3DRendering();
					Globe myGlobe = dc.getGlobe();
					Rectangle2DIntersector i = new Rectangle2DIntersector();

					// Get the iterator for the object updated LAST
					Iterator<Vec4> points = myLabelPoints.iterator();
					Iterator<String> strings = myLabelStrings.iterator();

					while(strings.hasNext()){
						final Vec4 v = points.next();
						final String l = strings.next();
						log.debug("STRING VALUE OF "+l);
						final Vec4 s = myView.project(v);
						Rectangle2D bounds = aText.getBounds(l);
						bounds.setRect(bounds.getX() + s.x, bounds.getY() + s.y,
							bounds.getWidth(), bounds.getHeight());
						if (!i.intersectsAdd(bounds)) {
							cheezyOutline(aText, l, (int) s.x, (int) s.y);
						}
						//log.debug("POINT "+j+", "+aSize+" "+v);
					}
					aText.end3DRendering();
				} finally {
					if (projectionPushed) {
						gl.glMatrixMode(GL.GL_PROJECTION);
						gl.glPopMatrix();
					}
					if (modelviewPushed) {
						gl.glMatrixMode(GL.GL_MODELVIEW);
						gl.glPopMatrix();
					}
					if (attribsPushed) {
						gl.glPopAttrib();
					}
				}
			}
		}
	}

	/**
	 * A cheezy outline behind the text that doesn't require an outline font
	 * to render. It shadows by shifting the text 1 pixel in every
	 * direction. Not very fast, but color keys are more about looks.
	 */
	public void cheezyOutline(TextRenderer t, String l, int x, int y) {

		// Draw a 'grid' of background to shadow the character....
		// We can get away with this because there aren't that many labels
		// in a color key really. Draw 8 labels shifted to get outline.
		t.setColor(Color.black);
		t.draw(l, x + 1, y + 1);
		t.draw(l, x, y + 1);
		t.draw(l, x - 1, y + 1);
		t.draw(l, x - 1, y);
		t.draw(l, x - 1, y - 1);
		t.draw(l, x, y - 1);
		t.draw(l, x + 1, y - 1);
		t.draw(l, x + 1, y);

		t.setColor(Color.white);
		t.draw(l, x, y);
	}

	/**
	 * Simple rectangle collection intersection. Not optimized, this could
	 * be better with an interval binary search tree....
	 */
	public static class Rectangle2DIntersector {

		ArrayList<Rectangle2D> myRects = new ArrayList<Rectangle2D>();
		/**
		 * Return if intersects. Add to collection
		 */
		public boolean intersectsAdd(Rectangle2D newOne) {
			boolean hits = false;
			for (Rectangle2D r : myRects) {  // O(N)
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
	public void doPick(DrawContext dc, java.awt.Point pickPoint) {
	}

	/**
	 * Update our data to the data of a worker. Note because of threads for
	 * brief time periods more than one worker might be going. (Fast
	 * changing of settings). The worker will stop on false
	 */
	public boolean updateData(BackgroundPolarGridMaker worker, ArrayList<Integer> off, Array1Dfloat poly,
		GrowList<Vec4> points, GrowList<String> labels, boolean done) {

		// WorkerLock --> drawLock.  Never switch order
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
		CommandManager.getInstance().getEarthBall().updateOnMinTime();
		return keepWorking;
	}
}
