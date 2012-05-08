package org.wdssii.gui.volumes;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.DrawContext;
import java.util.List;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.features.LLHAreaFeature;

/**
 * A 'Pin' or stick in the display.  An LLHArea with a single location
 * 
 * @author Robert Toomey
 */
public class LLHAreaHeightStick extends LLHArea {

	private String lastDrawnKey = "";

	public LLHAreaHeightStick(LLHAreaFeature f) {
		super(f);
	}

	/** Get a key that represents the GIS location of this stick.
	FIXME move up to LLHArea...*/
	public String getGISKey() {
		String newKey = "";

		// Add location and altitude...
		List<LatLon> locations = getLocationList();
		for (int i = 0; i < locations.size(); i++) {
			LatLon l = locations.get(i);
			newKey = newKey + l.getLatitude() + ":";
			newKey = newKey + l.getLongitude() + ":";
		}
		newKey = newKey + this.lowerAltitude;
		newKey = newKey + this.upperAltitude;
		return newKey;
	}

	@Override
	protected void doRenderGeometry(DrawContext dc, String drawStyle, List<LatLon> locations, List<Boolean> edgeFlags) {
		if (locations.isEmpty()) {
			return;
		}
		String key = getGISKey();
		if (key.compareTo(lastDrawnKey) != 0) {
			lastDrawnKey = key;
			// Fire changed event?  Is this enough? 
			CommandManager.getInstance().executeCommand(new FeatureCommand(), true);
		}
	}
}
