package org.wdssii.gui.features;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ColorMapRenderer;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.views.WorldWindView;

/**
 * Legend shows the color key as well as product type, etc.
 * 
 * We snag the standard world wind 2D overlays here, mostly because we
 * need to be able to keep things from overlapping, etc...this allows a
 * central place to configure all of that.
 *
 * @author Robert Toomey 
 */
public class LegendFeature extends Feature {

	private static Logger log = LoggerFactory.getLogger(LegendFeature.class);

	public static final String LegendGroup = "2D Overlay";

	/**
	 * The properties of the LegendFeature
	 */
	public static class LegendMemento extends FeatureMemento {

		// Properties
		public static final String SHOWMOUSE = "mousereadout";
		public static final String SHOWLABELS = "colorkeylabels";
		public static final String SHOWCOMPASS = "compass";
		public static final String SHOWSCALE = "scale";
		public static final String SHOWWORLDINSET = "inset";
		public static final String SHOWVIEWCONTROLS = "controls";
		public static final String UNITS = "units";

		public LegendMemento(LegendMemento m) {
			super(m);
		}

		public LegendMemento() {
			// Override initial feature delete to false
			initProperty(CAN_DELETE, false);
			initProperty(SHOWMOUSE, true);
			initProperty(SHOWLABELS, true);
			initProperty(SHOWCOMPASS, true);
			initProperty(SHOWSCALE, true);
			initProperty(SHOWWORLDINSET, false);
			initProperty(SHOWVIEWCONTROLS, true);
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
				Boolean on = m.getPropertyValue(LegendMemento.SHOWMOUSE);
				if (on) {
					WorldWindView v = FeatureList.theFeatures.getWWView();
					if (v != null) {
						//System.out.println("Drawing product outline "+counter++);
						v.DrawProductReadout(dc);
					}
				}
			}
		}

		@Override
		public void pick(DrawContext dc, Point p, FeatureMemento m) {
		}

		@Override
		public void preRender(DrawContext dc, FeatureMemento m) {
		}
	}

	public static class LegendCompass extends WorldWindLayerRenderer {
		CompassLayer myCompass;
		public LegendCompass(Layer l){
			super(l);
			myCompass = (CompassLayer) l;
		}
		@Override
		public void draw(DrawContext dc, FeatureMemento m) {
			Boolean on = m.getPropertyValue(LegendMemento.SHOWCOMPASS);
			if (on){
			   //String path = SwingIconFactory.getIconPath("compass.png");
			   //myCompass.setIconFilePath(path);
			   ///myCompass.setLocationCenter(dc.getView().getCenterPoint());
			   //Point vv = dc.getViewportCenterScreenPoint();

			   //log.debug("Center point is "+vv);
			   //myCompass.setLocationCenter(new Vec4(vv.x, vv.y, 0, 0));
			  // dc.getView().getCenterPoint());
			   super.draw(dc, m);
			}
		}
	}

	public static class LegendScale extends WorldWindLayerRenderer {
		public LegendScale(Layer l){
			super(l);
		}
		@Override
		public void draw(DrawContext dc, FeatureMemento m) {
			Boolean on = m.getPropertyValue(LegendMemento.SHOWSCALE);
			if (on){
			   super.draw(dc, m);
			}
		}
	}

	public static class LegendInset extends WorldWindLayerRenderer {
		public LegendInset(Layer l){
			super(l);
		}
		@Override
		public void draw(DrawContext dc, FeatureMemento m) {
			Boolean on = m.getPropertyValue(LegendMemento.SHOWWORLDINSET);
			if (on){
			   super.draw(dc, m);
			}
		}
	}

	public static class LegendViewControls extends WorldWindLayerRenderer {
		public LegendViewControls(Layer l){
			super(l);
		}
		@Override
		public void draw(DrawContext dc, FeatureMemento m) {
			Boolean on = m.getPropertyValue(LegendMemento.SHOWVIEWCONTROLS);
			if (on){
			   super.draw(dc, m);
			}
		}
	}

	/**
	 * The state we use for drawing the map.
	 */
	public LegendFeature(FeatureList f, CompassLayer c, ScalebarLayer s, WorldMapLayer i, ViewControlsLayer vc) {
		super(f, LegendGroup, new LegendMemento());
		setName("Legend");
		setKey("Legend");
		setMessage("ColorKey");

		// Add renderers in draw order (last on top) 
		addRenderer(new ColorMapRenderer());
		addRenderer(new LegendInset(i));
		addRenderer(new LegendCompass(c));
		addRenderer(new LegendScale(s));
		addRenderer(new LegendViewControls(vc));
		addRenderer(new simpleReadout());
	}

	/*
	 * Steal the stock worldwind layers from the world wind view and put
	 * them in our legend feature.  We need to control these to make sure
	 * they don't overlap and allow properties to be set/changed
	 */
	public static LegendFeature createLegend(FeatureList list, LayerList ll) {
		LegendFeature f;
		CompassLayer wwCompass = null;
		ScalebarLayer wwScale = null;
		WorldMapLayer wwInset = null;
		ViewControlsLayer wwControls = null;
		for (Layer l : ll) {
			if (l instanceof CompassLayer) {
				CompassLayer c = (CompassLayer) (l);
				ll.remove(l);
				wwCompass = c;
				continue;
			}
			if (l instanceof ScalebarLayer) {
				ScalebarLayer s = (ScalebarLayer) (l);
				ll.remove(l);
				wwScale = s;
				continue;
			}
			if (l instanceof WorldMapLayer) {
				WorldMapLayer s = (WorldMapLayer) (l);
				ll.remove(l);
				wwInset = s;
				continue;
			}
			if (l instanceof ViewControlsLayer) {
				ViewControlsLayer v = (ViewControlsLayer) (l);
				ll.remove(l);
				wwControls = v;
				continue;
			}
		}
		f = new LegendFeature(list, wwCompass, wwScale, wwInset, wwControls);
		return f;
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
