package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLBoxCamera;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.Picker;
import org.wdssii.gui.W2GLWorld;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.renderers.PolarGridRenderer.Rectangle2DIntersector;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaControlPoint;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.storage.GrowList;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Draws the lines and text for polygons from an LLD_X set
 * 
 * @author Robert Toomey
 *
 */
public class LLHPolygonRenderer extends Feature3DRenderer {

	private final static Logger LOG = LoggerFactory.getLogger(LLHPolygonRenderer.class);

	public int myFirstPick;
	public int myPickCount = 0;
	private LLHAreaFeature myFeature = null;

	public ArrayList<LLHAreaControlPoint> myLastControls;

	public LLHPolygonRenderer() {
	}

	@Override
	public void preRender(GLWorld w, FeatureMemento m) {
	}

	@Override
	public void initToFeature(Feature m) {
		if (m instanceof LLHAreaFeature) {
			myFeature = (LLHAreaFeature) (m);

		}
	}

	protected void drawControlPoint(GLWorld w, LLHAreaControlPoint controlPoint, boolean pick) {

		// Clip when outside the view area...
		V3 aV3 = controlPoint.getPoint();
		if (!w.inView(aV3)) {
			return;
		}
		GL glold = w.gl;
		final GL2 gl = glold.getGL().getGL2();

		// int i = controlPoint.getAltitudeIndex();
		int i = 0;
		if (i == 0) { // bottom points only...
			boolean selected = false;
			LLD_X x = controlPoint.getLocation();
			if (x != null) {
				selected = x.getSelected();
			}
			// Can't use my symbol library 'yet' because glTranslate messes up worldwind
			// picking...
			// need to make my own...
			// Ahhh need to translate the mouse point too right? lol
			V2 p = w.project(aV3);

			int z = 10;
			int aViewWidth = w.width;
			int aViewHeight = w.height;

			gl.glDisable(GL2.GL_LIGHTING);
			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glDisable(GL.GL_TEXTURE_2D); // no textures
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1); // TopLeft
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();

			if (pick) {
				// Draw the pick box area
				gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
			} else {

				gl.glColor4f(1.0f, 1.0f, 0.0f, 1.0f);
				gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
				z--;
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
				z--;
				// if (myPoint.selected){
				//
				// }
				if (selected) {
					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				} else {
					gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
				}
				gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
			}

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPopMatrix();
		}
	}

	/**
	 * Draw the product in the current dc. FIXME: Shared code with map renderer
	 * right now.....do we merge some of these classes or make util functions?
	 */
	@Override
	public void draw(GLWorld w, FeatureMemento mf) {

		// LLHAreaMemento llm = (LLHAreaMemento)(mf);
		if (myFeature != null) {

			LLHArea a = myFeature.getLLHArea();
			List<LLD_X> list = a.getLocations();

			int aSize = list.size();
			Array1D<Float> workPolygons = new Array1DfloatAsNodes(aSize * 3, 0.0f);
			GrowList<Integer> workOffsets = new GrowList<Integer>();
			GrowList<V3> workLabelPoints = new GrowList<V3>();
			GrowList<String> workLabelStrings = new GrowList<String>();

			int idx = 0;
			workOffsets.add(0); // Just one for now

			// foreach polygon:
			// float firstX, firstY, firstZ;
			boolean firstInPolygon = true;
			// int count = 0;
			// int closeAt = 4;
			int pointName = 1;
			int currentPolygon = 0;
			int newPolygon;

			for (LLD_X l : list) {
				// LLHAreaSetTableData d = new LLHAreaSetTableData();
				double latitude = l.latDegrees();
				double longitude = l.lonDegrees();
				double e = w.getElevation(latitude, longitude);
				V3 point = w.projectLLH(latitude, longitude, e);
				if (firstInPolygon) {
					currentPolygon = l.getPolygon();
					newPolygon = currentPolygon;
					// firstX = point.x; firstY = point.y; firstZ = point.z;
					firstInPolygon = false;
				} else {
					newPolygon = l.getPolygon();
				}

				// count++;
				// if (count >= closeAt){
				if (currentPolygon != newPolygon) {
					currentPolygon = newPolygon;
					// // Put the first one again...? No GL loop should handle this....
					workOffsets.add(idx);
					// count = 0;
				}
				workPolygons.set(idx++, (float) point.x);
				workPolygons.set(idx++, (float) point.y);
				workPolygons.set(idx++, (float) point.z);
				String note = l.getNote();
				if ((note == null) || note.isEmpty()) { // note can actually be null if never set
					note = String.format("%d", pointName);
				}
				pointName++;
				workLabelStrings.add(note);
				workLabelPoints.add(point);

				// if 'changed', close the polygon, start a new one...
			}

			workOffsets.add(idx);
			// end polygon

			boolean attribsPushed = false;
			boolean statePushed = false;
			final GL glold = w.gl;
			final GL2 gl = glold.getGL().getGL2();

			try {
				Object lock1 = workPolygons.getBufferLock();
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
					FloatBuffer z = workPolygons.getRawBuffer();
					// gl.glColor4f(r, g, b, a);
					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					gl.glLineWidth(2.0f);
					// GLUtil.renderArrays(w.gl, z, workOffsets, GL.GL_LINE_STRIP);
					GLUtil.renderArrays(w.gl, z, workOffsets, GL.GL_LINE_LOOP);

				}
			} finally {
				if (statePushed) {
					gl.glPopClientAttrib();
				}
				if (attribsPushed) {
					gl.glPopAttrib();
				}
			}
			// Globe myGlobe = dc.getGlobe();
			Rectangle2DIntersector i = new Rectangle2DIntersector();
			// Get the iterator for the object updated LAST
			Iterator<V3> points = workLabelPoints.iterator();
			Iterator<String> strings = workLabelStrings.iterator();
			// final View myView = dc.getView();

			TextRenderer aText = null;
			Font font = new Font("Arial", Font.PLAIN, 14);
			if (aText == null) {
				aText = new TextRenderer(font, true, true);
			}

			GLUtil.pushOrtho2D(w);
			aText.begin3DRendering();
			while (strings.hasNext()) {
				final V3 v = points.next();
				final String l = strings.next();
				// final V2 s = myView.project(v);
				final V2 so = w.project(v);
				final V2 plus = new V2(12, -10);
				//final V2 s = so.offset(12, -10);
				final V2 s = so.plus(plus);

				Rectangle2D bounds = aText.getBounds(l);
				bounds.setRect(bounds.getX() + s.x, bounds.getY() + s.y, bounds.getWidth(), bounds.getHeight());
				if (!i.intersectsAdd(bounds)) {
					GLUtil.cheezyOutline(aText, l, Color.WHITE, Color.BLACK, (int) s.x, (int) s.y);
				}
			}
			aText.end3DRendering();
			GLUtil.popOrtho2D(w.gl);

			// Ok so basically we can get rid of the LLHAreaControlPointRenderer...
			// We need ALL features to have the ability to handle mouse events basically...
			ArrayList<LLHAreaControlPoint> cpoints = a.getControlPoints(w);
			for (LLHAreaControlPoint pp : cpoints) {
				drawControlPoint(w, pp, false);
			}
			/*
			 * GLUtil.pushOrtho2D(w); aText.begin3DRendering(); GLUtil.cheezyOutline(aText,
			 * "POLYGON EDITING", Color.WHITE, Color.BLACK, (int) 100, (int) 100);
			 * aText.end3DRendering(); GLUtil.popOrtho2D(w.gl);
			 */
		}
	}

	/**
	 * Pick an object in the current dc at point
	 */
	@Override
	public void pick(GLWorld w, Point pickPoint, FeatureMemento m) {
		// FIXME: duplicated code...
		// Humm do we even need the point for rendering? Guess you
		// could do some preculling
		if (w instanceof W2GLWorld) {
			W2GLWorld w2 = (W2GLWorld) (w);
			GLBoxCamera c = w2.getCamera();
			if (c != null) {
				if (myFeature != null) {
					LLHArea a = myFeature.getLLHArea();
					ArrayList<LLHAreaControlPoint> cpoints = a.getControlPoints(w);
					myLastControls = cpoints; // Hack
					// myPicks = new ArrayList<Integer>();
					myPickCount = cpoints.size();
					if (cpoints.size() > 0) {
						boolean first = true;

						for (LLHAreaControlPoint zz : cpoints) {
							if (first) {
								this.myFirstPick = w.setUniqueGLColor();
								first = false;
							} else {
								w.setUniqueGLColor();
							}
							drawControlPoint(w, zz, true);
						}
					}

				}
			}
		}
	}
	
    @Override
    public FeatureRenderer.Level getDrawRank(){
        return FeatureRenderer.Level.LLHCONTROLS;  // Draw over others
    }
}