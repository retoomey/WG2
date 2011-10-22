package org.wdssii.gui.commands;

import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.ProductHandlerList;

/** Any command that modifies/changes the data or data viewing within the display
 *  GUI updating on this is not recommended, try to use a subclass if possible to
 *  avoid excessive GUI updating.
 *  This encloses all Source and Product commands
 * @author Robert Toomey
 *
 */
public abstract class DataCommand extends WdssiiCommand {

    /** Utility function.  Clear the products matching key from the list of products.
     * @param indexKey
     */
    protected void clearFromHandlerList(String indexKey) {

        System.out.println("---Clear products from current handler list....");
        ProductHandlerList theList = ProductManager.getInstance().getProductOrderedSet();
        if (theList != null) {
            if (indexKey == null) {
                theList.deleteSelectedProduct();
            } else {
                theList.deleteProductsMatchingSource(indexKey);
            }
        }
    }
}
