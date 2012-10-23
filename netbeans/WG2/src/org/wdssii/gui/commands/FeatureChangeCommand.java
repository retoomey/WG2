package org.wdssii.gui.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;

/**
 * FeatureChangeCommand sent when a stock feature setting is changed in the
 * FeatureView
 *
 * @author rtoomey
 */
public class FeatureChangeCommand extends FeatureCommand {

    private static Logger log = LoggerFactory.getLogger(FeatureChangeCommand.class);
    /**
     * The key of the Feature we will set visible for
     */
    protected String myFeatureKey;
    public FeatureMemento myChange;
    private boolean initialized = false;

    public FeatureChangeCommand() {
    }

    public FeatureChangeCommand(String llhAreaKey, FeatureMemento change) {
        myFeatureKey = llhAreaKey;
        myChange = change;
        initialized = true;
    }

    public FeatureChangeCommand(Feature f, FeatureMemento change) {
        myFeatureKey = f.getKey();
        myChange = change;
        initialized = true;
    }
    
    public void set(String key, FeatureMemento change){
        myFeatureKey = key;
        myChange = change;
        initialized = true;
    }

    @Override
    public boolean execute() {
        // Eventually theFeatures will be per world ball....
        if (initialized) {
            FeatureList.theFeatures.setMemento(myFeatureKey, myChange);
            FeatureList.theFeatures.updateOnMinTime();
        }
        return true;
    }
}
