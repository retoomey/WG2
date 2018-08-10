package org.wdssii.gui.commands;

import javax.swing.JOptionPane;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.views.WindowManager;

public class ChartDeleteCommand extends WdssiiCommand {
	
	public static class ChartDeleteParams {
		public String myName;

		public ChartDeleteParams(String name) {
			myName = name;
		}
	}

	private ChartDeleteParams myParams;

	public ChartDeleteCommand(ChartDeleteParams params) {
		myParams = params;
	}
	
	@Override
	public boolean execute() {
		boolean doIt = false;
		
		if (getUserConfirm()) {
			//JOptionPane.showMessageDialog(getRootComponent(), "Delete this thing",
			//		"Delete error", JOptionPane.ERROR_MESSAGE);
			int dialogResult = JOptionPane.showConfirmDialog(
					getRootComponent(), 
					"Delete window '"+myParams.myName+"'?","Warning",JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION) {
				doIt = true;
			}
		}else {
			doIt = true;
		}
		
		if (doIt) {
			WindowManager.deleteWindow(myParams.myName);
		}
		return true;
	}
}
