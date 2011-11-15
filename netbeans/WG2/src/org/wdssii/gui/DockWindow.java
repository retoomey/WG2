package org.wdssii.gui;

import net.infonode.docking.*;
import net.infonode.docking.mouse.DockingWindowActionMouseButtonListener;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.theme.*;
import net.infonode.docking.util.*;
import net.infonode.gui.laf.InfoNodeLookAndFeel;
import net.infonode.util.Direction;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.infonode.gui.laf.InfoNodeLookAndFeelTheme;
import org.wdssii.gui.views.WdssiiDockedViewFactory;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.storage.DataManager;

/**
 * The main GUI window.  This handles the main menu items as well as the
 * docking wrappers for each of our views.
 * 
 * @author Robert Toomey
 */
public class DockWindow {

    /** Default main window title string */
    final String WINDOWTITLE = "WDSSII GUI 2.0";
    
    /** Color themes for infonode */
    final InfoNodeLookAndFeelTheme[] myColorThemes;
    
    /** Infonode window style themes */
    final DockingWindowsTheme[] myWindowThemes;
    
    /** init long stuff... */
    {
        InfoNodeLookAndFeelTheme w1 =
                new InfoNodeLookAndFeelTheme("WDSSII DarkBlueGrey Theme",
                new Color(110, 120, 150),  // Control color
                new Color(0, 170, 0),      // primary control color
                new Color(80, 80, 80),      // Background color
                Color.WHITE,                // Text color
                new Color(0, 170, 0),       // selected textbackground color
                Color.WHITE,                // selected text color
                0.8);

        InfoNodeLookAndFeelTheme w2 =
                new InfoNodeLookAndFeelTheme("WDSSII OU Sooner Theme",
                new Color(153, 0, 0),
                new Color(0, 0, 255),
                Color.WHITE,
                Color.BLACK,
                Color.WHITE,               // selected textbackground color
                Color.BLACK,               // selected text color
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
        // Set InfoNode Look and Feel
        try {

            InfoNodeLookAndFeelTheme theme =
                    new InfoNodeLookAndFeelTheme("WDSSII Theme",
                    new Color(110, 120, 150),
                    new Color(0, 170, 0),
                    new Color(80, 80, 80),
                    Color.WHITE,
                    new Color(0, 170, 0),
                    Color.WHITE,
                    0.8);
            UIManager.setLookAndFeel(new InfoNodeLookAndFeel(theme));

        } catch (Exception e) {
        }
        // Docking windwos should be run in the Swing thread
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
    private ArrayList<View> views = new ArrayList<View>();
    /**
     * Contains all the static views
     */
    private ViewMap viewMap = new ViewMap();
    /** Helper factories for getting view info */
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
    private DockingWindowsTheme currentTheme = new ShapedGradientDockingTheme();

    /**
     * A dynamically created view containing an id.
     */
    public static class DynamicView extends View {

        private int id;

        /**
         * Constructor.
         *
         * @param title     the view title
         * @param icon      the view icon
         * @param component the view component
         * @param id        the view id
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
     * In this properties object the modified property values for close buttons etc. are stored. This object is cleared
     * when the theme is changed.
     */
    private RootWindowProperties properties = new RootWindowProperties();
    /**
     * Where the layouts are stored.
     */
    private byte[][] layouts = new byte[3][];
    /**
     * Menu item for enabling/disabling adding of a menu bar and a status label to all new floating windows.
     */
    // private JCheckBoxMenuItem enableMenuAndStatusLabelMenuItem = new JCheckBoxMenuItem(
    //         "Add Menu Bar and Status Label to all New Floating Windows",
    //         true);
    /**
     * The application frame
     */
    private JFrame myRootFrame = new JFrame(WINDOWTITLE);

    public DockWindow() {
    }

    public void setupRootWindow() {
        createRootWindow();
        setDefaultLayout();
        // Sync this to the selection in the menu creation...
        setTheme(new DefaultDockingTheme());
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
     * Returns a dynamic view with specified id, reusing an existing view if possible.
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

    /** Create a factory for given class shortname */
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

    private View createViewByID(String shortName) {
        View v = null;

        WdssiiDockedViewFactory f = getFactoryFor(shortName);
        if (f != null) {
            Icon i = f.getWindowIcon();
            String title = f.getWindowTitle();
            Component p = f.getNewComponent();
            v = new View(title, i, p);
            f.setDock(v);
        }
        return v;
    }

    private void addViewByID(String shortName) {
        View v = createViewByID(shortName);
        if (v != null) {
            synchronized (viewLock) {
                views.add(v);
            }
        }
    }

    private View getViewByID(String shortName) {
        WdssiiDockedViewFactory f = getFactoryFor(shortName);
        View v = null;
        if (f != null) {
            Object c = f.getDock();
            if (c instanceof View) {
                v = (View) (c);
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
        if (WorldWindView.USE_HEAVYWEIGHT){
            rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap, handler, true);
        }else{
            rootWindow = DockingUtil.createRootWindow(viewMap, handler,
                true);
        }

        // Set gradient theme. The theme properties object is the super object of our properties object, which
        // means our property value settings will override the theme values
        properties.addSuperObject(currentTheme.getRootWindowProperties());

        // Our properties object is the super object of the root window properties object, so all property values of the
        // theme and in our property object will be used by the root window
        rootWindow.getRootWindowProperties().addSuperObject(properties);

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
        addViewByID("JobsView");
        addViewByID("SourcesView");
        addViewByID("ProductsView");
        addViewByID("LayersView");
        addViewByID("ColorKeyView");
        addViewByID("TableProductView");
        addViewByID("ChartView");
        addViewByID("LLHAreaView");
        addViewByID("ProductGroupView");
        addViewByID("CacheView");

        // Add a mouse button listener that closes a window when it's clicked with the middle mouse button.
        rootWindow.addTabMouseButtonListener(DockingWindowActionMouseButtonListener.MIDDLE_BUTTON_CLOSE_LISTENER);
    }

    /**
     * Sets the default window layout.
     */
    private void setDefaultLayout() {

        // Stick all views in one tab..except those explicited referenced
        // later
        View[] v = views.toArray(new View[views.size()]);
        TabWindow all = new TabWindow(v);

        // Special windows
        View earth = getViewByID("WorldWindView");
        View products = getViewByID("ProductsView");
        View sources = getViewByID("SourcesView");
        View layers = getViewByID("LayersView");
        View nav = getViewByID("NavView");
        View chart = getViewByID("ChartView");
        View objects = getViewByID("LLHAreaView");
        View jobs = getViewByID("JobsView");
        View cache = getViewByID("CacheView");
        
        TabWindow debug = new TabWindow(new View[]{jobs, cache});
        TabWindow sourceProducts = new TabWindow(new View[]{sources, products});
        sourceProducts.setSelectedTab(0);

        SplitWindow chart3D = new SplitWindow(false, 0.3f, objects, chart);

        TabWindow stuff = new TabWindow(new DockingWindow[]{sourceProducts, chart3D, debug, all});
        rootWindow.setWindow(
                new SplitWindow(true, 0.5f,
                new SplitWindow(false, 0.7f, earth, nav), stuff));
        stuff.setSelectedTab(0);

        /* WindowBar windowBar = rootWindow.getWindowBar(Direction.DOWN);
        while (windowBar.getChildWindowCount() > 0) {
        windowBar.getChildWindow(0).close();
        }
        windowBar.addTab(layers);
         * 
         */
    }

    /**
     * Initializes the frame and shows it.
     */
    private void showFrame() {
        myRootFrame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
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
        /*new JToolBar();
        JLabel label = new JLabel("Drag New View");
        toolBar.add(label);
        new DockingWindowDragSource(label,
        new DockingWindowDraggerProvider() {
        
        public DockingWindowDragger getDragger(
        MouseEvent mouseEvent) {
        return getDynamicView(getDynamicViewId()).startDrag(rootWindow);
        }
        });
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
    private JMenu createLayoutMenu() {
    JMenu layoutMenu = new JMenu("Layout");
    
    layoutMenu.add("Default Layout").addActionListener(
    new ActionListener() {
    
    public void actionPerformed(ActionEvent e) {
    setDefaultLayout();
    }
    });
    
    layoutMenu.addSeparator();
    
    for (int i = 0; i < layouts.length; i++) {
    final int j = i;
    
    layoutMenu.add("Save Layout " + i).addActionListener(
    new ActionListener() {
    
    public void actionPerformed(ActionEvent e) {
    try {
    // Save the layout in a byte array
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(
    bos);
    rootWindow.write(out, false);
    out.close();
    layouts[j] = bos.toByteArray();
    } catch (IOException e1) {
    throw new RuntimeException(e1);
    }
    }
    });
    }
    
    layoutMenu.addSeparator();
    
    for (int i = 0; i < layouts.length; i++) {
    final int j = i;
    
    layoutMenu.add("Load Layout " + j).addActionListener(
    new ActionListener() {
    
    public void actionPerformed(ActionEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
    
    public void run() {
    if (layouts[j] != null) {
    try {
    // Load the layout from a byte array
    ObjectInputStream in = new ObjectInputStream(
    new ByteArrayInputStream(
    layouts[j]));
    rootWindow.read(in, true);
    in.close();
    } catch (IOException e1) {
    throw new RuntimeException(
    e1);
    }
    }
    }
    });
    }
    });
    }
    
    layoutMenu.addSeparator();
    
    layoutMenu.add("Show Window Layout Frame").addActionListener(
    new ActionListener() {
    
    public void actionPerformed(ActionEvent e) {
    DeveloperUtil.createWindowLayoutFrame(
    "Root Window Layout as Java Pseudo-like Code",
    rootWindow).setVisible(true);
    }
    });
    return layoutMenu;
    }
     */

    /**
     * Creates the menu where the theme can be changed.
     *
     * @return the theme menu
     */
    private JMenu createInfonodeWindowThemeMenu() {
        JMenu themesMenu = new JMenu("Themes");

        final RootWindowProperties titleBarStyleProperties = PropertiesUtil.createTitleBarStyleRootWindowProperties();

        final JCheckBoxMenuItem titleBarStyleItem = new JCheckBoxMenuItem(
                "Title Bar Style Theme");
        titleBarStyleItem.setSelected(true);
        properties.addSuperObject(titleBarStyleProperties);
        titleBarStyleItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (titleBarStyleItem.isSelected()) {
                    properties.addSuperObject(titleBarStyleProperties);
                } else {
                    properties.removeSuperObject(titleBarStyleProperties);
                }
            }
        });

        themesMenu.add(titleBarStyleItem);
        themesMenu.add(new JSeparator());

        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < myWindowThemes.length; i++) {
            final DockingWindowsTheme theme = myWindowThemes[i];

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getName());
            item.setSelected(i == 0);
            group.add(item);

            themesMenu.add(item).addActionListener(
                    new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // Clear the modified properties values
                            properties.getMap().clear(true);

                            setTheme(theme);
                        }
                    });
        }

