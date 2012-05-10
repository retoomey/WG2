package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import javax.swing.JComponent;
import org.wdssii.gui.features.Feature.FeatureTableInfo;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;

/** Factory which creates a 'slice'.  Two lat/lon points and a fixed height range between them*/
public class LLHAreaSliceFactory extends LLHAreaFactory {

    /** Counter for default name */
    static int counter = 1;

    @Override
    public String getFactoryNameDisplay() {
        return "Slice";
    }

    @Override
    public boolean create(WorldWindow wwd, LLHAreaFeature f, FeatureTableInfo data) {

        boolean success = true;

        // Create the visible object in world window
        String name = "Slice" + String.valueOf(counter++);
        
        data.visibleName = name;
        data.keyName = name;
        data.visible = true;

        LLHAreaSlice poly = new LLHAreaSlice(f);       
        poly.setAttributes(getDefaultAttributes());
        poly.setValue(AVKey.DISPLAY_NAME, name);
        poly.setAltitudes(0.0,  LLHArea.DEFAULT_HEIGHT_METERS);
        poly.setLocations(poly.getDefaultLocations(wwd));
        data.created = poly;

        setName(name);
        return success;
    }
    
    @Override
    public FeatureGUI createGUI(LLHArea a, JComponent parent){
        if (a instanceof LLHAreaSlice){
            LLHAreaSlice slice = (LLHAreaSlice)(a);
            LLHAreaSliceGUI gui = new LLHAreaSliceGUI(slice);          
            return gui;
        }
        return super.createGUI(a, parent);
    }
}