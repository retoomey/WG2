package org.wdssii.gui.commands;

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JideSplitButton;
import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.wdssii.core.CommandListener;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.swing.WdssiiCommandGUI;

/**
 * Command to follow a particular product, such as in a chart where it is
 * watching the top product or a particular product.
 *
 * @see ChartRCPView
 * @see
 * @author Robert Toomey
 *
 */
public class ProductFollowCommand extends ProductCommand {

    /**
     * The string representing following top and displayed to the user
     */
    public final static String top = "Follow selected (top) product";

    /**
     * Interface for a view following the current list of products.
     */
    public static interface ProductFollowerView extends CommandListener {

        /**
         * Set the product you are following to this key (can include top)
         */
        void setCurrentProductFollow(String changeTo);

        /**
         * Get the product follow key
         */
        String getCurrentProductFollow();
    }

    /**
     * Get the list of options for command. Sort them in drop-down or dialog
     * order
     */
    @Override
    public ArrayList<CommandOption> getCommandOptions() {

        // Go through products, get a sorted string list...
        ProductManager m = ProductManager.getInstance();
        //ProductHandlerList p = m.getProductOrderedSet();
        List<ProductFeature> p = m.getProductFeatures();

        Iterator<ProductFeature> iter = p.iterator();
        ArrayList<CommandOption> theList = new ArrayList<CommandOption>();
        int currentLine = 0;
        while (iter.hasNext()) {
            ProductFeature h = iter.next();
            theList.add(new CommandOption(h.getListName(), h.getKey()));
            currentLine++;
        }
        Collections.sort(theList, new Comparator<CommandOption>() {
            @Override
            public int compare(CommandOption o1, CommandOption o2) {
                return o1.visibleText.compareTo(o2.visibleText);
            }
        });
        theList.add(0, new CommandOption(top, ProductManager.TOP_PRODUCT));

        return theList;
    }

    /**
     * During RCP updateElements, each element of the list needs updating.
     */
    @Override
    public String getSelectedOption() {

        String choice = null;
        if (myTargetListener != null) {
            if (myTargetListener instanceof ProductFollowerView) {
                choice = ((ProductFollowerView) myTargetListener).getCurrentProductFollow();
            }
        }
        if (choice == null) {
            choice = ProductManager.TOP_PRODUCT;
        }
        return choice;
    }

    /**
     * Get the checked suboption...passing in active view (For example, each
     * chart view has a drop down that is view dependent
     */
    @Override
    public boolean execute() {

        // Get the parameter out of us.  Should be "wdssii.ChartSetTypeParameter"
        if (myParameters != null) {
            String value = myParameters.get(option);

            // Null choice currently means button was picked..should bring up dialog..
            if (value != null) {
                // Need the view in order to send the command...
                if (myTargetListener != null) {
                    if (myTargetListener instanceof ProductFollowerView) {
                        ((ProductFollowerView) myTargetListener).setCurrentProductFollow(value);
                    }
                }
            }
        } else {
            System.out.println("ProductFollowCommand needs main option to tell what to follow");
        }
        return true;
    }

    /**
     * Util to create a drop button for choosing which product to follow
     */
    public static JComponent getDropButton(CommandListener l) {
        // The product follow menu
        Icon link = SwingIconFactory.getIconByName("link.png");
        final CommandListener target = l;
        JideSplitButton b = new JideSplitButton("");
        b.setIcon(link);
        b.setAlwaysDropdown(true);
        b.setToolTipText("Choose product to follow");
        b.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
            @Override
            public void customize(JPopupMenu menu) {
                ProductFollowCommand f = new ProductFollowCommand();
                f.setTargetListener(target);
                WdssiiCommandGUI.fillCheckMenuFor(menu, f);
            }
        });
        return b;
    }
}
