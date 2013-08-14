package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.WorldWindChart;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.WorldwindStockFeature;
import org.wdssii.gui.gis.MapFeature;
import org.wdssii.gui.gis.PolarGridFeature;
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
    private WorldWindChart myWorld;

    public void setLLHAreaLayer(LLHAreaLayer layer) {
        myLLHAreaLayer = layer;
    }

    public void setWorld(WorldWindChart w) {
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
        f.preRenderFeatureGroup(w, LegendFeature.LegendGroup);
        f.preRenderFeatureGroup(w, WorldwindStockFeature.Group);

        // Pre-render the deferred/ordered surface renderables.
        this.preRenderOrderedSurfaceRenderables(dc);
    }

    @Override
    protected void draw(DrawContext dc) {
        try {
            GLWorldWW w = new GLWorldWW(dc, myWorld);
            FeatureList f = ProductManager.getInstance().getFeatureList();

            // Worldwind basemaps
            f.renderFeatureGroup(w, WorldwindStockFeature.Group);

            // Products
            f.renderFeatureGroup(w, ProductFeature.ProductGroup);

            // 3d layer
            if (myLLHAreaLayer != null) {
                dc.setCurrentLayer(myLLHAreaLayer);
                myLLHAreaLayer.render(dc);
            }
            dc.setCurrentLayer(null);

            // Have to draw last, so that stipple works 'behind' product...
            // It's 'behind' but actually renders on top..lol
            f.renderFeatureGroup(w, MapFeature.MapGroup);
            f.renderFeatureGroup(w, PolarGridFeature.PolarGridGroup);
            f.renderFeatureGroup(w, LegendFeature.LegendGroup);

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
        f.pickFeatureGroup(w, dc.getPickPoint(), LegendFeature.LegendGroup);
        // 3d layer
        if (myLLHAreaLayer != null) {
            if (myLLHAreaLayer.isPickEnabled()) {
                dc.setCurrentLayer(myLLHAreaLayer);
                myLLHAreaLayer.pick(dc, dc.getPickPoint());
            }
        }
        dc.setCurrentLayer(null);
    }
}
