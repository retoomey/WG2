package org.wdssii.gui;

import java.io.InputStream;

import org.wdssii.gui.util.FileUtil;

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
    private WdssiiXMLDocument theWdssiiXML = null;

    private SingletonManager() {
        // Exists only to defeat instantiation.

        // Read the wdssii xml file...
        //System.out.println("*********READING XML FILE IN");
        try {
            // FIXME: hunt for it of course...
            // FIXME: need this somewhere special
            InputStream s = FileUtil.streamFromFile("wdssii.xml");
            //String fullpath = FileUtil.getFilePath("wdssii.xml");
            theWdssiiXML = WdssiiXML.readDocument(s);
            /*	ArrayList<PerspectiveXMLDocument> a = doc.myPerspectives;
            for (PerspectiveXMLDocument d:a){
            if (d.loadOnStartup){
            PerspectiveUtil.openPerspectiveByName(d.className);
            }
            }
             */
        } catch (Exception e) {
            //System.out.println("*********Exception reading setup configuration file:"+e.toString());
        }
        //System.out.println("WDSSII XML FILE IS "+theWdssiiXML);
    }

    public WdssiiXMLDocument getSetupXML() {
        return theWdssiiXML;
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
