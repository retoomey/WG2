package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;

/**
 *
 * @author rtoomey
 */
public class FeatureCreateCommand extends FeatureCommand {

    private String myFactory;
    
    public FeatureCreateCommand(String factory){
        myFactory = factory;
    }
    
    @Override
    public boolean execute() {

        // For the moment just 'first' factory or slice....
        LLHAreaFeature A = new LLHAreaFeature();
        boolean success = A.createLLHArea("Slice");
        if (success){
            FeatureList.theFeatures.addFeature(A);
        }
        CommandManager.getInstance().getEarthBall().updateOnMinTime();
        return true;
    }
}
