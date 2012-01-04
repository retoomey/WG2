package org.wdssii.gui.commands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import java.util.TreeMap;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.views.CommandListener;

/** The root class for all commands in the gui. 
 * Commands are executed by the CommandManager singleton.
 * These are units of code that can be executed by the GUI in scripts, for example.
 *
 * 'Events' in this system are just commands with an empty execute
 * 
 * Any command called directly from the GUI must be in the org.wdssii.gui.commands package.
 * 
 * @author Robert Toomey
 *
 */
public abstract class WdssiiCommand {

    /** Parameter that is the main option for this command in a list.
    For example, ProductFollowCommand has to tell what product to follow.
     * There will be a key --> value for this in parameters
     */
    public final static String option = "option";

    /** Get the command options.  For example, ProductFollowCommand might
    return 'follow top', 'follow ktlx reflectivity' as options
     */
    public ArrayList<CommandOption> getCommandOptions() {
        return new ArrayList<CommandOption>();
    }

    /** Get the current selected command option */
    public String getSelectedOption() {
        return "";
    }

    public static class CommandItem extends JCheckBoxMenuItem {

        public String option;

        public CommandItem(String text, String o) {
            super(text);
            option = o;
        }
    }

    /** Generate a dynamic popup menu from a given menu list command */
    public static JPopupMenu getSwingMenuFor(WdssiiCommand l) {
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        String current = l.getSelectedOption();
        ArrayList<CommandOption> list = l.getCommandOptions();
        JCheckBoxMenuItem item;
        final WdssiiCommand myCommand = l;
        ActionListener menuAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Using same command for all options...shouldn't have
                // a thread issue (only one command should fire for any
                // given menu in a particular window, for instance)
                Object s = e.getSource();
                if (s instanceof CommandItem) {
                    CommandItem theItem = (CommandItem) (s);
                    myCommand.setParameter(WdssiiCommand.option, theItem.option);
                    System.out.println("Setting command option to " + theItem.option);
                    CommandManager.getInstance().executeCommand(myCommand, true);
                }
            }
        };
        for (CommandOption m : list) {
            item = new CommandItem(m.visibleText, m.commandText);
            menu.add(item);
            group.add(item);
            if (m.commandText.equals(current)) {
                item.setSelected(true);
            }
            item.addActionListener(menuAction);
        }
        return menu;
    }

    /** Class used by WdssiiMenuList to return item info */
    public static class CommandOption {

        public String visibleText;  // The 'visible' text to user
        public String commandText;  // The 'command' text sent to command

        public CommandOption(String v, String c) {
            visibleText = v;
            commandText = c;
        }
    }
    /** The Wdssi view this command was run in, if any */
    protected CommandListener myTargetListener = null;
    /** The current state of the command if it's a toggle button */
    private boolean myToggleState = false;
    /** The key to value map for parameters to command */
    protected Map<String, String> myParameters;

    public WdssiiCommand() {
        myTargetListener = null;
    }

    public WdssiiCommand(CommandListener v) {
        myTargetListener = v;
    }

    /** Do the stuff this command does.*/
    public abstract boolean execute();

    /** Return information for displaying a WJButton for this command */
    public ProductButtonStatus getButtonStatus() {
        return null;
    }

    /** Set the target for this command.  For example, a ProductFollowCommand
     * might affect just one chart view
     */
    public void setTargetListener(CommandListener view) {
        myTargetListener = view;
    }

    /** Parameters in the form "Key --> value" */
    public void setParameters(Map<String, String> params) {
        myParameters = params;
        if (params != null) {
            Set<String> keys = params.keySet();
            // for (String s : keys) {
            //     System.out.println("Got param: " + s + " which is " + params);
            // }
        }
    }

    /** Set a parameter to a command */
    public void setParameter(String key, String value) {
        if (myParameters == null){
            myParameters = new TreeMap<String, String>();
        }
        myParameters.put(key, value);
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
