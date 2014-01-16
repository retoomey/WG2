package org.wdssii.gui.worldwind;

import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.LegendFeature;

/**
 *
 * @author Robert Toomey
 */
public class WWLegendInset extends WorldWindLayerRenderer {
  
    public void initToFeature(Feature f){
        if (f instanceof LegendFeature){
            LegendFeature lf = (LegendFeature)(f);
            setKeyAndToken(lf.myInset, LegendFeature.LegendMemento.SHOWWORLDINSET);
        }
    }
}