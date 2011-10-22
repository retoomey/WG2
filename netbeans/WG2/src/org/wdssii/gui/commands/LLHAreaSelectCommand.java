package org.wdssii.gui.commands;

import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.volumes.LLHArea;

/**
 * Select a LLHArea given a key
 * 
 * @author Robert Toomey
 */
public class LLHAreaSelectCommand extends LLHAreaCommand {
    private String myLLHAreaKey = null;
    private LLHArea myLLHArea = null;
    
    /** Select an LLHArea by known keyname */
    public LLHAreaSelectCommand(String key) {
        myLLHAreaKey = key;
    }
    
    /** Select an LLHArea by known contained object */
    public LLHAreaSelectCommand(LLHArea area){
        myLLHArea = area;
    }

    @Override
    public boolean execute() {
        if (myLLHArea != null){
            LLHAreaManager.getInstance().selectLLHArea(myLLHArea);
        }else{
            LLHAreaManager.getInstance().selectLLHArea(myLLHAreaKey);
        }
        return true;
    }
}
