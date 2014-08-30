package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.PreferencesManager;

/**
 * Create a fresh setup and layout
 * 
 * @author Robert Toomey
 */
public class NewCommand extends WdssiiCommand {

    @Override
    public boolean execute() {
        // Check dirty first?
        
        PreferencesManager.getInstance().createNewDocument();
        return true;
    }
    
}
