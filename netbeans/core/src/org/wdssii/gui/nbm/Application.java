package org.wdssii.gui.nbm;

import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.wdssii.core.WDSSII;
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
        // Start up WDSSII base here ?
        log.info("WDSSII GUI VERSION 2.0 ----------------------------------------------------------");
        log.info("Startup: JAVA VERSION   = " + System.getProperty("java.specification.version"));
        log.info("Startup: USER DIRECTORY = " + System.getProperty("user.dir"));
        DataManager.getInstance();
        
        WDSSII.getInstance();
        
        // Gonna need netbeans versions of these....
        // FIXME: need a job handler class or nothing will work,
        // this needs to wrap around the netbeans job stack.
        //WdssiiJob.introduce(new EclipseJob.EclipseJobFactory());
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
    }

    public boolean closing() {
        // Ask the user to save any open, modified clipboard contents.
        // If the user selects "Cancel" on one of these dialogs, don't exit yet!
        System.out.println("Closing called on WDSSII MODULE");
        return true;
    }
}
