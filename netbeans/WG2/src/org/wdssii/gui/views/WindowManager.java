package org.wdssii.gui.views;

import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import org.wdssii.core.CommandManager;
import org.wdssii.core.W2Config;
import org.wdssii.geom.V3;
import org.wdssii.gui.Application;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.charts.W2DataView;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.IndexSourceAddParams;
import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.features.EarthBallFeature;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.LoopFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Rewrite of our view layout stuff. Try to reduce the mess. We need a more top
 * down approach. The current way is kind of a spaghetti mess.
 * 
 * Windows are completely unrelated to the GUI they are within. They store no
 * direct GUI information.
 * 
 * So this is the MODEL part of a MVC pattern. The tree of Window objects is
 * parsed and then holds a 'GUI' object (VIEW) that represents it in the widget
 * library. This allows me to swap/out control GUI items
 * 
 * @author Robert Toomey
 */
public class WindowManager {

	private final static Logger LOG = LoggerFactory.getLogger(WindowManager.class);

	/**
	 * Default top window title string
	 */
	public final static String WINDOWTITLE = Application.NAME + " " + Application.MAJOR_VERSION + "."
			+ Application.MINOR_VERSION;
	// public final static String UNTITLED = "Untitled";

	/**
	 * (MODEL) The top window. We call this desktop (this allows other 'main'
	 * windows to be created.)
	 */
	public static Window theTopWindow;

	/** Top window for data views at moment */
	private static Window theDataViews;

	/** Name of the main data view window */
	private static String myTopDataView = "";

	/** (VIEW) The thing that creates the actual GUI for our model objects */
	private static WindowMaker myGUI;

	/**
	 * interface to doing real window work in a GUI system
	 */
	public static interface WindowMaker {

		/**
		 * Create the GUI for given window by type. GUI usually wraps around this in
		 * some way, either a panel or something Object returned depends upon the maker
		 * gui library, swt based or swing, etc.
		 * 
		 * @param aWindow the window to start from, usually a root window
		 */
		Object createWindowGUI(Object aWindow);

		/** Create a GUI/window from a given window forest top */
		void init(final Window aWindow);

		// Notification of changes to model

		/** Notify when two windows are swapped in the model */
		void notifySwapped(Window a, Window b);

		/** Notify when a window is deleted in the model */
		void notifyDeleted(Window wasDeleted);

		/** Notify when a window is renamed in the model */
		void notifyRenamed(Window w);
	}

	/**
	 * Create menu bar for main window . FIXME: probably need a MenuMaker class.
	 * Menu might be a different widget set.
	 */
	public void createMenuBar() {

	}

