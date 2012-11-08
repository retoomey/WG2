package org.wdssii.gui.features;

import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorldwindStockFeature is a convenient group of all the world wind layers that
 * we have built in on start up or want to have available by default
 *
 * @author Robert Toomey 
 */
public class WorldwindStockFeature extends Feature {

    private static Logger log = LoggerFactory.getLogger(WorldwindStockFeature.class);
    public static final String Group = "3D Layers";

    /**
     * The properties of the LegendFeature
     */
    public static class WorldwindStockMemento extends FeatureMemento {

        // Properties

        public WorldwindStockMemento(FeatureMemento m) {
            super(m);
        }

        public WorldwindStockMemento() {
            // Override initial feature delete to false
            initProperty(CAN_DELETE, false);
        }
    };

    @Override
    public WorldwindStockMemento getNewMemento() {
        WorldwindStockMemento m = new WorldwindStockMemento((WorldwindStockMemento) getMemento());
        return m;
    }

    /**
     * The state we use for drawing the map.
     */
    public WorldwindStockFeature(FeatureList f) {
        super(f, Group, new WorldwindStockMemento());
        setName("Basemaps");
        setKey("Basemaps");
        setMessage("Stock worldwind layers");
    }

    /** Ignore memento values and use enable flag */
    public static class WorldWindEnabledLayerRenderer extends WorldWindLayerRenderer {

        public WorldWindEnabledLayerRenderer(Layer l) {
            super(l, "");
        }

        @Override
        public void draw(DrawContext dc, FeatureMemento m) {
            super.draw(dc, m);
        }

        @Override
        public boolean isVisible(FeatureMemento m) {
            return getLayer().isEnabled();
        }
    }
    /*
     * Steal any remaining stock worldwind layers from the world wind view
     */

    public static WorldwindStockFeature createLegend(FeatureList list, LayerList ll) {
        WorldwindStockFeature f;
        f = new WorldwindStockFeature(list);
        for (Layer l : ll) {
            ll.remove(l);
            // WorldWindLayerRenderer r = new WorldWindLayerRenderer(l, WorldwindStockMemento.SHOWWORLDWIND);
            WorldWindLayerRenderer r = new WorldWindEnabledLayerRenderer(l);

            f.addRenderer(r);
        }

        return f;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new WorldwindStockGUI(this);
    }
}
