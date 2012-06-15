package org.wdssii.gui.features;

import javax.swing.JComponent;

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
	 * The GUI for this Feature
	 */
	private LoopGUI myControls;

	/**
	 * The state we use for drawing the map.
	 */
	public LoopFeature(FeatureList f) {
		super(f, LoopGroup, new LoopMemento());
		setName("Loop");
		setKey("Loop");
		setMessage("Looping");
//		addRenderer(new ColorMapRenderer());
//		addRenderer(new simpleReadout());
	}

	@Override
	public void setupFeatureGUI(JComponent source) {

		// FIXME: general FeatureFactory..move code up into Feature
		boolean success = false;

		if (myControls == null) {
			//myControls = myFactory.createGUI(myLLHArea, source);
			myControls = new LoopGUI(this);
		}

		// Set the layout and add our controls
		if (myControls != null) {
			myControls.activateGUI(source);
			updateGUI();
			success = true;
		}
		//  }

		/**
		 * Fill in with default stuff if GUI failed or doesn't exist
		 */
		if (!success) {
			super.setupFeatureGUI(source);
		}
	}

	@Override
	public void updateGUI() {
		if (myControls != null) {
			myControls.updateGUI();
		}
	}
}
