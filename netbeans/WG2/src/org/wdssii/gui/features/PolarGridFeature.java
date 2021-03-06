package org.wdssii.gui.features;

import java.awt.Color;
import java.util.ArrayList;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.geom.V2;

/**
 * A PolarGrid feature renders a 3d set of range circles in a 3d window around a
 * center point. It can also use elevation.
 *
 * @author Robert Toomey
 */
public class PolarGridFeature extends Feature {

    /**
     * The properties of the PolarGridFeature
     */
    public static class PolarGridMemento extends FeatureMemento {

        // Properties
        public static final String LINE_THICKNESS = "line_thickness";
        public static final String LINE_COLOR = "line_color";
        public static final String CENTER = "center";
        public static final String ELEV_DEGREES = "degrees";
        public static final String RING_COUNT = "ring_count";
        public static final String RING_RANGE = "ring_range";

        public PolarGridMemento(PolarGridMemento m) {
            super(m);
        }

        public PolarGridMemento() {
            initProperty(LINE_THICKNESS, 2);
            initProperty(LINE_COLOR, Color.WHITE);
            initProperty(CENTER, new V2(35.3331, -97.2778));
            initProperty(ELEV_DEGREES, 0.5d);
            initProperty(RING_COUNT, 10);
            initProperty(RING_RANGE, 10000);
        }
    }
    private final static Logger LOG = LoggerFactory.getLogger(PolarGridFeature.class);
    public static final String PolarGridGroup = "POLARGRIDS";
    private static int counter = 1;

    /**
     * The state we use for drawing the polar grid.
     */
    public PolarGridFeature(FeatureList f) {
        super(f, PolarGridGroup, new PolarGridMemento());
        String name = "Circle" + counter++;
        setName(name);
        setKey(name);
        setMessage("");
    }

    @Override
    public ArrayList<FeatureRenderer> getNewRendererList(String type, String packageName) {
        ArrayList<FeatureRenderer> list = new ArrayList<FeatureRenderer>();
        addNewRendererItem(list, type, packageName, "PolarGridRenderer");    
        return list;
    }
        
    @Override
    public FeatureMemento getNewMemento() {
        PolarGridMemento m = new PolarGridMemento((PolarGridMemento) getMemento());
        return m;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new PolarGridGUI(this);
    }
}
