package org.wdssii.gui.commands;

import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;

/**
 * Command to select any feature in a window
 * @author Robert Toomey
 */
public class FeatureSelectCommand extends FeatureCommand {
    private String myFeatureKey = null;
    Feature myFeature = null;
    
    /** Select an LLHArea by known keyname */
    public FeatureSelectCommand(String key) {
        myFeatureKey = key;
    }
    
    /** Select an LLHArea by known contained object */
    public FeatureSelectCommand(Feature area){
        myFeature = area;
    }

    public Feature getFeature(){
	    if (myFeature != null){
		    return myFeature;
	    }else{
		    return FeatureList.getFeatureList().getFeature(myFeatureKey);
	    }
    }

    @Override
    public boolean execute() {
        if (myFeature != null){
            FeatureList.getFeatureList().setSelected(myFeature);
        }else{
            FeatureList.getFeatureList().setSelected(myFeatureKey);
        }
	FeatureList.getFeatureList().updateOnMinTime();
        return true;
    }
}
