package org.wdssii.gui.views;

import java.util.Vector;

final class TabWindow extends Window
{
	public TabWindow() {
		super(Window.WINDOW_TAB);
	}
}

/** This looks just like WDSIIView, lol.  Trying to make a cleaner rewrite, the infonode
 * stuff got a bit crazy, mostly because I was new to the library.  The goal of this
 * class is to allow multiple GUI layout and GUI types.
 * @author Robert Toomey
 */
public class Window {
	
	/** Our sub windows that belong to us. */
	public Vector<Window> theWindows = new Vector<Window>();  // Could lazy create
		
	/** Link back to our parent, if any.  Helps when swapping for example. */
	private Window myParent = null;
	
	/** A title typically shown at top of a window */
	private String myTitle = "None";

	/** Group number for sync.  Humm only charts have this though right? FIXME: maybe subclasses only */
	private int myGroupNumber = 0;

	/** A GUI object usable by a window maker */
	private Object myGUI = null;

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

    /** Get title of window, used by window maker */
    public String getTitle() {
    	return myTitle;
    }
    
    /** Set title of window, up to window maker to use this how it wants */
    public void setTitle(String s) {
    	myTitle = s;
    }
    
    /** Get our parent window, if any */
    public Window getParent() {
    	return myParent;
    }
    
    /** Set our parent window, if any */
    public void setParent(Window p) {
    	myParent = p;
    }
    
    /** Get group number of this window */
    public int getGroupNumber() {
    	return myGroupNumber;
    }
    
    /** Set the group number of this window */
    public void setGroupNumber(int g) {
    	myGroupNumber = g;
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
	
	public Window(int type)
	{		
		myType = type;
	}
	
//	public Window(int type, Object data) {
//		myType = type;
//	}
	
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

	public void doSyncGroup(Window w, int mode) {
		System.out.println("Synchronize group called...");
	}


}
