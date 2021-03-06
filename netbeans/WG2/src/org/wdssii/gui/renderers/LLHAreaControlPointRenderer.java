package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Point;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.Picker;
import org.wdssii.gui.volumes.LLHAreaControlPoint;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/** 
 * Renders a collection of LLHAreaControl points into a GLWorld
 * @author Robert Toomey
 *
 */
public class LLHAreaControlPointRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(LLHAreaControlPointRenderer.class);

	/** The latest pick results */
	private Picker pp;
	
	/** Allow opengl color picking here.  We could use a rectangle based picker for non-gl worlds, or worlds where we can't
	 *  do separate pick drawing passes (like AWIPS2)
	 * @author Robert Toomey
	 *
	 */
	private static class ourGLPicker extends PickWithOpenGLColor {
		private LLHAreaControlPointRenderer myR;
		private Iterable<? extends LLHAreaControlPoint> myC;
		
		public ourGLPicker(GLWorld w, LLHAreaControlPointRenderer r, Iterable<? extends LLHAreaControlPoint> controlPoints) {
			super(w);
			myR = r;
			myC = controlPoints;
		}

		@Override
		public void renderPick(){
			
			try {
				// Render each of the control points for picking though....
				for (LLHAreaControlPoint p : myC) {
					Color c = getUniqueColor();
                	final GL2 gln = gl.getGL().getGL2();

					gln.glColor3ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue());
					myR.drawControlPoint(world, p, true);
					addCandidate(c, p);
				}
			} finally {
				// Catch just to avoid rendering error spam
			}
		}
	}
		
	public LLHAreaControlPointRenderer() { 
	}

	public Picker getPicker(){
		return pp;
	}
	public void render(GLWorld w, Iterable<? extends LLHAreaControlPoint> controlPoints) {
			for (LLHAreaControlPoint p : controlPoints) {
				this.drawControlPoint(w, p, false);
			}
	}
    
	public void pick(GLWorld w, Iterable<? extends LLHAreaControlPoint> controlPoints, Point pickPoint) {
		Picker p = new ourGLPicker(w, this, controlPoints);  // Use colors.  Rectangle method should be replaceable here eventually...
		p.begin();
		p.pick(pickPoint.x, pickPoint.y);  // Find objects at the point, add to list...
		p.end();
		
		pp = p;  // Save to replace now, class is finished writing internally.	
	}

	protected void drawControlPoint(GLWorld w, LLHAreaControlPoint controlPoint, boolean pick) {

		// Clip when outside the view area...
		V3 aV3 = controlPoint.getPoint();
		if (!w.inView(aV3)){
			return;
		}
		GL glold = w.gl;
    	final GL2 gl = glold.getGL().getGL2();

		//int i = controlPoint.getAltitudeIndex();
		int i = 0;
		if (i == 0){ // bottom points only...
			boolean selected = false;
			LLD_X x = controlPoint.getLocation();
			if (x != null){
				selected = x.getSelected();
			}
			// Can't use my symbol library 'yet' because glTranslate messes up worldwind picking...
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
			gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1);  // TopLeft
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
				if (selected){
					gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				}else{
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
}
