package org.wdssii.gui.features;

import java.awt.Point;
import org.wdssii.geom.GLWorld;

/**
 * A Feature3DRenderer renders something in the 3D world view Features use
 * 3DRenderers to draw
 *
 * @author Robert Toomey
 */
public abstract class Feature3DRenderer extends FeatureRenderer {

    public static final int RASTER = 0;
    public static final int POINT = 1;

    public abstract void preRender(GLWorld w, FeatureMemento m);

    public abstract void draw(GLWorld w, FeatureMemento m);

    public abstract void pick(GLWorld w, Point p, FeatureMemento m);
}
