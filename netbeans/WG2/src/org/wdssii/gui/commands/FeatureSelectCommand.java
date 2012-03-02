package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;

/**
 * Command to select any feature in a window
 * @author rtoomey
 */
public class FeatureSelectCommand extends FeatureCommand {
    private String myFeatureKey = null;
    LLHAreaFeature myFeature = null;
    
    /** Select an LLHArea by known keyname */
    public FeatureSelectCommand(String key) {
        myFeatureKey = key;
    }
    
    /** Select an LLHArea by known contained object */
    public FeatureSelectCommand(LLHAreaFeature area){
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
