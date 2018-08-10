package org.wdssii.gui.views;

import java.util.Vector;

/** This looks just like WDSIIView, lol.  Trying to make a cleaner rewrite, the infonode
 * stuff got a bit crazy, mostly because I was new to the library.  The goal of this
 * class is to allow multiple GUI layout and GUI types.
 * @author Robert Toomey
 */
public class Window {
	
	/** Our sub windows that belong to us. */
	public Vector<Window> theWindows = new Vector<Window>();  // Could lazy create
		
	/** Our created components (not necessarily awt, but probably yeah for now).
	 * The layout can use place this object into the window tree */
	public Object myComponent1 = null;
	
	/** Link back to our parent, if any.  Helps when swapping for example. */
	private Window myParent = null;
	
	/** Group number for syncing.  */
	private int groupNumber = 0;
	
	/** Windows have a 'type' flag to help GUI generators create them 
	 * Should probably subclass these...antipattern.  For moment all types
	 * are stuck into one class for development.
	 * */
	private int myType;
	public static final int WINDOW_ROOT = 0;
	public static final int WINDOW_DATAVIEW = 1;
	public static final int WINDOW_CHART = 2;
	public static final int WINDOW_TAB = 3;
	public static final int WINDOW_SPLIT = 4;
	public static final int WINDOW_NAV = 5;
	public static final int WINDOW_SOURCES = 6;
	public static final int WINDOW_FEATURES = 7;
	public static final int WINDOW_CATALOG = 8;

	
	public Object myNode = null;
	
	/** Split percentage for split windows */
	private float mySplitPercentage  = 0.50f;
	
	public float getSplitPercentage() { return mySplitPercentage; }
	void setSplitPercentage(float p) { mySplitPercentage = p; }
	
	/** Orientation flag for split windows */
	private boolean myOrientation = false;


	public boolean getSplitHorizontal() { return myOrientation; }
	void setSplitHorizontal(boolean p) { myOrientation = !p; }
	
	// Node....
	/** A title typically shown at top of a window */
	public String myTitle = "None";

    /** Get title of window, used by window maker */
    public String getTitle() {
    	return myTitle;
    }
    
    /** Set title of window, up to window maker to use this how it wants */
    public void setTitle(String s) {
    	myTitle = s;
    }
    
    public Window getParent() {
    	return myParent;
    }
    
    public void setParent(Window p) {
    	myParent = p;
    }
    
	/** A single GUI thing stored within this window. It could be a JFrame or
	 * a JPanel, or Swt panel, or Infodock window, etc.  Up the the WindowMaker.
	 */
	//private Object myComponent;
	//private Object myComponent2;
	
	public Window(int type)
	{		
		myType = type;
		myNode = null;
	}
	
	public Window(int type, Object data) {
		myType = type;
		myNode = data;
	}
	
	private Object myGUI = null;
	public Object getGUI() { return myGUI; }
	public void setGUI(Object o) { myGUI = o; }
	
	/** Add a window */
	public void addWindow(Window aWindow)
	{
		theWindows.add(aWindow);
		aWindow.setParent(this);
	}
	
	/** Remove a window */
	public void removeWindow(Window aWindow)
	{
		int index = theWindows.indexOf(aWindow);
		if (index > 0) {
			theWindows.remove(aWindow);	
			aWindow.myParent = null;
		}
	}

	/** Set the type of window we are.  Makers use this to build GUIs */
	public void setType(int type) {
		myType = type;
	}
	
	/** Get the type of window we are.  Makers use this to build GUIs */
	public int getType()
	{
		return myType;
	}

}
