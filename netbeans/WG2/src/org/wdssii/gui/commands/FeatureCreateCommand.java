package org.wdssii.gui.commands;

import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;

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
        LLHAreaFeature A = new LLHAreaFeature(FeatureList.getFeatureList());
        boolean success = A.createLLHArea(myFactory, myInfo);
        if (success){
            FeatureList.getFeatureList().addFeature(A);
        }
	FeatureList.getFeatureList().updateOnMinTime();
        return true;
    }
}
