package org.wdssii.gui.worldwind;

import java.util.logging.Level;

import gov.nasa.worldwind.AbstractSceneController;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

/*
 * @author Robert Toomey
 * We want to control the mixing of our product layers and/or have special needs and the world wind layers.  
 */
public class WJSceneController extends AbstractSceneController {

    @Override
    public void doRepaint(DrawContext dc) {
        this.initializeFrame(dc);
        try {
            this.applyView(dc);
            this.createTerrain(dc);
            this.clearFrame(dc);
            this.pick(dc);
            this.clearFrame(dc);
            this.draw(dc);

        } finally {
            this.finalizeFrame(dc);
        }
    }

    @Override
    protected void draw(DrawContext dc) {
        try {

            // The worldwind layers...humm
            if (dc.getLayers() != null) {
                for (Layer layer : dc.getLayers()) {
                    try {
                        if (layer != null) {
                            // if (layer instanceof Experimental){
                            // System.out.println("Skipping experimental tile layer");
                            // }else{
                            layer.render(dc);
                            // }
                        }
                    } catch (Exception e) {
                        String message = Logging.getMessage(
                                "SceneController.ExceptionWhileRenderingLayer",
                                (layer != null ? layer.getClass().getName()
                                : Logging.getMessage("term.unknown")));
                        Logging.logger().log(Level.SEVERE, message, e);
                        // Don't abort; continue on to the next layer.
                    }
                }
            }

            // Ordered Renderables (2d overlays)
            //while (dc.getOrderedRenderables().peek() != null) {
            //	dc.getOrderedRenderables().poll().render(dc);
            //}

            while (dc.getOrderedSurfaceRenderables().peek() != null) {
                dc.getOrderedSurfaceRenderables().poll().render(dc);
            }

            /*
             * // Diagnostic displays. if (dc.getSurfaceGeometry() != null &&
             * dc.getModel() != null && (dc.getModel().isShowWireframeExterior()
             * || dc.getModel().isShowWireframeInterior() ||
             * dc.getModel().isShowTessellationBoundingVolumes())) { Model model
             * = dc.getModel();
             * 
             * float[] previousColor = new float[4];
             * dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, previousColor, 0);
             * 
             * for (SectorGeometry sg : dc.getSurfaceGeometry()) { if
             * (model.isShowWireframeInterior() ||
             * model.isShowWireframeExterior()) sg.renderWireframe(dc,
             * model.isShowWireframeInterior(),
             * model.isShowWireframeExterior());
             * 
             * if (model.isShowTessellationBoundingVolumes()) {
             * dc.getGL().glColor3d(1, 0, 0); sg.renderBoundingVolume(dc); } }
             * 
             * dc.getGL().glColor4fv(previousColor, 0); }
             */
        } catch (Throwable e) {
            Logging.logger().log(
                    Level.SEVERE,
                    Logging.getMessage("BasicSceneController.ExceptionDuringRendering"),
                    e);
        }
    }
}
