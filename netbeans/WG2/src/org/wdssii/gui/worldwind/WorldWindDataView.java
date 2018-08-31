package org.wdssii.gui.worldwind;

import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.wdssii.geom.Location;
import org.wdssii.gui.Application;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeaturePosition;
import org.wdssii.gui.features.LLHAreaSetGUI;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.renderers.ProductRenderer;
import org.wdssii.gui.volumes.LLHAreaController;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;
import gov.nasa.worldwind.view.orbit.OrbitView;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * Experimental. As I move towards multiwindow looks like stuff will be moving
 * 'up' the tree. Hopefully for better design long run.
 *
 * Create a worldwind earth view
 *
 * @author Robert Toomey
 */
public class WorldWindDataView extends DataView {

    private final static Logger LOG = LoggerFactory.getLogger(WorldWindDataView.class);
    /**
     * The WorldWindow we contain. Eventually we might want multiple of these
     */
    private WorldWindow myWorld;
    
    /** The GLWorld that we use.  It comes from the scene controller because
     * worldwind keeps the drawcontext inside that */
    private GLWorld myGLWorld;
    
    /**
     * The Readout
     */
    private ReadoutStatusBar myStatusBar;
    // Here for now I guess...
    private static LegendFeature theLegend = null;
    private static WorldwindStockFeature theWWStock = null;
    /**
     * The 3D object layer, objects for vslice, isosurfaces, etc...
     */
    private LLHAreaLayer myVolumeLayer;
    private LLHAreaController myLLHAreaController;
    private static ElevationModel theModel = null;

    // With multiview, we will probably have to synchronize render
    // and position events with multiple view types
    private class PosRenderHelper implements PositionListener,
            RenderingListener {

        @Override
        public void moved(PositionEvent pe) {
            // We need to synchronize all the status bars and 
            // other views...other view types won't be worldwind probably,
            // so we store position in feature list....
            //FeatureList.theFeatures.setTrackingPosition();
            Position newPos = pe.getPosition();
            if (newPos != null){
            float latDegrees = (float) newPos.latitude.degrees;
            float lonDegrees = (float) newPos.longitude.degrees;
            float elevKM = (float) newPos.elevation;
            FeaturePosition p = new FeaturePosition(latDegrees, lonDegrees, elevKM);
            FeatureList.getFeatureList().setTrackingPosition(p);
            }
            // This will be done globally at setTrackingPosition
            //myStatusBar.moved(pe, myWorld);
        }

        @Override
        public void stageChanged(RenderingEvent re) {
            // Not sure if I need this to be honest...
            //  myStatusBar.stageChanged(re, myWorld);
            // LOG.debug("Got stage changed event "+re);
        }
    }

    @Override
    public void setTrackingPosition(FeatureList fl, FeaturePosition f) {
        // Nothing by default
        if (myStatusBar != null){
            myStatusBar.moved(fl, f, myWorld);
        }
    }

    /**
     * Static method to create a vslice chart, called by reflection
     */
    public static WorldWindDataView create() {

        return new WorldWindDataView();

    }

    public void setGLWorld(GLWorld g){
    	myGLWorld = g;
    }
    
    public GLWorld getGLWorld(){
    	return myGLWorld;
    }
    
    public void addLegendFeature() {

    	/** Turn this off should be higher up.
    	 * However, how to 'patch' worldwind to allow layer control
    	 * Better design is for color key stuff etc. to be 'higher' 
    	 * in the design than at the view level.
    	 * FIXME: new way
    	 */
    	/*
        LayerList ll = myWorld.getModel().getLayers();
        ArrayList<String> layerNames = new ArrayList<String>();
        String compass = "";
        String scale = "";
        String insert = "";
        String controls = "";

        if (ll != null) {
            for (Layer l : ll) {
                if (l instanceof CompassLayer) {
                    compass = l.getName();
                    continue;
                }
                if (l instanceof ScalebarLayer) {
                    scale = l.getName();
                    continue;
                }
                if (l instanceof WorldMapLayer) {
                    insert = l.getName();
                    continue;
                }
                if (l instanceof ViewControlsLayer) {
                    controls = l.getName();
                    continue;
                }
            }
        }
        // FIXME: bad..dependent on worldwind to even exist...
     //   if (theLegend == null) {
      //      theLegend = LegendFeature.createLegend(FeatureList.theFeatures, compass, scale, insert, controls);
       //     FeatureList.theFeatures.addFeature(theLegend);
      //  }
      
       */
    }

