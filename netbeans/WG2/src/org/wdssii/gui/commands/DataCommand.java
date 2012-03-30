package org.wdssii.gui.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ProductManager;

/**
 * Any command that modifies/changes the data or data viewing within the display
 * GUI updating on this is not recommended, try to use a subclass if possible to
 * avoid excessive GUI updating. This encloses all Source and Product commands
 *
 * @author Robert Toomey
 *
 */
public abstract class DataCommand extends WdssiiCommand {
    private static Logger log = LoggerFactory.getLogger(DataCommand.class);

    /**
     * Utility function. Clear the products matching key from the list of
     * products.
     *
     * @param indexKey
     */
    protected void clearFromHandlerList(String indexKey) {

        log.info("---Clear products from current handler list....");
        if (indexKey == null) {
            ProductManager.getInstance().deleteSelectedProduct();
        } else {
            ProductManager.getInstance().deleteProductsMatchingSource(indexKey);
        }
    }
}
