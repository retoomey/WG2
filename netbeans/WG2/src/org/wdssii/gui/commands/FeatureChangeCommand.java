package org.wdssii.gui.commands;

import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;

/**
 *  FeatureChangeCommand sent when a stock feature setting is changed in 
 * the FeatureView
 * 
 * @author rtoomey
 */
public class FeatureChangeCommand extends FeatureCommand {
   
    /** The key of the Feature we will set visible for */
    protected String myFeatureKey;
    
    public FeatureMemento myChange;
    
    public FeatureChangeCommand(String llhAreaKey, FeatureMemento change){
        myFeatureKey = llhAreaKey;
        myChange = change;
    }
    
    public FeatureChangeCommand(Feature f, FeatureMemento change){
        myFeatureKey = f.getKey();
        myChange = change;
    }

    @Override
    public boolean execute() {
        // Eventually theFeatures will be per world ball....
        FeatureList.theFeatures.setMemento(myFeatureKey, myChange);
	FeatureList.theFeatures.updateOnMinTime();
        return true;
    }
}
