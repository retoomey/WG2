package org.wdssii.gui.features;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;

/**
 * Render a world wind layer
 *
 * @author Robert Toomey
 */
public class WorldWindLayerRenderer implements Feature3DRenderer {

	private Layer myLayer;

	public WorldWindLayerRenderer(Layer l) {
		myLayer = l;
	}

	@Override
	public void preRender(DrawContext dc, FeatureMemento m){
		Layer oldLayer = dc.getCurrentLayer();
		dc.setCurrentLayer(myLayer);// for proper pick event
		myLayer.preRender(dc);
		dc.setCurrentLayer(oldLayer);
	}

	@Override
	public void draw(DrawContext dc, FeatureMemento m) {
		Layer oldLayer = dc.getCurrentLayer();
		dc.setCurrentLayer(myLayer);// for proper pick event
		myLayer.render(dc);
		dc.setCurrentLayer(oldLayer);
	}

	@Override
	public void pick(DrawContext dc, Point p, FeatureMemento m) {
		Layer oldLayer = dc.getCurrentLayer();
		dc.setCurrentLayer(myLayer);// for proper pick event
		myLayer.pick(dc, p);
		dc.setCurrentLayer(oldLayer);
	}
}