        return themesMenu;
    }

    private void setColorTheme() {
    }

    /**
     * Creates the menu where the theme can be changed.
     *
     * @return the theme menu
     */
    private JMenu createInfonodeColorThemeMenu() {
        JMenu themesMenu = new JMenu("Color Themes");

        for (int i = 0; i < myColorThemes.length; i++) {
            final InfoNodeLookAndFeelTheme theme = myColorThemes[i];

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getName());
            item.setSelected(i == 0);

            themesMenu.add(item).addActionListener(
                    new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // Clear the modified properties values
                            //properties.getMap().clear(true);
                            try {
                                UIManager.setLookAndFeel(new InfoNodeLookAndFeel(theme));
                                SwingUtilities.updateComponentTreeUI(myRootFrame);
                            } catch (Exception e2) {
                                // what to do....
                            }
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
                        properties.getDockingWindowProperties().setCloseEnabled(true);
                    }
                });

        buttonsMenu.add("Hide Close Buttons").addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        properties.getDockingWindowProperties().setCloseEnabled(false);
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

    private JToolBar createWindowToolbar() {
        JToolBar bar = new JToolBar();
        return bar;
    }

    /**
     * Creates the menu where individual window bars can be enabled and disabled.
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
     * Creates the menu where a floating window with a dynamic view can
     * be created.
     *
     * @return the floating window menu
     */
    /*
    private JMenu createFloatingWindowMenu() {
    JMenu menu = new JMenu("Floating Window");
    menu.add(enableMenuAndStatusLabelMenuItem);
    
    JMenuItem item1 = new JMenuItem(
    "Create Floating Window with Dynamic View");
    item1.addActionListener(new ActionListener() {
    
    public void actionPerformed(ActionEvent e) {
    // Floating windows are created via the root window
    FloatingWindow fw = rootWindow.createFloatingWindow(
    new Point(50, 50), new Dimension(300, 200),
    getDynamicView(getDynamicViewId()));
    
    // Show the window
    fw.getTopLevelAncestor().setVisible(true);
    }
    });
    menu.add(item1);
    
    return menu;
    }
     */
    /**
     * Update the floating window by adding a menu bar and a status label if menu option is choosen.
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
     * Sets the docking windows theme.
     *
     * @param theme the docking windows theme
     */
    private void setTheme(DockingWindowsTheme theme) {
        properties.replaceSuperObject(currentTheme.getRootWindowProperties(), theme.getRootWindowProperties());
        currentTheme = theme;
    }

    public void Main(String Args[]) {
    }
}
