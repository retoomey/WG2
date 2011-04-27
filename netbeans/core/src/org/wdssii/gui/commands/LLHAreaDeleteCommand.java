package org.wdssii.gui.commands;

import org.wdssii.gui.LLHAreaManager;

/** Called by name from WdssiiDynamic */
public class LLHAreaDeleteCommand extends LLHAreaCommand {

    @Override
    public boolean execute() {
        LLHAreaManager.getInstance().deleteSelectedVolume();
        return true;
    }
}