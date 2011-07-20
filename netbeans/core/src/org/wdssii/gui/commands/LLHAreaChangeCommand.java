package org.wdssii.gui.commands;

import org.wdssii.gui.LLHAreaManager;

/**
 * A command that represents a change in an LLHArea object state
 * 
 * @author Robert Toomey
 */
public class LLHAreaChangeCommand extends LLHAreaCommand {

    /** Change state of product 'only' state */
    public static class LLHAreaOnlyCommand extends LLHAreaChangeCommand {

        /** The key of the LLHArea we will set only for */
        private String myLLHAreaKey;
        /** The value we will set it to */
        private boolean myFlag;

        public LLHAreaOnlyCommand(String productKeyName, boolean flag) {
            myLLHAreaKey = productKeyName;
            myFlag = flag;
        }

        @Override
        public boolean execute() {
            LLHAreaManager.getInstance().setOnlyMode(myLLHAreaKey, myFlag);
            return true;
        }
    }

    /** Change state of product 'visible' state */
    public static class LLHAreaVisibleCommand extends LLHAreaChangeCommand {

        /** The key of the LLHArea we will set visible for */
        private String myLLHAreaKey;
        /** The value we will set it to */
        private boolean myFlag;

        public LLHAreaVisibleCommand(String productKeyName, boolean flag) {
            myLLHAreaKey = productKeyName;
            myFlag = flag;
        }

        @Override
        public boolean execute() {
            LLHAreaManager.getInstance().setVisibleVolume(myLLHAreaKey, myFlag);
            return true;
        }
    }

    @Override
    public boolean execute() {
        return true;
    }
}
