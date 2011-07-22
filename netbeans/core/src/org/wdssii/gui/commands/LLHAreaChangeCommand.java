package org.wdssii.gui.commands;

import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHArea.LLHAreaMemento;

/**
 * A command that represents a change in an LLHArea object state
 * 
 * FIXME: create a memento object for state changes.
 * 
 * @author Robert Toomey
 */
public class LLHAreaChangeCommand extends LLHAreaCommand {
    
    /** The key of the LLHArea we will set visible for */
    protected String myLLHAreaKey;
    
    public LLHAreaMemento myChange;
    
    public LLHAreaChangeCommand(String llhAreaKey, LLHAreaMemento change){
        myLLHAreaKey = llhAreaKey;
        myChange = change;
    }
    
    public LLHAreaChangeCommand(LLHArea area, LLHAreaMemento change){
        myLLHAreaKey = LLHAreaManager.getInstance().getKey(area);
        myChange = change;
    }

    @Override
    public boolean execute() {
        LLHAreaManager.getInstance().setLLHAreaChange(myLLHAreaKey, myChange);
        return true;
    }
}
