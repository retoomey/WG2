package org.wdssii.gui.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.widgets.FileDialog;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.PlatformUI;
//import org.wdssii.gui.views.ChartView;
//import org.wdssii.storage.DataManager;

public class ChartSaveSnapshotCommand extends WdssiiCommand {

    private static Logger log = LoggerFactory.getLogger(ChartSaveSnapshotCommand.class);

    /** Take snapshot of the current chart, saves as a png file using the JFreeChart library */
    @Override
    public boolean execute() {

        /*DataManager.getInstance().printData();
        myWdssiiView = null;
        
        if (myWdssiiView != null){
        if (myWdssiiView instanceof ChartView){
        ChartView e = (ChartView)(myWdssiiView);
        String name ="chartsnapshot.png";
        
        // File Dialog part -------------------------------------------------------------------------------
        // Probably will need a util that sets this up for us in the proper eclipse rcp way
        IWorkbench workbench = PlatformUI.getWorkbench();
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        FileDialog fd = new FileDialog(shell, SWT.SAVE);
        fd.setText("Save Chart Snapshot");
        fd.setFilterPath("D:/");  // FIXME: linux?
        fd.setFileName(name);
        String[] filterExt = { "*.png", "*.*" };
        fd.setFilterExtensions(filterExt);
        name = fd.open();
        // ------------------------------------------------------------------------------------------------
        
        if (name != null){
        e.takeSnapshot(name);
        log.info("Took snapshot of chart '"+name+"'");
        }
        }
        }*/
        return false; // No gui update on snapshot needed
    }
}
