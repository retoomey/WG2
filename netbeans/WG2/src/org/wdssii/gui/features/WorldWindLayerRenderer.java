package org.wdssii.gui.features;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;

/**
 * Render a world wind layer
 *
 * @author Robert Toomey
 */
public class WorldWindLayerRenderer implements Feature3DRenderer {

    private Layer myLayer;
    /**
     * The token of the momento we use for visibility/pick check
     */
    private String myVisibleToken = "";

    public WorldWindLayerRenderer(Layer l, String visibleToken) {
        myLayer = l;
        myVisibleToken = visibleToken;
    }

    @Override
    public void preRender(DrawContext dc, FeatureMemento m) {
        Boolean on = m.getPropertyValue(myVisibleToken);
        if (on) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.preRender(dc);
            dc.setCurrentLayer(oldLayer);
        }
    }

    @Override
    public void draw(DrawContext dc, FeatureMemento m) {
        Boolean on = m.getPropertyValue(myVisibleToken);
        if (on) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.render(dc);
            dc.setCurrentLayer(oldLayer);
        }
    }

    @Override
    public void pick(DrawContext dc, Point p, FeatureMemento m) {
        Boolean on = m.getPropertyValue(myVisibleToken);
        if (on) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.pick(dc, p);
            dc.setCurrentLayer(oldLayer);
        }
    }
}
