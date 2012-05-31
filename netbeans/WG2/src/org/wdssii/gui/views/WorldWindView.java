package org.wdssii.gui.views;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;
import gov.nasa.worldwind.view.orbit.OrbitView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import javax.media.opengl.GL;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.DataCommand;
import org.wdssii.gui.commands.Snapshot3DWorldCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;
import org.wdssii.gui.products.renderers.ProductRenderer;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.volumes.LLHAreaLayerController;
import org.wdssii.gui.worldwind.LLHAreaLayer;
import org.wdssii.gui.worldwind.ProductLayer;
import org.wdssii.gui.worldwind.ReadoutStatusBar;
import org.wdssii.gui.worldwind.WorldwindUtil;

/**
 *  WorldWindView is our view for showing a worldwind data ball.
 * 
 * @author Robert Toomey
 */
public class WorldWindView extends JThreadPanel implements CommandListener {

	private static Logger log = LoggerFactory.getLogger(WorldWindView.class);
	public static final String ID = "worldwind";
	/** Determines if we use a heavyweight or lightweight java widget for
	 * displaying an earth ball.  Heavyweight used to be faster, but didn't
	 * fit well into the lightweight swing library. Heavyweight also doesn't
	 * work as well with the docking library.
	 */
	public static final boolean USE_HEAVYWEIGHT = false;

	// ----------------------------------------------------------------
	// Reflection called updates from CommandManager.
	// See CommandManager execute and gui updating for how this works
	// Typically ANY command dealing with data will require us to refresh
	// our world view. (Data commands consist of Product movement,
	// Source add/delete, etc...)
	public void DataCommandUpdate(DataCommand command) {
		updateOnMinTime(); // load, delete, etc..	
	}
	/** The WorldWindow we contain.  Eventually we might want multiple of these */
	private WorldWindow myWorld;
	/** The worldwind layer that holds our radar products */
	private ProductLayer myProducts;
	/** The Readout */
	private ReadoutStatusBar myStatusBar;
	private TextRenderer myText = null;
	/** The 3D object layer, objects for vslice, isosurfaces, etc... */
	protected LLHAreaLayer myVolumeLayer;

	@Override
	public void updateInSwingThread(Object info) {
		//  throw new UnsupportedOperationException("Not supported yet.");
	}
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JSplitPane jSplitPane1;

	/** Our factory, called by reflection to populate menus, etc...*/
	public static class Factory extends WdssiiDockedViewFactory {

		public Factory() {
			super("Earth", "world.png");
		}

		@Override
		public Component getNewComponent() {
			return new WorldWindView();
		}
	}

	public WorldWindView() {
		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		final String w = "50"; // MigLayout width parameter

		// Create top panel
		jPanel1 = new JPanel(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		// jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 0), 2));

		// Create bottom panel
		jPanel2 = new JPanel(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 51), 2));

