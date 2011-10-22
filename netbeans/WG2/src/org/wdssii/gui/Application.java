package org.wdssii.gui;

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

    public static void main(String[] args) {
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
}
