package org.wdssii.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.commands.WdssiiCommand;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.views.CacheView;
import org.wdssii.gui.views.EarthBallView;
import org.wdssii.gui.views.LLHAreaView;
import org.wdssii.gui.views.NavView;
import org.wdssii.gui.views.SourceManagerView;
import org.wdssii.gui.views.TableProductView;
import org.wdssii.gui.views.WdssiiView;
import org.wdssii.index.IndexRecord;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.layers.LayerList;

/**
 * ActionListeners are similar to a peer-to-peer network, this is a central
 * management system that requires at times explicit ordering of events (for
 * window syncing/looping properly of multiple windows, etc.)
 * 
 * @author rest
 * 
 */
public class CommandManager implements Singleton {

    private static CommandManager instance = null;
    private static Log log = LogFactory.getLog(CommandManager.class);
    private VisualCollection myVisualCollection = new VisualCollection();
    public static String CommandPath = "org.wdssii.gui.commands.";
    // FIXME: Eventually add the ability for a collection of these
    ProductHandlerList myProductOrderedSet = new ProductHandlerList();
    private TreeMap<String, WdssiiView> myNamedViews = new TreeMap<String, WdssiiView>();

    // You should not create NavigationAction
    // yourself. Any class method using a NavigationAction must have that
    // method called from the WorldManager. ActionListeners have no
    // implied ordering of response, and each listener acts independently
    // which we do not want (among other issues).
    // Downside: We couple with this class heavily.
    // Upside: We couple with this class heavily.
    // FIXME: These fields will be a superset of the IndexRecord Direction
    // fields
    // would be nice to have it automatically do that.
    // FIXME: These should be subclasses of WDSSIICommand in order to be scriptable
    public enum NavigationMessage {

        PreviousSubType, NextSubType, PreviousTime, NextTime, LatestTime, PreviousLowestSubType, // the
        // 'base'
        LatestUp, // Virtual volume up
        LatestDown, // Virtual volume down
        LatestBase, // Virtual volume base
        SyncCurrent
        // The 'current' centered record
    }

    // FIXME: remove this into command objects....
    // Think this is already partially done
    public class NavigationAction {

        protected NavigationMessage myMessage;
        protected boolean myRedraw = true; // For now all nav actions redraw the
        // world
        protected Product myRecord = null;

        public NavigationAction(NavigationMessage message) {

            myMessage = message;
        }

        public NavigationMessage message() {
            return myMessage;
        }

        public boolean redraw() {
            return myRedraw;
        }

        public Product record() {
            return myRecord;
        }

        @Override
        public String toString() {
            String theString;
            switch (myMessage) {
                case PreviousSubType:
                    theString = "Previous SubType";
                    break;
                case NextSubType:
                    theString = "Next SubType";
                    break;
                case PreviousTime:
                    theString = "Previous Time";
                    break;
                case NextTime:
                    theString = "Next Time";
                    break;
                case LatestTime:
                    theString = "Latest Time";
                    break;
                case PreviousLowestSubType:
                    theString = "BASE";
                    break;
                case LatestUp:
                    theString = "VirtualUp";
                    break;
                case LatestDown:
                    theString = "VirtualDown";
                    break;
                case LatestBase:
                    theString = "VirtualBase";
                    break;
                case SyncCurrent:
                    theString = "Sync";
                    break;
                default:
                    theString = "Unknown navigation message";
                    break;
            }
            return theString;
        }
    }

