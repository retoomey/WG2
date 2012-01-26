package org.wdssii.gui;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WDSSII;
import org.wdssii.storage.DataManager;

/**
 * The Application...
 * 
 * @author Robert Toomey
 */
public class Application {

    private static Log log = LogFactory.getLog(Application.class);

    public void start() {
        // Start up WDSSII base here ?
        log.info("WDSSII GUI VERSION 2.0 ----------------------------------------------------------");
        log.info("Startup: JAVA VERSION   = " + System.getProperty("java.specification.version"));
        log.info("Startup: USER DIRECTORY = " + System.getProperty("user.dir"));
        DataManager.getInstance();

        // Create the WDSSII low-level core for products
        WDSSII.getInstance();

        // Add the netbeans job creator
        JobManager.getInstance();
        //WdssiiJob.introduce(new NBJobHandler.NBJobFactory());

        // Add the netbeans preference manager
        PreferencesManager.introduce(new XMLPrefHandler());

        DockWindow.startWindows();
    }

    public static void main(String[] args) {
        
        System.out.println("WDSSII GUI VERSION 2.0 Startup");
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String bits = System.getProperty("sun.arch.data.model");
        
        System.out.println("Running on "+name+" ["+arch+"] ("+bits+" bit)");
        if (bits.equals("32")) {
            System.out.println("Sorry, currently no 64 bit support.\n  You really want to run this on a 64 bit OS");
            System.out.println("You may have 64 and 32 bit Java and be running the 32 version in your path");
            System.exit(0);
        }
       
        // Use the user directory (where we are running) to dynamically
        // add the OS information to the path.  This is where all of our
        // native libraries will be found
        String dir = System.getProperty("user.dir");
        
        // FIXME: move/cleanup native locations?
        dir += "/release/modules/lib/"+arch;
        System.out.println("Native library directory is: "+dir);
        try {
            addLibraryPath(dir);
        } catch (Exception ex) {
           System.out.println("Couldn't add native library path dynamically");
           System.exit(0);
        }
        
        // Modify java path for native libraries
        Application a = new Application();
        a.start();
    }

    /**
     * Adds the specified path to the java library path
     *
     * @param pathToAdd the path to add
     * @throws Exception
     */
    public static void addLibraryPath(String pathToAdd) throws Exception {
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[]) usrPathsField.get(null);

        //check if the path to add is already present
        for (String path : paths) {
            if (path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}
