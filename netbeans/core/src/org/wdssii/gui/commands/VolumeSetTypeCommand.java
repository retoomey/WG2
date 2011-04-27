package org.wdssii.gui.commands;

import org.wdssii.gui.views.WdssiiView;

/** Toggle a flag that says if we're using a regular volume or a virtual one
 * The chart view and LLHArea views use this for which volume to follow
 * @author Robert Toomey
 */
public class VolumeSetTypeCommand extends WdssiiCommand {

    /** Interface for a view following a virtual/regular volume toggle */
    public static interface VolumeTypeFollowerView extends WdssiiView {

        /** Set the volume type */
        void setUseVirtualVolume(boolean useVirtual);

        /** Get the current volume type */
        boolean getUseVirtualVolume();
    }

    @Override
    public boolean execute() {
        // We are a toggle command...look for current value:
        boolean current = getToggleState();

        // Get the parameter out of us.  Should be "wdssii.ChartSetTypeParameter"
        //if (myParameters != null){
        //String value = myParameters.get("wdssii.ChartSetTypeParameter");

        // Null choice currently means button was picked..should bring up dialog..
        //if (value != null){
        // Need the view in order to send the command...
        if (myWdssiiView != null) {
            if (myWdssiiView instanceof VolumeTypeFollowerView) {
                ((VolumeTypeFollowerView) myWdssiiView).setUseVirtualVolume(current);
            }
        }
        //}
        //}else{
        //System.out.println("EXECUTE ChartSetType without any params (FIXME: Dialog for user)");

        // Ok this is the 'top' button..not the drop menu...so bring up a dialog or do nothing?
        // Need the view though...
        //}
        return true;
    }
}
