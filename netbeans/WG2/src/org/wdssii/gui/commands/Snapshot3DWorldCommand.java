package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.views.WorldWindView;

/** Called by name from WdssiiDynamic */
public class Snapshot3DWorldCommand extends WdssiiCommand {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(Snapshot3DWorldCommand.class);

    @Override
    public boolean execute() {

        if (myTargetListener != null) {
            if (myTargetListener instanceof WorldWindView) {
                WorldWindView e = (WorldWindView) (myTargetListener);
                e.takeDialogSnapshot();
            }
        }
        return false;
    }
}