package org.wdssii.gui.views;

import java.util.Vector;

import javax.swing.Icon;

import org.wdssii.gui.swing.SwingIconFactory;

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
	
	/** Link back to our parent, if any */
	//private Window myParent = null;
	
	/** Windows have a 'type' flag to help GUI generators create them 
	 * Should probably subclass these...antipattern.  For moment all types
	 * are stuck into one class for development.
	 * */
	private int myType;
	public static final int WINDOW_NODE = -1; // Node store reused GUI items
	public static final int WINDOW_ROOT = 0;
	public static final int WINDOW_DATAVIEW = 1;
	public static final int WINDOW_CHART = 2;
	public static final int WINDOW_TAB = 3;
	public static final int WINDOW_SPLIT = 4;
	
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
	public String myTitle;

	/** A icon typically shown left of the title */
	public String myIconName;
	
    /**
     * The icon for this window, if any
     */
    public Icon getWindowIcon() {
        Icon i = null;
        if (!myIconName.isEmpty()) {
            i = SwingIconFactory.getIconByName(myIconName);
        }
        return i;
    }
    
    public String getTitle() {
    	return myTitle;
    }
	/** A single GUI thing stored within this window. It could be a JFrame or
	 * a JPanel, or Swt panel, or Infodock window, etc.  Up the the WindowMaker.
	 */
	//private Object myComponent;
	//private Object myComponent2;
	
	public Window(int type)
	{		
		myType = type;
	}
	
	public Window(Object o, String title, String iconName) {
		myType = WINDOW_NODE;
		myNode = o;
		myTitle = title;
		myIconName = iconName;
	}
	
	private Object myGUI = null;
	public Object getGUI() { return myGUI; }
	public void setGUI(Object o) { myGUI = o; }
	
	/** Add a window */
	public void addWindow(Window aWindow)
	{
		theWindows.add(aWindow);
	//	aWindow.setParent(this);
	}
	
	/** Remove a window */
	/*public void removeWindow(Window aWindow)
	{
		// CODE not used yet, maybe never right?  Well on delete
		// will need something like this, but GUI should go too
		int index = theWindows.indexOf(aWindow);
		if (index > 0) {
			theWindows.remove(aWindow);	
			aWindow.myParent = null;
			// Note the assumption is you're destroying this I think...
			// FIXME: kill the GUI for this?
		}
	}*/
	
	/** Name a window.  This can show up in a title bar, for instance. */
	public void renameWindow(String name)
	{
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
