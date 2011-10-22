package org.wdssii.gui.commands;

import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.ProductHandlerList;

/** Called when a product is selected in a product list
 * 
 * @author Robert Toomey
 *
 */
public class ProductSelectCommand extends ProductCommand {

    private String myProductKey;

    public ProductSelectCommand(String productKeyName) {
        myProductKey = productKeyName;
    }

    @Override
    public boolean execute() {
        ProductManager m = ProductManager.getInstance();
        ProductHandlerList list = m.getProductOrderedSet();
        list.selectProductHandler(myProductKey);
        return true;
    }
}
