package org.wdssii.gui.commands;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.views.WdssiiView;

/** The root class for all commands in the gui. 
 * Commands are executed by the CommandManager singleton.
 * These are units of code that can be executed by the GUI in scripts, for example.
 * We link RCP gui items to these commands using the WdssiiDynamic handler,
 * but there's nothing preventing us from calling them from somewhere else.
 * 
 * Each command can have a set of parameters in a map form of key--> value
 * which are sent in by RCP.  Most commands have constructors for commands when
 * not called by RCP.
 * 
 * 'Events' in this system are just commands with an empty execute
 * 
 * Any command called directly from the GUI must be in the org.wdssii.gui.commands package.
 * 
 * @author Robert Toomey
 *
 */
public abstract class WdssiiCommand {

    /** Interface for menu choices, used to dynamically generate drop down menus or list */
    public static interface WdssiiMenuList {

        /** This gives the list of suboptions for the command.  For example CreateLLHAreaCommand
         *  might have a list of "Box", "Slice", "Stick" as command options
         *  The RCP GUI uses this typically to dynamically fill in a drop-down menu, or
         *  possibly create a dialog for the user.  They should be sorted as wanted.
         *  
         *  There are two fields, one is the 'visible' item, the second is the text sent
         *  to the command.
         */
        public ArrayList<MenuListItem> getSuboptions();

        /** Get the string of the checked item in a suboptions list */
        public String getCurrentOptionInfo();
    }

    /** Class used by WdssiiMenuList to return item info */
    public static class MenuListItem {

        public String visibleText;  // The 'visible' text to user
        public String commandText;  // The 'command' text sent to command

        public MenuListItem(String v, String c) {
            visibleText = v;
            commandText = c;
        }
    }
    /** The Wdssi view this command was run in, if any */
    protected WdssiiView myWdssiiView = null;
    /** The current state of the command if it's a toggle button */
    private boolean myToggleState = false;
    /** The key to value map for parameters to command */
    protected Map<String, String> myParameters;

    /** Do the stuff this command does.*/
    public abstract boolean execute();

    /** This gives the list of suboptions for the command.  For example CreateLLHAreaCommand
     *  might have a list of "Box", "Slice", "Stick" as command options
     *  The RCP GUI uses this typically to dynamically fill in a drop-down menu, or
     *  possibly create a dialog for the user.  They should be sorted as wanted.
     *  
     *  There are two fields, one is the 'visible' item, the second is the text sent
     *  to the command.
     */
    //public ArrayList<MenuListItem> getSuboptions(){
    //	return null;
    //}
    /** Get the string of the checked item in a suboptions list */
    //public String getCurrentOptionInfo() {
    //	return null;
    //}
    /** Return information for displaying a WJButton for this command */
    public ProductButtonStatus getButtonStatus() {
        return null;
    }

    /** Set the ViewPart this command is being run for.  Commands in a view toolbar will only
     * affect the view they are connected to.
     */
    public void setWdssiiView(WdssiiView view) {
        myWdssiiView = view;
    }

    /** Parameters in the form "Key --> value" */
    public void setParameters(Map<String, String> params) {
        myParameters = params;
        if (params != null) {
            Set<String> keys = params.keySet();
            for (String s : keys) {
                System.out.println("Got param: " + s + " which is " + params);
            }
        }
    }

    /** Set the toggle state BEFORE execution of command.  This should be set to the new
     * value before execution.  Execute can use getToggleState and react based on current value
     * WdssiiDynamic sets this from the old widget state. 
     */
    public final void setToggleState(boolean buttonState) {
        myToggleState = buttonState;
    }

    public final boolean getToggleState() {
        return myToggleState;
    }
}
