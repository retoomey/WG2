package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.views.WindowManager;

public class ChartSwapCommand extends WdssiiCommand {

	public static class ChartSwapParams {
		public String myName1;
		public String myName2;

		public ChartSwapParams(String name1) {
			myName1 = name1;
			myName2 =  WindowManager.getTopDataViewName();
		}
		
		public ChartSwapParams(String name1, String name2) {
			myName1 = name1;
			myName2 = name2;
		}
	}

	private ChartSwapParams myParams;
	
	public ChartSwapCommand(ChartSwapParams params) {
		myParams = params;
	}
	
	@Override
	public boolean execute() {
		WindowManager.swapWindows(myParams.myName1, myParams.myName2);
		return true;
	}

}