	/**
	 * Linking to the thing that draws the view..the ViewMaker, WindowMaker,
	 * whatever Should be generic interface here of course
	 */
	public static void init(WindowMaker d) {
		theTopWindow = new Window(Window.WINDOW_ROOT); // Zero is 'base'
		theTopWindow.setTitle(WINDOWTITLE);

		// Add some data views..set titles properly in model
		Vector<DataView> myDataViews = new Vector<DataView>();
		int counter = 0;
		for (int i = 0; i < 3; i++) {
			W2DataView dv = new W2DataView();
			dv.setTitle("#" + Integer.toString(++counter));
			// Defaults for main window...
			FeatureList fl = dv.getFeatureList();

			// Ok put compass and loop stuff in every W2DataView..
			// FIXME: this code should probably actually be in W2DataView
			Feature legend = LegendFeature.createLegend(fl, "compass", "scale", "insert", "controls"); // FIXME: magic
																										// strings																						// better way
			fl.addFeature(legend);
			Feature loop = new LoopFeature(fl);
			fl.addFeature(loop);
			Feature earthBall = new EarthBallFeature(fl);
			fl.addFeature(earthBall);

			URL mapURL = W2Config.getURL("maps/shapefiles/usa/ok/okcnty.shp");
			if (mapURL != null) {
				String filename = mapURL.getPath();
				Feature testOne = new MapFeature(fl, filename);
				fl.addFeature(testOne);
			}
			
			// Just for main window...
			if (i == 0) {
				dv.setGroupNumber(1);
				
				// Lazy load examples up
				boolean loadExamples = true;
				if (loadExamples) {

					// Example source
					URL aURL = W2Config.getURL("data/KTLX_05031999/code_index.xml");
					if (aURL != null) {
						SourceAddParams params = new IndexSourceAddParams("KTLX-MAY-1999", aURL, false, true,
								IndexSource.HISTORY_ARCHIVE);
						SourceAddCommand c = new SourceAddCommand(params);
						c.setConfirmReport(false, false, null);
						CommandManager.getInstance().executeCommand(c, false);
					}
				}
			}else if (i ==1) {
				dv.setGroupNumber(1);
			}
			myDataViews.add(dv);
		}
		// myDataViews.add(new Data2DTableChart());

		myTopDataView = "#1";

		// Old layout
		/*
		 * Window w = new Window(Window.WINDOW_SPLIT); w.setSplitPercentage(0.5f);
		 * w.setSplitHorizontal(false);
		 * 
		 * // Left split dataview and nav below.. Window w2 = new
		 * Window(Window.WINDOW_SPLIT); w2.setSplitPercentage(0.7f);
		 * w2.setSplitHorizontal(true); Window data = new
		 * Window(Window.WINDOW_DATAVIEW); for (int i=0; i < myDataViews.size(); i++) {
		 * data.addWindow(new Window(myDataViews.get(i), "", "")); } w2.addWindow(data);
		 * w2.addWindow(new Window(new NavView())); w.addWindow(w2);
		 * 
		 * // Right side tabs Window t = new Window(Window.WINDOW_TAB); w.addWindow(t);
		 * t.addWindow(new Window(new SourcesView(false))); t.addWindow(new Window(new
		 * FeaturesView(false))); t.addWindow(new Window(new CatalogView()));
		 */

		// Top data view above, nav/etc below
		// Window w2 = new Window(Window.WINDOW_SPLIT);
		SplitWindow w2 = new SplitWindow(0.6f, true);

		// Add a window for each data view
		Window data = new Window(Window.WINDOW_DATAVIEW);
		theDataViews = data;
		Iterator<DataView> itr = myDataViews.iterator();
		while (itr.hasNext()) {
			DataView dv = itr.next();
			data.addWindow(dv);
		}
		w2.addWindow(data);

		// Bottom
		// Window t = new Window(Window.WINDOW_TAB);
		Window t = new TabWindow();

		t.addWindow(new Window(Window.WINDOW_NAV));
		t.addWindow(new Window(Window.WINDOW_SOURCES));
		t.addWindow(new Window(Window.WINDOW_FEATURES));
		t.addWindow(new Window(Window.WINDOW_CATALOG));
		w2.addWindow(t);
		theTopWindow.addWindow(w2);

		myGUI = d;
		myGUI.init(theTopWindow);
	}

	public static String getTopDataViewName() {
		return myTopDataView;
	}

	/** Is this window the top data view? */
	public static boolean isTopDataView(Window w) {
		String l = w.getTitle();
		return (l.equals(myTopDataView));
	}

	/**
	 * Return found windows matching a list of given names, passed in window's name
	 * is ignored. Non-recursive for now, can implement later if needed.
	 */
	public static Vector<Window> findWindows(Window top, Vector<String> names) {
		Vector<Window> results = new Vector<Window>();
		results.setSize(names.size()); // null fills

		// names.set(1, "#3");
		// names.set(0, "#2");

		Iterator<Window> iter = top.theWindows.iterator();
		final int aSize = names.size();
		int found = 0;
		while (iter.hasNext()) {
			Window at = iter.next();
			String title = at.getTitle();
			if (title != null) {
				for (int i = 0; i < aSize; i++) {
					if (title.equals(names.get(i))) {
						results.set(i, at);
						found++;
						if (found >= aSize) {
							break;
						}
					}
				}
			}
		}
		return results;
	}

	/** Find list of windows matching a sync group number */
	public static Vector<Window> findSyncGroup(Window top, int groupNumber) {
		Vector<Window> results = new Vector<Window>();
		Iterator<Window> iter = top.theWindows.iterator();
		while (iter.hasNext()) {
			Window at = iter.next();
			final int g = at.getGroupNumber();
			if (g == groupNumber) {
				results.add(at);
			}
		}
		return results;
	}

	/** Does data view with given name exist? */
	public static boolean windowExists(String aName) {
		Vector<String> names = new Vector<String>();
		names.add(aName);
		Vector<Window> windows = findWindows(theDataViews, names);
		return (windows.get(0) != null);
	}

	/** Rename a data window with a given name, return true on success */
	public static boolean renameWindow(String oldName, String newName) {
		if (newName.isEmpty()) {
			return false;
		}
		if (newName.equals(oldName)) {
			return true;
		}

		// For the moment, only allowing rename to data views...
		Vector<String> names = new Vector<String>();
		names.add(oldName);
		names.add(newName);
		Vector<Window> windows = findWindows(theDataViews, names);
		if (windows.get(1) != null) { // uh oh found the new name
			LOG.error("Can't rename window to " + newName + " since that window exists");
			return false;
		}
		Window w = windows.get(0);
		if (w != null) {
			if (w.getTitle().equals(myTopDataView)) {
				myTopDataView = newName;
			}
			w.setTitle(newName);
			myGUI.notifyRenamed(w);
			return true;
		}
		return false;
	}

