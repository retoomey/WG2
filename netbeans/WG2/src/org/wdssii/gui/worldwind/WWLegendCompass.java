package org.wdssii.gui.worldwind;

import org.wdssii.geom.GLWorld;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LegendFeature;

/**
 *
 * @author Robert Toomey
 */
public class WWLegendCompass extends WorldWindLayerRenderer {

    public void initToFeature(Feature f) {
        if (f instanceof LegendFeature) {
            LegendFeature lf = (LegendFeature) (f);
            setKeyAndToken(lf.myCompass, LegendFeature.LegendMemento.SHOWCOMPASS);
        }
    }

    @Override
    public void draw(GLWorld w, FeatureMemento m) {
        Boolean on = m.getPropertyValue(LegendFeature.LegendMemento.SHOWCOMPASS);
        if (on) {
            //String path = SwingIconFactory.getIconPath("compass.png");
            //myCompass.setIconFilePath(path);
            ///myCompass.setLocationCenter(dc.getView().getCenterPoint());
            //Point vv = dc.getViewportCenterScreenPoint();

            //LOG.debug("Center point is "+vv);
            //myCompass.setLocationCenter(new Vec4(vv.x, vv.y, 0, 0));
            // dc.getView().getCenterPoint());
            super.draw(w, m);
        }
    }
}