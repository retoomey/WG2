package org.wdssii.gui.views;

/** Our model split window class
 * 
 * @author Robert Toomey
 *
 */
public class SplitWindow extends Window
{
	/** Split percentage for split windows */
	private float mySplitPercentage  = 0.50f;
	
	/** Orientation flag for split windows */
	private boolean myOrientation = false;
	
	public SplitWindow(float f, boolean horizontal) {
		super(Window.WINDOW_SPLIT);
		mySplitPercentage = f;
		myOrientation = horizontal;
	}

	public float getSplitPercentage() { return mySplitPercentage; }
	
	void setSplitPercentage(float p) { mySplitPercentage = p; }

	public boolean getSplitHorizontal() { return myOrientation; }
	
	void setSplitHorizontal(boolean p) { myOrientation = !p; }
}
