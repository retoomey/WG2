package org.wdssii.gui.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.WdssiiCommand.WdssiiMenuList;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.views.WdssiiView;

/** Command to follow a particular product, such as in a chart where it is watching the top product or
 * a particular product.
 * 
 * @see ChartRCPView
 * @see
 * @author Robert Toomey
 *
 */
public class ProductFollowCommand extends ProductCommand implements WdssiiMenuList {

    /** The string representing following top and displayed to the user */
    public final static String top = "Follow selected (top) product";

    /** Interface for a view following the current list of products.*/
    public static interface ProductFollowerView extends WdssiiView {

        /** Set the product you are following to this key (can include top) */
        void setCurrentProductFollow(String changeTo);

        /** Get the product follow key */
        String getCurrentProductFollow();
    }

    /** Get the list of suboptions for command.  Sort them in drop-down or dialog order */
    @Override
    public ArrayList<MenuListItem> getSuboptions() {

        // Go through products, get a sorted string list...
        CommandManager m = CommandManager.getInstance();
        ProductHandlerList p = m.getProductOrderedSet();
        Iterator<ProductHandler> iter = p.getIterator();
        ArrayList<MenuListItem> theList = new ArrayList<MenuListItem>();
        int currentLine = 0;
        while (iter.hasNext()) {
            ProductHandler h = iter.next();
            theList.add(new MenuListItem(h.getListName(), h.getKey()));
            currentLine++;
        }
        Collections.sort(theList, new Comparator<MenuListItem>() {

            @Override
            public int compare(MenuListItem o1, MenuListItem o2) {
                return o1.visibleText.compareTo(o2.visibleText);
            }
        });
        theList.add(0, new MenuListItem(top, ProductHandlerList.TOP_PRODUCT));

        return theList;
    }

    /** During RCP updateElements, each element of the list needs updating. */
    @Override
    public String getCurrentOptionInfo() {

        String choice = null;
        if (myWdssiiView != null) {
            if (myWdssiiView instanceof ProductFollowerView) {
                choice = ((ProductFollowerView) myWdssiiView).getCurrentProductFollow();
            }
        }
        if (choice == null) {
            choice = ProductHandlerList.TOP_PRODUCT;
        }
        return choice;
    }

    /** Get the checked suboption...passing in active view (For example, each chart view has a drop
     * down that is view dependent */
    @Override
    public boolean execute() {

        // Get the parameter out of us.  Should be "wdssii.ChartSetTypeParameter"
        if (myParameters != null) {
            String value = myParameters.get("wdssii.ProductFollowParameter");

            // Null choice currently means button was picked..should bring up dialog..
            if (value != null) {
                // Need the view in order to send the command...
                if (myWdssiiView != null) {
                    if (myWdssiiView instanceof ProductFollowerView) {
                        ((ProductFollowerView) myWdssiiView).setCurrentProductFollow(value);
                    }
                }
            }
        } else {
            System.out.println("Choose a product to follow in the drop down menu");

            // Ok this is the 'top' button..not the drop menu...so bring up a dialog or do nothing?
            // Need the view though...
        }
        return true;
    }
}
