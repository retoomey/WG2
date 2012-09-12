package org.wdssii.gui.commands;

import org.wdssii.gui.features.FeatureList;

/** FeatureDeleteCommand deletes one of the objects in a FeatureList
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
	FeatureList.theFeatures.updateOnMinTime();
        return true;
    }
}