		// Put into split pane
		jSplitPane1 = new JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, true, jPanel1, jPanel2);
		jSplitPane1.setResizeWeight(1.0);
		add(jSplitPane1, new CC().minWidth(w).growX().growY());

		String bits = System.getProperty("sun.arch.data.model");
		if (bits.equals("32")) {
			JTextField info = new JTextField();
			info.setText("Sorry, currently no opengl 32 native library support.  You really want to run a 64 bit java if you can for this program.");
			jPanel1.add(info, new CC().growX().growY());
		} else {
			// Basic worldwind setup...
			Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
			if (USE_HEAVYWEIGHT) {
				myWorld = new WorldWindowGLCanvas();
			} else {
				myWorld = new WorldWindowGLJPanel();
			}
			myWorld.setModel(m);
			jPanel1.add((Component) (myWorld), new CC().minWidth(w).minHeight(w).growX().growY());
			jPanel1.setOpaque(false);

			// Either:
			// 1. current worldwind has a VBO bug
			// 2. Netbeans is using opengl somewhere and leaking opengl state (vbo)
			// 3. my nvidia driver is leaking..
			// 4.  something else.. lol
			// but we keep getting VBO exceptions...so turn it off in our worldwind for now...
			myWorld.getSceneController().getGLRuntimeCapabilities().setVertexBufferObjectEnabled(false);

			/** Create our personal layers */
			createWG2Layers();

			myStatusBar = new ReadoutStatusBar();
			jPanel2.add(myStatusBar, new CC().growX().growY());
			myStatusBar.setEventSource(myWorld);

			myWorld.addSelectListener(new SelectListener() {

				public void selected(SelectEvent event) {
					handleSelectEvent(event);
				}
			});
		}
		final WorldWindView theWorld = this;
		// FIXME: should probably make a preference for this that can be
		// toggled by user if it works correctly/incorrectly
		System.setProperty("sun.awt.noerasebackground", "true");
		CommandManager.getInstance().addListener(ID, this);

		delayedInit();
	}

	public final void createWG2Layers() {
		LayerList theLayers = myWorld.getModel().getLayers();

		/** The layer for standard products */
		myProducts = new ProductLayer();
		theLayers.add(myProducts);

		/** The layer for all 3D objects */
		myVolumeLayer = new LLHAreaLayer();
		myVolumeLayer.setName("3D Objects");
		theLayers.add(myVolumeLayer);
		// Controller adds listeners to world which keeps reference
		LLHAreaLayerController c = new LLHAreaLayerController(myWorld, myVolumeLayer);

		// Create and install the view controls layer and register a controller for it with the World Window.
		ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
		theLayers.add(viewControlsLayer);
		this.getWwd().addSelectListener(new ViewControlsSelectListener(this.getWwd(), viewControlsLayer));

	}

	public void takeDialogSnapshot() {
		System.out.println("Take a snapshot");
	}

	public LayerList getLayerList() {
		LayerList l = null;
		if (myWorld != null) {
			l = myWorld.getModel().getLayers();
		}
		return l;
	}

	public void setLayerEnabled(String name, boolean flag) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void updateOnMinTime() {
		if (myWorld != null) {
			myWorld.redraw();
		}
	}

	public String getProjection() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setProjection(String projection) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void loadProduct(Product aProduct) {
		if (myWorld != null) {
			myWorld.redraw();
		}
	}

	// Currently called by clicking on Jump button which is within Swing thread.
	// Calling command from different thread will require invokeLater probably.
	public void gotoLocation(Location loc) {
		gotoLocation(loc, false);
	}

	public void gotoLocation(Location loc, boolean useHeight) {
		OrbitView view = (OrbitView) (myWorld.getView());  // humm not always orbit right?
		if (view != null) {


			Position p = Position.fromDegrees(loc.getLatitude(), loc.getLongitude(), loc.getHeightKms());
			Position beginCenterPos = view.getCenterPosition();
			Position endCenterPos = p;
			Angle beginHeading = view.getHeading();
			Angle endHeading = beginHeading;
			Angle beginPitch = view.getPitch();
			Angle endPitch = beginPitch;
			double beginZoom = view.getZoom();   // here to set the 'height'
			double endZoom = useHeight? loc.getHeightKms(): beginZoom;
			long timeToMove = 2000;
			// boolean endCenterOnSurface = true;
			view.addAnimator(FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(
				view,
				beginCenterPos, endCenterPos,
				beginHeading, endHeading,
				beginPitch, endPitch,
				beginZoom, endZoom,
				timeToMove,
				1)); // What does animationMode mean?

			// OrbitView ov, Position pstn, Position pstn1, Angle angle, Angle angle1, Angle angle2, Angle angle3, double d, double d1, long l, int i
			view.firePropertyChange(AVKey.VIEW, null, view);
		}
	}

	public WorldWindow getWwd() {
		return myWorld;
	}

	public void DrawProductReadout(DrawContext dc) {
		Product aProduct = ProductManager.getInstance().getTopProduct();
		if (aProduct == null) {
			return;
		}
		Point p1 = dc.getPickPoint();
		if (p1 != null) {
			Rectangle rect = myWorld.getView().getViewport();
			ProductReadout pr = aProduct.getProductReadout(p1, rect, dc);
			String readout = pr.getReadoutString();
			myStatusBar.setReadoutString(readout);
			drawLabel(dc, readout, new Vec4(p1.x, p1.y, 0), Color.BLACK);

			/** Make data table view follow mouse...
			CommandListener c = CommandManager.getInstance().getNamedCommandListener(TableProductView.ID);
			if (c != null){
			TableProductView t = (TableProductView)(c);
			// Location should be from the product, not the status bar, but for moment I think
			// it will work...
			Position p = myStatusBar.lastPosition;
			Location loc = new Location(p.latitude.degrees, p.longitude.degrees, p.elevation/1000.0);
			t.scrollLocationToVisible(loc);
			}
			 */
			//System.out.println("READOUT BACK IS:"+aProduct.getReadoutString(p1, dc));
		}
	}

	/** Currently the drawer for readout. */
	public void drawLabel(DrawContext dc, String text, Vec4 screenPoint, Color textColor) {
		int x = (int) screenPoint.x();
		int y = (int) screenPoint.y();

		// We have to use VISIBLE viewport with lightweight in order to
		// get correct height to invert:
		Rectangle visibleRect = myWorld.getView().getViewport();
		int fullH = (int) (visibleRect.getHeight());
		y = fullH - y - 1;  // Invert Y for openGL...

		// if (myText == null) {  // Only create once for speed.  We draw a LOT
		myText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);

		// Bounds calculations.  We'll create an ortho projection based upon
		// the real size of the window.  With lightweight, this might be
		// bigger than the visible window.  Heavyweight it will match.
		// So orth might be 0-1000 in the Y, while 0-200 is the view
		// that is showing.
		java.awt.Rectangle viewport = dc.getView().getViewport();

		Rectangle2D rect = myText.getBounds(text);

		//	final int fontYOffset = 5;
		//	final Rectangle2D maxText = myText.getBounds("gW"); // a guess of
		// size
		// (FIXME:
		// better
		// guess?)
		//	final int textHeight = (int) (maxText.getHeight());	
		//	final int bheight = textHeight + fontYOffset + fontYOffset;
		//	int top = viewport.y + bheight - 1;
		//	int bottom = top - bheight;

		boolean attribsPushed = false;
		boolean modelviewPushed = false;
		boolean projectionPushed = false;

		GL gl = dc.getGL();

		try {
			gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
				| GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
				| GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
				| GL.GL_CURRENT_BIT);
			attribsPushed = true;

			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDisable(GL.GL_DEPTH_TEST);

			// Load a parallel projection with xy dimensions (viewportWidth,
			// viewportHeight)
			// into the GL projection matrix.
			gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
			gl.glPushMatrix();
			projectionPushed = true;

			gl.glLoadIdentity();
			gl.glOrtho(0d, viewport.width, 0d, viewport.height, -1,
				1);
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glPushMatrix();
			modelviewPushed = true;

			gl.glLoadIdentity();

			gl.glColor3i(0, 0, 255);
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex2f(x, y);
			gl.glVertex2f(x, y + (float) rect.getHeight());

			gl.glVertex2f(x + (float) rect.getWidth(), y + (float) rect.getHeight());
			gl.glVertex2f(x + (float) rect.getWidth(), y);
			gl.glEnd();
			myText.begin3DRendering();
			myText.setColor(Color.BLACK);
			myText.draw(text, x + 1, y + 1);
			myText.setColor(Color.WHITE);
			myText.draw(text, x, y);
			myText.end3DRendering();

		} catch (Exception e) {
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

	public void getColor(int x, int y) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public LLHAreaLayer getVolumeLayer() {
		return myVolumeLayer;
	}

	/** Have to pass these down to the product renderers somehow...
	If we do that though, then popups might overlap each other if we
	have multiple stuff trying to draw.  Might be better to keep one global
	select for any window....*/
	public void handleSelectEvent(SelectEvent e) {
		Product aProduct = ProductManager.getInstance().getTopProduct();
		if (aProduct == null) {
			return;
		}

		// Dispatch different events.  Should we just pass the SelectEvent in?
		if (e.getEventAction().equals(SelectEvent.ROLLOVER)) {

			ProductRenderer r = aProduct.getRenderer();
			if (r != null) {
				r.highlightObject(e.getTopObject());
			}
		}
	}

	/** Worldwind has jogl/openGL plus decking library, not sure how to do this better.
	 * Delay some stuff until world ball is fully realized....this includes the openGL context
	 * 
	 */
	public final void delayedInit() {
		//if (false) return;
		
		// Delayed due to starting up threads...
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// Stupid swing we can't do a jump command until after the worldwindview is
				// realized...we'll check valid in a loop once it is we'll move than
				// stop
				boolean ready = false;
				while (!ready) {
					boolean valid = false;
					if (myWorld != null){
						try {
							myWorld.redrawNow(); // Exception if not ready yet...
							ready = true;
						} catch (Exception e) {
							try {
								Thread.sleep(1000); // So what's better then?
							} catch (InterruptedException ex) {
							}
						}
					}
				}
				// Redraw didn't fail so we are good to go...
				Position p = WorldwindUtil.start;
				Location l = new Location(p.latitude.degrees, p.longitude.degrees, p.elevation);
				WorldWindView e = CommandManager.getInstance().getEarthBall();
				CommandManager.getInstance().getEarthBall().gotoLocation(l, true);
				WorldwindUtil.inPosition = true;
			}
		};
	    // Jobmanager not ready either (it uses swing to update job statuses) grrrrr
            Thread t = new Thread(r, "LazyEarthBallInit");
	    t.start();
	}
}