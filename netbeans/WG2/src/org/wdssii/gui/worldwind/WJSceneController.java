package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.gis.MapFeature;
import org.wdssii.gui.gis.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;

/*
 * @author Robert Toomey We want to control the mixing of our product layers
 * and/or have special needs and the world wind layers.
 */
public class WJSceneController extends BasicSceneController {

	private static Logger log = LoggerFactory.getLogger(WJSceneController.class);

//	@Override
//	public void drawOrderedSurfaceRenderables(DrawContext dc) {
//	}
	@Override
	public PickedObjectList getPickedObjectList() {
		PickedObjectList l = super.getPickedObjectList();
		if (l != null) {
			PickedObject o = l.getTopPickedObject();
		}
		return l;
	}

	@Override
	protected void preRender(DrawContext dc) {
		// Pre-render the layers.
		if (dc.getLayers() != null) {
			for (Layer layer : dc.getLayers()) {
				try {
					if (!(layer instanceof ViewControlsLayer)) {
						dc.setCurrentLayer(layer);
						layer.preRender(dc);
					}
				} catch (Exception e) {
					// Don't abort; continue on to the next layer.
				}
			}

			dc.setCurrentLayer(null);
		}

		// Make sure orderedRenderables added properly
		FeatureList f = ProductManager.getInstance().getFeatureList();
		f.preRenderFeatureGroup(dc, LegendFeature.LegendGroup);


		// Pre-render the deferred/ordered surface renderables.
		this.preRenderOrderedSurfaceRenderables(dc);
	}

	@Override
	protected void draw(DrawContext dc) {
		//super.draw(dc);
		//if (true) {
		//		return;
		//	}
		try {
			// Hack, draw all worldwind layers but 3d 
			if (dc.getLayers() != null) {
				for (Layer layer : dc.getLayers()) {
					try {
						if (layer != null) {
							if (!(layer instanceof LLHAreaLayer)) {
								dc.setCurrentLayer(layer);
								layer.render(dc);
							}
						}
					} catch (Exception e) {
					}
				}
			}
			dc.setCurrentLayer(null);

			// Get the current product list
			FeatureList f = ProductManager.getInstance().getFeatureList();
			f.renderFeatureGroup(dc, ProductFeature.ProductGroup);

			// For now...
			//f.renderFeatureGroup(dc, LLHAreaFeature.LLHAreaGroup);
			// Hack, draw 3d layer 
			if (dc.getLayers() != null) {
				for (Layer layer : dc.getLayers()) {
					try {
						if (layer != null) {
							if (layer instanceof LLHAreaLayer) {
								dc.setCurrentLayer(layer);
								layer.render(dc);
								break;
							}
						}
					} catch (Exception e) {
					}
				}
			}
			dc.setCurrentLayer(null);

			// Have to draw last, so that stipple works 'behind' product...
			// It's 'behind' but actually renders on top..lol
			f.renderFeatureGroup(dc, MapFeature.MapGroup);
			f.renderFeatureGroup(dc, PolarGridFeature.PolarGridGroup);
			f.renderFeatureGroup(dc, LegendFeature.LegendGroup);

			// Draw the deferred/ordered surface renderables.
			// This is all the 2d stuff on top...
			this.drawOrderedSurfaceRenderables(dc);

			if (this.screenCreditController != null) {
				this.screenCreditController.render(dc);
			}

			// Draw the deferred/ordered renderables.
			dc.setOrderedRenderingMode(true);
			while (dc.peekOrderedRenderables() != null) {
				OrderedRenderable test = dc.peekOrderedRenderables();
				try {
					dc.pollOrderedRenderables().render(dc);
				} catch (Exception e) {
				}
			}
			dc.setOrderedRenderingMode(false);

		} catch (Throwable e) {
		}
	}

	@Override
	protected void pickLayers(DrawContext dc) {
		if (dc.getLayers() != null) {
			for (Layer layer : dc.getLayers()) {
				try {
					if (layer != null && layer.isPickEnabled()) {
						dc.setCurrentLayer(layer);
						layer.pick(dc, dc.getPickPoint());
					}
				} catch (Exception e) {
					// Don't abort; continue on to the next layer.
				}
			}

			dc.setCurrentLayer(null);

			FeatureList f = ProductManager.getInstance().getFeatureList();
			f.pickFeatureGroup(dc, dc.getPickPoint(), LegendFeature.LegendGroup);
		}
	}

	protected void testdraw(DrawContext dc) {
		try {
			// Draw the layers.
			if (dc.getLayers() != null) {
				for (Layer layer : dc.getLayers()) {
					try {
						if (layer != null) {
							dc.setCurrentLayer(layer);
							layer.render(dc);
						}
					} catch (Exception e) {
						// Don't abort; continue on to the next layer.
					}
				}

				dc.setCurrentLayer(null);
			}

			// Draw the deferred/ordered surface renderables.
			this.drawOrderedSurfaceRenderables(dc);

			if (this.screenCreditController != null) {
				this.screenCreditController.render(dc);
			}

			// Draw the deferred/ordered renderables.
			dc.setOrderedRenderingMode(true);
			while (dc.peekOrderedRenderables() != null) {
				try {
					dc.pollOrderedRenderables().render(dc);
				} catch (Exception e) {
				}
			}
			dc.setOrderedRenderingMode(false);

		} catch (Throwable e) {
		}
	}
}
