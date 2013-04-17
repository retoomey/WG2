package org.wdssii.gui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WDSSII;
import org.wdssii.core.WdssiiJob;
import org.wdssii.storage.DataManager;

/**
 * The Application...
 *
 * @author Robert Toomey
 */
public class Application {

    private final static Logger LOG = LoggerFactory.getLogger(Application.class);

    public void start() {
        
        GUISingletonManager.setup();
        
        DataManager.getInstance();

        // Create the WDSSII low-level core for products
        WDSSII.getInstance();

        // Add the netbeans job creator
        WdssiiJob.introduce(new JobSwingFactory());
        


        // Defaults to UIManager
        // Don't allow double click to work in file chooser
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);

        org.wdssii.xml.PointColorMap.loadStockMaps();
        //  System.exit(1);
        // DockWindows are in the swing thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DockWindow.startWindows();
            }
        });
    }

    public static void main(String[] args) {

        String logmessage = initializeLogger();

        final String arch = System.getProperty("os.arch");
        final String name = System.getProperty("os.name");
        final String bits = System.getProperty("sun.arch.data.model");
        final String userdir = System.getProperty("user.dir");

        LOG.info("WDSSII GUI VERSION 2.0 [{}, {}, ({} bit)]", new Object[]{name, arch, bits});

        if (bits.equals("32")) {
            LOG.error("Sorry, currently no 64 bit support.\n  You really want to run this on a 64 bit OS");
            LOG.error("You may have 64 and 32 bit Java and be running the 32 version in your path");
            System.exit(0);
        }
        LOG.info("JAVA VERSION {}", System.getProperty("java.specification.version"));
        LOG.info("USER DIRECTORY {}", userdir);
        if (logmessage != null) {
            LOG.info(logmessage);
        }

        // initialize geotools to whatever sf4j bound too.  Bleh a commons
        // subclass wrapper logging to Sl4fj that may be bound to commons..rofl. 
        // Or logback, or log4j...We're using logback right now.
        try {
            org.geotools.util.logging.Logging.GEOTOOLS.setLoggerFactory(
                    "org.geotools.util.logging.Slf4jLoggerFactory");
        } catch (Exception e) {
            LOG.error("Couldn't bind GEOTOOLS logger system to ours " + e.toString());
        }

        // Use the user directory (where we are running) to dynamically
        // add the OS information to the path.  This is where all of our
        // native libraries will be found

        addNativeLibrariesOrDie(userdir);

        Application a = new Application();
        a.start();
    }

    /**
     * Initialize the logback system. If we're bound to it.
     *
     * If SLF4J is bound to logback in the current environment, then we manually
     * assign the logback.xml file. If deployed as a jar, we put the logback.xml
     * in the same directory. I don't want it inside the jar so that it can
     * easily be modified for debugging without having to know how to get it
     * in/out of the jar. Jars assume the classpath is only the jar typically by
     * default.
     *
     * So basically: 1. For deployment there is a user.dir such as
     * "WG2-timestamp" and the logback.xml file will be in this folder with the
     * deployed jar. 2. For development in the IDE the user.dir will be the root
     * IDE folder where I have a debug logback.xml by default.
     */
    public static String initializeLogger() {
        String message = null;

        ILoggerFactory ilog = LoggerFactory.getILoggerFactory();
        if (ilog instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) ilog;

            try {

                // Find the logback.xml file.  Otherwise we're stuck with
                // the default logback output.
                // For jar deployment this will be where the windows.bat, WG2.jar
                // is at.  For IDE running this will be the 'root' folder
                // of the IDE.  I have two logback.xml files, one for deployment
                // in util/run and another for debugging in IDE.
                String dir = System.getProperty("user.dir") + "/logback.xml";
                boolean exists = (new File(dir)).exists();

                // Problem with this is that it causes logging to happen,
                // and we aren't ready yet...
                // URL aURL = W2Config.getURL("logback.xml");
                if (exists) {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(context);
                    // Call context.reset() to clear any previous configuration, e.g. default 
                    // configuration. For multi-step configuration, omit calling context.reset().
                    context.reset();
                    configurator.doConfigure(dir);
                    message = "Logback configuration file " + dir;
                } else {
                    message = "Couldn't find logback configuration file, default logging is on";
                }
            } catch (JoranException je) {
                // StatusPrinter will handle this
            }
            // StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
        return message;
    }

    public static void addNativeLibrariesOrDie(String rootdir) {
        // FIXME: move/cleanup native locations?
        String arch = System.getProperty("os.arch");

        // Newer machines, mac in particular returning
        // x86_86 instead of amd64
        if (arch.equalsIgnoreCase("x86_64")) {
            arch = "amd64";
        }

        rootdir += "/release/modules/lib/" + arch;
        LOG.info("Native library directory is: " + rootdir);
        try {
            addLibraryPath(rootdir);
        } catch (Exception ex) {
            LOG.error("Couldn't add native library path dynamically");
            System.exit(0);
        }
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