    private CommandManager() {
        // Exists only to defeat instantiation.
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
            SingletonManager.registerSingleton(instance);
        }
        return instance;
    }

    public void registerView(String name, WdssiiView aView) {
        myNamedViews.put(name, aView);
    }

    public void deregisterView(String name) {
        myNamedViews.put(name, null);
        myNamedViews.remove(name);
    }

    public WdssiiView getNamedViewed(String name) {
        return (myNamedViews.get(name));
    }

    /*
    public void deregisterView(String name, ViewPart aView) {
    // getEarthBall()s.add(aView);
    }
     */
    // Stuff 'per' earth ball.  The EarthBall view isn't really this since it can be created/destoryed
    public VisualCollection getVisualCollection() {
        return myVisualCollection;
    }

    public EarthBallView getEarthBall() {
        return ((EarthBallView) getNamedViewed(EarthBallView.ID));
    }

    // / Used by the Layers view to get the list of global worldwind layers from
    // the primary window
    public LayerList getLayerList() {
        return getEarthBall().getLayerList();
    }

    public void setLayerEnabled(String name, boolean flag) {
        getEarthBall().setLayerEnabled(name, flag);
    }

    /*
     * // called by nav when selecting a product handler public void
     * selectProductHandler(ProductHandler h) { if (h != null){
     * 
     * // called from myProductOrderedSet.selectProductHandler(h); if
     * (myTableView != null){ // FIXME: eventually table will have a 'select'
     * myTableView.update(); } getEarthBall().loadProduct(h.getProduct()); }
     * myNavView.update(); getEarthBall().updateOnMinTime(); // redraw on update // +++
     * anything that cares
     * 
     */
    public void setProductHandlerVisible(ProductHandler h, boolean flag) {
        h.setIsVisible(flag);

        // This should be a command actually...
        //WdssiiView view = getNamedViewed(NavView.ID);
       // if (view instanceof NavView) {
        //    NavView nav = (NavView) (view);
       //     nav.updateGUI(null);
        //}

        getEarthBall().updateOnMinTime();
    }

    // called by ColorKeyLayer to get the current color map...
    public ColorMap getCurrentColorMap() {
        return (myProductOrderedSet.getCurrentColorMap());
    }

    // Called by NavView to get the current ordered set
    public ProductHandlerList getProductOrderedSet() {
        return myProductOrderedSet;
    }

    public FilterList getFilterList(String product) {
        FilterList aList = null;
        if (myProductOrderedSet != null) {
            ProductHandler tph = myProductOrderedSet.getProductHandler(product);
            if (tph != null) {
                aList = tph.getFList();
            }
        }
        return aList;
    }

    // Called to get the top product in the display
    public Product getTopProduct() {
        Product aProduct = null;
        ProductHandlerList list = CommandManager.getInstance().getProductOrderedSet();
        if (list != null) {
            ProductHandler h = list.getTopProductHandler();
            if (h != null) {
                aProduct = h.getProduct();
            }
        }
        return aProduct;
    }

    public void cacheManagerNotify() {
        WdssiiView view = getNamedViewed(CacheView.ID);
        if (view instanceof CacheView) {
            CacheView pcv = (CacheView) (view);
            pcv.update();
        }
    }

    /** Currently called by ReadoutStatusBar to get the text for readout */
    public String getReadout(PositionEvent event) {
        return (myProductOrderedSet.getReadout(event));
    }

    // All the 'move' commands and the 'load' record
    public void navigationMessage(NavigationMessage message) {
        NavigationAction nav = new NavigationAction(message);

        myProductOrderedSet.navigationAction(nav);
        if (nav.redraw()) {
            getEarthBall().updateOnMinTime();
        }

        // This should be a command actually
        //WdssiiView view = getNamedViewed(NavView.ID);
        //if (view instanceof NavView) {
        //    NavView navView = (NavView) (view);
        //    navView.update();
       // }
    }

    @Override
    public void singletonManagerCallback() {
    }

    // Hack called by radial set render to force update of window.
    // Humm...we'll eventually have multiple windows
    public void updateDuringRender() {
        if (getEarthBall() != null) {
            getEarthBall().updateOnMinTime();
        }
    }

    // Projection command
    public String getProjection() {
        String p = "Mercator";
        if (getEarthBall() != null) {
            p = getEarthBall().getProjection();
        }
        return p;
    }

    public void setProjection(String projection) {
        if (getEarthBall() != null) {
            getEarthBall().setProjection(projection);
        }
    }

    /*
     * // Get the simulation time for the current handler list public String
     * getSimulationTimeStamp() { // Add a product to current product group //
     * currently we only have one of them return
     * (myProductOrderedSet.getSimulationTimeStamp()); }
     * 
     * // Get the simulation time for the current handler list public Date
     * getSimulationTime() { // Add a product to current product group //
     * currently we only have one of them return
     * (myProductOrderedSet.getSimulationTime()); }
     */
    public void handleRecord(IndexRecord rec) {
        WdssiiView view = getNamedViewed(SourceManagerView.ID);
        if (view instanceof SourceManagerView) {
            SourceManagerView smv = (SourceManagerView) (view);
            smv.update();  // Different thread
        }
    }

    public GridVisibleArea getVisibleGrid() {
        WdssiiView view = getNamedViewed(TableProductView.ID);
        if (view instanceof TableProductView) {
            TableProductView table = (TableProductView) (view);
            return (table.getVisibleGrid());
        }
        return null;
    }

    public void hackWindField(Product aProduct) {
        if (getEarthBall() != null) {
            getEarthBall().loadProduct(aProduct);
        }
    }

    // Called from earth view to send a worldwind select event message to all views interested.
    // For the moment, just the volume view..needs to be generalized for other views
    // such as annotationsd
    public void earthViewSelectionEvent(WorldWindowGLCanvas world, SelectEvent event) {
        // TODO uncouple knowledge of VolumeView (probably need to subclass viewpart to make
        // a special event handling view or object)

        // This is being called from the worldwind thread...not the SWT.
        // Any SWT update code must wrap within async..
        WdssiiView view = getNamedViewed(LLHAreaView.ID);
        if (view != null) {
            LLHAreaView vv = (LLHAreaView) (view);
            vv.earthViewSelection(world, event);
        }
    }

    /** Generate a command by name */
    public WdssiiCommand generateCommand(String commandName, Map<String, String> optionalParms) {
        WdssiiCommand command = null;

        // Every '.' in the command name must have Command added:
        // SourceDelete.DeleteALL --> SourceDeleteCommand.DeleteAllCommand
        String createByName = commandName.replaceAll("\\.", "Command\\$");
        createByName = CommandPath + createByName + "Command";
        log.info("Generate command: " + createByName);
        Class<?> aClass = null;
        try {
            aClass = Class.forName(createByName);
            Constructor<?> c = aClass.getConstructor();
            if (c == null) {
                log.error("Couldn't find a constructor " + commandName);
            }
            command = (WdssiiCommand) c.newInstance();
            command.setParameters(optionalParms);
            log.debug("Generated command " + commandName);
        } catch (Exception e) {
            log.error("Couldn't create WdssiiCommand by name '"
                    + createByName + "' because " + e.toString());
        }
        return command;
    }

    /** Execute a command.
     * 
     * @param command 	 the command to execute
     * @param userAction true if caused by a GUI user action
     */
    public void executeCommand(WdssiiCommand command, boolean userAction) {

        if (command != null) {
            //log.info("CommandManager: Executing command "+command);
            // Does the command require an immediate update message sent?
            if (command.execute()) {
                fireUpdate(command);
            }
        }
    }

    /** Send the GUI/listener update
     * Normally, you don't call this directly.  You would call this if you had a separate
     * thread running a command and needed to notify the main GUI thread that you are done.
     * @see SourceConnectCommand
     * @param command the command to fire update for
     * @param userAction
     */
    public void fireUpdate(WdssiiCommand command) {
        // For the moment, only registered views are considered command listeners..
        // FIXME: Make command listeners their own separate list

        // This makes your coding easier once you get how it works.
        // For all WdssiiCommands, we call the first found method of the form:
        // commandClassNameUpdate(commandClassName)
        // Only one method is called, but it will hunt all the way up to
        // WdssiiCommandUpdate(WdssiiCommand).
        // This allows 'overriding', generic handling if wanted.  The point is
        // to avoid the switch(event type) non-OO design stuff that usually happens
        // here.

        // All views are considered to be command listeners for now...
        Collection<WdssiiView> c = myNamedViews.values();
        for (WdssiiView v : c) {
            Class<?> theClass = v.getClass();
            Class<?> commandClass = command.getClass();
            String rootClass = WdssiiCommand.class.getSimpleName();
            boolean keepLooking = true;

            // Keep looking up the subclass tree until we get to WdssiiCommand
            while (keepLooking) {
                keepLooking = false;

                // If current parameter is not WdssiiCommand, keep looking for methods...
                String currentName = commandClass.getSimpleName();
                if (!currentName.equalsIgnoreCase(rootClass)) {
                    keepLooking = true;
                }

                try {
                    // We will only accept a method with the command as a parameter, reducing the risk
                    // of any 'accidents' of name coincidence
                    String methodName = currentName + "Update";
                    Method test = theClass.getMethod(methodName, commandClass);
                    keepLooking = false;  // We found a method, so don't look anymore
                    try {
                        test.invoke(v, command);
                    } catch (IllegalArgumentException e) {  // We keep looking if arguments are wrong
                        // Maybe tell programmer the arguments are wrong?
                        log.warn("Warning.  Found method " + methodName + " in " + currentName + ", but expected same class name.");
                    } catch (IllegalAccessException e) {	// We keep looking if we don't have access
                        // Maybe tell programmer the access is wrong?
                        log.warn("Warning.  Found method " + methodName + " in " + currentName + ", but access is not public.");
                    } catch (InvocationTargetException e) {  // This is caused by something in your code
                        log.warn("Warning.  Unhandled exception in method '" + methodName + "' in " + theClass.getSimpleName());
                        log.warn("Exception is " + e.toString());
                        log.warn("This is a bug and needs to be fixed.  GUI will likely act strangely");
                    }
                } catch (SecurityException e) {
                } catch (NoSuchMethodException e) {
                    // Move up until we find the WdssiiCommand superclass.
                    commandClass = commandClass.getSuperclass();
                }
            }
        }
    }
}
