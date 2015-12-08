package org.wdssii.gui.worldwind;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.wdssii.gui.GLWorld;
import org.wdssii.gui.ProductManager;
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
import org.wdssii.gui.views.DataFeatureView;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

/*
 * @author Robert Toomey
 * 
 * We steal worldwind layers into our Feature classes so we can create
 * custom GUIS and control rendering order
 */
public class WJSceneController extends BasicSceneController {

    private final static Logger LOG = LoggerFactory.getLogger(WJSceneController.class);
    private LLHAreaLayer myLLHAreaLayer;
    private WorldWindDataView myWorld;
    private DrawContext theContext;
    private GLWorldWW myGLWorld;
    
    public static final String WORLDWIND = "WW";
    public static final String WW_RENDERERS = "org.wdssii.gui.worldwind";

    /** Since worldwind requires controller by XML reflection, initial extra stuff here */
    public void setRequiredInfo(WorldWindDataView w, LLHAreaLayer layer){
       myWorld = w;
       myLLHAreaLayer = layer;
    }
    
    public GLWorld setupGLWorld(DrawContext dc){
    	if (dc != theContext){
    		 myGLWorld  = new GLWorldWW(dc, myWorld);
    		 myWorld.setGLWorld(myGLWorld); // so LLHController can call get
    	}
    	return myGLWorld;
    }
    
    @Override
    public PickedObjectList getPickedObjectList() {
        PickedObjectList l = super.getPickedObjectList();
        //if (l != null) {
        //    PickedObject o = l.getTopPickedObject();
        //}
        return l;
    }

    @Override
    protected void preRender(DrawContext dc) {

        // Make sure orderedRenderables added properly
        FeatureList f = ProductManager.getInstance().getFeatureList();
        GLWorld w = setupGLWorld(dc);
        preRenderFeatureGroup(w, LegendFeature.LegendGroup);
        preRenderFeatureGroup(w, WorldwindStockFeature.Group);

        // Pre-render the deferred/ordered surface renderables.
        this.preRenderOrderedSurfaceRenderables(dc);
    }

    @Override
    protected void draw(DrawContext dc) {
        try {
        	GLWorld w = setupGLWorld(dc);
            FeatureList f = ProductManager.getInstance().getFeatureList();

            // Worldwind basemaps
            renderFeatureGroup(w, WorldwindStockFeature.Group);

            // Products
            renderFeatureGroup(w, ProductFeature.ProductGroup);

            // Render the LLHRenderer stuff...
            renderFeatureGroup(w, LLHAreaFeature.LLHAreaGroup);
            if (myLLHAreaLayer != null) {
            	myLLHAreaLayer.draw(w);
            }
            
            // Have to draw last, so that stipple works 'behind' product...
            // It's 'behind' but actually renders on top..lol
            renderFeatureGroup(w, MapFeature.MapGroup);
            renderFeatureGroup(w, PolarGridFeature.PolarGridGroup);
            renderFeatureGroup(w, LegendFeature.LegendGroup);
           
            // Draw the deferred/ordered surface renderables.
            // This is all the 2d stuff on top...
            // We do our icons ourselves now
            // this.drawOrderedSurfaceRenderables(dc);

            // Draw the deferred/ordered renderables.
            dc.setOrderedRenderingMode(true);
            while (dc.peekOrderedRenderables() != null) {
                OrderedRenderable test = dc.peekOrderedRenderables();
                try {
                    dc.pollOrderedRenderables().render(dc);
                } catch (Exception e) {
                }
            }
            dc.setOrderedRenderingMode(false);

            //LOG.debug("RENDERED " + counter + " ordered");

            // First attempt draw the DataViews
            DataFeatureView.drawViewsInWindow(w);

        } catch (Throwable e) {
            LOG.error("Exception during render " + e.toString());
        }
    }

    @Override
    protected void pickLayers(DrawContext dc) {

        // For now just pick legend layer or 3D layer buttons
        FeatureList f = ProductManager.getInstance().getFeatureList();
        GLWorldWW w = new GLWorldWW(dc, myWorld);
        pickFeatureGroup(w, dc.getPickPoint(), LegendFeature.LegendGroup);
       
        
        // 3d layer (should be 3d group right? */
        if (myLLHAreaLayer != null) {
           
                //dc.setCurrentLayer(myLLHAreaLayer);
               // myLLHAreaLayer.pick(dc, dc.getPickPoint());
                myLLHAreaLayer.pick(w, pickPoint);
            
        }
        
        dc.setCurrentLayer(null);
    }

    /**
     * preRender all features that are in the given group
     */
    public void preRenderFeatureGroup(GLWorld w, String g) {

        FeatureList fl = ProductManager.getInstance().getFeatureList();
        List<Feature> list = fl.getActiveFeatureGroup(g);
        Iterator<Feature> iter = list.iterator();
        while (iter.hasNext()) {
            Feature f = iter.next();
            FeatureMemento m = f.getMemento();

            //  ArrayList<FeatureRenderer> theList = myMap.get(f);
            ArrayList<FeatureRenderer> theList = f.getRendererList(WORLDWIND, WW_RENDERERS);
            if (theList != null) {
                for (FeatureRenderer fr : theList) {
                    if (fr instanceof Feature3DRenderer) {
                        Feature3DRenderer a3d = (Feature3DRenderer) (fr);
                        a3d.preRender(w, m);
                    }
                }
            }
        }
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
                    ArrayList<FeatureRenderer> theList = f.getRendererList(WORLDWIND, WW_RENDERERS);
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

    /**
     * Pick all features that are in the given group
     */
    public void pickFeatureGroup(GLWorld w, Point p, String g) {

        FeatureList fl = ProductManager.getInstance().getFeatureList();
        List<Feature> list = fl.getActiveFeatureGroup(g);
        Iterator<Feature> iter = list.iterator();
        while (iter.hasNext()) {
            Feature f = iter.next();
            // f.pick(w, p);

            FeatureMemento m = f.getMemento();
            // ArrayList<FeatureRenderer> theList = myMap.get(f);
            ArrayList<FeatureRenderer> theList = f.getRendererList(WORLDWIND, WW_RENDERERS);
            if (theList != null) {
                for (FeatureRenderer fr : theList) {
                    if (fr instanceof Feature3DRenderer) {
                        Feature3DRenderer a3d = (Feature3DRenderer) (fr);
                        a3d.pick(w, p, m);
                    }
                }
            }
        }
    }
}
