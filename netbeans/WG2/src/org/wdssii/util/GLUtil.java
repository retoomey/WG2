package org.wdssii.util;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.Iterator;
import javax.media.opengl.GL;
import org.wdssii.storage.GrowList;

/**
 * Some reusable opengl utilities...
 * 
 * @author Robert Toomey
 */
public class GLUtil {

	/**
	 * A cheezy outline behind the text that doesn't require an outline font
	 * to render. It shadows by shifting the text 1 pixel in every
	 * direction.
	 */
	public static void cheezyOutline(TextRenderer t, String l, Color fore, Color back, int x, int y) {

		// Draw a 'grid' of background to shadow the character....
		// We can get away with this because there aren't that many labels
		// in a color key really. Draw 8 labels shifted to get outline.
		t.setColor(back);
		t.draw(l, x + 1, y + 1);
		t.draw(l, x, y + 1);
		t.draw(l, x - 1, y + 1);
		t.draw(l, x - 1, y);
		t.draw(l, x - 1, y - 1);
		t.draw(l, x, y - 1);
		t.draw(l, x + 1, y - 1);
		t.draw(l, x + 1, y);

		t.setColor(fore);
		t.draw(l, x, y);
	}

	/** Push a standard 2d ortho projection */
	public static void pushOrtho2D(DrawContext dc) {
		final View myView = dc.getView();
		final GL gl = dc.getGL();
		final int aViewWidth = myView.getViewport().width;
		final int aViewHeight = myView.getViewport().height;

		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1);  // TopLeft
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
	}

	/** Pop a standard 2d ortho project */
	public static void popOrtho2D(DrawContext dc) {
		final GL gl = dc.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	public static void pushHiddenStipple(DrawContext dc) {
		final GL gl = dc.getGL();
		gl.glDepthFunc(GL.GL_GREATER);
		gl.glEnable(GL.GL_LINE_STIPPLE);
		gl.glLineStipple(1, (short) 0x00ff);
	}

	public static void popHiddenStipple(DrawContext dc) {
		final GL gl = dc.getGL();
		// FIXME: should push/pop attrib flags probably
		gl.glDisable(GL.GL_LINE_STIPPLE);
		gl.glDepthFunc(GL.GL_LESS);
	}

	/* Render arrays with offsets...
	   This assumes you have already called 
	   gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
	   gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
	 * We don't call it since you can call this multiple times within a single client state batch
	 */
	public static void renderArrays(DrawContext dc, FloatBuffer z, GrowList<Integer> offsets, int glMode) {
		// Only render if there is data to render
		if ((z != null) && (z.capacity() > 0)) {
			final GL gl = dc.getGL();
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());

			Iterator<Integer> itr = offsets.iterator();
			if (itr.hasNext()) {
				Integer now = itr.next();
				while (itr.hasNext()) {
					Integer plus1 = itr.next();
					if (plus1 != null) {
						int start_index = now;
						int end_index = plus1;
						int run_indices = end_index - start_index;
						int start_vertex = start_index / 3;
						int run_vertices = run_indices / 3;
						gl.glDrawArrays(glMode, start_vertex,
							run_vertices);
						now = plus1;
					}
				}
			}
		}

	}
}
