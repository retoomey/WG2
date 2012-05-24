package org.wdssii.gui.gis;

import java.awt.Color;
import java.net.URL;
import javax.swing.JComponent;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.W2Config;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;

/**
 * A Map feature draws a map in a window
 *
 * @author rtoomey
 */
public class MapFeature extends Feature {

	/**
	 * The properties of the MapFeature
	 */
	public static class MapMemento extends FeatureMemento {

		// Properties
		public static final String LINE_THICKNESS = "line_thickness";
		public static final String LINE_COLOR = "line_color";

		public MapMemento(MapMemento m) {
			super(m);
		}

		public MapMemento() {
			initProperty(LINE_THICKNESS,  2);
			initProperty(LINE_COLOR,  Color.WHITE);
		}
	}

	private static Logger log = LoggerFactory.getLogger(MapFeature.class);
	public static final String MapGroup = "MAPS";

	/**
	 * The GUI for this Feature
	 */
	private MapGUI myControls;

	/**
	 * The state we use for drawing the map.
	 */
	public MapFeature(FeatureList f, String source) {
		super(f, MapGroup, new MapMemento());
		URL u = W2Config.getURL(source);
		loadURL(u, source);
	}

	/**
	 * The state we use for drawing the map.
	 */
	public MapFeature(FeatureList f, URL u) {
		super(f, MapGroup, new MapMemento());
		String source = "bad url";
		if (u != null) {
			source = u.toString();
		}
		loadURL(u, source);
	}

	/**
	 *
	 * @param u the URL we have to try to load from
	 * @param source the original source string we tried to look up URL for
	 */
	protected final void loadURL(URL u, String source) {
		try {
			if (u != null) {
				FileDataStore store = FileDataStoreFinder.getDataStore(u);
				SimpleFeatureSource featureSource = store.getFeatureSource();

				// Set message to our shapefile url...
				String s = u.toString();
				setMessage(s);
				setKey(s);

				// Does this always work?  Get short name of map from URL
				int dot = s.lastIndexOf(".");
				int sep = s.lastIndexOf("/");  // "\"?
				String sub = s.substring(sep + 1, dot);
				setName(sub);

				MapRenderer m = new MapRenderer(featureSource);
				addRenderer(m);
			} else {
				setMessage("?? " + source);
				setKey(source);
				// Does this always work?  Get short name of map from URL
				int dot = source.lastIndexOf(".");
				int sep = source.lastIndexOf("/");  // "\"?
				String sub = source.substring(sep + 1, dot);
				setName(sub);
			}
		} catch (Exception e) {
			log.error("Got exception trying to use GeoTools. " + e.toString());
		}
	}

	@Override
	public FeatureMemento getNewMemento() {
		MapMemento m = new MapMemento((MapMemento) getMemento());
		return m;
	}

	@Override
	public void setupFeatureGUI(JComponent source) {

		// FIXME: general FeatureFactory..move code up into Feature
		boolean success = false;
		// if (myFactory != null) {

		if (myControls == null) {
			//myControls = myFactory.createGUI(myLLHArea, source);
			myControls = new MapGUI(this);
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
