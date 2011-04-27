package org.wdssii.gui.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.SourceManager.SourceCommand;

public class SourceConnectCommand extends SourceCommand {

    private static Log log = LogFactory.getLog(SourceConnectCommand.class);
    /** Number of times to try a connection before giving up */
    public final static int NUMBER_OF_TRIES = 3000;

    @Override
    public boolean execute() {

        final String key = getIndexName();
        final WdssiiCommand update = this;

        if (key != null) {
            String nice = SourceManager.getInstance().getNiceShortName(key);
            log.info("Connection attempt being made to source '" + nice + "' (" + key + ")");
            //PlatformUI.getWorkbench().getProgressService().showInDialog(shell, job);

            // This will make it so that our GUI will show 'connecting' icons, etc.
            aboutToConnect(key);

            WdssiiJob job = new WdssiiJob("Connecting to '" + nice + "'") {

                @Override
                public WdssiiJobStatus run(WdssiiJobMonitor monitor) {

                    // We'll try connecting a few times.
                    final int steps = NUMBER_OF_TRIES;
                    monitor.beginTask("Connecting...", steps); // IProgressMonitor.UNKNOWN
                    for (int i = 0; i < steps; i++) {
                        monitor.subTask("attempting connection (" + i + ")");

                        try {
                            // Connect on last step....
                            if (i == steps - 1) {
                                if (connect(key)) {
                                    monitor.worked(1);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Exception trying to connect to source " + e.toString());
                        }


                        // ------------------------------------------
                        monitor.worked(1);
                        // Allow canceling...
                        if (monitor.isCanceled()) {
                            break;
                        }
                    }

                    // Test GUI response to slow connection....
                    //try {
                    //	Thread.sleep(1000);
                    //	} catch (InterruptedException e) {
                    //}

                    monitor.done();

                    // Update GUI (manually do the fire event thing of command manager)
                    // Changes connecting icon to connected (for example)
                    try {
                        CommandManager.getInstance().fireUpdate(update);
                    } catch (Exception e) {
                        log.error("Exception during connection.  Hope it's the concurrent one");
                        e.printStackTrace();
                    }


                    return WdssiiJobStatus.OK_STATUS;
                }
            };

            //  job.setUser(true);  Don't bug user with pop up dialog, it will show on bottom anyway
            job.schedule();
        }
        return true; // Update to get the 'connecting' status
    }
}
