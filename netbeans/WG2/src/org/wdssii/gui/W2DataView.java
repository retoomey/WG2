package org.wdssii.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;

import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.log.LoggerFactory;

import com.jogamp.opengl.util.FPSAnimator;

/** Should we use an interface to 'hide' the mouse structure?  This
 * is useful if we ever share code with another toolkit.
 * @author Robert Toomey
 *
 */
enum W2ButtonState { NoButton, LeftButton, MidButton, RightButton };
enum W2WheelState { WheelNone, WheelIn, WheelOut };
final class myMouseEvent{
    int x;                // Current x for event
    int y;                // Current y for event
    int dx;               // Change since last x
    int dy;               // Change since last y

    W2ButtonState button; // One button only

    boolean leftButton;      // Flags for individual buttons
    boolean middleButton;
    boolean rightButton;

    boolean shiftDown;       // Is shift key down?
    boolean ctrlDown;        // Is the control key down?

    boolean _pressed;        // Flag for a pressed mouse button
    boolean _released;       // Flag for a released mouse button
    boolean _moving;         // Flag for a moving mouse

    W2WheelState _wheel_type;

    public boolean ButtonDown ()  { return _pressed; }
    public boolean ButtonUp ()  { return _released; }
    public boolean Moving ()  { return _moving; }
    public W2WheelState Wheeling ()  { return _wheel_type; }
 }
 
final class GLGoopWorld extends GLWorld {
	private final W2DataView world;

	public GLGoopWorld(GL aGL, int width, int height, W2DataView w ) {
		super(aGL, width, height);
		world = w;
	}

	@Override
	public V2 project(V3 a3d) {
	
		return null;
	}

