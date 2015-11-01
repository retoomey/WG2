package org.wdssii.gui.commands;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;
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

    private final static Logger LOG = LoggerFactory.getLogger(FeatureChangeCommand.class);
    /**
     * The key of the Feature we will set visible for
     */
    protected String myFeatureKey;
    public Memento myChange;
    private boolean initialized = false;

    public FeatureChangeCommand() {
    }

    public FeatureChangeCommand(String key, Memento change) {
        myFeatureKey = key;
        myChange = change;
        initialized = true;
    }

    public FeatureChangeCommand(Feature f, Memento change) {
        myFeatureKey = f.getKey();
        myChange = change;
        initialized = true;
    }
    
    public void set(String key, Memento change){
        myFeatureKey = key;
        myChange = change;
        initialized = true;
    }

    @Override
    public boolean execute() {
        // Eventually theFeatures will be per world ball....
        if (initialized) {
        	// Send the property changes...
            FeatureList.theFeatures.updateMemento(myFeatureKey, myChange);

        }
        return true; // GUI update (bleh full refresh)  
    }
}
