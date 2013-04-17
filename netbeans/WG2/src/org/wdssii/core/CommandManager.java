package org.wdssii.core;

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

/**
 * ActionListeners are similar to a peer-to-peer network, this is a central
 * management system that requires at times explicit ordering of events (for
 * window syncing/looping properly of multiple windows, etc.)
 *
 * By doing events this way we avoid thrashing of the display. Also, we could
 * have a 'script' run with a list of commands and have it run smoothly.
 *
 * @author Robert Toomey
 *
 */
public class CommandManager implements Singleton {

    private static CommandManager instance = null;
    private final static Logger LOG = LoggerFactory.getLogger(CommandManager.class);
    private final Object myViewLock = new Object();
    /**
     * Keep weak references to command listeners, we don't hold onto them. We
     * purge null references when we send messages out.
     */
    private TreeMap<String, WeakReference<CommandListener>> myListeners = new TreeMap<String, WeakReference<CommandListener>>();

    private CommandManager() {
        // Exists only to defeat instantiation.
    }
    
    public static Singleton create(){
        instance = new CommandManager();
        return instance;
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            LOG.debug("Command Manager must be created by SingletonManager");
        }
        return instance;
    }

    public void addListener(String name, CommandListener aView) {
        synchronized (myViewLock) {
            myListeners.put(name, new WeakReference<CommandListener>(aView));

        }
    }

    public void removeListener(String name) {
        synchronized (myViewLock) {
            myListeners.put(name, null);
            myListeners.remove(name);
        }
    }

    public CommandListener getNamedCommandListener(String name) {
        synchronized (myViewLock) {
            WeakReference<CommandListener> c = myListeners.get(name);
            if (c == null) {
                return null;
            }
            return c.get();
        }
    }

    @Override
    public void singletonManagerCallback() {
    }

    /**
     * Generate a command by name. This isn't used currently, but could be used
     * to run command from xml
     *
     * @param commandPath such as "org.wdssii.gui.commands."
     */
    public WdssiiCommand generateCommand(String commandPath, String commandName, Map<String, String> optionalParms) {
        WdssiiCommand command = null;

        // Every '.' in the command name must have Command added:
        // SourceDelete.DeleteALL --> SourceDeleteCommand.DeleteAllCommand
        String createByName = commandName.replaceAll("\\.", "Command\\$");
        createByName = commandPath + createByName + "Command";
        LOG.info("Generate command: " + createByName);
        Class<?> aClass = null;
        try {
            aClass = Class.forName(createByName);
            Constructor<?> c = aClass.getConstructor();
            if (c == null) {
                LOG.error("Couldn't find a constructor " + commandName);
            }
            command = (WdssiiCommand) c.newInstance();
            command.setParameters(optionalParms);
            LOG.debug("Generated command " + commandName);
        } catch (Exception e) {
            LOG.error("Couldn't create WdssiiCommand by name '"
                    + createByName + "' because " + e.toString());
        }
        return command;
    }

    /**
     * Execute a command.
     *
     * @param command the command to execute
     * @param userAction true if caused by a GUI user action
     */
    public void executeCommand(WdssiiCommand command, boolean userAction) {

        if (command != null) {
            //LOG.info("CommandManager: Executing command "+command);
            // Does the command require an immediate update message sent?
            if (command.execute()) {
                fireUpdate(command);
            }
        }
    }

    /**
     * Send the GUI/listener update Normally, you don't call this directly. You
     * would call this if you had a separate thread running a command and needed
     * to notify the main GUI thread that you are done.
     *
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
            Set<Entry<String, WeakReference<CommandListener>>> c = myListeners.entrySet();
            ArrayList<String> cleanup = new ArrayList<String>();
            for (Entry<String, WeakReference<CommandListener>> entry : c) {
                WeakReference<CommandListener> r = entry.getValue();
                CommandListener v = r.get();
                if (v == null) { // It's gone...purge it..
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
                            LOG.warn("Warning.  Found method " + methodName + " in " + currentName + ", but expected same class name.");
                        } catch (IllegalAccessException e) {	// We keep looking if we don't have access
                            // Maybe tell programmer the access is wrong?
                            LOG.warn("Warning.  Found method " + methodName + " in " + currentName + ", but access is not public.");
                        } catch (InvocationTargetException e) {  // This is caused by something in your code
                            LOG.warn("Warning.  Unhandled exception in method '" + methodName + "' in " + theClass.getSimpleName());
                            LOG.warn("Exception is " + e.toString());
                            LOG.warn("This is a bug and needs to be fixed.  GUI will likely act strangely");
                        }
                    } catch (SecurityException e) {
                    } catch (NoSuchMethodException e) {
                        // Move up until we find the WdssiiCommand superclass.
                        commandClass = commandClass.getSuperclass();
                    }
                }
            }
            // Purge....
            for (String s : cleanup) {
                myListeners.remove(s);
//		    LOG.debug("COMMAND MANAGER PURGE LISTENER "+s);
            }
            // Debug dump...
	    /*
             Set<String> aSet = myNamedViews.keySet();
             LOG.debug("DUMP listeners----------------"+c.size());
             for (String s:aSet) {
             LOG.debug("CURRENT LISTENER "+s);
             }
             LOG.debug("END DUMP listeners");
             * 
             */
        }
    }
}
