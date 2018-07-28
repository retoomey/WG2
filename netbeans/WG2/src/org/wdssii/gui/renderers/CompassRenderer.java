package org.wdssii.gui.renderers;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.geom.D3;

/** Simple 'North' compass thing from old W2 display.  Except I avoided all the
 * tessellation and just did it on graph paper.
 * 
 * FIXME: Maybe make this a symbol renderer? Could be useful in an icon or maybe
 * we can share the property setting abilities 
 * 
 * @author Robert Toomey
 *
 */
public class CompassRenderer {

	/** Just draw the north compass and arrow in given gl world */
	public void DrawCompass(GL glold, D3 compassPos, double scale) {
		final GL2 gl = glold.getGL2();
	
		/*
		// --------------------------------------------------------------
		// OK ALL THE COMPASS CODE FOR QUICK implementation
		D3 compassPos = new D3(myCamera.myCameraState.ref);
		if ((compassPos.x == 0) && (compassPos.y == 0) && (compassPos.y == 0)) {
			// D3 cv = myCamera.myCameraState.view.newUnit().times(D3.EARTH_RADIUS_KMS);
			D3 cv = new D3(myCamera.myCameraState.view).toUnit().times(D3.EARTH_RADIUS_KMS);
			compassPos.plus(cv);
		}
		final double scale = myCamera.getPointToScale(compassPos, drawable.getHeight()) * 1.50;
*/
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

		gl.glColor3d(1.0, 1.0, 1.0);
		gl.glLineWidth(1.0f);
		gl.glFrontFace(GL2.GL_CCW);
		// Yawn could like be modern gl and not use begin/end. Just a piece of
		// paper and draw this out since we'll never change it. Could also use strips
		// but would have to overlap here
		gl.glColor3d(1.0, 1.0, 1.0);

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

		gl.glPopMatrix();
		gl.glPopAttrib();
		gl.glLineWidth(1.0f);
	}
}
