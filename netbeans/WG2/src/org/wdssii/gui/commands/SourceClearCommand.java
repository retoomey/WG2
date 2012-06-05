package org.wdssii.gui.commands;

import org.wdssii.gui.ProductManager;

/** A source clear command clears all products in the GUI/index/cache of a given source
 * 
 * @author Robert Toomey
 *
 */
public class SourceClearCommand extends SourceCommand {

    @Override
    public boolean execute() {

        if (validSourceOrSelected()) {
            String toClear = getSourceKey();

            // 1. Remove any products from handler list matching us...
            clearFromHandlerList(toClear);

            // 2.  Trim cache of the disconnected source.
            trimProductCache(toClear);
        }
        return true;
    }

    /** Trim all cache items matching a given index key.  Only clear commands do this
     */
    protected void trimProductCache(String key) {
        if (key != null) {
            int deleted = ProductManager.getInstance().trimCacheMatchingIndexKey(key);
            System.out.println("Removed " + deleted + " items from cache for " + key);
        }
    }
}
