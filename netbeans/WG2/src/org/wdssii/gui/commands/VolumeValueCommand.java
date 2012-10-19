package org.wdssii.gui.commands;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.wdssii.gui.swing.JwgDropDownButton;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.views.CommandListener;

/**
 * Command to change way in which the volume is interpreted.
 *
 * @author Robert Toomey
 */
public class VolumeValueCommand extends WdssiiCommand {

	/**
	 * The string representing following top and displayed to the user
	 */
	public final static String top = "Follow selected (top) product";

	/**
	 * Interface for a view following the current list of products.
	 */
	public static interface VolumeValueFollowerView extends CommandListener {

		/**
		 * Set the volume value you are using to this key
		 */
		void setCurrentVolumeValue(String changeTo);

		/**
		 * Get the product follow key
		 */
		String getCurrentVolumeValue();

		/**
		 * Get the current list of volume values from our target
		 */
		List<String> getValueNameList();
	}

	/**
	 * Get the list of options for command. Sort them in drop-down or dialog
	 * order
	 */
	@Override
	public ArrayList<WdssiiCommand.CommandOption> getCommandOptions() {

		// Need the volume followed to generate the stuff....hummm
		ArrayList<WdssiiCommand.CommandOption> theList = new ArrayList<WdssiiCommand.CommandOption>();

		if (myTargetListener != null) {
			if (myTargetListener instanceof VolumeValueFollowerView) {
				VolumeValueFollowerView v = (VolumeValueFollowerView) (myTargetListener);
				List<String> l = v.getValueNameList();
				if (l != null) {

					for (String s : l) {
						theList.add(new WdssiiCommand.CommandOption(s, s));
					}
				}
			}
		} else {
			theList.add(new WdssiiCommand.CommandOption("Missing", ""));
		}

		return theList;
	}

	/**
	 * During RCP updateElements, each element of the list needs updating.
	 */
	@Override
	public String getSelectedOption() {

		String choice = null;
		if (myTargetListener != null) {
			if (myTargetListener instanceof VolumeValueCommand.VolumeValueFollowerView) {
				choice = ((VolumeValueCommand.VolumeValueFollowerView) myTargetListener).getCurrentVolumeValue();
			}
		}
		//	if (choice == null) {
		//		choice = ProductManager.TOP_PRODUCT;
		//	}
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
					if (myTargetListener instanceof VolumeValueCommand.VolumeValueFollowerView) {
						((VolumeValueCommand.VolumeValueFollowerView) myTargetListener).setCurrentVolumeValue(value);
					}
				}
			}
		} else {
			System.out.println("VolumeValueCommand needs main option to tell what to use");
		}
		return true;
	}

	/**
	 * Util to create a drop button for choosing which product to follow
	 */
	public static JComponent getDropButton(CommandListener l) {
		// The product follow menu
		Icon link = SwingIconFactory.getIconByName("layers.png");
		final CommandListener target = l;
		JwgDropDownButton b = new JwgDropDownButton(link) {

			@Override
			public void generateMenu() {
				// Because the list dynamically changes
				VolumeValueCommand f = new VolumeValueCommand();
				f.setTargetListener(target);
				JPopupMenu menu = WdssiiCommand.getSwingCheckMenuFor(f);
				setMenu(menu);
			}
		};
		b.setToolTipText("Choose the way volume values are generated");
		return b;
	}
}
