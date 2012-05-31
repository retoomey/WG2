package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import javax.swing.JComponent;
import org.wdssii.gui.ColorMapRenderer;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.views.WorldWindView;

/**
 * Legend shows the color key as well as product type, etc.
 *
 * @author Robert Toomey 
 */
public class LegendFeature extends Feature {

	public static final String LegendGroup = "2D Overlay";

	/**
	 * The properties of the LegendFeature
	 */
	public static class LegendMemento extends FeatureMemento {

		// Properties
	        public static final String SHOWMOUSE = "mousereadout";
	        public static final String SHOWLABELS = "colorkeylabels";
	        public static final String UNITS = "units";

		public LegendMemento(LegendMemento m) {
			super(m);
		}

		public LegendMemento() {
			// Override initial feature delete to false
			initProperty(CAN_DELETE, false);
			initProperty(SHOWMOUSE, true);
			initProperty(SHOWLABELS, true);
			initProperty(UNITS, "");
		}
	};

	@Override
	public FeatureMemento getNewMemento() {
		LegendMemento m = new LegendMemento((LegendMemento) getMemento());
		return m;
	}
	/**
	 * The GUI for this Feature
	 */
	private LegendGUI myControls;

	public static class simpleReadout implements Feature3DRenderer {

		@Override
		public void draw(DrawContext dc, FeatureMemento m) {
			if (!dc.isPickingMode()) {
				Boolean on = m.getProperty(LegendMemento.SHOWMOUSE);
				if (on) {
					WorldWindView v = CommandManager.getInstance().getEarthBall();
					if (v != null) {
						//System.out.println("Drawing product outline "+counter++);
						v.DrawProductReadout(dc);
					}
				}
			}
		}
	}

	/**
	 * The state we use for drawing the map.
	 */
	public LegendFeature(FeatureList f) {
		super(f, LegendGroup, new LegendMemento());
		setName("Legend");
		setKey("Legend");
		setMessage("ColorKey");
		addRenderer(new ColorMapRenderer());
		addRenderer(new simpleReadout());
	}

	@Override
	public void setupFeatureGUI(JComponent source) {

		// FIXME: general FeatureFactory..move code up into Feature
		boolean success = false;

		if (myControls == null) {
			//myControls = myFactory.createGUI(myLLHArea, source);
			myControls = new LegendGUI(this);
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
