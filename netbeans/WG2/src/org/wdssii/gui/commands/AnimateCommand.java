package org.wdssii.gui.commands;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.ProductButtonStatus;

/** The root class of the animation commands.  This command turns on the animation/loop state of the GUI
 */
public class AnimateCommand extends WdssiiCommand {

    boolean myHaveFlag = false;
    boolean myOn = false;

    /** Called by setting the flag explicitly */
    public AnimateCommand(boolean flag) {
        myOn = flag;
        myHaveFlag = true;
    }

    public AnimateCommand() {
        myHaveFlag = false;
    }

    @Override
    public boolean execute() {

        AnimateManager v = CommandManager.getInstance().getVisualCollection();
        if (myHaveFlag) {
            v.setLoopEnabled(myOn);

        } else {
            v.toggleLoopEnabled();

        }
        return true;
    }

    /** Sent by the cancellation of job in progress dialog to notify state change */
    public static class AnimateNotifyCancelCommand extends AnimateCommand {

        @Override
        public boolean execute() {
            return true;
        }
    }

    @Override
    public ProductButtonStatus getButtonStatus() {

        String label, tip, icon;
        icon = "icons/arrow_rotate_clockwise.png";
        boolean useColor = false;
        int red = 0, green = 0, blue = 0;
        ProductButtonStatus status = new ProductButtonStatus();

        tip = "Toggle animation, including time looping (Use Animation View)";

        status.setToolTip(tip);
        status.setIconString(icon);
        status.setEnabled(true);

        useColor = true;
        AnimateManager v = CommandManager.getInstance().getVisualCollection();
        if (v.getLoopEnabled()) {
            red = blue = 0;
            green = 255;
            label = "Stop Loop";

        } else {
            red = 255;
            green = blue = 0;
            label = "Loop";
        }
        status.setButtonText(label);
        if (useColor) {
            status.setColor(red, green, blue);
        }
        return status;
    }
}
