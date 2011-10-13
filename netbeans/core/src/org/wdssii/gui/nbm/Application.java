package org.wdssii.gui.nbm;

import java.awt.Frame;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.WindowManager;
import org.wdssii.core.WDSSII;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.JobManager;
import org.wdssii.gui.PreferencesManager;
import org.wdssii.storage.DataManager;

/**
 * Code called when wdssii module loaded/unloaded.
 * 
 * @author Robert Toomey
 */
public class Application extends ModuleInstall {

    private static Log log = LogFactory.getLog(Application.class);

    @Override
    public void installed() {
        System.out.println("WDSSII Module installed");
    }

    @Override
    public void restored() {
        
        //To remove the annoying error alert on the right-corner
       // System.setProperty("netbeans.exception.alert.min.level", "99999");
        //To remove the annoying error dialog box
        //System.setProperty("netbeans.exception.report.min.level", "99999"); 
        
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
        PreferencesManager.introduce(new NBPrefHandler());

        // Make any swing GUI items use native look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // GTK might need this line according to eclipse docs if above doesn't work..
            // Currently coding in windows so can't test it yet.
            // FIXME: test and make sure look and feel is correct in linux/mac/etc.
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            // Ignore it..not that huge a deal.
        }

        // Fixme: probably need a log4j appender to wrap the output console
        InputOutput io = IOProvider.getDefault().getIO("WDSSII", true);
        io.getOut().println("WDSSII GUI VERSION 2.0");
        
        // Only change the window title when all the UI components are fully loaded.
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                Frame w = WindowManager.getDefault().getMainWindow();
                if (w != null){
                 // String currentTitle = w.getTitle();
                  String dir = DataManager.getInstance().getRootTempDir();
                 // String newTitle = currentTitle.replaceAll("tempdir", "["+dir+"]");
                  String newTitle = "WDSSII GUI 2.0 "+dir;
                  w.setTitle(newTitle);
                }
            }
        });
    }

    public boolean closing() {
        // Ask the user to save any open, modified clipboard contents.
        // If the user selects "Cancel" on one of these dialogs, don't exit yet!
        System.out.println("Closing called on WDSSII MODULE");
        return true;
    }
}
