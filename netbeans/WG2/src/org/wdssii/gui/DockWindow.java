package org.wdssii.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import net.infonode.docking.*;
import net.infonode.docking.mouse.DockingWindowActionMouseButtonListener;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.theme.*;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.MixedViewHandler;
import net.infonode.docking.util.ViewMap;
import net.infonode.gui.laf.InfoNodeLookAndFeel;
import net.infonode.gui.laf.InfoNodeLookAndFeelTheme;
import net.infonode.tabbedpanel.TabAreaVisiblePolicy;
import net.infonode.tabbedpanel.TabLayoutPolicy;
import net.infonode.util.Direction;
import org.wdssii.gui.views.WdssiiDockedViewFactory;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.storage.DataManager;

/**
 * The main GUI window. This handles the main menu items as well as the docking
 * wrappers for each of our views.
 *
 * @author Robert Toomey
 */
public class DockWindow {

	/**
	 * Default main window title string
	 */
	final static String WINDOWTITLE = "WDSSII GUI 2.0";
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

	/**
	 * init long stuff...
	 */
	static {
		InfoNodeLookAndFeelTheme w1 =
			new InfoNodeLookAndFeelTheme("WDSSII DarkBlueGrey Theme",
			new Color(110, 120, 150), // Control color
			new Color(0, 170, 0), // primary control color
			new Color(80, 80, 80), // Background color
			Color.WHITE, // Text color
			new Color(0, 170, 0), // selected textbackground color
			Color.WHITE, // selected text color
			0.8);

		InfoNodeLookAndFeelTheme w2 =
			new InfoNodeLookAndFeelTheme("WDSSII OU Sooner Theme",
			new Color(153, 0, 0),
			new Color(0, 0, 255),
			Color.WHITE,
			Color.BLACK,
			Color.WHITE, // selected textbackground color
			Color.BLACK, // selected text color
			0.8);
		myColorThemes = new InfoNodeLookAndFeelTheme[]{w1, w2};

		myWindowThemes = new DockingWindowsTheme[]{
			new DefaultDockingTheme(),
			new LookAndFeelDockingTheme(),
			new BlueHighlightDockingTheme(),
			new SlimFlatDockingTheme(), new GradientDockingTheme(),
			new ShapedGradientDockingTheme(),
			new SoftBlueIceDockingTheme(),
			new ClassicDockingTheme()};
	}

	public static void startWindows() {
		// This actually sets the java UI, so we do this first
		setColorTheme(myCurrentColorThemeIndex);
		// Docking windows should be run in the Swing thread
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				DockWindow d = new DockWindow();
				d.setupRootWindow();
			}
		});
	}
	/**
	 * The one and only root window
	 */
	private RootWindow rootWindow;
	/**
	 * An array of the static views
	 */
	private final Object viewLock = new Object();
	private ArrayList<DockingWindow> views = new ArrayList<DockingWindow>();
	/**
	 * Contains all the static views
	 */
	private ViewMap viewMap = new ViewMap();
	/**
	 * Helper factories for getting view info
	 */
	private Map<String, WdssiiDockedViewFactory> myFactory = new TreeMap<String, WdssiiDockedViewFactory>();
	/**
	 * The view menu items
	 */
	//  private JMenuItem[] viewItems = new JMenuItem[views.length];
	/**
	 * Contains the dynamic views that has been added to the root window
	 */
	private HashMap dynamicViews = new HashMap();
	/**
	 * The currently applied docking windows theme
	 */
	private static DockingWindowsTheme currentTheme = null;

	/**
	 * A dynamically created view containing an id.
	 */
	public static class DynamicView extends View {

		private int id;

		/**
		 * Constructor.
		 *
		 * @param title the view title
		 * @param icon the view icon
		 * @param component the view component
		 * @param id the view id
		 */
		DynamicView(String title, Icon icon, Component component, int id) {
			super(title, icon, component);
			this.id = id;
		}

		/**
		 * Returns the view id.
		 *
		 * @return the view id
		 */
		public int getId() {
			return id;
		}
	}
	/**
	 * In this properties object the modified property values for close buttons
	 * etc. are stored. This object is cleared when the theme is changed.
	 */