    public Layer getLayer(String key) {

        Layer la = null;
        la = myWorld.getModel().getLayers().getLayerByName(key);

        int counter = 1;
        if (la == null) {
            LayerList layers = myWorld.getModel().getLayers();
            ArrayList<String> names = new ArrayList<String>();
            for (Layer l : layers) {
                LOG.debug("LAYER: " + l.getName());
            }
        }
        return la;

    }

    public void addWorldWindFeatures() {
    	/*
        if (theWWStock == null) {
            LayerList layers = myWorld.getModel().getLayers();
            ArrayList<String> names = new ArrayList<String>();
            for (Layer l : layers) {
                if (l instanceof CompassLayer) {
                    continue;
                }
                if (l instanceof ScalebarLayer) {
                    continue;
                }
                if (l instanceof WorldMapLayer) {
                    continue;
                }
                if (l instanceof ViewControlsLayer) {
                    continue;
                }
                names.add(l.getName());
            }
            theWWStock = WorldwindStockFeature.grabsAllLayers(FeatureList.theFeatures, names);
            FeatureList.theFeatures.addFeature(theWWStock);
        }
        */
    }

    public WorldWindDataView() {
    }

    public void updateChart(boolean force) {
        updateOnMinTime();
    }

    @Override
    public Object getNewGUIForChart(Object parent) {

        JPanel worldHolder = new JPanel();
        //worldHolder.setLayout(new WorldLayoutManager());
        worldHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        // Check for 32 bit.  I'm not supporting this at moment, though
        // it could be done by adding libraries. 
        String bits = System.getProperty("sun.arch.data.model");
        if (bits.equals("32")) {
            JTextArea info = new JTextArea();
            info.setText("You're running this in 32 bit java.\n Currently no opengl 32 native library support.\n You really want to run a 64 bit java for this program.");
            worldHolder.add(info, new CC().growX().growY());
            return worldHolder;
        }
        
        JTextArea info = new JTextArea();
        info.setText("Disabled.");
        worldHolder.add(info, new CC().growX().growY());
        return worldHolder;
   
        /*
        final String w = "50"; // MigLayout width parameter

        JSplitPane jSplitPane1;
        myWorld = makeWorldWindow(null);

        // JPanel worldHolder = new JPanel();
        // worldHolder.setLayout(new WorldLayoutManager());

        // worldHolder.setLayout(new MigLayout("insets 0",
        //	"[grow]",               // 1 column 
        //	"[][]"));  // 4 rows, last scroll so grow
        worldHolder.setOpaque(true);

        worldHolder.add((Component) myWorld, new CC().growX().growY());

        // for (int i = 0; i < 1; i++) {
        //     WorldWindow number2 = makeWorldWindow(myWorld);
        //     worldHolder.add((Component) number2, new CC().growX().growY());
        // }

 //       myStatusBar = new ReadoutStatusBar();
        // myStatusBar.setEventSource(myWorld);
 //       worldHolder.add((Component) myStatusBar, new CC().dockSouth());

        //  // Put into split pane
        // jSplitPane1 = new JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, true, worldHolder, myStatusBar);
        // jSplitPane1.setResizeWeight(1.0);
        // worldHolder.add(jSplitPane1, new CC().minWidth(w).growX().growY());

        // This assumes one global window...
        createWG2Layers();

        // FIXME: should probably make a preference for this that can be
        // toggled by user if it works correctly/incorrectly
     //   System.setProperty("sun.awt.noerasebackground", "true");
        // CommandManager.getInstance().addListener(ID, this);
//        PosRenderHelper h = new PosRenderHelper();
 //       myWorld.addPositionListener(h);
 //       myWorld.addRenderingListener(h);
 //       delayedInit();
      //  return worldHolder;
        */
    }

    public final void createWG2Layers() {

        // 3D Object layer for slices, etc.
        myVolumeLayer = new LLHAreaLayer();
       // myVolumeLayer.setName("3D Objects");

        // Give the 3D layer to our special scene controller...
        SceneController scene = myWorld.getSceneController();
        if (scene instanceof WJSceneController) {
            WJSceneController w = (WJSceneController) (scene);
            w.setRequiredInfo(this, myVolumeLayer);
        }
        // Controller adds listeners to world which keeps reference
        LLHAreaController c = new LLHAreaController(this.getGLWorld(),  myVolumeLayer);
        myLLHAreaController = c;
        InputHandler h = myWorld.getInputHandler();
        h.addKeyListener(c);
        h.addMouseListener(c);
        h.addMouseMotionListener(c);
        
        // FIXME: really control of these points should be in the feature
        // not within the world ball, since we might add different viewers
        LLHAreaSetGUI.setLLHAreaController(c);

        // Create and install the view controls layer and register a controller for it with the World Window.
        // This will be snagged by our LegendFeature
        ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
        LayerList theLayers = myWorld.getModel().getLayers();
        theLayers.add(viewControlsLayer);
        myWorld.addSelectListener(new ViewControlsSelectListener(myWorld, viewControlsLayer));

        // Add these features once on first creation...they will control
        // all WorldWind views
        addLegendFeature();
        addWorldWindFeatures();

        Globe globe = myWorld.getModel().getGlobe();
        ElevationModel m = globe.getElevationModel();
        setElevationModel(m);

        FeatureList.getFeatureList().addDataView(this);

    }