	@Override
	public V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters) {
		double r = D3.EARTH_RADIUS_KMS+(heightMeters/1000.0f);
		double phi = Math.toRadians(lonDegrees);
		double beta = Math.toRadians(latDegrees);
		double cos_beta = Math.cos(beta);
		double x = r*Math.cos(phi)*cos_beta;
		double y = r*Math.sin(phi)*cos_beta;
		double z = r*Math.sin(beta);
		return new V3(x,y,z);
		
	}

	@Override
	public V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters) {
		double r = D3.EARTH_RADIUS_KMS+(heightMeters/1000.0f);
		double phi = Math.toRadians(lonDegrees);
		double beta = Math.toRadians(latDegrees);
		double cos_beta = Math.cos(beta);
		double x = r*Math.cos(phi)*cos_beta;
		double y = r*Math.sin(phi)*cos_beta;
		double z = r*Math.sin(beta);
		return new V3(x,y,z);
	}

	@Override
	public V3 projectV3ToLLH(V3 in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V3 project2DToEarthSurface(double x, double y, double elevation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getElevation(float latDegrees, float lonDegrees) {
		return 0;  // WG is flat as a pancake sphere
	}

	@Override
	public double getElevation(double latDegrees, double lonDegrees) {
		return 0;  // Flat
	}

	@Override
	public boolean isPickingMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getVerticalExaggeration() {
		return 0;
	}

	@Override
	public boolean inView(V3 a3d) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void redraw() {
	  // GOOP
	}

	
}

final class testGL implements GLEventListener,
    // Oh only in Java.  Couldn't we just have one it's not like there
	// are a billion methods, lol...
	MouseListener, MouseMotionListener, MouseWheelListener
	{
    private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(testGL.class);

   
	   private GLU glu;  // for the GL Utility
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
	
	// ^^^^^ End mouse stuff

    public testGL(W2DataView w2DataView) {
    	myW2DataView = w2DataView;
	}

	/**
     * Render all features that are in the given group
     */
    public void renderFeatureGroup(GLWorld w, String g) {

        FeatureList fl = ProductManager.getInstance().getFeatureList();
        List<Feature> list = fl.getActiveFeatureGroup(g);

        // For each rank...draw over lower ranks...
        for (int i = 0; i <= Feature.MAX_RANK; i++) {
            Iterator<Feature> iter = list.iterator();
            while (iter.hasNext()) {
                Feature f = iter.next();
                if (f.getRank() == i) {
                    // f.render(w);
                    FeatureMemento m = f.getMemento();
                    ArrayList<FeatureRenderer> theList = f.getRendererList("WW", "org.wdssii.gui.worldwind");
                    //ArrayList<FeatureRenderer> theList = myMap.get(f);
                    if (theList != null) {
                        for (FeatureRenderer fr : theList) {
                            if (fr instanceof Feature3DRenderer) {
                                Feature3DRenderer a3d = (Feature3DRenderer) (fr);
                                a3d.draw(w, m);
                            }
                        }
                    }

                }
            }
        }
    }
    
	@Override
	public void display(GLAutoDrawable drawable) {
		//LOG.error("DISPLAY CALLED!");
		// TODO Auto-generated method stub
		//LOG.error("DISPLAY CALLED!!!!"+ drawable);
		
		// FIXME: This gonna get called way too much for multi windows, need
		// background saving
		   GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context
		   gl.getContext().makeCurrent();
		   
		   // Set up camera GL on render always would be better right?
		   GLU glu = new GLU();
		   myCamera.setUpCamera(gl, glu, 0, 0, drawable.getWidth(), drawable.getHeight());
		   
		   GLGoopWorld w = new GLGoopWorld(gl, drawable.getWidth(), drawable.getHeight(), myW2DataView);
		   
           // Products
		   // dirty flag with overlay would be good here..
		   gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear color and depth buffers
           renderFeatureGroup(w, ProductFeature.ProductGroup);
           
           renderFeatureGroup(w, MapFeature.MapGroup);
           renderFeatureGroup(w, PolarGridFeature.PolarGridGroup);
           renderFeatureGroup(w, LegendFeature.LegendGroup);
		   /*
		      gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear color and depth buffers
		      gl.glLoadIdentity();  // reset the model-view matrix
		 
		      // ----- Your OpenGL rendering code here (Render a white triangle for testing) -----
		      gl.glTranslatef(0.0f, 0.0f, -6.0f); // translate into the screen
		      if (myIn) {
		    	  gl.glColor3f(1.0f, 0.0f, 0.0f);
		      }else {
			      gl.glColor3f(0.0f, 1.0f, 0.0f);

		      }
		      gl.glBegin(GL2.GL_TRIANGLES); // draw using triangles
		         gl.glVertex3f(0.0f, 1.0f, 0.0f);
		         gl.glVertex3f(-1.0f, -1.0f, 0.0f);
		         gl.glVertex3f(1.0f, -1.0f, 0.0f);
		      gl.glEnd();
		      */
		   gl.getContext().release();
		   

				
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		//LOG.error("GOT TO INIT..let's see what happens...");
	      GL2 gl = drawable.getGL().getGL2();      // get the OpenGL graphics context
	      //glu = new GLU();                         // get GL Utilities
	      //myCamera.goToLocation(-97.60f, 33.84f, 1500.0f, 0.0f, 0.0f);
	      myCamera.goToLocation(-97.1640f, 35.1959f, 400.0f, 0.0f, 0.0f);

	      
	     /* TEST
	      gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
	      gl.glClearDepth(1.0f);      // set clear depth value to farthest
	      gl.glEnable(GL2.GL_DEPTH_TEST); // enables depth testing
	      gl.glDepthFunc(GL2.GL_LEQUAL);  // the type of depth test to do
	      gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST); // best perspective correction
	      gl.glShadeModel(GL2.GL_SMOOTH); // blends colors nicely, and smoothes out lighting
	      */
	      
	      /** WG port right
	      gl.glEnable(GL2.GL_DEPTH_TEST);
	      double mat_shininess[] = { 30.0 };
	      double mat_specular [] = { 1.0, 1.0, 1.0, 1.0 };
	      double lightPos[] =
	    	   {
	    	        view.x,
	    	        view.y,
	    	        view.z,
	    	        0.0
	    	   };

	      gl.glLightfv   ( GL2.GL_LIGHT0, GL2.GL_POSITION,  lightPos );
	      gl.glMaterialfv( GL2.GL_FRONT,  GL2.GL_SPECULAR,  mat_specular  );
	      gl.glMaterialfv( GL2.GL_FRONT,  GL2.GL_SHININESS, mat_shininess );
*/

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	      GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context
	      glu = new GLU();                         // get GL Utilities

	      // Shouldn't this be part of the camera setup to be honest?
	      
	      if (height == 0) height = 1;   // prevent divide by zero
	    //  float aspect = (float)width / height;
	 
	      // Set the view port (display area) to cover the entire window
	      gl.glViewport(0, 0, width, height);

	      // This is duplicated in setUpCamera... notice..hummm
	      
/*	      // Setup perspective projection, with aspect ratio matches viewport
	      gl.glMatrixMode(GL2.GL_PROJECTION);  // choose projection matrix
	      gl.glLoadIdentity();             // reset projection matrix
	      glu.gluPerspective(45.0, aspect, 0.1, 100.0); // fovy, aspect, zNear, zFar
	 
	      // Enable the model-view transform
	      gl.glMatrixMode(GL2.GL_MODELVIEW);
	      gl.glLoadIdentity(); // reset
*/
	      
	  // Ok do it doing redraw is better right? Maybe it can be split up for speed.
	    // myCamera.setUpCamera(gl, glu, 0, 0, width, height);
	      
	      // Try to render earth ball?

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		//LOG.error("Mouse entered again!");

		myIn = true;
        canvas.repaint();

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		//LOG.error("Mouse exited again!");

		myIn = false;
        canvas.repaint();

	}

	@Override
	public void mousePressed(MouseEvent e) {
		//LOG.error("Mouse pressed");
		//LOG.error("BUTTON 1 PRESSED is " + e.getButton());
		
		// prepOnMouseDown right?
		leftDown = (e.getButton() == MouseEvent.BUTTON1);
		middleDown = (e.getButton() == MouseEvent.BUTTON2);
		shiftDown = e.isShiftDown();
		ctrlDown = e.isControlDown();
		
		leftX = e.getX();
		leftY = canvas.getHeight()-e.getY();  // FIXME: Check under/overflow?
		//leftY = e.getY();
	


	/*	  makeCurrentBox();

		  // If we already are locked into a view, the mouse
		  // goes to it...
		  wg_GLView* m = 0;
		  if (myLockView){
		    m = myLockView;
		  }else{
		    m = getViewMouseIn(e->x, e->y);
		  }
		  myMouseView = m;

		  if (m){
		    myLockView = m;
		    m->initView(*this, false);
		    mouseToView(e, m);
		    m->handlePress(this, e);
		  }
*/
		//this.myCamera.setClip(true);
		
		/*
		  if (myGLBox){

			  // Save LOD on mouse press, will restore on release
			  toOldLOD();

			  // Set default clip to use clipping planes
			  wg_GLBoxCamera* cam = camera();
			  if(cam){ cam->setClip(true);}

			  // Let top interactor get first grab at mouse
			  bool handled = false;
			  wg_GLBoxInteractor* selected = myGLBox->getSelected();
			  if (selected != 0){
			    handled = selected->handleMousePress(e, *this);
			  }

			  // Let other interactors get a shot
			  if (!handled){
			    std::vector<wg_ViewInteractor*>* i = myGLBox->getInteractors();
			    for (interactors_t::iterator iter=i->begin();
			      iter != i->end(); ++iter){
			      if ((*iter) != selected)
			        { handled = (*iter)->handleMousePress(e, *this); }
			      if (handled) break;
			    }
			  }
			  }
	*/
		/*
		// handle zoom....
		  wg_GLBoxManager& g = wg_GLBoxManager::instance();
		  bool oldFlat = drawFlat();
		  wg_GLBoxWorldCamera* camera = (wg_GLBoxWorldCamera*)(myCamera);
		  bool needFullRedraw = camera->zoom(dx, dy, shift);
		  bool newFlat = drawFlat();
		  if (oldFlat != newFlat){ g.flatToggled(glc); }
		  if (needFullRedraw){
		    cameraChanged(glc);
		    g.syncCameras(glc);
		  }
		  g.updateGroup(glc, !needFullRedraw);
*/
		canvas.display();
		//this.myW2DataView.repaint();

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		//LOG.error("Mouse released");

		leftDown = false;
		middleDown = false;
		
	   //  myCamera.goToLocation(-97.1640f, 35.1959f, 400.0f, 0.0f, 0.0f);

		/*
		  makeCurrentBox();
		  wg_GLView* m = 0;
		  if (myLockView){
		    m = myLockView;
		  }else{
		    m = getViewMouseIn(e->x, e->y);
		  }
		  myMouseView = m;

		  // Release lock
		  myLockView = 0;  // assuming each mouse up/down separate!
		  if (m){
		    m->initView(*this, false);
		    mouseToView(e, m);
		    m-> handleRelease(this, e);
		  }
*/
		canvas.display();
		//this.myW2DataView.repaint();

	}

	// MouseMotion stuff...
	@Override
	public void mouseDragged(MouseEvent e) {
		// Camera action?
		
		// Snag x,y, set up canvas for mathz
		final int x = e.getX();
		final int y = canvas.getHeight()-e.getY();
		int dx = x - leftX;
		int dy = y - leftY;
		
		if (leftDown) {
			

			GL glold = canvas.getGL();
			glold.getContext().makeCurrent();

			myCamera.prepMouseDownFlags(glold, x, y, dx, dy);
			
			myCamera.dragPan(-dx, -dy, shiftDown);
			leftX = x;
			leftY = y;
			canvas.display();
			glold.getContext().release();

		}else if (middleDown) // what if both buttons down? eh?  eh??
		{
			GL glold = canvas.getGL();
			glold.getContext().makeCurrent();
			myCamera.prepMouseDownFlags(glold, x, y, dx, dy);

			myCamera.zoom(dx, dy, shiftDown);
			leftX = x;
			leftY = y;
			canvas.display();
			glold.getContext().release();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		GL glold = canvas.getGL();
		glold.getContext().makeCurrent();
		final int y = canvas.getHeight()-e.getY();
		D3 loc = myCamera.locationOnSphere(glold, 0,e.getX(), y);
		//LOG.error("MOVE PROJECTION: "+loc.x+", "+loc.y+", "+loc.z);
		//LOG.error("Mouse moved");

		/*// TODO Auto-generated method stub
		  makeCurrentBox();
		  wg_GLView* m = 0;
		  if (myLockView){
		    m = myLockView;
		  }else{
		    m = getViewMouseIn(e->x, e->y);
		  }
		  myMouseView = m;

		 //  TEST: have a view follow mouse
		 //  if (myViews.size() > 1){
		 //  myViews[1]->myWidth = e->x+200;
		 //  myViews[1]->myY = myHeight-(e->y);
		 // }

		  if (m){
		    m->initView(*this, false);
		    mouseToView(e, m);
		    m->handleMove(this, e);
		  }
		  */
		// From wg_GLBoxCamera.cpp
//		MouseInfo.getNumberOfButtons();
		//if (e.getButton() == MouseEvent.BUTTON1){
		//	LOG.error("BUTTON 1 MOVING is " + e.getButton());
		//}
		//myCamera.prepMouseDownFlags(glold, e.X(), y, dx, dy);
		
		//dragPan(-e->dx -e->dy, e->shiftDown);
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// TODO Auto-generated method stub
		// prepOnMouseDown right?
		/*
				leftDown = (e.getButton() == MouseEvent.BUTTON1);
				middleDown = (e.getButton() == MouseEvent.BUTTON2);
				shiftDown = e.isShiftDown();
				ctrlDown = e.isControlDown();
				
				leftX = e.getX();
				leftY = canvas.getHeight()-e.getY();  // FIXME: Check under/overflow?
				//leftY = e.getY();
			
*/
		//LOG.error("OK wheel moved event...");
		//LOG.error("Stuff "+e.getY()+", "+e.getYOnScreen()+","+e.getUnitsToScroll()+","
		//		);
		// Camera action?
		
		// Snag x,y, set up canvas for mathz
		final int x = e.getX();
		final int y = canvas.getHeight()-e.getY();
		int dx = x - leftX;
		int dy = y - leftY;
		
		// Not sure we need this here....
		GL glold = canvas.getGL();
		glold.getContext().makeCurrent();
		myCamera.prepMouseDownFlags(glold, x, y, dx, dy);
		
		// Could be zero for a while with a super resolution wheel...
		// we'll wait the a strictly positive or negative before
		// doing anything.  Negative is away from user, positive towards..
		int wheelRotation = e.getWheelRotation();
		if (wheelRotation > 0) {
			myCamera.zoom(0, -20, shiftDown);
			leftX = x;
			leftY = y;
			canvas.display();
			glold.getContext().release();
		}else if (wheelRotation < 0) {
			myCamera.zoom(0,  20, shiftDown);
			
			//myCamera.zoom(dx, dy, shiftDown);
			leftX = x;
			leftY = y;
			canvas.display();
			glold.getContext().release();
		}

	}

}


public class W2DataView extends DataView {
    /**
     * Static method to create a vslice chart, called by reflection
     */
    public static W2DataView create() {

        return new W2DataView();

    }
    
    @Override
    public Object getNewGUIForChart(Object parent) {

        //JPanel worldHolder = new JPanel();
        //worldHolder.setLayout(new WorldLayoutManager());
        //worldHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        //worldHolder.setBackground(Color.blue);
        
        boolean heavyweight = true;
       // GLCanvas canvas = new GLCanvas();
       // parent.getContentPane().add(canvas);
        GLJPanel canvas = new GLJPanel(); // lightweight is slower...
        testGL test = new testGL(this);
        test.canvas = canvas;
        canvas.addGLEventListener(test);
        
        // Do we really need three different classes?
        canvas.addMouseListener(test);
        canvas.addMouseMotionListener(test);
        canvas.addMouseWheelListener(test);
        
        // Really?
        final FPSAnimator animator = new FPSAnimator(canvas, 60, true);
        animator.start();
        
       // FPSAnimator animator = new FPSAnimator(canvas, 60);
        
        return canvas;

        //return worldHolder;
    }
}
