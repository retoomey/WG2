package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
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
    private final static Logger LOG = LoggerFactory.getLogger(DataCommand.class);

    /**
     * Utility function. Clear the products matching key from the list of
     * products.
     *
     * @param indexKey
     */
    protected void clearFromHandlerList(String indexKey) {

        LOG.info("---Clear products from current handler list....");
        if (indexKey == null) {
            ProductManager.getInstance().deleteSelectedProduct();
        } else {
            ProductManager.getInstance().deleteProductsMatchingSource(indexKey);
        }
    }
}