//	private static RootWindowProperties properties = new RootWindowProperties();
	private static RootWindowProperties properties = new RootWindowProperties();

	// Seeing a strange intermittent startup bug with properties, checking to see if
	// it's a sync issue...
	private static final Object propSync = new Object();

//		PropertiesUtil.createTitleBarStyleRootWindowProperties();
//	private static RootWindowProperties titleBarStyleProperties = PropertiesUtil.createTitleBarStyleRootWindowProperties();
	/**
	 * Where the layouts are stored.
	 */
	private byte[][] layouts = new byte[3][];
	/**
	 * Menu item for enabling/disabling adding of a menu bar and a status label
	 * to all new floating windows.
	 */
	// private JCheckBoxMenuItem enableMenuAndStatusLabelMenuItem = new JCheckBoxMenuItem(
	//         "Add Menu Bar and Status Label to all New Floating Windows",
	//         true);
	/**
	 * The application frame
	 */
	private static JFrame myRootFrame = new JFrame(WINDOWTITLE);

	public DockWindow() {
	}

	public void setupRootWindow() {
		setTheme(myCurrentThemeIndex);
		setUpGlobalProperties();
		createRootWindow();
		setDefaultLayout();
		showFrame();
	}

	/**
	 * Creates a view component containing the specified text.
	 *
	 * @param text the text
	 * @return the view component
	 */
	private static JComponent createViewComponent(String text) {
		return new JScrollPane(new JTextArea());
	}

	/**
	 * Returns a dynamic view with specified id, reusing an existing view if
	 * possible.
	 *
	 * @param id the dynamic view id
	 * @return the dynamic view
	 */
	private View getDynamicView(int id) {
		View view = (View) dynamicViews.get(new Integer(id));

		if (view == null) {
			view = new DynamicView("Dynamic View " + id, null,
				createViewComponent("Dynamic View " + id), id);
		}

		return view;
	}

	/**
	 * Returns the next available dynamic view id.
	 *
	 * @return the next available dynamic view id
	 */
	private int getDynamicViewId() {
		int id = 0;

		while (dynamicViews.containsKey(new Integer(id))) {
			id++;
		}

		return id;
	}

	/**
	 * Create a factory for given class shortname
	 */
	private WdssiiDockedViewFactory getFactoryFor(String shortName) {
		WdssiiDockedViewFactory f = null;
		f = myFactory.get(shortName);
		if (f == null) {
			String className = "org.wdssii.gui.views." + shortName + "$Factory";
			Class<?> aClass = null;
			try {
				aClass = Class.forName(className);
				Object o = aClass.getConstructor().newInstance();
				if (o instanceof WdssiiDockedViewFactory) {
					f = (WdssiiDockedViewFactory) (o);
					myFactory.put(shortName, f);
				}
			} catch (Exception e) {
				f = null;
			}
		}
		return f;
	}

	private DockingWindow createViewByID(String shortName) {
		DockingWindow v = null;

		WdssiiDockedViewFactory f = getFactoryFor(shortName);
		if (f != null) {
			v = f.getNewDockingWindow();
			f.setDock(v);
		}
		return v;
	}

	private void addViewByID(String shortName) {
		DockingWindow v = createViewByID(shortName);
		if (v != null) {
			synchronized (viewLock) {
				views.add(v);
			}
		}
	}

	private DockingWindow getViewByID(String shortName) {
		WdssiiDockedViewFactory f = getFactoryFor(shortName);
		DockingWindow v = null;
		if (f != null) {
			Object c = f.getDock();
			if (c instanceof DockingWindow) {
				v = (DockingWindow) (c);
			}
		}
		return v;
	}

	private void addMenuByID(JMenu root, String shortName) {
		WdssiiDockedViewFactory f = getFactoryFor(shortName);
		if (f != null) {
			Icon i = f.getWindowIcon();
			String title = f.getWindowTitle();
			JMenuItem m = new JMenuItem(title, i);
			root.add(m);
		}
	}

	/**
	 * Create a root window for internal docking within a view
	 */
	public static RootWindow createARootWindow() {

		RootWindowProperties override = new RootWindowProperties();
		RootWindow aWindow;

		ViewMap someViewMap = new ViewMap();
		// FIXME: don't get this yet really....
		// The mixed view map makes it easy to mix static and dynamic views inside the same root window
		MixedViewHandler handler = new MixedViewHandler(someViewMap,
			new ViewSerializer() {

				@Override
				public void writeView(View view,
					ObjectOutputStream out) throws IOException {
					// out.writeInt(((DynamicView) view).getId());
				}

				@Override
				public View readView(ObjectInputStream in)
					throws IOException {
					return null;
					//return getDynamicView(in.readInt());
				}
			});

		aWindow = DockingUtil.createRootWindow(someViewMap, handler,
			true);

		// Default to the properties global
		synchronized(propSync){
		aWindow.getRootWindowProperties().addSuperObject(properties);
		}

		return aWindow;
	}

	private void setUpGlobalProperties() {

		// Make properties use the theme settings first...
		synchronized(propSync){
		properties.addSuperObject(currentTheme.getRootWindowProperties());

		/*
		// Try to replace close for properties...
		ButtonFactory b = new ButtonFactory() {

			class myButton extends JButton {

				@Override
				protected void paintComponent(Graphics g) {
					// super.paintComponent(g);

					Dimension originalSize = super.getPreferredSize();
					int gap = (int) (originalSize.height * 0.2);
					int x = originalSize.width + gap;
					int y = gap;
					int diameter = originalSize.height - (gap * 2);

					g.setColor(Color.RED);
					g.fillOval(x, y, diameter, diameter);
				}
			}

			@Override
			public AbstractButton createButton(Object object) {
				return new myButton();
			}
		};
		 *
		 */

		// These are the 'tab windows' which we hide normally...all the buttons are also in the view title bars, which we show
		//properties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getCloseButtonProperties().setFactory(b);
		//properties.getTabWindowProperties().getCloseButtonProperties().setFactory(b);

		// We use the 'view' buttons only in the title bar...
		//final ViewTitleBarStateProperties v = properties.getViewProperties().getViewTitleBarProperties().getNormalProperties();
		//v.getCloseButtonProperties().setFactory(b);
		
		// Lots of default properties to show the 'title bar' in the view
		setupTitleBarStyleProperties(properties);
		}

	}

	/**
	 * Creates the root window and the views.
	 */
	private void createRootWindow() {

		// FIXME: don't get this yet really....
		// The mixed view map makes it easy to mix static and dynamic views inside the same root window
		MixedViewHandler handler = new MixedViewHandler(viewMap,
			new ViewSerializer() {

				@Override
				public void writeView(View view,
					ObjectOutputStream out) throws IOException {
					out.writeInt(((DynamicView) view).getId());
				}

				@Override
				public View readView(ObjectInputStream in)
					throws IOException {
					return getDynamicView(in.readInt());
				}
			});
		if (WorldWindView.USE_HEAVYWEIGHT) {
			rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap, handler, true);
		} else {
			rootWindow = DockingUtil.createRootWindow(viewMap, handler,
				true);
		}

		synchronized(propSync){
		rootWindow.getRootWindowProperties().addSuperObject(properties);
		}

		// Enable the bottom window bar
		rootWindow.getWindowBar(Direction.DOWN).setEnabled(true);

		// Add a listener which shows dialogs when a window is closing or closed.
		rootWindow.addListener(new DockingWindowAdapter() {

			@Override
			public void windowAdded(DockingWindow addedToWindow,
				DockingWindow addedWindow) {
				// updateViews(addedWindow, true);

				// If the added window is a floating window, then update it
				if (addedWindow instanceof FloatingWindow) {
					updateFloatingWindow((FloatingWindow) addedWindow);
				}
			}

			@Override
			public void windowRemoved(DockingWindow removedFromWindow,
				DockingWindow removedWindow) {
				// updateViews(removedWindow, false);
				// FIXME: we could delete the swing stuff, etc...here
			}

			@Override
			public void windowClosing(DockingWindow window)
				throws OperationAbortedException {
				// Confirm close operation
				// if (JOptionPane.showConfirmDialog(frame,
				//         "Really close window '" + window + "'?") != JOptionPane.YES_OPTION) {
				//     throw new OperationAbortedException(
				//             "Window close was aborted!");
				// }
			}

			@Override
			public void windowDocking(DockingWindow window)
				throws OperationAbortedException {
				// Confirm dock operation
				// if (JOptionPane.showConfirmDialog(frame,
				//         "Really dock window '" + window + "'?") != JOptionPane.YES_OPTION) {
				//     throw new OperationAbortedException(
				//             "Window dock was aborted!");
				// }
			}

			@Override
			public void windowUndocking(DockingWindow window)
				throws OperationAbortedException {
				// Confirm undock operation 
				// if (JOptionPane.showConfirmDialog(frame,
				//         "Really undock window '" + window + "'?") != JOptionPane.YES_OPTION) {
				//     throw new OperationAbortedException(
				//             "Window undock was aborted!");
				// }
			}
		});

		// Create the views
		Icon i = null;
		View v;

		// FIXME: Should hunt by reflection, auto handle this...
		// For the moment creating ALL views...
		addViewByID("WorldWindView");
		addViewByID("NavView");

		addViewByID("DebugView");
		addViewByID("CatalogView");
		addViewByID("SourcesView");

		addViewByID("ChartView");
		addViewByID("FeaturesView");

		// Add a mouse button listener that closes a window when it's clicked with the middle mouse button.
		rootWindow.addTabMouseButtonListener(DockingWindowActionMouseButtonListener.MIDDLE_BUTTON_CLOSE_LISTENER);
	}

	/**
	 * Sets the default window layout.
	 */
	private void setDefaultLayout() {

		// Stick all views in one tab..except those explicited referenced
		// later
		DockingWindow[] v = views.toArray(new DockingWindow[views.size()]);
		TabWindow all = new TabWindow(v);

		// Special windows
		DockingWindow earth = getViewByID("WorldWindView");
		DockingWindow catalog = getViewByID("CatalogView");
		DockingWindow sources = getViewByID("SourcesView");
		DockingWindow nav = getViewByID("NavView");
		DockingWindow chart = getViewByID("ChartView");
		DockingWindow debug = getViewByID("DebugView");
		DockingWindow features = getViewByID("FeaturesView");

		// TabWindow debug = new TabWindow(new DockingWindow[]{jobs, cache});
		TabWindow sourceProducts = new TabWindow(new DockingWindow[]{sources, features, catalog});
		sourceProducts.setSelectedTab(0);

		// SplitWindow chart3D = new SplitWindow(false, 0.3f, objects, chart);

		TabWindow stuff = new TabWindow(new DockingWindow[]{sourceProducts, chart});
		rootWindow.setWindow(
			new SplitWindow(true, 0.5f,
			new SplitWindow(false, 0.7f, earth, nav), stuff));
		stuff.setSelectedTab(0);

		/*
		 * WindowBar windowBar = rootWindow.getWindowBar(Direction.DOWN); while
		 * (windowBar.getChildWindowCount() > 0) {
		 * windowBar.getChildWindow(0).close(); } windowBar.addTab(layers);
		 *
		 */
	}

	/**
	 * Initializes the frame and shows it.
	 */
	private void showFrame() {
		//myRootFrame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
		myRootFrame.getContentPane().add(rootWindow, BorderLayout.CENTER);
		myRootFrame.setJMenuBar(createMenuBar());
		myRootFrame.setSize(900, 700);
		myRootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// String currentTitle = w.getTitle();
		String dir = DataManager.getInstance().getRootTempDir();
		// String newTitle = currentTitle.replaceAll("tempdir", "["+dir+"]");
		String newTitle = WINDOWTITLE + " " + dir;
		myRootFrame.setTitle(newTitle);

		myRootFrame.setVisible(true);
	}

	/**
	 * Creates the frame tool bar.
	 *
	 * @return the frame tool bar
	 */
	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();
		// Assumes called after factories created...
		// FIXME: order/sort the menus...
		for (Map.Entry<String, WdssiiDockedViewFactory> e : myFactory.entrySet()) {
			final WdssiiDockedViewFactory f = e.getValue();
			JButton newOne = new JButton(f.getMenuIcon());
			newOne.setToolTipText(f.getMenuTitle());
			newOne.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							Object o = f.getDock();
							if (o != null) {

								// What to do if view exists.....
								if (o instanceof View) {
									View v = (View) (o);
									// Ensure the view is shown in the root window
									DockingUtil.addWindow(v,
										rootWindow);
									//Transfer focus to the view
									v.restoreFocus();
								}
							} else {
								// FIXME: later on, create the view.
								// Eventually we want to only make views that
								// are being used or called up...
							}

						}
					});
				}
			});
			toolBar.add(newOne);
		}
		/*
		 * new JToolBar(); JLabel label = new JLabel("Drag New View");
		 * toolBar.add(label); new DockingWindowDragSource(label, new
		 * DockingWindowDraggerProvider() {
		 *
		 * public DockingWindowDragger getDragger( MouseEvent mouseEvent) {
		 * return getDynamicView(getDynamicViewId()).startDrag(rootWindow); }
		 * });
		 */
		return toolBar;
	}

	/**
	 * Creates the frame menu bar.
	 *
	 * FIXME: Could generate this from xml...
	 *
	 * @return the menu bar
	 */
	private JMenuBar createMenuBar() {

		JMenuBar menu = new JMenuBar();
		menu.add(createFileMenu());
		// menu.add(createFocusViewMenu());

		// menu.add(createPropertiesMenu());
		// menu.add(createWindowBarsMenu());
		menu.add(createWindowMenu());
		// menu.add(createFloatingWindowMenu());
		return menu;
	}

	/**
	 * Creates the menu where layout can be saved/loaded and a frame shown with
	 * Java pseudo-like code over the current layout in the root window.
	 *
	 * @return the layout menu
	 */
	/*
	 * private JMenu createLayoutMenu() { JMenu layoutMenu = new
	 * JMenu("Layout");
	 *
	 * layoutMenu.add("Default Layout").addActionListener( new ActionListener()
	 * {
	 *
	 * public void actionPerformed(ActionEvent e) { setDefaultLayout(); } });
	 *
	 * layoutMenu.addSeparator();
	 *
	 * for (int i = 0; i < layouts.length; i++) { final int j = i;
	 *
	 * layoutMenu.add("Save Layout " + i).addActionListener( new
	 * ActionListener() {
	 *
	 * public void actionPerformed(ActionEvent e) { try { // Save the layout in
	 * a byte array ByteArrayOutputStream bos = new ByteArrayOutputStream();
	 * ObjectOutputStream out = new ObjectOutputStream( bos);
	 * rootWindow.write(out, false); out.close(); layouts[j] =
	 * bos.toByteArray(); } catch (IOException e1) { throw new
	 * RuntimeException(e1); } } }); }
	 *
	 * layoutMenu.addSeparator();
	 *
	 * for (int i = 0; i < layouts.length; i++) { final int j = i;
	 *
	 * layoutMenu.add("Load Layout " + j).addActionListener( new
	 * ActionListener() {
	 *
	 * public void actionPerformed(ActionEvent e) {
	 * SwingUtilities.invokeLater(new Runnable() {
	 *
	 * public void run() { if (layouts[j] != null) { try { // Load the layout
	 * from a byte array ObjectInputStream in = new ObjectInputStream( new
	 * ByteArrayInputStream( layouts[j])); rootWindow.read(in, true);
	 * in.close(); } catch (IOException e1) { throw new RuntimeException( e1); }
	 * } } }); } }); }
	 *
	 * layoutMenu.addSeparator();
	 *
	 * layoutMenu.add("Show Window Layout Frame").addActionListener( new
	 * ActionListener() {
	 *
	 * public void actionPerformed(ActionEvent e) {
	 * DeveloperUtil.createWindowLayoutFrame( "Root Window Layout as Java
	 * Pseudo-like Code", rootWindow).setVisible(true); } }); return layoutMenu;
	 * }
	 */
	private static void setTitleBarMode(boolean flag) {
		if (flag) {
//			properties.addSuperObject(titleBarStyleProperties);
		} else {
//			properties.removeSuperObject(titleBarStyleProperties);
		}
	}

	/**
	 * Creates the menu where the theme can be changed.
	 *
	 * @return the theme menu
	 */
	private static JMenu createInfonodeWindowThemeMenu() {
		JMenu themesMenu = new JMenu("Themes");

		ButtonGroup group = new ButtonGroup();

		for (int i = 0; i < myWindowThemes.length; i++) {
			final DockingWindowsTheme theme = myWindowThemes[i];
			final int index = i;

			JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getName());
			item.setSelected(i == myCurrentThemeIndex);
			group.add(item);

			themesMenu.add(item).addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setTheme(index);
					}
				});
		}

		return themesMenu;
	}

	/**
	 * Creates the menu where the theme can be changed.
	 *
	 * @return the theme menu
	 */
	private static JMenu createInfonodeColorThemeMenu() {
		JMenu themesMenu = new JMenu("Color Themes");

		ButtonGroup group = new ButtonGroup();

		for (int i = 0; i < myColorThemes.length; i++) {
			final InfoNodeLookAndFeelTheme theme = myColorThemes[i];
			final int index = i;

			JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getName());
			item.setSelected(i == myCurrentColorThemeIndex);
			group.add(item);

			themesMenu.add(item).addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setColorTheme(index);
					}
				});
		}

		return themesMenu;
	}

	/**
	 * Creates the menu where different property values can be modified.
	 *
	 * @return the properties menu
	 */
	private JMenu createPropertiesMenu() {
		JMenu buttonsMenu = new JMenu("Properties");

		buttonsMenu.add("Enable Close").addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
		synchronized(propSync){
					properties.getDockingWindowProperties().setCloseEnabled(true);
		}
				}
			});

		buttonsMenu.add("Hide Close Buttons").addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
		synchronized(propSync){
					properties.getDockingWindowProperties().setCloseEnabled(false);
		}
				}
			});

		buttonsMenu.add("Freeze Layout").addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					freezeLayout(true);
				}
			});

		buttonsMenu.add("Unfreeze Layout").addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					freezeLayout(false);
				}
			});

		return buttonsMenu;
	}

	/**
	 * Freezes or unfreezes the window layout and window operations.
	 *
	 * @param freeze true for freeze, otherwise false
	 */
	private void freezeLayout(boolean freeze) {
		// Freeze window operations
		synchronized(propSync){
		properties.getDockingWindowProperties().setDragEnabled(!freeze);
		properties.getDockingWindowProperties().setCloseEnabled(!freeze);
		properties.getDockingWindowProperties().setMinimizeEnabled(
			!freeze);
		properties.getDockingWindowProperties().setRestoreEnabled(
			!freeze);
		properties.getDockingWindowProperties().setMaximizeEnabled(
			!freeze);
		properties.getDockingWindowProperties().setUndockEnabled(
			!freeze);
		properties.getDockingWindowProperties().setDockEnabled(!freeze);

		// Freeze tab reordering inside tabbed panel
		properties.getTabWindowProperties().getTabbedPanelProperties().setTabReorderEnabled(!freeze);
		}
	}

	private JToolBar createWindowToolbar() {
		JToolBar bar = new JToolBar();
		return bar;
	}

	/**
	 * Creates the menu where individual window bars can be enabled and
	 * disabled.
	 *
	 * @return the window bar menu
	 */
	private JMenu createWindowBarsMenu() {
		JMenu barsMenu = new JMenu("Side Dock Bars");

		for (int i = 0; i < 4; i++) {
			final Direction d = Direction.getDirections()[i];
			JCheckBoxMenuItem item = new JCheckBoxMenuItem("Toggle "
				+ d);
			item.setSelected(d == Direction.DOWN);
			barsMenu.add(item).addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// Enable/disable the window bar
					rootWindow.getWindowBar(d).setEnabled(
						!rootWindow.getWindowBar(d).isEnabled());
				}
			});
		}

		return barsMenu;
	}

	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.setMnemonic('F');

		JMenuItem eMenuItem = new JMenuItem("Exit");
		eMenuItem.setMnemonic(KeyEvent.VK_C);
		eMenuItem.setToolTipText("Exit application");
		eMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});
		menu.add(eMenuItem);

		// menu.addSeparator();
		return menu;
	}

	/**
	 * Creates the menu where not shown views can be shown.
	 *
	 * @return the view menu
	 */
	private JMenu createWindowMenu() {

		JMenu menu = new JMenu("Window");
		menu.setMnemonic('W');

		// Assumes called after factories created...
		// FIXME: order/sort the menus...
		for (Map.Entry<String, WdssiiDockedViewFactory> e : myFactory.entrySet()) {
			final WdssiiDockedViewFactory f = e.getValue();
			JMenuItem newOne = new JMenuItem(f.getMenuTitle(), f.getMenuIcon());

			newOne.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							Object o = f.getDock();
							if (o != null) {

								// What to do if view exists.....
								if (o instanceof View) {
									View v = (View) (o);
									// Ensure the view is shown in the root window
									DockingUtil.addWindow(v,
										rootWindow);
									//Transfer focus to the view
									v.restoreFocus();
								}
							} else {
								// FIXME: later on, create the view.
								// Eventually we want to only make views that
								// are being used or called up...
							}

						}
					});
				}
			});
			menu.add(newOne);
		}

		menu.addSeparator();
		// Theme controls how windows look
		menu.add(createInfonodeWindowThemeMenu());
		menu.add(createInfonodeColorThemeMenu());
		menu.add(createWindowBarsMenu());
		menu.add(createPropertiesMenu());

		return menu;
	}

	/**
	 * Creates the menu where a floating window with a dynamic view can be
	 * created.
	 *
	 * @return the floating window menu
	 */
	/*
	 * private JMenu createFloatingWindowMenu() { JMenu menu = new
	 * JMenu("Floating Window"); menu.add(enableMenuAndStatusLabelMenuItem);
	 *
	 * JMenuItem item1 = new JMenuItem( "Create Floating Window with Dynamic
	 * View"); item1.addActionListener(new ActionListener() {
	 *
	 * public void actionPerformed(ActionEvent e) { // Floating windows are
	 * created via the root window FloatingWindow fw =
	 * rootWindow.createFloatingWindow( new Point(50, 50), new Dimension(300,
	 * 200), getDynamicView(getDynamicViewId()));
	 *
	 * // Show the window fw.getTopLevelAncestor().setVisible(true); } });
	 * menu.add(item1);
	 *
	 * return menu; }
	 */
	/**
	 * Update the floating window by adding a menu bar and a status label if
	 * menu option is choosen.
	 *
	 * @param fw the floating window
	 */
	private void updateFloatingWindow(FloatingWindow fw) {
		// Only update with if menu is selected
		// if (enableMenuAndStatusLabelMenuItem.isSelected()) {
		// Create a dummy menu bar as example
		// JMenuBar bar = new JMenuBar();
		// bar.add(new JMenu("Menu 1")).add(
		//        new JMenuItem("Menu 1 Item 1"));
		// bar.add(new JMenu("Menu 2")).add(
		//         new JMenuItem("Menu 2 Item 1"));
		// Set it in the root pane of the floating window
		//fw.getRootPane().setJMenuBar(bar);
		// Create and add a status label
		// JLabel statusLabel = new JLabel("I'm a status label!");
		// Add it as the SOUTH component to the root pane's content pane. Note that the actual floating
		// window is placed in the CENTER position and must not be removed.
		// fw.getRootPane().getContentPane().add(statusLabel,
		//         BorderLayout.SOUTH);
		//  }
	}

	/**
	 * Set the Color theme.
	 * This sets the actual java look and feel, vs the infonode 'theme' which
	 * is setting for how infonode draws itself.
	 * 
	 * @param index 
	 */
	private static void setColorTheme(int index) {
		try {
			final InfoNodeLookAndFeelTheme theme = myColorThemes[index];
			UIManager.setLookAndFeel(new InfoNodeLookAndFeel(theme));
			myCurrentColorThemeIndex = index;
			SwingUtilities.updateComponentTreeUI(myRootFrame);
		} catch (Exception e2) {
			// what to do....
		}

	}

	/** Set the InfoNode theme type.  This is how the InfoNodeLookAndFeel draws itself */
	private static void setTheme(int index) {
		try {
			DockingWindowsTheme theme = myWindowThemes[index];
			if (currentTheme != null) {
				synchronized(propSync){
				properties.replaceSuperObject(currentTheme.getRootWindowProperties(), theme.getRootWindowProperties());
				}
			}
			currentTheme = theme;
			myCurrentThemeIndex = index;
		} catch (Exception e) {
			// what to do....
		}

	}

	public void Main(String Args[]) {
	}

	/** Copied from infonode PropertyUtil, allows me to have super fine control of appearance */
	private static void setupTitleBarStyleProperties(RootWindowProperties titleBarStyleProperties) {
		titleBarStyleProperties.getViewProperties().getViewTitleBarProperties().setVisible(true);
		titleBarStyleProperties.getTabWindowProperties().getTabbedPanelProperties().setTabAreaOrientation(Direction.DOWN).setTabLayoutPolicy(TabLayoutPolicy.SCROLLING);
		titleBarStyleProperties.getTabWindowProperties().getTabbedPanelProperties().getTabAreaProperties().setTabAreaVisiblePolicy(TabAreaVisiblePolicy.MORE_THAN_ONE_TAB);

		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getMinimizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getRestoreButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getCloseButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getUndockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getDockButtonProperties().setVisible(false);

		titleBarStyleProperties.getTabWindowProperties().getCloseButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getMaximizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getMinimizeButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getUndockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getDockButtonProperties().setVisible(false);
		titleBarStyleProperties.getTabWindowProperties().getRestoreButtonProperties().setVisible(false);

		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getMinimizeButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getRestoreButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getCloseButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getUndockButtonProperties().setVisible(true);
		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabProperties().getHighlightedButtonProperties().getDockButtonProperties().setVisible(true);

		titleBarStyleProperties.getWindowBarProperties().getTabWindowProperties().getTabbedPanelProperties().setTabLayoutPolicy(TabLayoutPolicy.SCROLLING);
	}
}
