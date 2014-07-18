package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/** Called by name from WdssiiDynamic */
public class Snapshot3DWorldCommand extends WdssiiCommand {

    @SuppressWarnings("unused")
    private final static Logger LOG = LoggerFactory.getLogger(Snapshot3DWorldCommand.class);

    @Override
    public boolean execute() {

        if (myTargetListener != null) {
            // FIXME: MULTIVIEW
            //if (myTargetListener instanceof WorldWindView) {
            //    WorldWindView e = (WorldWindView) (myTargetListener);
            //    e.takeDialogSnapshot();
            //}
        }
        return false;
    }
}