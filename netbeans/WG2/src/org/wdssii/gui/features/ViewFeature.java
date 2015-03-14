package org.wdssii.gui.features;

/**
 * A View feature has its own separate window, that is currently held by the
 * DataView
 *
 * @author Robert Toomey
 */
public class ViewFeature extends Feature {

    public static final String ViewGroup = "DataView";

    /**
     * The properties of the ViewFeature
     */
    public static class ViewMemento extends FeatureMemento {

        // Properties 
        public ViewMemento(ViewMemento m) {
            super(m);
        }

        public ViewMemento() {
        }
    }

    /**
     * The state we use for drawing the map.
     */
    public ViewFeature(FeatureList f) {
        super(f, ViewGroup, new ViewMemento());
        setName("Window");
        setKey("WindowKey");
        setMessage("A window");
    }
}
