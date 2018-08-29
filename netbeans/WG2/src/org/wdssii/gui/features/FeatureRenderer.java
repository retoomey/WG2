package org.wdssii.gui.features;

import java.awt.Point;

import org.wdssii.gui.GLWorld;

/**
 * Base for all FeatureRenderers
 * 
 * @author Robert Toomey
 */
public abstract class FeatureRenderer {

	// Draw levels for renderers
	public enum Level {
		BASE, // Earth ball base map
		RASTER, // Products
		LINEMAP, // Line map level
		LINE3D, // Polargrid...
		POINT, // Icons, mping stuff..
		LLHCONTROLS, // Controls for set lines..
		OVERLAY // Current readout, compass, etc..
	};

	/** Used for any post-reflection created initializing from calling Feature */
	public void initToFeature(Feature f) {

	}

	private FeatureList myFeatureList = null;

	/** For renderers that need the particular list, such as color map */
	public void setCurrentFeatureList(FeatureList l) {
		myFeatureList = l;
	}

	/** For renderers that need the particular list, such as color map */
	public FeatureList getCurrentFeatureList() {
		return myFeatureList;
	}

	public abstract void preRender(GLWorld w, FeatureMemento m);

	public abstract void draw(GLWorld w, FeatureMemento m);

	public abstract void pick(GLWorld w, Point p, FeatureMemento m);

	/** Default rank for renderer, determining drawing order */
	public FeatureRenderer.Level getDrawRank() {
		return Level.RASTER;
	}

}
