package org.wdssii.gui.commands;

import org.wdssii.gui.ProductManager;

/** Called by name from WdssiiDynamic */
public class CacheClearCommand extends CacheCommand {

    @Override
    public boolean execute() {
        ProductManager.getInstance().clearProductCache();
        return true;
    }
}
