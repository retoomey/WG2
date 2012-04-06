package org.wdssii.gui.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;

public class SourceConnectCommand extends SourceCommand {

    private static Logger log = LoggerFactory.getLogger(SourceConnectCommand.class);
    /** Number of times to try a connection before giving up */
    // @todo this could be a preference
    public final static int NUMBER_OF_TRIES = 5;

    /** Called by other code */
    public SourceConnectCommand(String key) {
        setSourceKey(key);
    }

    @Override
    public boolean execute() {

        final String key = getSourceKey();
        final WdssiiCommand update = this;
        if (key != null) {
            final Source s = SourceList.theSources.getSource(key);
            final String nice = s.getVisibleName();
            //final String nice = SourceManager.getInstance().getNiceShortName(key);
            log.info("Connection attempt being made to source '" + nice + "' (" + key + ")");

            // This will make it so that our GUI will show 'connecting' icons, etc.
           // aboutToConnect(key, true);

            WdssiiJob job = new WdssiiJob("Connecting to '" + nice + "'") {

                @Override
                public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
                                      
                    // We'll try connecting a few times.
                    final int steps = NUMBER_OF_TRIES;
                    boolean success = false;
                    monitor.beginTask("Connecting...", steps); // IProgressMonitor.UNKNOWN
                    for (int i = 1; i <= steps; i++) {
                        
                         // Have to do it here, the connect routine turns off
                        // the connect flag....
                        aboutToConnect(key, true);
                        CommandManager.getInstance().fireUpdate(update);
                        
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
                    aboutToConnect(key, false);
                    
                    // Update GUI (manually do the fire event thing of command manager)
                    // Changes connecting icon to connected (for example)
                    CommandManager.getInstance().fireUpdate(update);

                    // Write to a tab on the output view......
                   // OutputWriter io = IOProvider.getDefault().getIO(nice, true).getOut();
                   // io.print("Success was " + success);
                    return WdssiiJobStatus.OK_STATUS;
                }
            };

            //  job.setUser(true);  Don't bug user with pop up dialog, it will show on bottom anyway
            job.schedule();
        }
        return true; // Update to get the 'connecting' status
    }
}
