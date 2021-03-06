package org.wdssii.gui.features;

/**
 * Legend shows the color key as well as product type, etc.
 *
 * @author Robert Toomey 
 */
public class LoopFeature extends Feature {

    public static final String LoopGroup = "2D Overlay";

    /**
     * The properties of the LoopFeature
     */
    public static class LoopMemento extends FeatureMemento {

        // Properties
        public static final String FRAME_TIME_MS = "frametimems";
        public static final String FIRST_DWELL_SECS = "firstdwellsecs";
        public static final String LAST_DWELL_SECS = "lastdwellsecs";
        public static final String NUM_OF_FRAMES = "numofframes";
        public static final String ROCK_LOOP = "rockloop";

        public LoopMemento(LoopMemento m) {
            super(m);
        }

        public LoopMemento() {
            // Override initial feature delete to false
            initProperty(CAN_DELETE, false);
            initProperty(FRAME_TIME_MS, 500);
            initProperty(FIRST_DWELL_SECS, 0);
            initProperty(LAST_DWELL_SECS, 1);
            initProperty(NUM_OF_FRAMES, 10);
            initProperty(ROCK_LOOP, false);
        }
    };

    @Override
    public FeatureMemento getNewMemento() {
        LoopMemento m = new LoopMemento((LoopMemento) getMemento());
        return m;
    }

    /**
     * The state we use for drawing the map.
     */
    public LoopFeature(FeatureList f) {
        super(f, LoopGroup, new LoopMemento());
        setName("Loop Controls");
        setKey("Loop");
        setMessage("Loop controls");
//		addRenderer(new ColorMapRenderer());
//		addRenderer(new simpleReadout());
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new LoopGUI(this);
    }
}
