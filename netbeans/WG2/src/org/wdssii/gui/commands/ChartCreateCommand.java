package org.wdssii.gui.commands;

import java.util.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.wdssii.gui.swing.JwgDropDownButton;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.views.CommandListener;

/** Command to create a chart
 * 
 * @author Robert Toomey
 *
 */
public class ChartCreateCommand extends ProductCommand {

	/** Interface for a view following the current list of products.*/
	public static interface ChartFollowerView extends CommandListener {

		/** Set the product you are following to this key (can include top) */
		void addChart(String name);
	}
        
	/** Get the list of options for command.  Sort them in drop-down or dialog order */
	@Override
	public ArrayList<CommandOption> getCommandOptions() {
		ArrayList<CommandOption> theList = new ArrayList<CommandOption>();
                
                // FIXME: Should factory this eventually
                // These strings are class names...
                // VSlice --> VSliceChart.java
                // Data2DTable --> Data2DTableChart
                theList.add(new CommandOption("Add VSlice Chart", "VSlice"));
		theList.add(new CommandOption("Add 2D Tracking Table", "Data2DTable"));
                theList.add(new CommandOption("Add Data Readout Chart", "DataRangeValue"));
		return theList;
	}

	/** During RCP updateElements, each element of the list needs updating. */
	@Override
	public String getSelectedOption() {

            return "";  // Create menu has no selection
	}

	/** Get the checked suboption...passing in active view (For example, each chart view has a drop
	 * down that is view dependent */
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

	/** Util to create a drop button for choosing which product to follow */
	public static JComponent getDropButton(CommandListener l) {
		// The product follow menu
		Icon link = SwingIconFactory.getIconByName("plus.png");
		final CommandListener target = l;
		JwgDropDownButton b = new JwgDropDownButton(link) {

			@Override
			public void generateMenu() {
				// Because the list dynamically changes
				ChartCreateCommand f = new ChartCreateCommand();
				f.setTargetListener(target);
				JPopupMenu menu = WdssiiCommand.getSwingMenuFor(f);
				setMenu(menu);
			}
		};
		b.setToolTipText("Choose chart to create");
		return b;
	}
}
