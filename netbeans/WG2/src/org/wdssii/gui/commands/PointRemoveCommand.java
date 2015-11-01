package org.wdssii.gui.commands;

import java.util.ArrayList;
import org.wdssii.geom.LLD_X;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * Delete point number index (0 based), or all if index = -1;
 *
 * @author Robert Toomey
 */
public class PointRemoveCommand extends PointCommand {

	private int myIndex = -1;
	private int myPolygon = -1;
	private boolean myDeletePoly = false;

	public PointRemoveCommand(LLHAreaSet area, int value, boolean deletePolygon) {
		super(area);

		// ... -1, false --> delete all
		// ... 10, false --> delete point 10
		// ... 15, true --> delete all in polygon 15

		myDeletePoly = deletePolygon;
		if (deletePolygon) {
			myPolygon = value;
		} else {
			myIndex = value;
		}
	}

	@Override
	public boolean execute() {

		// Create a list copy without the point.
		FeatureMemento m = set().getMemento(); // vs getNewMemento as in gui
												// control...hummm
		@SuppressWarnings("unchecked")
		ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
		if (list != null) {

			// Delete any matching our polygon value....
			if (myDeletePoly) {
				ArrayList<LLD_X> remainingList = new ArrayList<LLD_X>();
				boolean polygonSelected = false;
				int previousLast = -1;
				int lastLast = -1;
				for (int i = 0; i < list.size(); i++) {
						LLD_X l = list.get(i);
						
					// Get last point of last previous polygon if any....
					if (l.getPolygon() < myPolygon){
						previousLast = i;
					}
					
					// Keep polygons not in the group...
					if (l.getPolygon() != myPolygon) {
						remainingList.add(l);
						lastLast = i;
					}else{
						if (l.getSelected()){ // If any polygon in group selected, should reselect
							polygonSelected = true;
						}
					}
				}
				if (polygonSelected){ // We deleted the selection, move to previous last polygon..
					if (previousLast == -1){
						previousLast = lastLast; // No previous, try last remaining polygon
					}
					if (select1) {
						for (int i = 0; i < list.size(); i++) {
							LLD_X l = list.get(i);
							if (i == previousLast) {
								l.setSelected(true);
							} else {
								l.setSelected(false);
							}
						}
					}
				}
				m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, remainingList);
			} else {

				// Remove ALL
				if (myIndex == -1) {
					m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, new ArrayList<LLD_X>());
				} else {
					// or remove just one...
					LLD_X toDelete = list.get(myIndex);
					int select = -1;
					if (toDelete != null) {
						if (toDelete.getSelected()) {
							select = myIndex - 1;
							if (select < 0){ select = list.size(); }
						}
					}
					list.remove(myIndex);

					// Try to select previous point on delete, iff it was already
					// selected..right?
					if (select > -1) {
						if (select1) {
							for (int i = 0; i < list.size(); i++) {
								LLD_X l = list.get(i);
								if (i == select) {
									l.setSelected(true);
								} else {
									l.setSelected(false);
								}
							}
						}
					}

				}
			}
		}

		// Delete them
		if (set() != null) {
			set(set().getFeature().getKey(), m);
			return super.execute();
		}
		return true;
	}
}
