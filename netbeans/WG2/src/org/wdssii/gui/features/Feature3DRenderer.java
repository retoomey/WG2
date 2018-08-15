package org.wdssii.gui.features;

import java.awt.Point;
import org.wdssii.gui.GLWorld;

/**
 * A Feature3DRenderer renders something in the 3D world view Features use
 * 3DRenderers to draw
 *
 * @author Robert Toomey
 */
public abstract class Feature3DRenderer extends FeatureRenderer {

    public static final int RASTER = 0;
    public static final int POINT = 1;
	private FeatureList myFeatureList = null;

    /** For renderers that need the particular list, such as color map */
    public void setCurrentFeatureList(FeatureList l) {
    	myFeatureList  = l;
    }
    
    /** For renderes that need the particular list, such as color map */
    public FeatureList getCurrentFeatureList() {
    	return myFeatureList;
    }
    
    public abstract void preRender(GLWorld w, FeatureMemento m);

    public abstract void draw(GLWorld w, FeatureMemento m);

    public abstract void pick(GLWorld w, Point p, FeatureMemento m);
}
