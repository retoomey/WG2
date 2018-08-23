package org.wdssii.gui.charts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import org.wdssii.core.CommandManager;
import org.wdssii.core.WdssiiCommand;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLBoxCamera;
import org.wdssii.gui.GLCacheManager;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.W2GLWorld;
import org.wdssii.gui.commands.ChartDeleteCommand;
import org.wdssii.gui.commands.ChartDeleteCommand.ChartDeleteParams;
import org.wdssii.gui.commands.ChartRenameCommand;
import org.wdssii.gui.commands.ChartRenameCommand.ChartRenameParams;
import org.wdssii.gui.commands.ChartSetGroupNumberCommand;
import org.wdssii.gui.commands.ChartSetGroupNumberCommand.ChartSetGroupNumberParams;
import org.wdssii.gui.commands.ChartSwapCommand;
import org.wdssii.gui.commands.ChartSwapCommand.ChartSwapParams;
import org.wdssii.gui.features.EarthBallFeature;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.views.Window;
import org.wdssii.gui.views.WindowManager;
import org.wdssii.log.LoggerFactory;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Should we use an interface to 'hide' the mouse structure? This is useful if
 * we ever share code with another toolkit.
 * 
 * @author Robert Toomey
 *
 */
enum W2ButtonState {
	NoButton, LeftButton, MidButton, RightButton
};

enum W2WheelState {
	WheelNone, WheelIn, WheelOut
};

final class myMouseEvent {
	int x; // Current x for event
	int y; // Current y for event
	int dx; // Change since last x
	int dy; // Change since last y

	W2ButtonState button; // One button only

	boolean leftButton; // Flags for individual buttons
	boolean middleButton;
	boolean rightButton;

	boolean shiftDown; // Is shift key down?
	boolean ctrlDown; // Is the control key down?

	boolean _pressed; // Flag for a pressed mouse button
	boolean _released; // Flag for a released mouse button
	boolean _moving; // Flag for a moving mouse

	W2WheelState _wheel_type;

	public boolean ButtonDown() {
		return _pressed;
	}

	public boolean ButtonUp() {
		return _released;
	}

	public boolean Moving() {
		return _moving;
	}

	public W2WheelState Wheeling() {
		return _wheel_type;
	}
}

