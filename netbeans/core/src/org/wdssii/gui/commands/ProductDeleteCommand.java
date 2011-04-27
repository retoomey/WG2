package org.wdssii.gui.commands;

/** Called by name from WdssiiDynamic
 * @author Robert Toomey
 * This command can delete 1 product, or all products matching an index key
 */
public class ProductDeleteCommand extends ProductCommand {

    private String myFilterName = null;

    /** Delete products with a given source name */
    public ProductDeleteCommand(String indexName) {
        myFilterName = indexName;
    }

    /** Delete the current selected product when executed, 
     * this is required to have no parameters for RCP button linking */
    public ProductDeleteCommand() {
    }

    @Override
    public boolean execute() {

        String t = Thread.currentThread().getName();
        // If you wanted a warning dialog, would be here...
        System.out.println("DELETE PRODUCT COMMAND CALLED ! " + t);
        clearFromHandlerList(myFilterName);

        /*ProductHandlerList theList = CommandManager.getInstance().getProductOrderedSet();
        if (theList != null){
        if (myFilterName == null){
        theList.deleteSelectedProduct();
        }else{
        theList.deleteProductsMatchingSource(myFilterName);
        }
        }*/
        return true;
    }
}