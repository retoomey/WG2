package org.wdssii.gui.commands;

import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;

/**
 * Create a new feature in a feature list
 * @author Robert Toomey
 */
public class FeatureCreateCommand extends FeatureCommand {

    private String myFactory;
    
    public FeatureCreateCommand(String factory){
        myFactory = factory;
    }
    
    @Override
    public boolean execute() {

        // For the moment just 'first' factory or slice....
        LLHAreaFeature A = new LLHAreaFeature(FeatureList.theFeatures);
        boolean success = A.createLLHArea(myFactory);
        if (success){
            FeatureList.theFeatures.addFeature(A);
        }
	FeatureList.theFeatures.updateOnMinTime();
        return true;
    }
}
