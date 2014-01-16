package org.wdssii.gui.commands;

import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.volumes.LLHAreaFeature;

/**
 * Create a new feature in a feature list
 * @author Robert Toomey
 */
public class FeatureCreateCommand extends FeatureCommand {

    private String myFactory;
    private Object myInfo;
    
    public FeatureCreateCommand(String factory,
             Object info){
        myFactory = factory;
        myInfo = info;
    }
    
    @Override
    public boolean execute() {

        // For the moment just 'first' factory or slice....
        LLHAreaFeature A = new LLHAreaFeature(FeatureList.theFeatures);
        boolean success = A.createLLHArea(myFactory, myInfo);
        if (success){
            FeatureList.theFeatures.addFeature(A);
        }
	FeatureList.theFeatures.updateOnMinTime();
        return true;
    }
}
