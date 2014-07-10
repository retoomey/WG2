package org.wdssii.gui.worldwind;

import org.wdssii.gui.features.Feature3DRenderer;
import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;

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
    public static final String WORLDWIND = "WW";
    public static final String WW_RENDERERS = "org.wdssii.gui.worldwind";
    
    /**
     * Cache renderers
     */
    //private TreeMap<Object, ArrayList<FeatureRenderer>> myMap = new TreeMap<Object, ArrayList<FeatureRenderer>>();

    public void setLLHAreaLayer(LLHAreaLayer layer) {
        myLLHAreaLayer = layer;
    }

    public void setWorld(WorldWindDataView w) {
        myWorld = w;
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
        GLWorldWW w = new GLWorldWW(dc, myWorld);
        preRenderFeatureGroup(w, LegendFeature.LegendGroup);
        preRenderFeatureGroup(w, WorldwindStockFeature.Group);

        // Pre-render the deferred/ordered surface renderables.
        this.preRenderOrderedSurfaceRenderables(dc);
    }

    @Override
    protected void draw(DrawContext dc) {
        try {
            GLWorldWW w = new GLWorldWW(dc, myWorld);
            FeatureList f = ProductManager.getInstance().getFeatureList();

            // Worldwind basemaps
            renderFeatureGroup(w, WorldwindStockFeature.Group);

            // Products
            renderFeatureGroup(w, ProductFeature.ProductGroup);

            // 3d layer
            if (myLLHAreaLayer != null) {
                dc.setCurrentLayer(myLLHAreaLayer);
                myLLHAreaLayer.render(dc);
            }
            dc.setCurrentLayer(null);

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
        // 3d layer
        if (myLLHAreaLayer != null) {
            if (myLLHAreaLayer.isPickEnabled()) {
                dc.setCurrentLayer(myLLHAreaLayer);
                myLLHAreaLayer.pick(dc, dc.getPickPoint());
            }
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
