package org.wdssii.gui.commands;

import java.util.ArrayList;

import org.wdssii.geom.LLD_X;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 *
 * @author Robert Toomey
 */
public class PointAddCommand extends PointCommand {

	private ArrayList<LLD_X> myList = null;
	private boolean myAppend = false;
	private int myIndex = -1;
	private LLD_X myNewLocation = null;
	private boolean myNewPolygon = false;
	private int finalIndex = 0;

	public PointAddCommand(LLHAreaSet area, LLD_X newLocation, int index, boolean newPolygon) {
		super(area);

		myNewLocation = newLocation;
		myNewPolygon = newPolygon;
	}

	public PointAddCommand(LLHAreaSet area, ArrayList<LLD_X> list, boolean append, boolean newPolygon) {
		super(area);

		myNewLocation = null;
		myList = list;
		myAppend = append;
		myNewPolygon = newPolygon;
	}

	// because brain-damaged ww requires index of point for control point...
	public int getFinalIndex() {
		return finalIndex;
	}

	@Override
	public boolean execute() {

		// Create a list copy
		FeatureMemento m = set().getMemento(); // vs getNewMemento as in gui
												// control...hummm
		@SuppressWarnings("unchecked")
		ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));

		if (list != null) {
			if (myList != null) { // Group add/replace

				if (myAppend) {
					// need to start with new polygon number and add each one...
				} else {
					m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, myList);
					// myArea.setLocations(myList);
				}
			} else { // Single add
				// We enforce polygon number ordering. So and this point to the
				// end of a group of polygons with
				// the same number.
				myIndex = 0; // Don't care about passed in index.
				boolean foundGroup = false;
				int p = set().currentPolygonNumber; // This is the polygon group
													// this new point goes into
				for (LLD_X l : list) {
					if (l.getPolygon() == p) {
						foundGroup = true; // Found the group...
					} else {
						if (foundGroup) {
							// The group changed. Put it HERE..
							break;
						}
					}
					myIndex++;
				}
				myNewLocation.setPolygon(p);
				list.add(myIndex, myNewLocation);

				// Auto select the new one on add...
				if (select1) {
					for (int i = 0; i < list.size(); i++) {
						LLD_X l = list.get(i);
						if (i == myIndex) {
							l.setSelected(true);
						} else {
							l.setSelected(false);
						}
					}
				}
			}
			finalIndex = myIndex;
		}

		if (set() != null) {
			set(set().getFeature().getKey(), m);
			return super.execute();
		}
		return false;
	}
}
