package org.wdssii.gui.volumes;

import org.wdssii.gui.features.Feature.FeatureTableInfo;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.features.LLHAreaSetGUI;

/** Factory which creates a 'slice'.  Two lat/lon points and a fixed height range between them*/
public class LLHAreaSetFactory extends LLHAreaFactory {

    /** Counter for default name */
    static int counter = 1;

    @Override
    public String getFactoryNameDisplay() {
        return "Set";
    }

    @Override
    public boolean create(LLHAreaFeature f, FeatureTableInfo data, Object params) {

        boolean success = true;

        // Create the visible object in world window
        String name = "Set" + (counter++);
        
        data.visibleName = name;
        data.keyName = name;
        data.visible = true;

        LLHAreaSet poly = new LLHAreaSet(f);       
       // poly.setAttributes(getDefaultAttributes());
    //    poly.setValue(AVKey.DISPLAY_NAME, name);
        poly.setAltitudes(0.0,  LLHArea.DEFAULT_HEIGHT_METERS);
       // poly.setLocations(poly.getDefaultLocations(wwd, params));
        poly.setLocations(poly.getDefaultLocations(params));

        data.created = poly;

        setName(name);
        return success;
    }
    
    @Override
    public FeatureGUI createGUI(LLHAreaFeature f, LLHArea a){
        if (a instanceof LLHAreaSet){
            LLHAreaSet set = (LLHAreaSet)(a);
            LLHAreaSetGUI gui = new LLHAreaSetGUI(f, set);  
            return gui;
        }
        return super.createGUI(f, a);
    }
}