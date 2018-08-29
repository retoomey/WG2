package org.wdssii.gui.features;

import java.awt.event.MouseEvent;

/** Mouse event information for features */
public class FeatureMouseEvent {

	// Input stuff...
	public boolean leftDown = false;
	public boolean middleDown = false;
	public boolean shiftDown = false;
	public boolean ctrlDown = false;
	public int dx = 0;
	public int dy = 0;
	public int x = 0;
	public int y = 0;
	
	// Output stuff...
	
	/** Fill in input variables from a mouse event */
	public void mouseEventToFeatureMouseEvent(MouseEvent e, int height) {
		// Delta value from previous settings...
		final int newX = e.getX();
		final int newY = height - e.getY();
		dx = newX - x;
		dy = newY - y;

		// Update current x/y
		x = newX;
		y = newY;

		// Buttons (note that drag/move don't have the state so
		// generalizing this requires using the masks instead.
		// f.leftDown = (e.getButton() == MouseEvent.BUTTON1);
		// f.middleDown = (e.getButton() == MouseEvent.BUTTON2);
		// f.shiftDown = e.isShiftDown();
		// f.ctrlDown = e.isControlDown();

		int mods = e.getModifiersEx();
		leftDown = (mods & MouseEvent.BUTTON1_DOWN_MASK) != 0;
		middleDown = (mods & MouseEvent.BUTTON2_DOWN_MASK) != 0;
		shiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
		ctrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;

	}
		
}
