package org.wdssii.gui.views.infonode;

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JideSplitButton;
import org.wdssii.gui.views.WdssiiDockedViewFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.infonode.docking.*;
import net.infonode.docking.mouse.DockingWindowActionMouseButtonListener;
import net.infonode.docking.properties.DockingWindowProperties;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.properties.ViewProperties;
import net.infonode.docking.properties.ViewTitleBarProperties;
import net.infonode.docking.theme.*;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.MixedViewHandler;
import net.infonode.docking.util.ViewMap;
import net.infonode.docking.util.WindowMenuUtil;
import net.infonode.gui.laf.InfoNodeLookAndFeel;
import net.infonode.gui.laf.InfoNodeLookAndFeelTheme;
import net.infonode.tabbedpanel.TabAreaVisiblePolicy;
import net.infonode.tabbedpanel.TabLayoutPolicy;
import net.infonode.util.Direction;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.Application;
import org.wdssii.gui.commands.ExitCommand;
import org.wdssii.gui.commands.NewCommand;
import org.wdssii.gui.commands.OpenCommand;
import org.wdssii.gui.commands.SaveCommand;
import org.wdssii.gui.views.ViewManager.RootContainer;
import org.wdssii.gui.views.ViewManager.ViewMaker;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;
import org.wdssii.gui.views.WdssiiView;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.worldwind.WorldWindDataView;

/**
 * The main window layout using infonode
 *
 * @author Robert Toomey
 */
public class InfonodeViews implements ViewMaker {

    private final static Logger LOG = LoggerFactory.getLogger(InfonodeViews.class);
    /**
     * Default main window title string
     */
    //public final static String WINDOWTITLE = "WDSSII GUI 2.0";
    public final static String WINDOWTITLE =
            Application.NAME + " " + Application.MAJOR_VERSION + "."
            + Application.MINOR_VERSION;
    public final static String UNTITLED = "Untitled";
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

    @Override
    public org.wdssii.xml.views.RootWindow getLayoutXML() {
        org.wdssii.xml.views.DockWindow rr = getChildWindows(rootWindow, 0);
        return (org.wdssii.xml.views.RootWindow) (rr);
    }

    @Override
    public void setLayoutXML(org.wdssii.xml.views.RootWindow r) {
        org.wdssii.xml.views.DockWindow oldLayout = getChildWindows(rootWindow, 0);
        try {
            // Have to close all the old floating windows first, quirk with
            // infonode
            closeAllFloatingWindows();
            // Build the window using the xml
            buildWindow(r);
        } catch (Exception e) {
            closeAllFloatingWindows();
            buildWindow(oldLayout);
        }
    }

    @Override
    public void setNewLayout() {
        setDefaultLayout();
        // isn't the main window bound part of the default layout?
        // Rectangle r = getScreenBounds();
        // setMainWindowBounds(r);
    }

    @Override
    public void setConfigPath(URL aURL) {
        String s;
        if (aURL == null) {
            s = UNTITLED;
        } else {
            s = aURL.toString();
        }
        setMainTitle(s);
    }

    private void setMainTitle(String s) {
        // String currentTitle = w.getTitle();
        //String dir = DataManager.getInstance().getRootTempDir();
        // String newTitle = currentTitle.replaceAll("tempdir", "["+dir+"]");
        // FIXME: shorten it to shortest path?
        myRootFrame.setTitle(s + " - " + WINDOWTITLE); //+ " " + dir)
    }

    public static class IView extends View {

        public WdssiiView wv;

        public IView(WdssiiView w, String paramString, Icon paramIcon, Component paramComponent) {
            super(paramString, paramIcon, paramComponent);
            wv = w;
        }

        public String getWdssiiKey() {
            return wv.getKey();
        }
    }

