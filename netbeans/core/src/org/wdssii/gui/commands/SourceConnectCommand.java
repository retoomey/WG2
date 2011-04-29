package org.wdssii.gui.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.windows.IOProvider;
import org.openide.windows.OutputWriter;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.SourceManager.SourceCommand;

public class SourceConnectCommand extends SourceCommand {

    private static Log log = LogFactory.getLog(SourceConnectCommand.class);
    /** Number of times to try a connection before giving up */
    // @todo this could be a preference
    public final static int NUMBER_OF_TRIES = 5;

    @Override
    public boolean execute() {

        final String key = getIndexName();
        final WdssiiCommand update = this;
        if (key != null) {
            final String nice = SourceManager.getInstance().getNiceShortName(key);
            log.info("Connection attempt being made to source '" + nice + "' (" + key + ")");
            //PlatformUI.getWorkbench().getProgressService().showInDialog(shell, job);

            // This will make it so that our GUI will show 'connecting' icons, etc.
            aboutToConnect(key);

            WdssiiJob job = new WdssiiJob("Connecting to '" + nice + "'") {

                @Override
                public WdssiiJobStatus run(WdssiiJobMonitor monitor) {

                    // We'll try connecting a few times.
                    final int steps = NUMBER_OF_TRIES;
                    boolean success = false;
                    monitor.beginTask("Connecting...", steps); // IProgressMonitor.UNKNOWN
                    for (int i = 1; i <= steps; i++) {
                        monitor.subTask("Connecting to " + nice + " Attempt " + i);
                        try {
                            if (connect(key)) {
                                success = true;
                                break;
                            } else {
                                Thread.sleep(2000);
                            }

                        } catch (Exception e) {
                            log.warn("Exception trying to connect to source " + e.toString());
                        }

                        // ------------------------------------------
                        monitor.worked(1);
                        // Allow canceling...
                        // @todo doesn't work...add cancel ability to nbjob
                        if (monitor.isCanceled()) {
                            break;
                        }
                    }
                    monitor.done();

                    // Update GUI (manually do the fire event thing of command manager)
                    // Changes connecting icon to connected (for example)
                    if (success) {
                        try {
                            CommandManager.getInstance().fireUpdate(update);
                        } catch (Exception e) {
                            log.error("Exception during connection.  Hope it's the concurrent one");
                        }
                    }
                    // Write to a tab on the output view......
                    OutputWriter io = IOProvider.getDefault().getIO(nice, true).getOut();
                    io.print("Success was " + success);
                    return WdssiiJobStatus.OK_STATUS;
                }
            };

            //  job.setUser(true);  Don't bug user with pop up dialog, it will show on bottom anyway
            job.schedule();
        }
        return true; // Update to get the 'connecting' status
    }
}
