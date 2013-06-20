package org.wdssii.gui.features;

import gov.nasa.worldwind.layers.*;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.GLWorld;

/**
 * WorldwindStockFeature is a convenient group of all the world wind layers that
 * we have built in on start up or want to have available by default
 *
 * @author Robert Toomey 
 */
public class WorldwindStockFeature extends Feature {

    private final static Logger LOG = LoggerFactory.getLogger(WorldwindStockFeature.class);
    public static final String Group = "3D Layers";
    public Map<String, Feature3DRenderer> myLookup = new TreeMap<String, Feature3DRenderer>();

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
        setMessage("Basemap tile layers");
    }

    /**
     * Ignore memento values and use enable flag
     */
    public static class WorldWindEnabledLayerRenderer extends WorldWindLayerRenderer {

        public WorldWindEnabledLayerRenderer(Layer l) {
            super(l, "");
        }

        @Override
        public void draw(GLWorld w, FeatureMemento m) {
            super.draw(w, m);
        }

        @Override
        public boolean isVisible(FeatureMemento m) {
            return getLayer().isEnabled();
        }
    }
    /*
     * Steal any remaining stock worldwind layers from the world wind view
     */

    public static WorldwindStockFeature grabsAllLayers(FeatureList list, LayerList ll) {
        WorldwindStockFeature f;
        f = new WorldwindStockFeature(list);
        if (ll != null) {
            for (Layer l : ll) {
                ll.remove(l);
                f.addWWLayer(l);
            }
        }
        return f;
    }

    /*
     * Add a world wind layer to feature list, if not already there...
     * 
     */
    public boolean addWWComponent(JComponent root, String name, Object o) {
        boolean success = false;
        if (myLookup.containsKey(name)) {
            JOptionPane.showMessageDialog(root, "Layer already exists in basemaps",
                    "Add failure", JOptionPane.ERROR_MESSAGE);
        } else {
            if (o instanceof Layer) {
                Layer l = (Layer) (o);
                WorldWindLayerRenderer r = new WorldWindEnabledLayerRenderer(l);
                myLookup.put(name, r);
                addRenderer(r);
                JOptionPane.showMessageDialog(root, "Added layer " + name + " to basemaps",
                        "Add success", JOptionPane.INFORMATION_MESSAGE);
                success = true;
            } else {
                LOG.error("Do not know how to add this worldwind component " + o.getClass().getSimpleName());
                JOptionPane.showMessageDialog(root, "Unknown component type in WMS " + o.getClass().getSimpleName(),
                        "Add failure", JOptionPane.ERROR_MESSAGE);
            }
        }
        return success;
    }

    /*
     * Add a world wind layer to feature list, if not already there...
     * 
     */
    public void addWWLayer(Layer l) {
        WorldWindLayerRenderer r = new WorldWindEnabledLayerRenderer(l);
        addRenderer(r);
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new WorldwindStockGUI(this);
    }
}
