package org.wdssii.gui.views.infonode;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.wdssii.gui.Application;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.commands.OpenCommand;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.views.CatalogView;
import org.wdssii.gui.views.FeaturesView;
import org.wdssii.gui.views.NavView;
import org.wdssii.gui.views.SourcesView;
import org.wdssii.gui.views.SplitWindow;
import org.wdssii.gui.views.WdssiiDockedViewFactory.DockView;
import org.wdssii.gui.views.Window;
import org.wdssii.gui.views.WindowManager.WindowMaker;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import net.infonode.docking.DockingWindow;
import net.infonode.docking.RootWindow;
//import net.infonode.docking.SplitWindow;
import net.infonode.docking.TabWindow;
import net.infonode.docking.View;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.theme.BlueHighlightDockingTheme;
import net.infonode.docking.theme.ClassicDockingTheme;
import net.infonode.docking.theme.DefaultDockingTheme;
import net.infonode.docking.theme.DockingWindowsTheme;
import net.infonode.docking.theme.GradientDockingTheme;
import net.infonode.docking.theme.LookAndFeelDockingTheme;
import net.infonode.docking.theme.ShapedGradientDockingTheme;
import net.infonode.docking.theme.SlimFlatDockingTheme;
import net.infonode.docking.theme.SoftBlueIceDockingTheme;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.ViewMap;
import net.infonode.gui.laf.InfoNodeLookAndFeel;
import net.infonode.gui.laf.InfoNodeLookAndFeelTheme;
import net.infonode.tabbedpanel.TabAreaVisiblePolicy;
import net.infonode.tabbedpanel.TabLayoutPolicy;
import net.infonode.util.Direction;

/** A window maker that will generate an infonode based layout */
public class Infonode implements WindowMaker {

	private final static Logger LOG = LoggerFactory.getLogger(Infonode.class);

	// ----------------------------------------------
	// Java Swing color theme support
	//
	/**
	 * Color themes for infonode
	 */
	final static InfoNodeLookAndFeelTheme[] myColorThemes;
	private static int myCurrentColorThemeIndex = 0;

	/**
	 * Infonode window style themes
	 */
	final static DockingWindowsTheme[] myWindowThemes;
	private static int myCurrentThemeIndex = 2;

	private static final Object propSync = new Object();

	static {
		InfoNodeLookAndFeelTheme w1 = new InfoNodeLookAndFeelTheme("WDSSII DarkBlueGrey Theme",
				new Color(110, 120, 150), // Control color
				new Color(0, 170, 0), // primary control color
				new Color(80, 80, 80), // Background color
				Color.WHITE, // Text color
				new Color(0, 170, 0), // selected textbackground color
				Color.WHITE, // selected text color
				0.8);

		InfoNodeLookAndFeelTheme w2 = new InfoNodeLookAndFeelTheme("WDSSII OU Sooner Theme", new Color(153, 0, 0),
				new Color(0, 0, 255), Color.WHITE, Color.BLACK, Color.WHITE, // selected textbackground color
				Color.BLACK, // selected text color
				0.8);
		myColorThemes = new InfoNodeLookAndFeelTheme[] { w1, w2 };

		myWindowThemes = new DockingWindowsTheme[] { new DefaultDockingTheme(), new LookAndFeelDockingTheme(),
				new BlueHighlightDockingTheme(), new SlimFlatDockingTheme(), new GradientDockingTheme(),
				new ShapedGradientDockingTheme(), new SoftBlueIceDockingTheme(), new ClassicDockingTheme() };
	}

	/**
	 * Set the Color theme. This sets the actual java look and feel, vs the infonode
	 * 'theme' which is setting for how infonode draws itself.
	 *
	 * @param index
	 */
	private void setColorTheme(int index) {
		try {
			LOG.error("CALLING SET COLOR THEME TO " + index);
			final InfoNodeLookAndFeelTheme theme = myColorThemes[index];
			UIManager.setLookAndFeel(new InfoNodeLookAndFeel(theme));
			myCurrentColorThemeIndex = index;
			SwingUtilities.updateComponentTreeUI(myRootFrame);
		} catch (Exception e2) {
			// what to do....
		}
	}

	/**
	 * Set the InfoNode theme type. This is how the InfoNodeLookAndFeel draws itself
	 */
	private static void setTheme(int index) {
		/*
		 * try { DockingWindowsTheme theme = myWindowThemes[index]; if (currentTheme !=
		 * null) { synchronized (propSync) {
		 * properties.replaceSuperObject(currentTheme.getRootWindowProperties(),
		 * theme.getRootWindowProperties()); } } currentTheme = theme;
		 * myCurrentThemeIndex = index; } catch (Exception e) { // what to do.... }
		 */
	}

