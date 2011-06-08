package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.products.ProductHandlerList;

/**
 * A commmand that represents a change in a product, a change due to updating/moving,
 * toggling only mode, etc.  Usually this means a vslice must change or a table, etc...anything looking at
 * a product.
 * 
 * There isn't a 'direct' action for this command, it's more of an event.
 * @author Robert Toomey
 *
 */
public class ProductChangeCommand extends ProductCommand {

    /** A filter has changed due to direct GUI interaction.  The filter view needs to know if just a filter changed so it doesn't regenerate
     * its GUI */
    public static class ProductFilterCommand extends ProductChangeCommand {
    }

    /** Change state of product 'only' state */
    public static class ProductOnlyCommand extends ProductChangeCommand {

        private String myProductKey;
        private boolean myFlag;
        public ProductOnlyCommand(String productKeyName, boolean flag) {
            myProductKey = productKeyName;
            myFlag = flag;
        }

        @Override
        public boolean execute() {
            CommandManager m = CommandManager.getInstance();
            ProductHandlerList list = m.getProductOrderedSet();
            list.setOnlyMode(myProductKey, myFlag);
            return true;
        }
    }
    
    /** Change state of product 'visible' state */
    public static class ProductVisibleCommand extends ProductChangeCommand {

        private String myProductKey;
        private boolean myFlag;
        public ProductVisibleCommand(String productKeyName, boolean flag) {
            myProductKey = productKeyName;
            myFlag = flag;
        }

        @Override
        public boolean execute() {
            CommandManager m = CommandManager.getInstance();
            ProductHandlerList list = m.getProductOrderedSet();
            list.setVisibleHandler(myProductKey, myFlag);
            return true;
        }
    }

    @Override
    public boolean execute() {
        return true;
    }
}
