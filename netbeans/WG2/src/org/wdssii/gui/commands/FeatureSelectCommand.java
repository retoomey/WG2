package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;

/**
 * Command to select any feature in a window
 * @author rtoomey
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

    @Override
    public boolean execute() {
        if (myFeature != null){
            FeatureList.theFeatures.setSelected(myFeature);
        }else{
            FeatureList.theFeatures.setSelected(myFeatureKey);
        }
        CommandManager.getInstance().getEarthBall().updateOnMinTime();
        return true;
    }
}
