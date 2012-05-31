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

		pushOrtho2D(gl, aViewWidth, aViewHeight);
	}

	public static void pushOrtho2D(GL gl, int width, int height) {
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, width, 0, height, -1, 1);  // TopLeft
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		// Extra default settings for rendering pure 2D
		// FIXME: check all these needed for below..
		gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
				| GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
				| GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
				| GL.GL_CURRENT_BIT);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_TEXTURE_2D); // no textures
		gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
	}

	/** Pop a standard 2d ortho project */
	public static void popOrtho2D(DrawContext dc) {
		popOrtho2D(dc.getGL());
	}

	public static void popOrtho2D(GL gl) {
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glPopAttrib();
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
