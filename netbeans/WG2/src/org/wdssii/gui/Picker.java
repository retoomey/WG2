package org.wdssii.gui;

import java.util.ArrayList;

/**
 * Used to pick an object. Basically if you click in the display window you
 * 'pick' an item, this handles the logic.
 * 
 * @author Robert Toomey
 * 
 *         Keep a stored list of picked objects from a 2D point
 **/
public class Picker {

	private ArrayList<Object> myPicked = new ArrayList<Object>();

	/** Get the list of picked objects */
	public ArrayList<Object> getPicked() {
		return myPicked;
	}

	/** Anything needed to start/prepare for  picking */
	public void begin() {
		myPicked = new ArrayList<Object>();
	}

	/** Any clean up needed after picking */
	public void end() {

	}

	/** Add on successful pick */
	public void add(Object stuff) {
		myPicked.add(stuff);
	}

	/** Do the actual pick by some method.  Subclasses override and add to myPicked on successful hits */
	public void pick(int x, int y) {

	}

}