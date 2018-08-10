package org.wdssii.gui.commands;

import javax.swing.JOptionPane;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.views.WindowManager;

public class ChartRenameCommand extends WdssiiCommand {

	public static class ChartRenameParams {
		public String myOldName;
		public String myNewName;

		public ChartRenameParams(String oldOne, String newOne) {
			myOldName = oldOne;
			myNewName = newOne;
		}
	}

	private ChartRenameParams myParams;

	public ChartRenameCommand(ChartRenameParams params) {
		myParams = params;
	}

	@Override
	public boolean execute() {
		if (getUserConfirm()) {
			while (true) {
				String result = JOptionPane.showInputDialog(getRootComponent(), "Change window name:",
						myParams.myOldName);
				if (result == null) { break; }
				if ((result.isEmpty()) || (WindowManager.windowExists(result))) {
					JOptionPane.showMessageDialog(getRootComponent(), "That name is already taken or invalid",
							"Rename error", JOptionPane.ERROR_MESSAGE);
				} else {
					if (result != myParams.myOldName) {
						WindowManager.renameWindow(myParams.myOldName, result);
						break;
					}
				}
			}
		}else {
			// Command only..can fail if name exists
			if (!myParams.myNewName.isEmpty()) {
				WindowManager.renameWindow(myParams.myOldName, myParams.myNewName);
			}
		}
		return true;
	}
}