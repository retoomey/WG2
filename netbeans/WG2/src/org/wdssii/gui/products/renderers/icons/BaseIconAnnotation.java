package org.wdssii.gui.products.renderers.icons;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.AnnotationRenderer;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.util.OGLStackHandler;

/**
 *
 * @author Robert Toomey
 */
public class BaseIconAnnotation extends GlobeAnnotation {

    public BaseIconAnnotation(String text, Position p,
            AnnotationAttributes defaults) {
        super(text, p, defaults);
    }

    // same code
    @Override
    protected void drawTopLevelAnnotation(DrawContext dc, int x, int y, int width, int height, double scale,
            double opacity, Position pickPosition) {

        OGLStackHandler stackHandler = new OGLStackHandler();
        this.beginDraw(dc, stackHandler);
        try {
            this.applyScreenTransform(dc, x, y, width, height, scale);
            this.draw(dc, width, height, opacity, pickPosition);
        } finally {
            this.endDraw(dc, stackHandler);
        }
    }

    // same code
    @Override
    public void draw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        double finalOpacity = opacity * this.computeOpacity(dc);
        this.doDraw(dc, width, height, finalOpacity, pickPosition);
    }

    public String getReadoutString(){
        return this.toString();
    }
    
    /** The '3d' component of our annotation. */
    public void do3DDraw(DrawContext dc) {
    }

    @Override
    public void render(DrawContext dc) {
        if (!this.getAttributes().isVisible()) {
            return;
        }

        // This object sorts the icons and draws in order...
        BasicAnnotationRenderer b;
        dc.getAnnotationRenderer();
        dc.getCurrentLayer(); // product layer
        //   r.render(dc, this, null, l);

       // DataTableRenderer.myRenderer.render(dc, this, null, l);
        renderNow(dc);
    }

    @Override
    public void pick(DrawContext dc, java.awt.Point pickPoint) {
        if (!this.getAttributes().isVisible()) {
            return;
        }
       // DataTableRenderer.myRenderer.pick(dc, this, null, pickPoint, null);
    }

    /** The regular render now stuff for 2D.... */
    @Override
    public void renderNow(DrawContext dc) {
        if (dc == null) {
            return;
        }
        if (!this.getAttributes().isVisible()) {
            return;
        }
        //  if (dc.isPickingMode() && !this.isPickEnabled()) {
        //      return;
        //  }

        // doRenderNow
        //  if (dc.isPickingMode() && this.getPickSupport() == null) {
        //      return;
        //   }
        Vec4 point = this.getAnnotationDrawPoint(dc);
        if (point == null) {
            return;
        }
        if (dc.getView().getFrustumInModelCoordinates().getNear().distanceTo(point) < 0) {
            return;
        }
        Vec4 screenPoint = dc.getView().project(point);
        if (screenPoint == null) {
            return;
        }

        java.awt.Dimension size = this.getPreferredSize(dc);
        Position pos = dc.getGlobe().computePositionFromPoint(point);

        double[] scaleAndOpacity = computeDistanceScaleAndOpacity(dc, point, size);
        this.setDepthFunc(dc, screenPoint);


        this.drawTopLevelAnnotation(dc, (int) screenPoint.x, (int) screenPoint.y, size.width, size.height,
                scaleAndOpacity[0], scaleAndOpacity[1], pos);
    }
}
