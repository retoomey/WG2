package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.views.WindowManager;

public class ChartSetGroupNumberCommand extends WdssiiCommand {

	public static class ChartSetGroupNumberParams {
		public String myName;
		public int myGroupNumber;

		public ChartSetGroupNumberParams(String name, int g) {
			myGroupNumber = g;
			myName = name;
		}
	}

	private ChartSetGroupNumberParams myParams;

	public ChartSetGroupNumberCommand(ChartSetGroupNumberParams params) {
		myParams = params;
	}

	@Override
	public boolean execute() {
		WindowManager.setGroupWindow(myParams.myName, myParams.myGroupNumber);
		return true;
	}
}