	public static void dumpWindows(Window top)
	{
		Iterator<Window> iter = top.theWindows.iterator();
		while (iter.hasNext()) {
			Window at = iter.next();
			LOG.error("Window name is '"+at.getTitle()+"' "+at);
		}
	}
	
	/**
	 * Find and swap two windows by name in the model and notify the WindowMaker to
	 * take action on this
	 * 
	 * @param aName
	 * @param bName
	 * @return
	 */
	public static boolean swapWindows(String aName, String bName) {
		Vector<String> names = new Vector<String>();
		names.add(aName);
		names.add(bName);

		Vector<Window> windows = findWindows(theDataViews, names);
		Window a = windows.get(0);
		Window b = windows.get(1);
		if ((a != null) && (b != null)) {

			// New top window perhaps...
			String aTitle = a.getTitle();
			String bTitle = b.getTitle();
			if (aTitle.equals(myTopDataView)) {
				myTopDataView = bTitle;
			} else if (bTitle.equals(myTopDataView)) {
				myTopDataView = aTitle;
			}

			// Swap windows in model
			Window.swapWindows(a, b);
			
			// Swap windows in GUI
			myGUI.notifySwapped(a, b);
		} else {
			LOG.error("Couldn't find windows to swap..."+aName+", "+bName);
		}
		return true;
	}

	/** Delete a data view window */
	public static boolean deleteWindow(String aName) {
		Vector<String> names = new Vector<String>();
		names.add(aName);
		Vector<Window> windows = findWindows(theDataViews, names);
		Window w = windows.get(0);
		if (w != null) {

			// Don't allow deletion of main window? We could swap in another..
			// need to check how wg does it...for now, don't allow
			if (w.getTitle().equals(myTopDataView)) {
				LOG.error("Can't delete the main window");
				return false;
			}

			// FIXME: Allow data view/windows to clean up..I think openGL could benefit
			// from explicit cleanup due to native issues
			Window parent = w.getParent();
			if (parent != null) {
				parent.removeWindow(w); // References should die here
			}
			myGUI.notifyDeleted(w);
			return true;
		} else {
			LOG.error("Can't find view '" + aName + "' to delete!");
		}
		return false;
	}

	/** Set the group number of the named window */
	public static void setGroupWindow(String aName, int aGroupNumber) {
		Vector<String> names = new Vector<String>();
		names.add(aName);
		Vector<Window> windows = findWindows(theDataViews, names);
		Window w = windows.get(0);
		if (w != null) {
			w.setGroupNumber(aGroupNumber);
		}
	}

	/** Get the maximum number of sync groups we have */
	public static int getMaxGroups() {
		return 10;
	}

	/**
	 * Hack for command manager to repaint data views on any command.
	 * To be honest, most commands affect the views...but it could be
	 * more efficient to have a subset.
	 */
	public static void repaintDataViews() {
		if (theDataViews != null) {
			Vector<Window> results = new Vector<Window>();
			Iterator<Window> iter = theDataViews.theWindows.iterator();
			while (iter.hasNext()) {
				Window at = iter.next();
				if (at instanceof DataView) {
					DataView dv = (DataView) (at);
					dv.repaint();
				}
			}
		}
	}

	/**
	 * Notify all windows sharing a group to perform sync action on the given group
	 * number 0 -- Camera change 1 -- Readout change
	 */
	public static void syncWindows(Window w, int mode, V3 readoutPoint, boolean inside) {
		int groupNumber = w.getGroupNumber();

		// FIXME: Might be a good idea to keep lists of the groups for speed..
		// This will get called during pan/zoom etc..
		if (groupNumber > 0) {
			Vector<Window> results = findSyncGroup(theDataViews, groupNumber);
			Iterator<Window> iter = results.iterator();
			while (iter.hasNext()) {
				Window at = iter.next();
				at.doSyncGroup(w, mode, readoutPoint, inside);
			}
		} else {
			// Window is solo..just send to itself
			w.doSyncGroup(w, mode, readoutPoint, inside);
		}
	}

	/**
	 * Kinda slow we should cache..entire display uses the feature list constantly
	 */
	public static FeatureList getTopFeatureList() {
		return (getFeatureListFor(myTopDataView));
	}

	public static FeatureList getFeatureListFor(String aName) {
		Vector<String> names = new Vector<String>();
		names.add(aName);
		Vector<Window> windows = findWindows(theDataViews, names);
		Window w = windows.get(0);
		if (w != null) {
			if (w instanceof DataView) { // Not sure how to handle subclasses having different stuff...
				return ((DataView) (w)).getFeatureList();
			}
		}
		LOG.error("Uh oh..the feature list is null...");
		return null;
	}
}
