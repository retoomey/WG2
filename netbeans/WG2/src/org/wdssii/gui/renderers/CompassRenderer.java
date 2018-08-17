package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Point;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.geom.D3;
import org.wdssii.gui.GLBoxCamera;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.W2GLWorld;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.gui.features.MapFeature.MapMemento;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Simple 'North' compass thing from old W2 display. Except I avoided all the
 * tessellation and just did it on graph paper.
 * 
 * @author Robert Toomey
 *
 */
public class CompassRenderer extends Feature3DRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(CompassRenderer.class);

	/** Id of compass for latest pick pass. */
	private int myCompassPickID = -1;

	/** Just draw the north compass and arrow in given gl world */
	public void DrawCompass(GL glold, D3 compassPos, double scale, boolean pick, boolean inside) {
		final GL2 gl = glold.getGL2();

		// Point north from compass position...
		D3 OC = new D3(compassPos).toUnit();
		D3 ox = new D3(0, 0, 1).cross(OC).toUnit();
		D3 oy = new D3(OC).cross(ox).toUnit();
		final double m[] = { ox.x, ox.y, ox.z, 0, oy.x, oy.y, oy.z, 0, OC.x, OC.y, OC.z, 0, 0, 0, 0, 1

		};

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL2.GL_DEPTH_TEST);

		gl.glTranslated(compassPos.x, compassPos.y, compassPos.z);
		gl.glMultMatrixd(m, 0);

		gl.glScaled(scale, scale, scale);

		gl.glLineWidth(1.0f);
		gl.glFrontFace(GL2.GL_CCW);
		// Yawn could like be modern gl and not use begin/end. Just a piece of
		// paper and draw this out since we'll never change it. Could also use strips
		// but would have to overlap here
		if (pick) {
			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2d(-10.0, -15.0);
			gl.glVertex2d(10.0, -15.0);
			gl.glVertex2d(10.0, 12.0);
			gl.glVertex2d(-10.0, 12.0);
			gl.glEnd();
		} else {

			if (inside) {
				gl.glColor3d(1.0, 0.0, 0.0);
			}else {
				gl.glColor3d(1.0, 1.0, 1.0);
			}
			gl.glBegin(GL2.GL_TRIANGLES); // Counter clockwise

			// Letter N for north...
			gl.glVertex2d(-9.0, 0.0); // Left N leg
			gl.glVertex2d(-9.0, -15.0);
			gl.glVertex2d(-4.0, -15.0);
			gl.glVertex2d(-9.0, 0.0);
			gl.glVertex2d(-4.0, -15.0);
			gl.glVertex2d(-4.0, 0.0);
			gl.glVertex2d(-4.0, 0.0); // N center
			gl.glVertex2d(-4.0, -7.0);
			gl.glVertex2d(4.0, -8.0);
			gl.glVertex2d(4.0, -8.0);
			gl.glVertex2d(-4.0, -7.0);
			gl.glVertex2d(4.0, -15.0);
			gl.glVertex2d(4.0, 0.0); // Right N leg
			gl.glVertex2d(4.0, -15.0);
			gl.glVertex2d(9.0, -15.0);
			gl.glVertex2d(4.0, 0.0);
			gl.glVertex2d(9.0, -15.0);
			gl.glVertex2d(9.0, 0.0);

			// Arrow on top...
			gl.glVertex2d(0.0, 12.0);
			gl.glVertex2d(-10.0, 5.0);
			gl.glVertex2d(10.0, 5.0);

			gl.glVertex2d(-3.0, 5.0);
			gl.glVertex2d(-3.0, 2.0);
			gl.glVertex2d(3.0, 2.0);
			gl.glVertex2d(-3.0, 5.0);
			gl.glVertex2d(3.0, 2.0);
			gl.glVertex2d(3.0, 5.0);

			gl.glEnd();

			gl.glColor3d(1.0, 0.0, 0.0);

			// Outside N
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex2d(-4.0, 0.0);
			gl.glVertex2d(-9.0, 0.0);
			gl.glVertex2d(-9.0, -15.0);
			gl.glVertex2d(-4.0, -15.0);
			gl.glVertex2d(-4.0, -7.0);
			gl.glVertex2d(4.0, -15.0);
			gl.glVertex2d(9.0, -15.0);
			gl.glVertex2d(9.0, 0.0);
			gl.glVertex2d(4.0, 0.0);
			gl.glVertex2d(4.0, -8.0);
			gl.glEnd();

			// Outside arrow
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex2d(0.0, 12.0);
			gl.glVertex2d(-10.0, 5.0);
			gl.glVertex2d(-3.0, 5.0);
			gl.glVertex2d(-3.0, 2.0);
			gl.glVertex2d(3.0, 2.0);
			gl.glVertex2d(3.0, 5.0);
			gl.glVertex2d(10.0, 5.0);
			gl.glEnd();

			gl.glColor3d(0.0, 0.0, 0.0);
		}

		gl.glPopMatrix();
		gl.glPopAttrib();
		gl.glLineWidth(1.0f);
	}

	@Override
	public void preRender(GLWorld w, FeatureMemento m) {

	}

	@Override
	public void draw(GLWorld w, FeatureMemento m) {
		if (w instanceof W2GLWorld) {
			W2GLWorld w2 = (W2GLWorld) (w);
			GLBoxCamera c = w2.getCamera();
			if (c != null) {

				// Calculate compass size/location from camera
				// for a center drawing compass with direction pointing north
				D3 compassPos = new D3(c.myRef);
				if ((compassPos.x == 0) && (compassPos.y == 0) && (compassPos.y == 0)) {
					D3 cv = new D3(c.myView).toUnit().times(D3.EARTH_RADIUS_KMS);
					compassPos.plus(cv);
				}
				final double scale = c.getPointToScale(compassPos, w.height) * 1.50;
				boolean inside = m.get(LegendMemento.INCOMPASS, false);
				DrawCompass(w.gl, compassPos, scale, false, inside);
			}
		}
	}

	@Override
	public void pick(GLWorld w, Point p, FeatureMemento m) {

		// FIXME: duplicated code...
		// Humm do we even need the point for rendering? Guess you
		// could do some preculling
		if (w instanceof W2GLWorld) {
			W2GLWorld w2 = (W2GLWorld) (w);
			GLBoxCamera c = w2.getCamera();
			if (c != null) {

				// Calculate compass size/location from camera
				// for a center drawing compass with direction pointing north
				D3 compassPos = new D3(c.myRef);
				if ((compassPos.x == 0) && (compassPos.y == 0) && (compassPos.y == 0)) {
					D3 cv = new D3(c.myView).toUnit().times(D3.EARTH_RADIUS_KMS);
					compassPos.plus(cv);
				}
				final double scale = c.getPointToScale(compassPos, w.height) * 1.50;
				LOG.error("Get unique color compass");
				myCompassPickID = w.setUniqueGLColor();
				// LOG.error("PICK COMPASS IS "+myCompassPickID);
				DrawCompass(w.gl, compassPos, scale, true, false);
			}
		}
	}

	/** Called to see if we were picked */
	public int getCompassPickID() {
		return myCompassPickID;
	}
}