	// ----------------------------------------------

	private JFrame myRootFrame;

	private RootWindow rootWindow;
	private ViewMap viewMap;

	/** Start it up */
	@Override
	public void init(final Window aWindow) {
		// This actually sets the java UI, so we do this first
		setColorTheme(myCurrentColorThemeIndex);
		// Docking windows should be run in the Swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createWindowGUI(aWindow);
			}
		});
	}

	/**
	 * Copied from infonode PropertyUtil, allows me to have super fine control of
	 * appearance
	 */
	private static void setupTitleBarStyleProperties(RootWindowProperties titleBarStyleProperties) {
		titleBarStyleProperties.getViewProperties().getViewTitleBarProperties().setVisible(true);
		titleBarStyleProperties.getTabWindowProperties().getTabbedPanelProperties()
				.setTabAreaOrientation(Direction.LEFT).setTabLayoutPolicy(TabLayoutPolicy.SCROLLING);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getTitledTabProperties()
				.getNormalProperties().setDirection(Direction.UP);
		titleBarStyleProperties.getTabWindowProperties().getTabbedPanelProperties().getTabAreaProperties()
				.setTabAreaVisiblePolicy(TabAreaVisiblePolicy.MORE_THAN_ONE_TAB);

		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties()
				.getMinimizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties()
				.getRestoreButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties()
				.getCloseButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties()
				.getUndockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties()
				.getDockButtonProperties().setVisible(false);

		titleBarStyleProperties.getTabWindowProperties().getCloseButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getMaximizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getMinimizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getUndockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getDockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getRestoreButtonProperties().setVisible(false);

		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties()
				.getHighlightedButtonProperties().getMinimizeButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties()
				.getHighlightedButtonProperties().getRestoreButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties()
				.getHighlightedButtonProperties().getCloseButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties()
				.getHighlightedButtonProperties().getUndockButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties()
				.getHighlightedButtonProperties().getDockButtonProperties().setVisible(true);

		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabbedPanelProperties()
				.setTabLayoutPolicy(TabLayoutPolicy.SCROLLING);
	}

    /**
     * FIXME: I should use the command model I set up for this...
     *
     * @return
     */
    private JMenu createFileMenu() {

        // The file menu
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');

        JMenuItem eMenuItem;

/*
        eMenuItem = new JMenuItem("New");
        eMenuItem.setToolTipText("Create a new setup");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
             //   doNew();
            }
        });
        menu.add(eMenuItem);
        menu.addSeparator();

        eMenuItem = new JMenuItem("Open...");
        eMenuItem.setToolTipText("Create a new setup");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
              //  doOpen();
            }
        });
        menu.add(eMenuItem);
        menu.addSeparator();

        menu.add(OpenCommand.getRecentDocumentMenu());
        menu.addSeparator();

        eMenuItem = new JMenuItem("Save");
        //eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Save state of display, layout, sources to XML file");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
              //  doSave();
            }
        });
        menu.add(eMenuItem);

        eMenuItem = new JMenuItem("Save As...");
        //eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Save state of display, layout, sources to XML file...");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
              //  doSaveAs();
            }
        });
        menu.add(eMenuItem);

        menu.addSeparator();
        
        */
        eMenuItem = new JMenuItem("Exit");
        eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Exit application");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
              //  doExit();
            	System.exit(0);
            }
        });
        menu.add(eMenuItem);

        // menu.addSeparator();
        return menu;
    }
    
    /**
     * Creates the frame menu bar.
     * 
     * We could create a 'menu' maker class..push this into model..
     * 
     * @return the menu bar
     */
	private JMenuBar createMenuBar()
	{
        JMenuBar menu = new JMenuBar();
        menu.add(createFileMenu());
        // menu.add(createFocusViewMenu());

        // menu.add(createPropertiesMenu());
        // menu.add(createWindowBarsMenu());
      //  menu.add(createWindowMenu());
        // menu.add(createFloatingWindowMenu());
        return menu;
	}
	
	/** Create the root window */
	public Object createWindowRoot(Window aWindow) {
		// JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame(aWindow.getTitle());

		// Keep root window as GUI item...
		aWindow.setGUI(frame);
		
		// Ok so root menu could be done here instead....
		JMenuBar menuBar = createMenuBar();
		frame.setJMenuBar(menuBar);

		// Infonode rootwindow has to go into a frame
		myRootFrame = frame;
		viewMap = new ViewMap();

		boolean heavy = Application.USE_HEAVYWEIGHT_GL;

		heavy = true;
		if (heavy) {
			// rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap,
			// handler, true);
			rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap, true);
		} else {
			// rootWindow = DockingUtil.createRootWindow(viewMap, handler,
			// true);
			rootWindow = DockingUtil.createRootWindow(viewMap, true);
		}
		RootWindowProperties properties = new RootWindowProperties();
		setupTitleBarStyleProperties(properties);

		synchronized (propSync) { // Needed or not?
			rootWindow.getRootWindowProperties().addSuperObject(properties);
		}
		// Enable the bottom window bar
		rootWindow.getWindowBar(Direction.DOWN).setEnabled(true);
		// frame.getContentPane().add(rootWindow, BorderLayout.CENTER);
		frame.add(rootWindow);

		int count = aWindow.theWindows.size();
		if (count != 1) {
			// I think root infonode can actually handle more than one window...
			LOG.error("Root window can only handle one child at moment, sorry");
		} else {
			DockingWindow dd = (DockingWindow) createWindowGUI(aWindow.theWindows.get(0));
			rootWindow.setWindow(dd);
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		frame.setBounds(100, 100, 1000, 1000);

		aWindow.setGUI(rootWindow);
		return rootWindow;
	}

	/**
	 * Create the data view window, which is special because it can change custom
	 * layouts
	 */
	public Object createDataView(Window w) {

		// Custom layout class.

		// Use custom tile layout for all charts at moment.
		// This means no docking per-say here...
		// In theory I could use a single GL world and do my own camera subviews,
		// this would give us a big speed increase.
		JPanel panel = new JPanel(); // Could be a single heavyweight opengl,
		// would be our own class then that handled special opengl 'views' added to it
		panel.setLayout(new TileLayout());
		View view = new View("DataView", null, panel); // Dataview can dedock..
		// Problem is old way uses the DataFeatureView class to organize all the view
		// so how to execute creation commands
		// addTo.add(ChartCreateCommand.getDropButton(this));

		// Children will be charts...
		int count = w.theWindows.size();
		for (int i = 0; i < count; i++) {
			// Not letting children dedock yet...new layout playing

			// Note this break since not a component...?
			// DockingWindow childGUI = (DockingWindow)
			// createWindowGUI(w.theWindows.get(i));

			// Use simple tile layout engine for this.
			// Dataviews are currently their own GUI factories
			Object n = w.theWindows.get(i);
			if (n instanceof DataView) {
				DataView dv = (DataView) (n);
				Component addMe = (Component) dv.getNewGUIForChart(null);
				panel.add(addMe);
				w.theWindows.get(i).setGUI(addMe);
			} else {
				LOG.error("Data view expects subclasses of DataView for children!");
			}
		}
		w.setGUI(view);
		return view;
	}

	public Icon getWindowIcon(String myIconName) {
		Icon i = null;
		if (!myIconName.isEmpty()) {
			i = SwingIconFactory.getIconByName(myIconName);
		}
		return i;
	}

	/** Utility method to wrap a component stored in window's node in a DockWindow/View */
	private Object wrapInfoView(Window w, Component c, String aTitle, String aIconName)
	{
		// Wrap component in a infonode docking window
		View newOne = new View(aTitle, getWindowIcon(aIconName), c);
		//w.myNode = c;  // Store it in case we need to reference it later
		
		// If this is a wdssii DockView class object, it has menu ability at moment,
		// so call the DockView method as MenuMaker here.
		if (c instanceof DockView) {
			DockView theView = (DockView) (c);
			List<Object> l = new ArrayList<Object>();
			theView.addGlobalCustomTitleBarComponents(l); // Note class can cache them
			@SuppressWarnings("unchecked")
			List<Object> realList = newOne.getCustomTitleBarComponents();
			for (Object o : l) {
				realList.add(o);
			}

		}
		return newOne;	
	}



	/** Create a split window return new GUI item */
	private Object createSplit(SplitWindow w) {
		int count = w.theWindows.size();
		if (count != 2) {
			// FIXME: we could handle having n children by auto-nesting splits, right?
			LOG.error("This split window GUI can only handle 2 children at moment, sorry");
			return null;
		}
		DockingWindow leftGUI = (DockingWindow) createWindowGUI(w.theWindows.get(0));
		DockingWindow rightGUI = (DockingWindow) createWindowGUI(w.theWindows.get(1));
		net.infonode.docking.SplitWindow split = new net.infonode.docking.SplitWindow(!w.getSplitHorizontal(), w.getSplitPercentage(), leftGUI, rightGUI);
		rightGUI = split;

		w.setGUI(split);
		return split;
	}

	/** Create a tab window */
	private Object createTabs(Window w) {
		int count = w.theWindows.size();
		DockingWindow[] tabchilds = new DockingWindow[count];
		for (int i = 0; i < count; i++) {
			tabchilds[i] = (DockingWindow) createWindowGUI(w.theWindows.get(i));
			// Fill on error
			if (tabchilds[i] == null) {
				tabchilds[i] = new View("Empty", null, new JLabel("Error creating this view"));
			}
		}
		
		TabWindow tabs;
		if (count < 1) {
			tabs = new TabWindow(new View("Empty", null, new JPanel()));
		}else {
			tabs = new TabWindow(tabchilds); 
		}
		tabs.setName("GOOP");
		tabs.setSelectedTab(0); // Usually first is wanted

		w.setGUI(tabs);
		return tabs;
	}

	@Override
	public Object createWindowGUI(Object stuff) {

		// Factory creating true GUI objects based on window type
		if (stuff instanceof Window) {
			// Ok more windows to go before final node
			Window aWindow = (Window) (stuff);

			// Ok maybe we 'should' subclass these... lol

			int type = aWindow.getType();
			switch (type) {
			case Window.WINDOW_ROOT:
				return createWindowRoot(aWindow);
			case Window.WINDOW_DATAVIEW:
				return createDataView(aWindow);
			case Window.WINDOW_SPLIT:
				return createSplit((SplitWindow)(aWindow));
			case Window.WINDOW_TAB:
				return createTabs(aWindow);
				
			// Right now these are direct swing gui items...
			case Window.WINDOW_NAV:
				return wrapInfoView(aWindow, new NavView(), "Navigator", "eye.png");
			case Window.WINDOW_SOURCES:
				return wrapInfoView(aWindow, new SourcesView(false), "Sources", "brick_add.png");
			case Window.WINDOW_FEATURES:
				return wrapInfoView(aWindow, new FeaturesView(false), "Features", "brick_add.png");
			case Window.WINDOW_CATALOG:
				return wrapInfoView(aWindow, new CatalogView(), "Catalog", "cart_add.png");
			default:
				LOG.error("UNKNOWN WINDOW TYPE FOR THIS MAKER!!! " + type);
				break;
			}

		} else if (stuff instanceof Component) { // Swing item, we wrap it..
			LOG.error("MASSIVE GUI ERROR!");
		} else {
			LOG.error("Ignoring unknown type in window model tree");
		}

		return null;
	}

	@Override
	public void notifySwapped(Window a, Window b) {
		
		// For data views we could just swap the gl info they point to.
		Object gui1 = a.getGUI();
		Object gui2 = b.getGUI();
		if ((gui1 != null) && (gui2 != null) && (gui1 instanceof Component)
				&& (gui2 instanceof Component)) {
			Component c1 = (Component)(gui1);
			Component c2 = (Component)(gui2);	
			
			// Parent's might be same, snag component child location from parents first
			Container p1 = c1.getParent();
			int at1 = p1.getComponentZOrder(c1);
			Container p2 = c2.getParent();
			int at2 = p2.getComponentZOrder(c2);
	
			// If parent the same, can just change z orders
			if (p1 == p2) {
				p2.setComponentZOrder(c1, at2);
				p1.setComponentZOrder(c2, at1);
				p1.revalidate();
				p1.repaint();
			}else {
				p1.remove(c1);
				p2.remove(c2);
				p1.add(c2);
				p2.add(c1);
				p2.setComponentZOrder(c1, at2);
				p1.setComponentZOrder(c2, at1);
				p1.revalidate();
				p2.revalidate();
				p1.repaint();
				p2.repaint();
			}
				
		}else {
			System.out.println("Unable to swap physical guis of windows");
		}
		
	}
	

	@Override
	public void notifyDeleted(Window wasDeleted) {
		// For data views we could just swap the gl info they point to.
		Object gui1 = wasDeleted.getGUI();
		if ((gui1 != null) && (gui1 instanceof Component)) {
			Component c1 = (Component)(gui1);
			
			// Remove from parent container if exists...
			Container p1 = c1.getParent();
			if (p1 != null) {
				p1.remove(c1);
				p1.revalidate();
				p1.repaint();
			}
		}
	}

	@Override
	public void notifyRenamed(Window w) {
	    // String currentTitle = w.getTitle();
	    //String dir = DataManager.getInstance().getRootTempDir();
	    // String newTitle = currentTitle.replaceAll("tempdir", "["+dir+"]");
	    // FIXME: shorten it to shortest path?
		if (w.getGUI() == myRootFrame) {
			myRootFrame.setTitle(w.getTitle());
	  //  myRootFrame.setTitle(s + " - " + WINDOWTITLE); //+ " " + dir)
		}
	}
}