    /**
     * Initial the start up window configuration
     */
    @Override
    public void init() {
        // This actually sets the java UI, so we do this first
        setColorTheme(myCurrentColorThemeIndex);
        // Docking windows should be run in the Swing thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // DockWindow d = new DockWindow();
                setupRootWindow();
                // setupTestWindow();
            }
        });
    }

    public void setFullScreen(DisplayMode dm, JFrame win) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice vc = ge.getDefaultScreenDevice();
        //Remove the Title Bar, Maximization , Minimization Button...
        win.setUndecorated(true);

        // Can not be resized
        win.setResizable(false);

        //Make the win(JFrame) Full Screen
        vc.setFullScreenWindow(win);

        //check low-level display changes are supported for this graphics device.
        if (dm != null && vc.isDisplayChangeSupported()) {
            try {
                vc.setDisplayMode(dm);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        }
    }

    public static void checkOutDisplays() {
        // Test if each monitor will support my app's window
        // Iterate through each monitor and see what size each is
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsDevice def = ge.getDefaultScreenDevice();
        //Dimension mySize = new Dimension(myWidth, myHeight);
        //Dimension maxSize = new Dimension(minRequiredWidth, minRequiredHeight);
        for (int i = 0; i < gs.length; i++) {
            DisplayMode dm = gs[i].getDisplayMode();
            if (gs[i] == def) {
                LOG.debug("*************************** PDISPLAY FOUND " + dm.getWidth() + ", " + dm.getHeight());
            } else {
                LOG.debug("*************************** DISPLAY FOUND " + dm.getWidth() + ", " + dm.getHeight());
            }

        }
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
     * Helper factories for getting view info FIXME: Move this to ViewManager..
     */
    private Map<String, WdssiiDockedViewFactory> myFactory = new TreeMap<String, WdssiiDockedViewFactory>();
    /**
     * The currently applied docking windows theme
     */
    private static DockingWindowsTheme currentTheme = null;
    /**
     * In this properties object the modified property values for close buttons
     * etc. are stored. This object is cleared when the theme is changed.
     */
    private static RootWindowProperties properties = new RootWindowProperties();
    // Seeing a strange intermittent startup bug with properties, checking to see if
    // it's a sync issue...
    private static final Object propSync = new Object();
    /**
     * The application frame
     */
    private static JFrame myRootFrame;

    public InfonodeViews() {
    }

    /**
     * Get the rectangle for the screen boundary of the default display
     */
    public Rectangle getScreenBounds() {
        // FIXME: Eventually going to have to add multiple screen layout support
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        //GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsDevice def = ge.getDefaultScreenDevice();
        DisplayMode dm = def.getDisplayMode();
        return new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
    }

    public void setupRootWindow() {

        // Do we make these savable?
        setTheme(myCurrentThemeIndex);
        setUpGlobalProperties();

        // Create a root window, then a frame
        createRootWindow();
        createNewFrame();

        // Set the default layout
        setDefaultLayout();
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

    public static void wrapWdssiiViewWithGUI2(WdssiiView wv) {
        if (wv.container == null) {
            View v = new IView(wv, wv.getTitle(), wv.getIcon(), wv.getComponent());

            // Add wanting title bar controls from WdssiiView to the real view list
            List<Object> list = wv.getWindowTitleItems();
            if (list != null) {
                List<Object> realList = v.getCustomTitleBarComponents();
                for (Object o : list) {
                    realList.add(o);
                }
            }
            wv.container = v; // Keep a reference to the GUI
        }
    }

    /**
     * Wrap a WdssiiView with the GUI item used by our windowing system, we
     * store information to the GUI item into the WdssiiView. In our case, we
     * wrap with an infonode dockable window
     */
    @Override
    public void wrapWdssiiViewWithGUI(WdssiiView wv) {
        if (wv.container == null) {
            View v = new IView(wv, wv.getTitle(), wv.getIcon(), wv.getComponent());

            // Add wanting title bar controls from WdssiiView to the real view list
            List<Object> list = wv.getWindowTitleItems();
            if (list != null) {
                List<Object> realList = v.getCustomTitleBarComponents();
                for (Object o : list) {
                    realList.add(o);
                }
            }
            wv.container = v; // Keep a reference to the GUI
        }
    }

    private View getViewByID(String shortName) {
        WdssiiDockedViewFactory f = getFactoryFor(shortName);
        View v = null;
        if (f != null) {
            WdssiiView wv = f.getWdssiiView();
            wv.setKey(shortName);
            wrapWdssiiViewWithGUI(wv);
            v = (View) wv.container;
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
     * A Rootwindow with some extra stuff
     */
    public static class WRootWindow extends RootWindow implements RootContainer {

        private List<WdssiiView> theList = new ArrayList<WdssiiView>();

        public WRootWindow(boolean paramBoolean, ViewSerializer paramViewSerializer, DockingWindow paramDockingWindow) {
            super(paramBoolean, paramViewSerializer, paramDockingWindow);
        }

        public void addWdssiiView(WdssiiView wv, MDockView md) {
            InfonodeViews.wrapWdssiiViewWithGUI2(wv);
            theList.add(wv);
            View theView = (View) (wv.container);
            DockingWindow base = getWindow();

            // Depending on how user has moved stuff around, should be one of three
            // possibilities:
            // 1.  All closed, so null --> create a new tabwindow
            // 2.  Dragged into split window...
            // 3.  TabWindow already...
            if (base == null) {
                TabWindow theTab = new TabWindow(theView);
                setWindow(theTab);
            } else {
                if (base instanceof TabWindow) {
                    TabWindow theTab = (TabWindow) (base);
                    theTab.addTab(theView);
                } else if (base instanceof SplitWindow) {
                    TabWindow theTab = new TabWindow(new DockingWindow[]{theView, base});
                    setWindow(theTab);
                } else {
                    LOG.error("Unknown window type...We should handle this type (FIXME)");
                }
            }
            if (md != null) {
                final MDockView link = md;
                final WdssiiView wvfinal = wv;
                link.windowAdded();
                theView.addListener(new DockingWindowAdapter() {
                    @Override
                    public void windowClosing(DockingWindow window)
                            throws OperationAbortedException {
                        LOG.debug("Window closing");
                        theList.remove(wvfinal);
                        if (link != null) {
                            link.windowClosing();
                        }
                    }

                    @Override
                    public void windowClosed(DockingWindow window) {
                        LOG.debug("Window closed");
                    }

                    @Override
                    public void windowRemoved(DockingWindow window, DockingWindow w2) {
                        LOG.debug("Window removed");
                    }
                });
            }

        }

        @Override
        public void addControls(List<Object> addTo) {
            // The product follow menu
            //Icon link = SwingIconFactory.getIconByName("plus.png");
            JideSplitButton b = new JideSplitButton("");
            //b.setIcon(link);
            b.setText("Window");
            b.setAlwaysDropdown(true);
            b.setToolTipText("Windows");
            b.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
                @Override
                public void customize(JPopupMenu menu) {
                    menu.removeAll();
                    // FIXME: sync lock? probably
                    for (WdssiiView v : theList) {
                        JMenuItem newOne = new JMenuItem(v.getTitle());
                        newOne.addActionListener(new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent evt) {
                                LOG.debug("Picked a window item");
                            }
                        });
                        menu.add(newOne);
                    }
                }
            });
            addTo.add(b);
        }

        @Override
        public void createSingleSubWindow(String controlTitle, Icon i, Component f, Component c) {
            // Inside the root window we'll add two views...

            View controls = new View(controlTitle, i, c);
            View select = new View(controlTitle + " selection", i, f);

            // The select is our 'root', so make it non-dragable.  Basically
            // the main view will be the actual holder for this.  By making it a view,
            // we allow the other view to be docked above it, etc..
            ViewProperties vp = select.getViewProperties();
            ViewTitleBarProperties tp = vp.getViewTitleBarProperties();
            tp.setVisible(false);

            // Since menu allows changing, make a new window properties
            DockingWindowProperties org = controls.getWindowProperties();
            org.setCloseEnabled(false);
            org.setMaximizeEnabled(false);
            org.setMinimizeEnabled(false);

            SplitWindow w = new SplitWindow(false, select, controls);
            setWindow(w);
            // Add a 'close' listener to our internal root so that if the
            // control window is closed we redock instead.. (we could close
            // it but we'll need some control to get it back then)
            // Add a listener which shows dialogs when a window is closing or closed.
            addListener(new DockingWindowAdapter() {
                @Override
                public void windowClosing(DockingWindow window)
                        throws OperationAbortedException {
                    window.dock();
                }
            });
        }
    }

    /**
     * Create a root component. We create an infonode RootWindow, so that stuff
     * will 'dock' to us
     *
     * @return
     */
    @Override
    public RootContainer createRootContainer() {
        //RootWindowProperties override = new RootWindowProperties();
        WRootWindow aWindow;

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

        TabWindow localTabWindow = new TabWindow();
        for (int i = 0; i < someViewMap.getViewCount(); i++) {
            localTabWindow.addTab(someViewMap.getViewAtIndex(i));
        }
        localTabWindow.setSelectedTab(0);
        aWindow = new WRootWindow(false, handler, localTabWindow);
        aWindow.setPopupMenuFactory(WindowMenuUtil.createWindowMenuFactory(someViewMap, true));

        //aWindow = DockingUtil.createRootWindow(someViewMap, handler,
        //        true);
        // aWindow = DockingUtil.createRootWindow(false, someViewMap, handler,
        //        true);
        // Default to the properties global
        synchronized (propSync) {
            aWindow.getRootWindowProperties().addSuperObject(properties);
        }

        return aWindow;
    }

    /**
     * Create a root window for internal docking within a view
     */
    public static RootWindow createARootWindow() {

        //RootWindowProperties override = new RootWindowProperties();
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
        synchronized (propSync) {
            aWindow.getRootWindowProperties().addSuperObject(properties);
        }

        return aWindow;
    }

    private void setUpGlobalProperties() {

        // Make properties use the theme settings first...
        synchronized (propSync) {
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

        if (Application.USE_HEAVYWEIGHT_GL) {
            // rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap, handler, true);
            rootWindow = DockingUtil.createHeavyweightSupportedRootWindow(viewMap, true);

        } else {
            // rootWindow = DockingUtil.createRootWindow(viewMap, handler,
            //         true);
            rootWindow = DockingUtil.createRootWindow(viewMap,
                    true);
        }

        synchronized (propSync) {
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

        // FIXME: Should hunt by reflection, auto handle this...
        // For the moment creating ALL views...
        //  addViewByID("WorldWindView");
        getViewByID("NavView");
        // getViewByID("DebugView");
        getViewByID("CatalogView");
        getViewByID("SourcesView");
        getViewByID("DataFeatureView");
        getViewByID("FeaturesView");

        // Add a mouse button listener that closes a window when it's clicked with the middle mouse button.
        rootWindow.addTabMouseButtonListener(DockingWindowActionMouseButtonListener.MIDDLE_BUTTON_CLOSE_LISTENER);
    }

    /**
     * Sets a super basic default window layout in case layout files fail for
     * some reason
     */
    private void setDefaultLayout() {

        // Stick all views in one tab..except those explicited referenced
        // later
        DockingWindow[] v = views.toArray(new DockingWindow[views.size()]);

        // Special windows
        DockingWindow catalog = getViewByID("CatalogView");
        DockingWindow sources = getViewByID("SourcesView");
        DockingWindow nav = getViewByID("NavView");
        DockingWindow chart = getViewByID("DataFeatureView");
        //DockingWindow debug = getViewByID("DebugView");
        DockingWindow features = getViewByID("FeaturesView");

        //TabWindow debug = new TabWindow(new DockingWindow[]{jobs, cache});
        TabWindow sourceProducts = new TabWindow(new DockingWindow[]{sources, features, catalog});
        sourceProducts.setSelectedTab(0);

        // SplitWindow chart3D = new SplitWindow(false, 0.3f, objects, chart);

        // TabWindow stuff = new TabWindow(new DockingWindow[]{sourceProducts, chart});
        TabWindow stuff = new TabWindow(new DockingWindow[]{sourceProducts});
        rootWindow.setWindow(
                new SplitWindow(true, 0.5f,
                new SplitWindow(false, 0.7f, chart, nav), stuff));
        stuff.setSelectedTab(0);
        //rootWindow.setWindow(new SplitWindow(false, 0.7f, features, chart));

        /*
         * WindowBar windowBar = rootWindow.getWindowBar(Direction.DOWN); while
         * (windowBar.getChildWindowCount() > 0) {
         * windowBar.getChildWindow(0).close(); } windowBar.addTab(layers);
         *
         */
        // isn't the main window bound part of the default layout?
        Rectangle r = getScreenBounds();
        r.grow(-50, -50); // Margin
        setMainWindowBounds(r);
    }

    /**
     * Initializes the frame and shows it.
     */
    private void createNewFrame() {
        //myRootFrame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
        myRootFrame = new JFrame();
        myRootFrame.getContentPane().add(rootWindow, BorderLayout.CENTER);
        myRootFrame.setJMenuBar(createMenuBar());
        myRootFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        myRootFrame.addWindowListener(new WindowListener(){

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                doExit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
            
        });
        //   // String currentTitle = w.getTitle();
        //   String dir = DataManager.getInstance().getRootTempDir();
        //   // String newTitle = currentTitle.replaceAll("tempdir", "["+dir+"]");
        setMainTitle(UNTITLED);
    }

    public void pinToScreenBounds(Rectangle b) {
        Rectangle r = getScreenBounds();

        if (b.x < r.x) {
            b.x = r.x;
        }
        if (b.y < r.y) {
            b.y = r.y;
        }
        if (b.width > r.width) {
            b.width = r.width;
        }
        if (b.height > r.height) {
            b.height = r.height;
        }
    }

    private void setMainWindowBounds(Rectangle b) {

        pinToScreenBounds(b);

        // Note: have to set initial bounds before visible for openGL
        myRootFrame.setBounds(b);
        myRootFrame.setVisible(true);
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
     * Print out the tree of window children
     */
    public void dumpChildren(DockingWindow r, int level) {
        int win = r.getChildWindowCount();
        String space = "";
        for (int s = 0; s < level; s++) {
            space += " ";
        }
        for (int i = 0; i < win; i++) {
            DockingWindow subWindow = r.getChildWindow(i);

            LOG.debug(space + i + " " + subWindow.getClass().getSimpleName());
            this.dumpChildren(subWindow, ++level);
        }
    }

    public DockingWindow buildWindow(org.wdssii.xml.views.DockWindow r) {

        DockingWindow dock = null;

        // ---------------------------------------------------
        // First, create and gather all child windows....
        List<DockingWindow> theList = new ArrayList<DockingWindow>();
        if (r.window == null) {
        } else {
            for (org.wdssii.xml.views.DockWindow dw : r.window) {
                DockingWindow c = buildWindow(dw);
                if (c == null) {
                    LOG.debug("We got a null child....");
                }
                theList.add(c);

            }
        }

        // ---------------------------------------------------
        // Then create this one, using the children as parameters...
        if (r instanceof org.wdssii.xml.views.RootWindow) {
            LOG.debug("Rootwindow found");
            DockingWindow stuff = theList.get(4);
            // Floating Windows can cause the internal root window to be
            // completely empty...
            if (!(stuff instanceof FloatingWindow)) {
                rootWindow.setWindow(stuff);
            }
            org.wdssii.xml.views.RootWindow rw = (org.wdssii.xml.views.RootWindow) (r);
            setMainWindowBounds(new Rectangle(rw.x, rw.y, rw.width, rw.height));

        } else if (r instanceof org.wdssii.xml.views.WindowBar) {
            dock = null;
            LOG.debug("Windowbar found");
        } else if (r instanceof org.wdssii.xml.views.TabWindow) {
            LOG.debug("TabWindow found");
            DockingWindow[] test = (DockingWindow[]) theList.toArray(new DockingWindow[0]);
            dock = new TabWindow(test);
        } else if (r instanceof org.wdssii.xml.views.SplitWindow) {
            LOG.debug("SplitWindow found");
            org.wdssii.xml.views.SplitWindow sw = (org.wdssii.xml.views.SplitWindow) (r);
            dock = new SplitWindow(sw.isHorizontal, sw.dividerLocation, theList.get(0), theList.get(1));
        } else if (r instanceof org.wdssii.xml.views.View) {
            org.wdssii.xml.views.View sw = (org.wdssii.xml.views.View) (r);
            LOG.debug("Key for window is " + sw.key);
            dock = this.getViewByID(sw.key);
        } else if (r instanceof org.wdssii.xml.views.FloatingWindow) {
            org.wdssii.xml.views.FloatingWindow fw = (org.wdssii.xml.views.FloatingWindow) (r);
            // dock = new FloatingWindow(rootWindow, theList.get(0), new Point(10,10), new Dimension (100,100));
            LOG.debug("Floating window at " + fw.x + ", " + fw.y + ", " + fw.width + ", " + fw.height);
            Rectangle b = new Rectangle(fw.x, fw.y, fw.width, fw.height);
            pinToScreenBounds(b);
            Point p = new Point(b.x, b.y);
            Dimension d = new Dimension(b.width, b.height);
            DockingWindow l = theList.get(0);
            LOG.debug("Docking window part is " + l);

            dock = rootWindow.createFloatingWindow(p, d, l);
            ((FloatingWindow) dock).getTopLevelAncestor().setVisible(true);
        }
        return dock;
    }

    /**
     * Get all child windows from given and below in XML format
     */
    public org.wdssii.xml.views.DockWindow getChildWindows(DockingWindow r, int level) {

        org.wdssii.xml.views.DockWindow dock = null;

        // RootWindow, TabWindow, SplitWindow, WindowBar are subclasses of DockingWindow
        // 'Could' reflect these.  We just make a dao class for each infonode class
        // This should rarely change.  We'll probably add our own mini-super layout window
        // here eventually.
        if (r instanceof RootWindow) {
            org.wdssii.xml.views.RootWindow rx = new org.wdssii.xml.views.RootWindow();
            dock = rx;
            Rectangle rect = myRootFrame.getBounds();
            rx.x = (int) rect.getX();
            rx.y = (int) rect.getY();
            rx.width = (int) rect.getWidth();
            rx.height = (int) rect.getHeight();
        } else if (r instanceof TabWindow) {
            dock = new org.wdssii.xml.views.TabWindow();
        } else if (r instanceof WindowBar) {
            dock = new org.wdssii.xml.views.WindowBar();
        } else if (r instanceof SplitWindow) {
            org.wdssii.xml.views.SplitWindow swx = new org.wdssii.xml.views.SplitWindow();
            dock = swx;
            SplitWindow sw = (SplitWindow) (r);
            swx.dividerLocation = sw.getDividerLocation();
            swx.isHorizontal = sw.isHorizontal();
            // Could save properties too...
            //SplitWindowProperties x= sw.getSplitWindowProperties();
            //int div = x.getDividerSize();
            //LOG.error("Div size is "+div);
            //x.setDividerSize(20);
        } else if (r instanceof IView) {   // Special view with one of our things in it
            org.wdssii.xml.views.View vx = new org.wdssii.xml.views.View();
            dock = vx;
            IView v = (IView) (r);
            vx.title = v.getTitle();
            vx.key = v.getWdssiiKey();
        } else if (r instanceof FloatingWindow) {
            org.wdssii.xml.views.FloatingWindow fx = new org.wdssii.xml.views.FloatingWindow();
            dock = fx;
            FloatingWindow f = (FloatingWindow) (r);
            Container c = f.getTopLevelAncestor();
            fx.x = c.getX();
            fx.y = c.getY();
            fx.width = f.getWidth();
            fx.height = f.getHeight();

        } else {
            LOG.error("Unknown window type encountered " + r.getClass().getCanonicalName());
            LOG.error("I don't know how to save this window or it's subwindows");
        }

        // For each child of the dockwindow, add it...
        if (dock != null) {
            int win = r.getChildWindowCount();
            for (int i = 0; i < win; i++) {
                DockingWindow subWindow = r.getChildWindow(i);
                org.wdssii.xml.views.DockWindow child = getChildWindows(subWindow, ++level);
                if (child != null) {
                    dock.addChild(child);
                }
            }
        }

        return dock;
    }

    private JMenu testMenu() {
        JMenu testMenu = new JMenu("Test");
        // Dangerous if rootWindow changes..
        final RootWindow r = rootWindow;

        JMenuItem item = new JMenuItem("Save layout");
        testMenu.add(item).addActionListener(
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dumpChildren(r, 0);

                    // JAXB OUTPUT
                    org.wdssii.xml.views.DockWindow rr = getChildWindows(r, 0);
                    File file = new File("c:/timesheets/testlayout.xml");
                    JAXBContext jaxbContext = JAXBContext.newInstance(org.wdssii.xml.views.RootWindow.class);
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                            true);

                    jaxbMarshaller.marshal(rr, file);
                    jaxbMarshaller.marshal(rr, System.out);

                } catch (Exception ex) {
                    LOG.error("Couldn't save layout " + ex.toString());
                }
            }
        });

        JMenuItem item2 = new JMenuItem("Restore layout");
        testMenu.add(item2).addActionListener(
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    JAXBContext jc = JAXBContext.newInstance(org.wdssii.xml.views.RootWindow.class);

                    StreamSource xml = new StreamSource("c:/timesheets/testlayout.xml");
                    Unmarshaller unmarshaller = jc.createUnmarshaller();
                    // JAXBElement<org.wdssii.xml.views.RootWindow> theRoot =
                    JAXBElement<org.wdssii.xml.views.RootWindow> theRoot =
                            unmarshaller.unmarshal(xml, org.wdssii.xml.views.RootWindow.class);

                    // Have to close all the old floating windows first, quirk with
                    // infonode
                    closeAllFloatingWindows();

                    // Build the window using the xml
                    buildWindow(theRoot.getValue());
                    //     rootWindow.setWindow(
                    // new SplitWindow(true, 0.5f,
                    //new SplitWindow(false, 0.7f, chart, nav), stuff));

                } catch (Exception ex) {
                    LOG.error("Couldn't read layout " + ex.toString());
                }
            }
        });
        return testMenu;
    }

    private void closeAllFloatingWindows() {
        int current = rootWindow.getChildWindowCount();
        List<FloatingWindow> list = new ArrayList<FloatingWindow>();
        for (int i = 0; i < current; i++) {
            DockingWindow win = rootWindow.getChildWindow(i);
            if (win instanceof FloatingWindow) {
                list.add((FloatingWindow) (win));
            }
        }
        for (FloatingWindow f : list) {
            f.close();
        }
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
                synchronized (propSync) {
                    properties.getDockingWindowProperties().setCloseEnabled(true);
                }
            }
        });

        buttonsMenu.add("Hide Close Buttons").addActionListener(
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (propSync) {
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
        synchronized (propSync) {
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

        /**
         * The new menu
         */
        eMenuItem = new JMenuItem("New");
        eMenuItem.setToolTipText("Create a new setup");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doNew();
            }
        });
        menu.add(eMenuItem);
        menu.addSeparator();

        /**
         * The new menu
         */
        eMenuItem = new JMenuItem("Open...");
        eMenuItem.setToolTipText("Create a new setup");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doOpen();
            }
        });
        menu.add(eMenuItem);
        menu.addSeparator();

        /**
         * The recent documents menu
         */
        menu.add(OpenCommand.getRecentDocumentMenu());
        menu.addSeparator();

        eMenuItem = new JMenuItem("Save");
        //eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Save state of display, layout, sources to XML file");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doSave();
            }
        });
        menu.add(eMenuItem);

        eMenuItem = new JMenuItem("Save As...");
        //eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Save state of display, layout, sources to XML file...");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doSaveAs();
            }
        });
        menu.add(eMenuItem);

        menu.addSeparator();
        eMenuItem = new JMenuItem("Exit");
        eMenuItem.setMnemonic(KeyEvent.VK_C);
        eMenuItem.setToolTipText("Exit application");
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doExit();
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

                            WdssiiView wv = f.getWdssiiView();
                            if (wv != null) {

                                Object o = wv.container;
                                // What to do if view exists.....
                                if (o instanceof DockingWindow) {
                                    DockingWindow v = (DockingWindow) (o);
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
        menu.add(testMenu());
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
     * Set the Color theme. This sets the actual java look and feel, vs the
     * infonode 'theme' which is setting for how infonode draws itself.
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

    /**
     * Set the InfoNode theme type. This is how the InfoNodeLookAndFeel draws
     * itself
     */
    private static void setTheme(int index) {
        try {
            DockingWindowsTheme theme = myWindowThemes[index];
            if (currentTheme != null) {
                synchronized (propSync) {
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

    /**
     * Copied from infonode PropertyUtil, allows me to have super fine control
     * of appearance
     */
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

    public void doNew() {
        NewCommand c = new NewCommand();
        c.setConfirmReport(true, true, rootWindow);
        CommandManager.getInstance().executeCommand(c, true);
    }

    public void doOpen() {
        OpenCommand c = new OpenCommand();
        c.setConfirmReport(true, true, rootWindow);
        CommandManager.getInstance().executeCommand(c, true);
    }

    public void doSave() {
        SaveCommand c = new SaveCommand(false);
        c.setConfirmReport(true, true, rootWindow);
        CommandManager.getInstance().executeCommand(c, true);
    }

    public void doSaveAs() {
        SaveCommand c = new SaveCommand(true);
        c.setConfirmReport(true, true, rootWindow);
        CommandManager.getInstance().executeCommand(c, true);
    }

    public void doExit() {
        ExitCommand c = new ExitCommand();
        c.setConfirmReport(true, true, rootWindow);
        CommandManager.getInstance().executeCommand(c, true);
    }
}
