package org.wdssii.gui.commands;

import org.wdssii.core.WdssiiCommand;
import org.wdssii.core.CommandListener;

/** Toggle a flag that says if we're using a regular volume or a virtual one
 * The chart view and LLHArea views use this for which volume to follow
 * @author Robert Toomey
 */
public class VolumeSetTypeCommand extends WdssiiCommand {

    /** Interface for a view following a virtual/regular volume toggle */
    public static interface VolumeTypeFollowerView extends CommandListener {

        /** Set the volume type */
        void setUseVirtualVolume(boolean useVirtual);

        /** Get the current volume type */
        boolean getUseVirtualVolume();
    }

    public VolumeSetTypeCommand(CommandListener v, boolean newState){
        myTargetListener = v;
        setToggleState(newState);
    }
    
    @Override
    public boolean execute() {
        // We are a toggle command...look for current value:
        boolean current = getToggleState();

        // Need the view in order to send the command...
        if (myTargetListener != null) {
            if (myTargetListener instanceof VolumeTypeFollowerView) {
                ((VolumeTypeFollowerView) myTargetListener).setUseVirtualVolume(current);
            }
        }
        return true;
    }
}
