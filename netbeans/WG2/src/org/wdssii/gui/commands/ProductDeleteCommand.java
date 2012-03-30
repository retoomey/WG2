package org.wdssii.gui.commands;

import org.wdssii.gui.ProductManager;

/** Called by name from WdssiiDynamic
 * @author Robert Toomey
 * This command can delete 1 product, or all products matching an index key
 */
public class ProductDeleteCommand extends ProductCommand {

    private String myFilterName = null;
    private String myHandlerKey = null;
   
    public void ProductDeleteIndex(String indexName){
        myFilterName = indexName;
    }
    
    public void ProductDeleteByKey(String keyname){
        myHandlerKey = keyname;
    }

    @Override
    public boolean execute() {
        
        //ProductHandlerList theList = ProductManager.getInstance().getProductOrderedSet();
        //if (theList != null) {
            
            if (myFilterName != null) {
               //clearFromHandlerList(myFilterName);
               ProductManager.getInstance().deleteProductsMatchingSource(myFilterName);
            } else if (myHandlerKey != null){
               ProductManager.getInstance().deleteProduct(myHandlerKey);
            }
             // theList.deleteSelectedProduct();
       // }
        return true;
    }
}