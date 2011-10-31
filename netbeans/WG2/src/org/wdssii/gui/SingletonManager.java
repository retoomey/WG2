package org.wdssii.gui;

import java.io.InputStream;

import java.net.URL;
import org.wdssii.core.W2Config;
import org.wdssii.gui.util.FileUtil;
import org.wdssii.xml.wdssiiConfig.Tag_setup;

/**
 * Singleton getInstance can cause order issues, since each singleton
 * initializes data/etc and may end up calling another singleton, etc. This
 * caused start-up issues in the c++ version. With the manager, we control the
 * initialization part of the singleton with the singletonManagerCallback
 * method. Also, we can call a function on destruction of the display as well,
 * allowing singletons to 'cleanup' if needed.
 * 
 * Singleton subclasses shouldn't do any startup work inside the constructor,
 * they should do it in the singletonManagerCallback
 * 
 * FIXME: needs some work here.
 * 
 * @author Robert Toomey
 * 
 */
public class SingletonManager {

    private static SingletonManager instance = null;
    private static boolean notCreated = true;
    // Don't let it be read while it's reading...
    private Object wdssiiReadLock = new Object();
    private Tag_setup theWdssiiXML;

    private SingletonManager() {
        try {
            synchronized (wdssiiReadLock) {
                URL u = W2Config.getURL("wdssii.xml");
                theWdssiiXML = new Tag_setup();
                theWdssiiXML.processAsRoot(u);
            }

        } catch (Exception e) {
            //System.out.println("*********Exception reading setup configuration file:"+e.toString());
        }
    }

    public Tag_setup getSetupXML() {
        synchronized (wdssiiReadLock) {
            return theWdssiiXML;
        }
    }

    /**
     * As soon as ANY singleton registers, we create all of them, then call the
     * singletonManagerCallback method ordered on each.  Then we ignore any future
     * registerSingleton calls.  This forces the creation order
     * 
     * FIXME: we could specify this from xml maybe...
     * @param caller
     */
    public static void registerSingleton(Object caller) {

        if (notCreated) {

            // Ignore future calls to registerSingleton...
            notCreated = false;

            // Create all the other singletons (not registering)
            // Note we don't control the creation order of the singleton objects,
            // first caller is already created.
            if (!(caller instanceof PreferencesManager)) {
                PreferencesManager.getInstance();
            }

            if (!(caller instanceof ProductManager)) {
                ProductManager.getInstance();
            }
            if (!(caller instanceof CommandManager)) {
                CommandManager.getInstance();
            }

            if (!(caller instanceof SourceManager)) {
                SourceManager.getInstance();
            }
            if (!(caller instanceof LLHAreaManager)) {
                LLHAreaManager.getInstance();
            }

            // Here is where we control code order.  A singleton should do any 'work'
            // within it's singletonManagerCallback routine.  At this time, all
            // singletons are created.

            // -----------------------------------------------------------
            // Make sure preferences are loaded/created before anyone else.
            PreferencesManager.getInstance().singletonManagerCallback();
            // -----------------------------------------------------------

            ProductManager.getInstance().singletonManagerCallback();
            CommandManager.getInstance().singletonManagerCallback();
            SourceManager.getInstance().singletonManagerCallback();
            LLHAreaManager.getInstance().singletonManagerCallback();

        }
    }

    public static SingletonManager getInstance() {
        if (instance == null) {
            instance = new SingletonManager();
        }
        return instance;
    }
}