final class W2DataViewListener implements GLEventListener,
// Oh only in Java. Couldn't we just have one it's not like there
// are a billion methods, lol...
		MouseListener, MouseMotionListener, MouseWheelListener {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(W2DataViewListener.class);

	private GLU glu; // for the GL Utility
	private boolean myIn;
	public GLJPanel canvas;
	public GLBoxCamera myCamera = new GLBoxCamera(true);

	private W2DataView myW2DataView;

	// Mouse stuff...could be put into mouse event...

	private boolean leftDown = false;

	private int leftX;

	private int leftY;

	private boolean middleDown = false;

	private boolean shiftDown = false;

	private boolean ctrlDown = false;

	private boolean myFirstTime = true;

	// private LLHAreaLayer myLLHAreaLayer;

	private TextRenderer myText;

	private int myDrawCounter;

	/** Is the drawn view dirty? For example, readout drew over it. */
	private boolean myDirty = true;

	/** Did the scene change in our view? */
	private boolean mySceneChanged = true;

	private int myLastWidth = 0;
	private int myLastHeight = 0;

	private ByteBuffer myBuffer = null;

	private V3 myReadoutPoint;

	// ^^^^^ End mouse stuff

	public W2DataViewListener(W2DataView w2DataView) {
		myW2DataView = w2DataView;
	}

	/**
	 * Render all features that are in the given group.
	 * 
	 * FIXME: We should sort during add/remove so that we can just call draw/pick I
	 * think without having to group here. This is slower...
	 */
	public void renderFeatureGroup(GLWorld w, String g) {

	//	pickFeatureGroup(w, g, w.width, w.height);
	//	return;
		
		// Why product manager? lol.
		FeatureList fl = myW2DataView.getFeatureList();

		List<Feature> list = fl.getActiveFeatureGroup(g);

		// For each rank...draw over lower ranks...
		for (int i = 0; i <= Feature.MAX_RANK; i++) {
			Iterator<Feature> iter = list.iterator();
			while (iter.hasNext()) {
				Feature f = iter.next();
				if (f.getRank() == i) {
					// f.render(w);
					FeatureMemento m = f.getMemento();
					// eh? How did it work?
					// ArrayList<FeatureRenderer> theList = f.getRendererList("WW",
					// "org.wdssii.gui.worldwind.renderers");
					ArrayList<FeatureRenderer> theList = f.getRendererList("", "org.wdssii.gui.renderers");

					// ArrayList<FeatureRenderer> theList = myMap.get(f);
					if (theList != null) {
						for (FeatureRenderer fr : theList) {
							if (fr instanceof Feature3DRenderer) {
								Feature3DRenderer a3d = (Feature3DRenderer) (fr);
								a3d.setCurrentFeatureList(fl);
								a3d.draw(w, m);
							}
						}
					}

				}
			}
		}
	}

	public void pickFeatureGroup(GLWorld w, String g, int x, int y) {
		/*
		 * * FIXME: We should sort during add/remove so that we can just call draw/pick
		 * I think without having to group here. This is slower...
		 */
		// FeatureList fl = ProductManager.getInstance().getFeatureList();
		FeatureList fl = myW2DataView.getFeatureList();

		List<Feature> list = fl.getActiveFeatureGroup(g);

		// For each rank...draw over lower ranks...
		for (int i = 0; i <= Feature.MAX_RANK; i++) {
			Iterator<Feature> iter = list.iterator();
			while (iter.hasNext()) {
				Feature f = iter.next();
				if (f.getRank() == i) {
					// f.render(w);
					FeatureMemento m = f.getMemento();
					// eh? How did it work?
					// ArrayList<FeatureRenderer> theList = f.getRendererList("WW",
					// "org.wdssii.gui.worldwind.renderers");
					ArrayList<FeatureRenderer> theList = f.getRendererList("", "org.wdssii.gui.renderers");

					// ArrayList<FeatureRenderer> theList = myMap.get(f);
					if (theList != null) {
						for (FeatureRenderer fr : theList) {
							if (fr instanceof Feature3DRenderer) {
								Feature3DRenderer a3d = (Feature3DRenderer) (fr);
								// LOG.error("Pick called on feature "+x+" "+y);
								a3d.pick(w, new Point(x, y), m);
							}
						}
					}

				}
			}
		}

		// Now do a pick read?
		int pickId = w.doPickRead(w, x, y);
		//LOG.error("PICK ID BACK IS " + pickId);
		// see problem is what to do with it..feature will want to drag or
		// something...which
		// can affect renderers right..that can be handled by memento settings coming
		// from
		// feature...
		Iterator<Feature> iter = list.iterator();
		while (iter.hasNext()) {
			Feature f = iter.next();
			f.handlePickText(pickId, leftDown); // Ok pass full mouse state to subclasses...
		}
	}

	/**
	 * Beginning experiment for snapshotting the screen. Humm I think we need a
	 * custom GLDrawable so we can adjust the screensize for output from
	 * preferences, right?
	 * 
	 * @param gl
	 * @param width
	 * @param height
	 */
	protected void saveImage(GL gl, int width, int height) {

		GL3 gl3 = gl.getGL3();

		BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = screenshot.getGraphics();

		// FIXME: We could reuse buffers when width/height same for speed.
		ByteBuffer buffer = GLBuffers.newDirectByteBuffer(width * height * 4);
		// be sure you are reading from the right fbo (here is supposed to be the
		// default one)
		// bind the right buffer to read from
		gl3.glReadBuffer(GL.GL_BACK);
		// if the width is not multiple of 4, set unpackPixel = 1
		gl3.glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);

		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {
				// The color are the three consecutive bytes, it's like referencing
				// to the next consecutive array elements, so we got red, green, blue..
				// red, green, blue, and so on..+ ", "
				graphics.setColor(new Color((buffer.get() & 0xff), (buffer.get() & 0xff), (buffer.get() & 0xff)));
				buffer.get(); // consume alpha
				graphics.drawRect(w, height - h, 1, 1); // height - h is for flipping the image
			}
		}
		// FIXME: Do we need to clean, or does java handle. Guess we'll find out

		try {
			File outputfile = new File("WGSnapshot1.png");
			ImageIO.write(screenshot, "png", outputfile);
		} catch (IOException ex) {
		}
	}

	/** Dirty means overlay is drawn or being drawn */
	public void setDirty() {
		myDirty = true;
	}

	/** Scene changed, graphic caching is invalid */
	public void setSceneChanged() {
		mySceneChanged = true;
	}

	@Override
	public void display(GLAutoDrawable drawable) {

		// If view isn't dirty or scene hasn't change, no need to draw anything at all
		// Linux seems ok with this..need to check on windows and mac. I know clipping
		// window for example might not get us any other event other than display..so we
		// wouldn't draw properly then.
		if (!(myDirty || mySceneChanged)) {
			return;
		}

		// Scene changed requires a redraw of gl, due to camera or size, etc..
		// and we will then go ahead and fill myBuffer for faster next time rendering
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		gl.getContext().makeCurrent();
		final int w = drawable.getWidth();
		final int h = drawable.getHeight();

		// Update the pick here?  Does this work with buffer/overlay?
		W2GLWorld glw = setUpGLWorld(gl, w, h);
		pickScene(glw, w, h);
		
		if (mySceneChanged == true) {
		//	W2GLWorld glw = setUpGLWorld(gl, w, h);
			renderFullScene(gl, w, h, glw);
		} else {
			if (myDirty == true) {
				// If we have buffer, use it...
				if (fromBuffer(gl, w, h)) {
					myDirty = false;
					// otherwise full render...
				} else {
				//	W2GLWorld glw = setUpGLWorld(gl, w, h);
					renderFullScene(gl, w, h, glw);
				}
			}
		}
	}

	public boolean fromBuffer(GL2 gl, int w, int h) {
		if (myBuffer != null) {
			GLUtil.pushOrtho2D(gl, w, h);
			gl.glDrawBuffer(GL.GL_BACK);

			gl.glRasterPos2i(0, 0);
			// Do we need alpha here?
			// gl3.glReadPixels(0, 0, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, myBuffer);
			gl.glDrawPixels(w, h, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, myBuffer);

			// Do readout immediately to avoid flicker issues...
			if (myIn) {
				renderReadoutOverlay(gl, leftX, leftY);
			}

			GLUtil.popOrtho2D(gl);
			return true;
		}
		return false;
	}

	public void toBuffer(GL3 gl, int w, int h) {
		// Store the rendered scene...
		if (myBuffer == null) {
			// GLBuffers.sizeof(gl,... )
			// FIXME: buffer size is complicated it seems..
			myBuffer = GLBuffers.newDirectByteBuffer(w * h * 4); // RGB = 3. RGBA = 4
		}
		// GLUtil.pushOrtho2D(gl, w, h);
		// GL3 gl3 = gl.getGL3();
		gl.glReadBuffer(GL.GL_BACK);
		// if the width is not multiple of 4, set unpackPixel = 1 ?? Do we need this or
		// not
		gl.glReadPixels(0, 0, w, h, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, myBuffer);
		// GLUtil.popOrtho2D(gl);
	}

	/**
	 * Only called from within draw routines to avoid flickering from setup.
	 * Requires: 2D ortho
	 */
	public void renderReadoutOverlay(GL gli, int x, int y) {
		boolean readoutOn = true;

		// Begin overlay readout testing...
		if (readoutOn) {
			if (myText != null) {

				// CROSSHAIR
				final GL2 gl = gli.getGL2();

				// p is 2d point in 2d ortho
				gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT | GL2.GL_LINE_BIT);
				// glEnable(GL_COLOR_LOGIC_OP);
				// glLogicOp(GL_INVERT);
				gl.glDisable(GL2.GL_DEPTH_TEST);
				gl.glEnable(GL2.GL_LINE_SMOOTH); // make crosshair lines draw smooth
				gl.glColor3d(1.0, 1.0, 1.0);

				gl.glLineWidth(3.0f); // must be odd for symmetry
				/*
				 * if (false){ gl.glBegin(GL.GL_LINE_STRIP); gl.glVertex2d(x-3,y-3);
				 * gl.glVertex2d(x-3,y+3); gl.glVertex2d(x+3,y+3); gl.glVertex2d(x+3,y-3);
				 * gl.glVertex2d(x-3,y-3); gl.glEnd(); }else{
				 */
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(x - 6, y);
				gl.glVertex2d(x - 1, y);
				gl.glVertex2d(x + 1, y);
				gl.glVertex2d(x + 6, y);
				gl.glVertex2d(x, y - 6);
				gl.glVertex2d(x, y - 1);
				gl.glVertex2d(x, y + 1);
				gl.glVertex2d(x, y + 6);
				gl.glEnd();
				// }
				gl.glPopAttrib();
				y = y - 50; // test so I can see it..remove me

				String l = "Readout Test";
				myText.begin3DRendering();
				// Rectangle2D bounds = myText.getBounds(l);

				// bounds.setRect(bounds.getX() + x, bounds.getY() +y, bounds.getWidth(),
				// bounds.getHeight());
				GLUtil.cheezyOutline(myText, "Readout", Color.WHITE, Color.BLACK, (int) x, (int) y);
				myText.end3DRendering();
			}
		}
	}

	public void pickScene(GLWorld world, int w, int h) {
		world.gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear color and depth buffers

		// Probably not where we want it to end up....
		// Think we should create a 'stack' of objects based upon mouse location in
		// window...
		// ...so that all features can react to mouse...
		// pickFeatureGroup(glw, LLHAreaFeature.LLHAreaGroup, leftX, h-leftY); // pick y
		// in gl coordinates at moment
		//pickFeatureGroup(world, LegendFeature.LegendGroup, leftX, h - leftY); // pick y in gl coordinates at moment
		pickFeatureGroup(world, LegendFeature.LegendGroup, leftX, leftY); // pick y in gl coordinates at moment

		pickFeatureGroup(world, LLHAreaFeature.LLHAreaGroup, leftX, leftY); // pick y

	}

	public W2GLWorld setUpGLWorld(GL gl, int w, int h) {
		if (myFirstTime == true) {
			myCamera.goToLocation(-97.1640f, 35.1959f, 400.0f, 0.0f, 0.0f);
			Font font = new Font("Arial", Font.PLAIN, 28);
			myText = new TextRenderer(font, true, true);
			myFirstTime = false;
		}

		// if the camera changes after creating this, it is no longer valid.
		myCamera.setUpCamera(gl, glu, 0, 0, (int) w, (int) h);
		W2GLWorld glw = new W2GLWorld(gl, myCamera, w, h, myW2DataView);
		return glw;
	}

	public void renderFullScene(GL gli, int w, int h, W2GLWorld glw) {
		GL2 gl = gli.getGL2();

		myDirty = false;
		mySceneChanged = false; // might need a lock for this if other threads mess with us

		// A Draw counter for debugging frame drawing...
		myDrawCounter++;

		if (myDrawCounter > 10000) {
			myDrawCounter = 0;
		}

		// myCamera.setUpCamera(gl, glu, 0, 0, (int) w, (int) h);
		// Create world object which allows renderers to know projection to draw into...
		// NOTE: this will cache the current modelview state for efficiency in
		// projection, so
		// if the camera changes after creating this, it is no longer valid.
		// W2GLWorld glw = new W2GLWorld(gl, myCamera, w, h, myW2DataView);
		
	//	pickScene(glw, w, h); // I'm thinking always pick scene when full render is done....
		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear color and depth buffers

		// This method searches entire list...could we sort them then just render in
		// order?

		// Basemaps first (below products)
		renderFeatureGroup(glw, EarthBallFeature.MapGroup);

		renderFeatureGroup(glw, ProductFeature.ProductGroup);

		renderFeatureGroup(glw, MapFeature.MapGroup);

		// Draw lines, labels
		renderFeatureGroup(glw, LLHAreaFeature.LLHAreaGroup);

		// Draw actual control points. Humm why not part of feature?
		// Yeah rewrite into our own model. ALL features should have picking
		// ability, right? And should be able to handle mouse events...
		/*
		 * if (myLLHAreaLayer == null) { myLLHAreaLayer = new LLHAreaLayer(); } if
		 * (myLLHAreaLayer != null) { myLLHAreaLayer.draw(glw); }
		 */

		renderFeatureGroup(glw, PolarGridFeature.PolarGridGroup);

		renderFeatureGroup(glw, LegendFeature.LegendGroup);

		final double m = 0.5;
		GLUtil.pushOrtho2D(gl, w, h);

		gl.glColor3d(1.0, 0.0, 0.0);
		gl.glLineWidth(3.0f);
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glVertex2d(m, m); // bottom left
		gl.glVertex2d(w - m, m); // bottom right
		gl.glVertex2d(w - m, h - m); // top right
		gl.glVertex2d(m, h - m); // top left
		gl.glEnd();
		gl.glLineWidth(1.0f);

		// FIXME: Need something to coordinate overlay locations, so for example
		// color key doesn't draw over us.
		String l = myW2DataView.getTitle();
		if (WindowManager.isTopDataView(myW2DataView)) {
			l += " (Main)";
		}

		// Visually see how much we're redrawing for debugging. We should only draw on
		// window
		// changing in some way, such as size or content. We're gonna have a lot of
		// windows.
		l += " Frame:" + Integer.toString(myDrawCounter);
		l += " " + this;

		myText.begin3DRendering();
		Rectangle2D bounds = myText.getBounds(l);
		final int x = 10;
		final int y = (int) (bounds.getHeight() - m - m - m);

		// bounds.setRect(bounds.getX() + x, bounds.getY() +y, bounds.getWidth(),
		// bounds.getHeight());
		GLUtil.cheezyOutline(myText, l, Color.WHITE, Color.BLACK, (int) x, (int) y);

		// Do readout inside
		myText.end3DRendering();

		// if (myDrawCounter == 20) {
		// saveImage(gl, w, h);
		// System.exit(1);
		// }
		GL3 gl3 = gl.getGL3();
		toBuffer(gl3, w, h);

		// Do readout here AFTER buffer capture...
		if (myIn) {
			renderReadoutOverlay(gl, leftX, leftY);
		}

		GLUtil.popOrtho2D(gl);

		gl.getContext().release();

	}

	/** This can be called just by undocking, etc...so don't clean up GL here */
	@Override
	public void dispose(GLAutoDrawable drawable) {
		// LOG.error("****************************DISPOSE CALLED FOR THIS
		// OBJECT!!!!!!");
	}

	/** Init can be called on docking change as well, so don't 'reset' everything */
	@Override
	public void init(GLAutoDrawable drawable) {
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		glu = new GLU(); // get GL Utilities

		// Shouldn't this be part of the camera setup to be honest?
		if (height == 0) {
			height = 1; // prevent divide by zero
		}
		// float aspect = (float)width / height;

		// Changing width or height automatically changes the scene and needs to
		// be redrawn in openGL, and invalids the background buffer..
		if (myLastHeight != height) {
			setSceneChanged();
			myLastHeight = height;
			myBuffer = null; // Only recreate buffer when size changes, otherwise fill old one
		}
		if (myLastWidth != width) {
			setSceneChanged();
			myLastHeight = width;
			myBuffer = null;
		}

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// We'll bring up our pop up ourselves, avoids
		// issues with multiple windows..
		boolean rightDown = (e.getButton() == MouseEvent.BUTTON3);
		if (rightDown) {
			JPopupMenu m = myW2DataView.getMainPopupMenu();
			m.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/** Setup readout point location */
	public void doReadoutGroupRender(GL gl, int x, int y, int mode) {
		myIn = true;
		leftX = x;
		leftY = y;
		gl.getContext().makeCurrent();
		W2GLWorld glw = setUpGLWorld(gl, canvas.getWidth(), canvas.getHeight());
		myReadoutPoint = glw.project2DToEarthSurface(leftX, leftY, 0);
		WindowManager.syncWindows(myW2DataView, mode, myReadoutPoint, myIn);
	}

	@Override
	public void mouseEntered(MouseEvent e) {

		doReadoutGroupRender(canvas.getGL(), e.getX(), canvas.getHeight() - e.getY(), 0);
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		myIn = false;
		WindowManager.syncWindows(myW2DataView, 1, myReadoutPoint, myIn);
	}

	@Override
	public void mousePressed(MouseEvent e) {

		leftDown = (e.getButton() == MouseEvent.BUTTON1);
		middleDown = (e.getButton() == MouseEvent.BUTTON2);
		shiftDown = e.isShiftDown();
		ctrlDown = e.isControlDown();
		
		// Full redraw on mouse down...for picked objects
		//setSceneChanged();
		doReadoutGroupRender(canvas.getGL(), e.getX(), canvas.getHeight() - e.getY(), 0);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		leftDown = false;
		middleDown = false;
		
		// Full redraw on mouse release...for picked objects.
		//setSceneChanged();
		doReadoutGroupRender(canvas.getGL(), e.getX(), canvas.getHeight() - e.getY(), 0);
	}

	// MouseMotion stuff...
	@Override
	public void mouseDragged(MouseEvent e) {
		// Camera action?

		// Snag x,y, set up canvas for mathz
		final int x = e.getX();
		final int y = canvas.getHeight() - e.getY();
		LOG.error("DRAG X/Y is "+x+", "+y);
		int dx = x - leftX;
		int dy = y - leftY;

		shiftDown = e.isShiftDown();

		// I 'think' if dx/dy are zero we can quick break out...
		if ((dx == 0) && (dy == 0)) {
			return;
		}

		if (leftDown || middleDown) {
			GL gl = canvas.getGL();
			gl.getContext().makeCurrent();
			myCamera.prepMouseDownFlags(gl, x, y, dx, dy);

			if (leftDown) {
				myCamera.dragPan(-dx, -dy, shiftDown);
			} else if (middleDown) // what if both buttons down? eh? eh??
			{
				myCamera.zoom(dx, dy, shiftDown);
			}

			doReadoutGroupRender(gl, x, y, 0);

			/*
			 * setSceneChanged(); canvas.display(); renderReadoutOverlay(gl, leftX, leftY);
			 */
			gl.getContext().release();

		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		LOG.error("INCOMING X/Y is "+e.getX()+", "+e.getY());
		leftX = e.getX(); // Pick using left and y
		leftY = canvas.getHeight() - e.getY();
		
		doReadoutGroupRender(canvas.getGL(), e.getX(), canvas.getHeight() - e.getY(), 1);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		// Snag x,y, set up canvas for mathz
		final int x = e.getX();
		final int y = canvas.getHeight() - e.getY();
		int dx = x - leftX;
		int dy = y - leftY;

		// Not sure we need this here....
		GL gl = canvas.getGL();
		gl.getContext().makeCurrent();
		myCamera.prepMouseDownFlags(gl, x, y, dx, dy);

		// Could be zero for a while with a super resolution wheel...
		// we'll wait the a strictly positive or negative before
		// doing anything. Negative is away from user, positive towards..
		int wheelRotation = e.getWheelRotation();
		if (wheelRotation != 0) {
			final int wheelY = (wheelRotation > 0) ? -20 : 20;
			myCamera.zoom(0, wheelY, shiftDown);
			doReadoutGroupRender(canvas.getGL(), x, y, 0);
		}
		gl.getContext().release();
	}

	/** Call back from window manager to render as part of a window group */
	public void doSyncGroup(Window win, int mode, V3 readoutPoint, boolean inside) {
		if (!(win instanceof W2DataView)) {
			return; // FIXME: eventually sync could come from a non W2DataView..such as a vslice
					// chart
		}
		W2DataView w = (W2DataView) (win);
		boolean fullRedraw = false;
		boolean readout = (mode == 1);

		GL gl = canvas.getGL();
		gl.getContext().makeCurrent();

		// It's us calling for a camera change..or a readout
		if (w == myW2DataView) {
			fullRedraw = (mode == 0);
		} else { // We're another window
			// If camera is different need full redraw on any mode...
			final GLBoxCamera c = w.getGLCamera();
			float[] l = c.getLocation();
			fullRedraw = myW2DataView.getGLCamera().goToLocation(l);

			// If fully redrawing, or readout overlay..project our readout from other's
			// point
			if (fullRedraw || readout) {
				W2GLWorld glw = setUpGLWorld(gl, canvas.getWidth(), canvas.getHeight());
				V2 v = glw.project(readoutPoint);
				leftX = (int) v.x;
				leftY = (int) v.y;
				myIn = inside; // Other box is inside, so we are too
			}
		}

		// Do actual redraw or back copy...
		if (fullRedraw) {
			setSceneChanged();
		} else if (readout) {
			setDirty();
		}
		canvas.display();
		gl.getContext().release();
	}
}

final class myPopupActionListener implements ActionListener {
	public W2DataView myView = null;
	private int myGroup = -100;
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(myPopupActionListener.class);

	public myPopupActionListener(W2DataView v) {
		myView = v;
	}

	public myPopupActionListener(W2DataView v, int group) {
		myView = v;
		myGroup = group;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String c = e.getActionCommand();
		// LOG.error("MENU ITEM CALLED! [" + c + "]");
		WdssiiCommand command = null;
		if (c == W2DataView.RENAME_MENU) {
			command = new ChartRenameCommand(new ChartRenameParams(myView.getTitle(), "NEWNAME"));
			command.setConfirmReport(true, true, (JComponent) myView.getGUI());
		} else if (c == W2DataView.SWAP_MENU) {
			command = new ChartSwapCommand(new ChartSwapParams(myView.getTitle()));
			command.setConfirmReport(true, true, (JComponent) myView.getGUI());
		} else if (c == W2DataView.DELETE_MENU) {
			command = new ChartDeleteCommand(new ChartDeleteParams(myView.getTitle()));
			command.setConfirmReport(true, true, (JComponent) myView.getGUI());
		} else {
			// Assume Sync command
			if (myGroup > -100) {
				command = new ChartSetGroupNumberCommand(new ChartSetGroupNumberParams(myView.getTitle(), myGroup));
			}
		}
		if (command != null) {
			CommandManager.getInstance().executeCommand(command, false);
		}
	}

}

public class W2DataView extends DataView {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(W2DataView.class);
	public static final String RENAME_MENU = "Rename...";
	public static final String DELETE_MENU = "Delete this window...";
	public static final String SWAP_MENU = "Swap with main window";

	/** Contain or subclass...humm FIXME: */
	private W2DataViewListener myListener = null;

	/**
	 * Static method to create a vslice chart, called by reflection
	 */
	public static W2DataView create() {

		return new W2DataView();

	}

	/** Get camera for this W2DataView */
	public GLBoxCamera getGLCamera() {
		return myListener.myCamera;
	}

	@Override
	public void repaint() {
		if (myListener != null) {
			myListener.setSceneChanged();
		}
	}

	/**
	 * Update chart when needed (check should be done by chart)
	 */
	@Override
	public void updateChart(boolean force) {
		if (myListener != null) {
			myListener.setSceneChanged();
		}
	}

	@Override
	public Object getNewGUIForChart(Object parent) {

		// boolean heavyweight = true;

		// Make sure all W2DataViews use same GLContext to share resources.
		GLAutoDrawable sharedDrawable = GLCacheManager.getInstance().getSharedGLAutoDrawable();
		GLJPanel canvas = new GLJPanel();
		canvas.setSharedAutoDrawable(sharedDrawable);

		// Listener.
		W2DataViewListener test = new W2DataViewListener(this);
		myListener = test;
		test.canvas = canvas;
		canvas.addGLEventListener(test);
		canvas.addMouseListener(test);
		canvas.addMouseMotionListener(test);
		canvas.addMouseWheelListener(test);
		canvas.setRequestFocusEnabled(true);

		// I'll do it myself when I know it's ready...
		// canvas.setAutoSwapBufferMode(false);

		// Bleh not sure I like how this works. Currently have to select
		// window before popup will work...
		// JPopupMenu menu = getMainPopupMenu();
		// canvas.setComponentPopupMenu(menu);

		// Really? FIXME: we're gonna need dirty handling, this gonna hammer display
		// Seems to work without this now, except for overlay..if we can get that to
		// work properly, dump this...
		final FPSAnimator animator = new FPSAnimator(canvas, 300, true);
		animator.start();

		return canvas;
	}

	/** Add a popup menu to the view */
	public JPopupMenu getMainPopupMenu() {

		myPopupActionListener al = new myPopupActionListener(this);

		JPopupMenu popupmenu = new JPopupMenu();

		JMenuItem i;
		// Only if this window isn't main window...
		if (!WindowManager.isTopDataView(this)) {
			i = new JMenuItem("Swap with main window");
			popupmenu.add(i);
			i.addActionListener(al);
		}

		// Menu for changing sync groups. Since state
		// is stored by us, no need for button group...
		int group = this.getGroupNumber() - 1; // -1 since zero is reserved for none
		JMenu sub1 = new JMenu("Set Group");
		JRadioButtonMenuItem zz = new JRadioButtonMenuItem("No Sync Group");
		zz.setSelected(group < 0);
		zz.addActionListener(new myPopupActionListener(this, 0));
		sub1.add(zz);
		final int maxGroups = WindowManager.getMaxGroups();
		for (int x = 0; x < maxGroups; x++) {
			zz = new JRadioButtonMenuItem("Sync Group '" + (char) ('A' + x) + "'");
			zz.setSelected(group == x);
			zz.addActionListener(new myPopupActionListener(this, x + 1));
			sub1.add(zz);
		}
		popupmenu.add(sub1);

		popupmenu.add(new JSeparator());

		i = new JMenuItem(W2DataView.RENAME_MENU);
		popupmenu.add(i);
		i.addActionListener(al);

		// "Take Snapshot..."

		// FIXME: What to do when only one window...wg just doesn't do anything in that
		// case
		popupmenu.add(new JSeparator());
		i = new JMenuItem("Delete this window...");
		popupmenu.add(i);
		i.addActionListener(al);

		return popupmenu;
	}

	@Override
	public void doSyncGroup(Window w, int mode, V3 readoutPoint, boolean inside) {
		myListener.doSyncGroup(w, mode, readoutPoint, inside);
	}
}
