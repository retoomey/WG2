package org.wdssii.gui.features;

import java.awt.Point;
import org.wdssii.geom.GLWorld;

/**
 * A Feature3DRenderer renders something in the 3D world view Features use
 * 3DRenderers to draw
 *
 * @author Robert Toomey
 */
public interface Feature3DRenderer {

    public static final int RASTER = 0;
    public static final int POINT = 1;

    public void preRender(GLWorld w, FeatureMemento m);

    public void draw(GLWorld w, FeatureMemento m);

    public void pick(GLWorld w, Point p, FeatureMemento m);
}
