package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.features.FeatureList;

/** LLHAreaDeleteCommand deletes one of the objects in the 3D Objects list
 * 
 * @author Robert Toomey
 */
public class FeatureDeleteCommand extends FeatureCommand {
    
    private String myFeatureKey = null;
    
    public FeatureDeleteCommand(String key) {
        myFeatureKey = key;
    }

    @Override
    public boolean execute() {
        FeatureList.theFeatures.removeFeature(myFeatureKey);
        CommandManager.getInstance().getEarthBall().updateOnMinTime();
        return true;
    }
}
