package org.wdssii.gui.commands;

import org.wdssii.gui.views.WdssiiView;

/** Toggle a flag that says if we're using a regular volume or a virtual one
 * The chart view and LLHArea views use this for which volume to follow
 * @author Robert Toomey
 */
public class ProductToggleFilterCommand extends ProductChangeCommand {

    /** Interface for a view following a use filter toggle */
    public static interface ProductFilterFollowerView extends WdssiiView {

        /** Set the use filter */
        void setUseFilter(boolean useFilter);

        /** Get the current use filter  */
        boolean getUseFilter();
    }

    @Override
    public boolean execute() {
        // We are a toggle command...look for current value:
        boolean current = getToggleState();

        if (myWdssiiView != null) {
            if (myWdssiiView instanceof ProductFilterFollowerView) {
                ((ProductFilterFollowerView) myWdssiiView).setUseFilter(current);
            }
        }
        return true;
    }
}