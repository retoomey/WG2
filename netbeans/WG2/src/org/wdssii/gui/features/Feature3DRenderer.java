package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;

/**
 * A Feature3DRenderer renders something in the 3D world view
 * Features use 3DRenderers to draw
 * 
 * @author Robert Toomey
 */
public interface Feature3DRenderer {

	public void draw(DrawContext dc, FeatureMemento m);
	
}
