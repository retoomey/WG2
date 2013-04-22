package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;

/**
 * A Feature3DRenderer renders something in the 3D world view Features use
 * 3DRenderers to draw
 *
 * @author Robert Toomey
 */
public interface Feature3DRenderer {

    public static final int RASTER = 0;
    public static final int POINT = 1;

    public void preRender(DrawContext dc, FeatureMemento m);

    public void draw(DrawContext dc, FeatureMemento m);

    public void pick(DrawContext dc, Point p, FeatureMemento m);
}
