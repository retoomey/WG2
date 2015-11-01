package org.wdssii.gui.commands;

import org.wdssii.gui.volumes.LLHAreaSet;

/** Select a point in a LLHAreaSet 
 * 
 * @author Robert Toomey
 *
 */
public class PointSelectCommand extends PointCommand {

	private int myIndex = -1;

	public PointSelectCommand(LLHAreaSet area, int index){
		super(area);

		myIndex = index;
	}


	@Override
	public boolean execute() {
		
		if (set() != null){
			set().selectLocation(myIndex, true);  /** force one **/
		}

		return true;
	}
}