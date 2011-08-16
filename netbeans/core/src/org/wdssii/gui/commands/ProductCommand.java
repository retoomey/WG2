package org.wdssii.gui.commands;

import org.wdssii.datatypes.DataType;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;

/** The root class for all product based commands, such as delete product, load product,
 * etc..
 * @author Robert Toomey
 *
 */
public abstract class ProductCommand extends DataCommand {

    /* If myProduct is NULL, then grab the current top product in the display for
     * movement, otherwise, use the product we were set to.
     * FIXME: we're only getting top right now */
    public Product getOurProduct() {
        Product p = null;

        // Snag top product in the display
        ProductHandlerList pos = ProductManager.getInstance().getProductOrderedSet();
        if (pos != null) {
            ProductHandler tph = pos.getTopProductHandler();
            if (tph != null) {
                p = tph.getProduct();
            }
        }
        return p;
    }
    
    public DataType getOurDataType() {
        DataType dt = null;
        Product p = getOurProduct();
        if (p != null){
            dt = p.getRawDataType();
        }
        return dt;
    }
}
