package org.wdssii.gui.worldwind;

import org.wdssii.gui.features.Feature3DRenderer;
import gov.nasa.worldwind.layers.Layer;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.FeatureRenderer;

/**
 * WorldwindStockFeature is a convenient group of all the world wind layers that
 * we have built in on start up or want to have available by default
 * 
 * FIXME: Humm in the new design, having a direct worldwind feature might be
 * an issue...
 * 
 * @author Robert Toomey 
 */
public class WorldwindStockFeature extends Feature {

    private final static Logger LOG = LoggerFactory.getLogger(WorldwindStockFeature.class);
    public static final String Group = "3D Layers";
    private ArrayList<FeatureRenderer> myList = new ArrayList<FeatureRenderer>();
    
    /**
     * An existence check for layer by layer.getName()
     */
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

        public WorldWindEnabledLayerRenderer(String l) {
            super(l, "");
        }

        @Override
        public void draw(GLWorld w, FeatureMemento m) {
            super.draw(w, m);
        }

        @Override
        public boolean isVisible(FeatureMemento m) {
            // return getLayer().isEnabled();
            return true;
        }
    }
    /*
     * Steal any remaining stock worldwind layers from the world wind view,
     */

    public static WorldwindStockFeature grabsAllLayers(FeatureList list, ArrayList<String> ll) {
        WorldwindStockFeature f;
        f = new WorldwindStockFeature(list);
        if (ll != null) {
            for (String l : ll) {
                f.addWWLayer(l);
            }
        }
        return f;
    }

    /*
     * Add a world wind layer to feature list, if not already there...
     * 
     */
    public boolean addWWComponent(JComponent root, String visualName, Object component) {

        boolean success = false;
        // Only can add layers..we need the layer.getName()
        if (component instanceof Layer) {
            Layer l = (Layer) (component);
            String name = l.getName(); // Not the same as the LayerInfo name..bleh
            if (myLookup.containsKey(name)) {
                JOptionPane.showMessageDialog(root, "Layer already exists in basemaps",
                        "Add failure", JOptionPane.ERROR_MESSAGE);
            } else {
                WorldWindLayerRenderer r = new WorldWindEnabledLayerRenderer(name);
                myLookup.put(name, r);
                myList.add(r);
                FeatureList.getFeatureList().addViewComponent(name, component);
                JOptionPane.showMessageDialog(root, "Added layer " + name + " to basemaps",
                        "Add success", JOptionPane.INFORMATION_MESSAGE);
                success = true;
            }
        } else {
            JOptionPane.showMessageDialog(root, "Can't add  " + visualName + " to basemaps",
                    "Add failure", JOptionPane.ERROR_MESSAGE);
            success = false;
        }

        return success;
    }

    /*
     * Add a world wind layer to feature list, if not already there...
     * 
     */
    public void addWWLayer(String l) {
        WorldWindLayerRenderer r = new WorldWindEnabledLayerRenderer(l);
        myList.add(r);
    }

    /** Only thing we handle is worldwind so we just bypass the default way */
    @Override
    public ArrayList<FeatureRenderer> getRendererList(String id, String packageName){
        return myList;
    }
    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new WorldwindStockGUI(this);
    }
}