    /**
     * Add a new world to our collection
     */
    public final WorldWindow makeWorldWindow(WorldWindow first) {
        WorldWindow w;
        // Always use our scene controller...
        String name = WJSceneController.class.getName();
        Configuration.setValue(AVKey.SCENE_CONTROLLER_CLASS_NAME, name);

        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        // Model m2 = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        if (Application.USE_HEAVYWEIGHT_GL) {
            if (first == null) {
                w = new WorldWindowGLCanvas();
            } else {
                w = new WorldWindowGLCanvas();
            }
        } else {
            if (first == null) {
                w = new WorldWindowGLJPanel();
            } else {
                w = new WorldWindowGLJPanel();
            }
        }

        //FIXME: Problem with multiview...
        // Just broke looping...yay
        // if (first == null) {
        //    AnimateManager.setEarthView(w);
        // }

        w.setModel(m);

        // Either:
        // 1. current worldwind has a VBO bug
        // 2. Netbeans is using opengl somewhere and leaking opengl state (vbo)
        // 3. my nvidia driver is leaking..
        // 4.  something else.. lol
        // but we keep getting VBO exceptions...so turn it off in our worldwind for now...
        w.getSceneController().getGLRuntimeCapabilities().setVertexBufferObjectEnabled(false);

        // Passing something down..
        //FIXME: is this used anymore?
        w.addSelectListener(new SelectListener() {
            @Override
            public void selected(SelectEvent event) {
                handleSelectEvent(event);
            }
        });
        return w;
    }

    /**
     * Have to pass these down to the product renderers somehow... If we do that
     * though, then popups might overlap each other if we have multiple stuff
     * trying to draw. Might be better to keep one global select for any
     * window....
     */
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

    /**
     * Worldwind has jogl/openGL plus decking library, not sure how to do this
     * better. Delay some stuff until world ball is fully realized....this
     * includes the openGL context
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
                    if (myWorld != null) {
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
                // WorldWindView e = FeatureList.theFeatures.getWWView();
                // if (e != null) {
                gotoLocation(l, true);
                // }
                WorldwindUtil.setInPosition(true);
            }
        };
        // Jobmanager not ready either (it uses swing to update job statuses) grrrrr
        Thread t = new Thread(r, "LazyEarthBallInit");
        t.start();
    }

    public void gotoLocation(Location loc, boolean useHeight) {
        OrbitView view = (OrbitView) (myWorld.getView());  // humm not always orbit right?
        if (view != null) {


            Position p = Position.fromDegrees(loc.latDegrees(), loc.lonDegrees(), loc.getHeightKms());
            Position beginCenterPos = view.getCenterPosition();
            Position endCenterPos = p;
            Angle beginHeading = view.getHeading();
            Angle endHeading = beginHeading;
            Angle beginPitch = view.getPitch();
            Angle endPitch = beginPitch;
            double beginZoom = view.getZoom();   // here to set the 'height'
            double endZoom = useHeight ? loc.getHeightKms() : beginZoom;
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

    @Override
    public void updateOnMinTime() {
        if (myWorld != null) {
            myWorld.redraw();
        }
    }

    @Override
    public void repaint() {
        if (myWorld != null) {
            myWorld.redraw();
        }
    }

    @Override
    public void addViewComponent(String name, Object component) {
        if (component instanceof Layer) {
            Layer l = (Layer) (component);

            Layer exist = myWorld.getModel().getLayers().getLayerByName(l.getName());
            if (exist == null) {
                myWorld.getModel().getLayers().add(l);

                Layer exist2 = myWorld.getModel().getLayers().getLayerByName(l.getName());
                if (exist2 == null) {
                    int a = 1;
                    a = 2;
                } else {
                    int a = 1;
                    a = 2;
                }
            }
        }
    }

    public static ElevationModel getElevationModel() {
        return theModel;
    }

    // Not sure this stays if world ball dies...
    private static void setElevationModel(ElevationModel m) {
        theModel = m;
    }
}
