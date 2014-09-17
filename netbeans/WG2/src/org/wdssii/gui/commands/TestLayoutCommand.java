package org.wdssii.gui.commands;

/**
 *  Testing a multi-layout for dataviews, that allows us to use the 
 * power of custom layout, with the power of minimal power layouts..
 * 
 * @author Robert Toomey
 */

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JideSplitButton;
import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.wdssii.core.CommandListener;
import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.swing.WdssiiCommandGUI;

/**
 * Command to change layouts in the data view..
 *
 * @author Robert Toomey
 *
 */
public class TestLayoutCommand extends WdssiiCommand {


    /**
     * Interface for a view following the current list of products.
     */
    public static interface TestLayoutView extends CommandListener {

        /**
         * Set the product you are following to this key (can include top)
         */
        void doTestLayout(String name);
    }

    /**
     * Get the list of options for command. Sort them in drop-down or dialog
     * order
     */
    @Override
    public ArrayList<WdssiiCommand.CommandOption> getCommandOptions() {
        ArrayList<WdssiiCommand.CommandOption> theList = new ArrayList<WdssiiCommand.CommandOption>();

        // I wouldn't do this normally, do for now at least we have a short list
        // that shouldn't change much
        theList.add(new WdssiiCommand.CommandOption("Toggle simple/advanced layout", "simple"));
       
        return theList;
    }

    /**
     * During RCP updateElements, each element of the list needs updating.
     */
    @Override
    public String getSelectedOption() {

        return "";  // Create menu has no selection
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
                    if (myTargetListener instanceof TestLayoutView) {
                        ((TestLayoutView) myTargetListener).doTestLayout(value);
                    }
                }
            }
        } else {
            System.out.println("TestLayoutViewView needs main option to tell what to create");
        }
        return true;
    }

    /**
     * Util to create a drop button for choosing which product to follow
     */
    public static JComponent getDropButton(CommandListener l) {
        // The product follow menu
        Icon link = SwingIconFactory.getIconByName("eye.png");
        final CommandListener target = l;
        JideSplitButton b = new JideSplitButton("");
        b.setIcon(link);
        b.setAlwaysDropdown(true);
        b.setToolTipText("Testing layout stuff...");
        b.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
            @Override
            public void customize(JPopupMenu menu) {
                TestLayoutCommand f = new TestLayoutCommand();
                f.setTargetListener(target);
                WdssiiCommandGUI.fillMenuFor(menu, f);
            }
        });
        return b;
    }
}
