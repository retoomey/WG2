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

    /**
     * Return the worldwind layer we wrap around
     */
    public Layer getLayer() {
        return myLayer;
    }

    public boolean isVisible(FeatureMemento m) {
        Boolean on = m.getPropertyValue(myVisibleToken);
        return ((on != null) && on);
    }

    @Override
    public void preRender(DrawContext dc, FeatureMemento m) {
        if (isVisible(m)) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.setEnabled(true);
            myLayer.preRender(dc);
            dc.setCurrentLayer(oldLayer);
        } else {
            myLayer.setEnabled(false);
        }
    }

    @Override
    public void draw(DrawContext dc, FeatureMemento m) {
        if (isVisible(m)) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.setEnabled(true);
            myLayer.render(dc);
            dc.setCurrentLayer(oldLayer);
        } else {
            myLayer.setEnabled(false);
        }
    }

    @Override
    public void pick(DrawContext dc, Point p, FeatureMemento m) {
        if (isVisible(m)) {
            Layer oldLayer = dc.getCurrentLayer();
            dc.setCurrentLayer(myLayer);// for proper pick event
            myLayer.pick(dc, p);
            dc.setCurrentLayer(oldLayer);
        }
    }
}
