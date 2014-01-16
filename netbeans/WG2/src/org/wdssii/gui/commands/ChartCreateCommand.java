package org.wdssii.gui.commands;

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JideSplitButton;
import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.wdssii.core.CommandListener;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.swing.WdssiiCommandGUI;

/**
 * Command to create a chart
 *
 * @author Robert Toomey
 *
 */
public class ChartCreateCommand extends ProductCommand {

    /**
     * Interface for a view following the current list of products.
     */
    public static interface ChartFollowerView extends CommandListener {

        /**
         * Set the product you are following to this key (can include top)
         */
        void addChart(String name);
    }

    /**
     * Get the list of options for command. Sort them in drop-down or dialog
     * order
     */
    @Override
    public ArrayList<CommandOption> getCommandOptions() {
        ArrayList<CommandOption> theList = new ArrayList<CommandOption>();

        theList.add(new CommandOption("Add Worldwind", "WorldWindDataView"));
       // theList.add(new CommandOption("Add GeoToolDataView", "GeoToolDataView"));
        theList.add(new CommandOption("Add VSlice Chart", "VSliceChart"));
        theList.add(new CommandOption("Add 2D Tracking Table", "Data2DTableChart"));
        theList.add(new CommandOption("Add Data Readout Chart", "DataRangeValueChart"));

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
                    if (myTargetListener instanceof ChartFollowerView) {
                        ((ChartFollowerView) myTargetListener).addChart(value);
                    }
                }
            }
        } else {
            System.out.println("ChartCreateCommand needs main option to tell what to create");
        }
        return true;
    }

    /**
     * Util to create a drop button for choosing which product to follow
     */
    public static JComponent getDropButton(CommandListener l) {
        // The product follow menu
        Icon link = SwingIconFactory.getIconByName("plus.png");
        final CommandListener target = l;
        JideSplitButton b = new JideSplitButton("");
        b.setIcon(link);
        b.setAlwaysDropdown(true);
        b.setToolTipText("Choose chart to create");
        b.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
            @Override
            public void customize(JPopupMenu menu) {
                ChartCreateCommand f = new ChartCreateCommand();
                f.setTargetListener(target);
                WdssiiCommandGUI.fillMenuFor(menu, f);
            }
        });
        return b;
    }
}
