package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;

import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.renderers.PolarGridRenderer.Rectangle2DIntersector;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.storage.GrowList;

import com.sun.opengl.util.j2d.TextRenderer;

/** Draws the lines and text for polygons from an LLD_X set
 * 
 * @author Robert Toomey
 *
 */
public class LLHPolygonRenderer extends Feature3DRenderer {

	private final static Logger LOG = LoggerFactory.getLogger(LLHPolygonRenderer.class);

	private LLHAreaFeature myFeature = null;

	public LLHPolygonRenderer() {
	}

	@Override
	public void pick(GLWorld w, Point p, FeatureMemento m) {
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
	/**
	 * Draw the product in the current dc. FIXME: Shared code with map
	 * renderer right now.....do we merge some of these classes or make util
	 * functions?
	 */
	@Override
	public void draw(GLWorld w, FeatureMemento mf) {

		//LLHAreaMemento llm = (LLHAreaMemento)(mf);
		if (myFeature != null){
			
			LLHArea a = myFeature.getLLHArea();
			List<LLD_X> list = a.getLocations();

			int aSize = list.size();
			Array1D<Float> workPolygons = new Array1DfloatAsNodes(aSize*3, 0.0f);
			GrowList<Integer> workOffsets = new GrowList<Integer>();
			GrowList<V3> workLabelPoints = new GrowList<V3>();
			GrowList<String> workLabelStrings = new GrowList<String>();

			int idx = 0;
			workOffsets.add(0);  // Just one for now

			// foreach polygon:
			//float firstX, firstY, firstZ;
			boolean firstInPolygon=true;
			//int count = 0;
			//int closeAt = 4;
			int pointName = 1;
			int currentPolygon = 0;
			int newPolygon;
			
			for (LLD_X l : list) {
				//LLHAreaSetTableData d = new LLHAreaSetTableData();
				double latitude = l.latDegrees();
				double longitude = l.lonDegrees();
				double e = w.getElevation(latitude, longitude);
				V3 point = w.projectLLH(latitude, longitude, e); 
				if (firstInPolygon){
					currentPolygon = l.getPolygon();
					newPolygon = currentPolygon;
					//firstX = point.x; firstY = point.y; firstZ = point.z;
					firstInPolygon = false;
				}else{
				    newPolygon = l.getPolygon();
				}
				
				//count++;
				//if (count >= closeAt){
				if (currentPolygon != newPolygon){
					currentPolygon = newPolygon;
				//	// Put the first one again...? No GL loop should handle this....
					workOffsets.add(idx);
				//	count = 0;
				}
				workPolygons.set(idx++, (float) point.x);
				workPolygons.set(idx++, (float) point.y);
				workPolygons.set(idx++, (float) point.z);
				String note = l.getNote();
				if ((note == null) || note.isEmpty()){ // note can actually be null if never set
					note = String.format("%d",  pointName);
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
			final GL gl = w.gl;

			try {
				Object lock1 = workPolygons.getBufferLock();
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

					// Smooth?
					gl.glEnable(GL.GL_LINE_SMOOTH);
					gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
					gl.glEnable(GL.GL_BLEND);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

					statePushed = true;
					FloatBuffer z = workPolygons.getRawBuffer();
					//gl.glColor4f(r, g, b, a);
					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
					gl.glLineWidth(2.0f);
					//GLUtil.renderArrays(w.gl, z, workOffsets, GL.GL_LINE_STRIP);
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
			//Globe myGlobe = dc.getGlobe();
			Rectangle2DIntersector i = new Rectangle2DIntersector();
			// Get the iterator for the object updated LAST
			Iterator<V3> points = workLabelPoints.iterator();
			Iterator<String> strings = workLabelStrings.iterator();
			//final View myView = dc.getView();

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
				//final V2 s = myView.project(v);
				final V2 so =  w.project(v);
				final V2 s = so.offset(12, -10);

				Rectangle2D bounds = aText.getBounds(l);
				bounds.setRect(bounds.getX() + s.x, bounds.getY() + s.y,
						bounds.getWidth(), bounds.getHeight());
				if (!i.intersectsAdd(bounds)) {
					GLUtil.cheezyOutline(aText, l, Color.WHITE, Color.BLACK, (int) s.x, (int) s.y);
				}
			}
			aText.end3DRendering();
			GLUtil.popOrtho2D(w.gl);



			/*GLUtil.pushOrtho2D(w);
			aText.begin3DRendering();
			GLUtil.cheezyOutline(aText, "POLYGON EDITING", Color.WHITE, Color.BLACK, (int) 100, (int) 100);
			aText.end3DRendering();
			GLUtil.popOrtho2D(w.gl);*/
		}
	}


	/**
	 * Pick an object in the current dc at point
	 */
	public void doPick(GLWorld w, java.awt.Point pickPoint) {
	}


}