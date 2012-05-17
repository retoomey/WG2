package org.wdssii.gui.features;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.PolarGridGUI;
import org.wdssii.gui.PolarGridRenderer;

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

		public PolarGridMemento(boolean v, boolean o, int line) {
			super(v, o);
			initProperty(LINE_THICKNESS,  1);
			initProperty(LINE_COLOR,  Color.WHITE);
			initProperty(CENTER, LatLon.fromDegrees(35.3331, -97.2778));
			initProperty(ELEV_DEGREES, 0.5d);
			initProperty(RING_COUNT, 10);
			initProperty(RING_RANGE, 10000);
		}
	}

	private static Logger log = LoggerFactory.getLogger(PolarGridFeature.class);
	public static final String PolarGridGroup = "POLARGRIDS";
	/**
	 * The renderer we use for drawing the polar grid.
	 */
	private PolarGridRenderer myRenderer;
	/**
	 * The GUI for this Feature
	 */
	private PolarGridGUI myControls;
	private static int counter = 1;

	/**
	 * The state we use for drawing the polar grid.
	 */
	public PolarGridFeature(FeatureList f) {
		super(f, PolarGridGroup, new PolarGridMemento(true, false, 2));
		myRenderer = new PolarGridRenderer();
		String name = "Circle" + counter++;
		setName(name);
		setKey(name);
		setMessage("");
	}

	@Override
	public FeatureMemento getNewMemento() {
		PolarGridMemento m = new PolarGridMemento((PolarGridMemento) getMemento());
		return m;
	}

	@Override
	public void setMemento(FeatureMemento f) {
		/**
		 * Handle polar grid mementos
		 */
		if (f instanceof PolarGridMemento) {
			PolarGridMemento mm = (PolarGridMemento) (f);
			((PolarGridMemento) getMemento()).syncToMemento(mm);
		} else {
			super.setMemento(f);
		}
	}

	/**
	 * Render a feature
	 */
	@Override
	public void render(DrawContext dc) {

		if (myRenderer != null) {
			myRenderer.draw(dc, getMemento());
		}
	}

	@Override
	public void setupFeatureGUI(JComponent source) {

		// FIXME: general FeatureFactory..move code up into Feature
		boolean success = false;
		// if (myFactory != null) {

		if (myControls == null) {
			//myControls = myFactory.createGUI(myLLHArea, source);
			myControls = new PolarGridGUI(this);
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
