package org.wdssii.gui;

import gov.nasa.worldwind.layers.LayerList;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.commands.WdssiiCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.views.CommandListener;
import org.wdssii.gui.views.SourceManagerView;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.index.IndexRecord;

/**
 * ActionListeners are similar to a peer-to-peer network, this is a central
 * management system that requires at times explicit ordering of events (for
 * window syncing/looping properly of multiple windows, etc.)
 * 
 * @author Robert Toomey
 * 
 */
public class CommandManager implements Singleton {

    private static CommandManager instance = null;
    private static Logger log = LoggerFactory.getLogger(CommandManager.class);
    private AnimateManager myVisualCollection = new AnimateManager();
    public static String CommandPath = "org.wdssii.gui.commands.";
    private final Object myViewLock = new Object();

    /** Keep weak references to command listeners, we don't hold onto them.
     We purge null references when we send messages out.
     */
    private TreeMap<String, WeakReference<CommandListener>> myNamedViews = new TreeMap<String, WeakReference<CommandListener>>();

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

    public void addListener(String name, CommandListener aView) {
        synchronized (myViewLock) {
		/*
            if (myNamedViews.containsKey(name)){
               log.error("ADDED a duplicate listener.  This is a bug "+name);
	       	WeakReference<CommandListener> c = myNamedViews.get(name);
		if (c == null){
                   log.error("Humm the reference was null it's ok...");

		}
	       // Maybe not with weak references...might not have purged the
	       // old one yet...
	    }
		 */
            myNamedViews.put(name, new WeakReference<CommandListener>(aView));
	    
        }
    }

    public void removeListener(String name) {
        synchronized (myViewLock) {
            myNamedViews.put(name, null);
            myNamedViews.remove(name);
        }
    }

    public CommandListener getNamedCommandListener(String name) {
        synchronized (myViewLock) {
	    WeakReference<CommandListener> c = myNamedViews.get(name);
	    if (c == null){ return null; }
	    return c.get();
        }
    }

    // Stuff 'per' earth ball.  The EarthBall view isn't really this since it can be created/destoryed
    public AnimateManager getVisualCollection() {
        return myVisualCollection;
    }

    public WorldWindView getEarthBall() {
        return ((WorldWindView) getNamedCommandListener(WorldWindView.ID));
    }

    // / Used by the Layers view to get the list of global worldwind layers from
    // the primary window
    public LayerList getLayerList() {
        LayerList list;
        WorldWindView v = getEarthBall();
        if (v != null) {
            list = v.getLayerList();
        } else {
            list = null;
        }
        return list;
    }

    public void setLayerEnabled(String name, boolean flag) {
        getEarthBall().setLayerEnabled(name, flag);
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
        CommandListener view = getNamedCommandListener(SourceManagerView.ID);
        if (view instanceof SourceManagerView) {
            SourceManagerView smv = (SourceManagerView) (view);
            smv.update();  // Different thread
        }
    }

    public void hackWindField(Product aProduct) {
        if (getEarthBall() != null) {
            getEarthBall().loadProduct(aProduct);
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
        synchronized (myViewLock) {
	   Set<Entry<String, WeakReference<CommandListener>>> c = myNamedViews.entrySet();
	    ArrayList<String> cleanup = new ArrayList<String>();
            for (Entry<String, WeakReference<CommandListener>> entry : c) {
		WeakReference<CommandListener> r = entry.getValue();
		CommandListener v =  r.get();
		if (v == null){ // It's gone...purge it..
			cleanup.add(entry.getKey());
			continue;
		}
		
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
	    // Purge....
	    for(String s:cleanup){
		    myNamedViews.remove(s);
//		    log.debug("COMMAND MANAGER PURGE LISTENER "+s);
	    }
	    // Debug dump...
	    /*
            Set<String> aSet = myNamedViews.keySet();
	    log.debug("DUMP listeners----------------"+c.size());
            for (String s:aSet) {
		    log.debug("CURRENT LISTENER "+s);
	    }
	    log.debug("END DUMP listeners");
	     * 
	     */
        }
    }
}
