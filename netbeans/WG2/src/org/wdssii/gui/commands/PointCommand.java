package org.wdssii.gui.commands;

import org.wdssii.gui.volumes.LLHAreaSet;

public class PointCommand extends FeatureChangeCommand {
	public final static boolean select1 = true;
	protected LLHAreaSet myPointSet = null;
	
	public PointCommand(LLHAreaSet a){
		myPointSet = a;
	}
	
	public LLHAreaSet set(){
		return myPointSet;
	}
}
