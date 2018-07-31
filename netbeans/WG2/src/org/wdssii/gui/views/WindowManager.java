package org.wdssii.gui.views;

import org.wdssii.gui.charts.W2DataView;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/** Rewrite of our view layout stuff.  Try to reduce the mess.  We need a more
 * top down approach.  The current way is kind of a spaghetti mess.
 * 
 * Windows are completely unrelated to the GUI they are within.  They store
 * no direct GUI information.
 * 
 * So this is the MODEL part of a MVC pattern.  The tree of Window objects
 * is parsed and then holds a 'GUI' object (VIEW) that represents it in the widget library.
 * This allows me to swap/out control GUI items
 * 
 * @author Robert Toomey
 */
public class WindowManager {
	
	private final static Logger LOG = LoggerFactory.getLogger(WindowManager.class);
			
	/** (MODEL) The top window. We call this desktop (this allows other 'main' windows to be created.) */
	public static Window theTopWindow;
	
	/** (VIEW) The thing that creates the actual GUI for our model objects */
	private static WindowMaker myGUI;

	/**
	 * interface to doing real window work in a GUI system
	 */
	public static interface WindowMaker {

		/** Create the GUI for given window by type. GUI usually
		 * wraps around this in some way, either a panel or something
		 * Object returned depends upon the maker gui library, swt based or swing, etc.
		 * 
		 * @param aWindow the window to start from, usually a root window
		 */
		Object createWindowGUI(Object aWindow);

		/** Create a GUI/window from a given window forest top */
		void init(final Window aWindow);

	}
	
	/** Create menu bar for main window .
	 * FIXME: probably need a MenuMaker class.  Menu might be a different
	 * widget set. */
	public void createMenuBar()
	{
		
	}
	
	/** Linking to the thing that draws the view..the ViewMaker, WindowMaker, whatever
	 * Should be generic interface here of course*/
	public static void init(WindowMaker d)
	{	
		// Humm if I wanna hot swap complete window layouts, need to precreate the nodes
		NavView nav = new NavView();
		SourcesView sview = new SourcesView(false);
		FeaturesView fview = new FeaturesView(false);
		CatalogView cat = new CatalogView();
		Object data1 = new W2DataView().getNewGUIForChart(null);
		Object data2 = new W2DataView().getNewGUIForChart(null);
		Object data3 = new W2DataView().getNewGUIForChart(null);

		theTopWindow = new Window(Window.WINDOW_ROOT); // Zero is 'base'
	
		// Old layout
		/*Window w = new Window(Window.WINDOW_SPLIT);
		w.setSplitPercentage(0.5f);
		w.setSplitHorizontal(false);
		
		// Left split dataview and nav below..
		Window w2 = new Window(Window.WINDOW_SPLIT);
		w2.setSplitPercentage(0.7f);
		w2.setSplitHorizontal(true);	
		Window data = new Window(Window.WINDOW_DATAVIEW);
		data.addWindow(new Window(new W2DataView().getNewGUIForChart(null)));
		w2.addWindow(data); // Still have to set title, icon for example
		w2.addWindow(new Window(new NavView()));
		w.addWindow(w2);
		
		// Right side tabs
		Window t = new Window(Window.WINDOW_TAB);
		w.addWindow(t);
		t.addWindow(new Window(new SourcesView(false)));
		t.addWindow(new Window(new FeaturesView(false)));
		t.addWindow(new Window(new CatalogView()));
		*/
		
		// Top data view above, nav/etc below
		Window w2 = new Window(Window.WINDOW_SPLIT);
		w2.setSplitPercentage(0.7f);
		w2.setSplitHorizontal(true);	
		Window data = new Window(Window.WINDOW_DATAVIEW);
		data.addWindow(new Window(data1, "", ""));
		data.addWindow(new Window(data2, "", ""));
		data.addWindow(new Window(data3, "", ""));
		w2.addWindow(data); // Still have to set title, icon for example
		
		// Bottom
		Window t = new Window(Window.WINDOW_TAB);	
		t.addWindow(new Window(nav, "Navigator", "eye.png"));
		t.addWindow(new Window(sview, "Sources", "brick_add.png"));
		t.addWindow(new Window(fview, "Features", "brick_add.png"));
		t.addWindow(new Window(cat, "Catalog", "cart_add.png"));
		w2.addWindow(t);
		theTopWindow.addWindow(w2);

		myGUI = d;
		myGUI.init(theTopWindow);
	}
}
