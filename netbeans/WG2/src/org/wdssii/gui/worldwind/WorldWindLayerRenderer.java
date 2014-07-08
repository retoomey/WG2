package org.wdssii.gui.worldwind;

import org.wdssii.gui.features.Feature3DRenderer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.GLWorld;
import org.wdssii.gui.features.FeatureMemento;

/**
 * Render a world wind layer
 *
 * @author Robert Toomey
 */
public class WorldWindLayerRenderer extends Feature3DRenderer {
    private final static Logger LOG = LoggerFactory.getLogger(WorldWindLayerRenderer.class);

    // MULTIVIEW can't hold onto layer since there are multiple layer
    // objects, one per world wind chart....
    // private Layer myLayer;
    private String myLayerKey;
    /**
     * The token of the momento we use for visibility/pick check
     */
    private String myVisibleToken = "";

    public WorldWindLayerRenderer(){
        
    }
    
    public WorldWindLayerRenderer(String l, String visibleToken) {
        myLayerKey = l;
       myVisibleToken = visibleToken;
    }
    
    public void setKeyAndToken(String l, String visibleToken){
        myLayerKey = l;
        myVisibleToken = visibleToken;
    }

    /**
     * Return the worldwind layer we wrap around
     */
    //public Layer getLayer() {
    //    return myLayer;
    // }
    public boolean isVisible(FeatureMemento m) {
        Boolean on = m.getPropertyValue(myVisibleToken);
        return ((on != null) && on);
    }

    private void renderaction(GLWorld w, FeatureMemento m, int type, Object o) {
        if (w instanceof GLWorldWW) {
            GLWorldWW g = (GLWorldWW) (w);
            DrawContext dc = g.getDC();
            WorldWindDataView c = g.getWWWorld();
            if (c != null) {
                Layer l = c.getLayer(myLayerKey);
                if (l == null) {
                    LOG.debug("Layer " + myLayerKey + " is NULL");
                } else {
                    if (isVisible(m)) {
                        Layer oldLayer = dc.getCurrentLayer();
                        dc.setCurrentLayer(l);// for proper pick event
                        l.setEnabled(true);
                        switch (type) {
                            case 0:
                                l.preRender(dc);
                                break;
                            case 1: 
                                l.render(dc);
                                break;
                            case 2:
                                Point p = (Point)(o);
                                l.pick(dc, p);
                                break;
                            default:
                                LOG.error("FIX THIS Shouldn't be here");
                                break;
                        }
                        dc.setCurrentLayer(oldLayer);
                    } else {
                        l.setEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public void preRender(GLWorld w, FeatureMemento m) {
        renderaction(w, m, 0, null);
    }

    @Override
    public void draw(GLWorld w, FeatureMemento m) {
        renderaction(w, m, 1, null);
    }

    @Override
    public void pick(GLWorld w, Point p, FeatureMemento m) {
        renderaction(w, m, 2, p);
    }
}
