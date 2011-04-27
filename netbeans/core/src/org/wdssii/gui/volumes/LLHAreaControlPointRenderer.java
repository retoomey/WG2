package org.wdssii.gui.volumes;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Point;

public interface LLHAreaControlPointRenderer {

    void render(DrawContext dc, Iterable<? extends LLHAreaControlPoint> controlPoints);

    void pick(DrawContext dc, Iterable<? extends LLHAreaControlPoint> controlPoints, Point pickPoint, Layer layer);
}