package org.wdssii.gui.commands;

import org.wdssii.gui.LLHAreaManager;

/** LLHAreaDeleteCommand deletes one of the objects in the 3D Objects list
 * 
 * @author Robert Toomey
 */
public class LLHAreaDeleteCommand extends LLHAreaCommand {

    private String myLLHAreaKey = null;
    //private LLHArea myLLHArea = null;
    
    public LLHAreaDeleteCommand(String llhAreaKey) {
        myLLHAreaKey = llhAreaKey;
    }

    @Override
    public boolean execute() {
        LLHAreaManager.getInstance().deleteLLHArea(myLLHAreaKey);
        return true;
    }
}