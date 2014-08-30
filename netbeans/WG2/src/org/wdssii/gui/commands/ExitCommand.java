package org.wdssii.gui.commands;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.wdssii.core.CommandManager;
import org.wdssii.core.WdssiiCommand;

/**
 * Sent by application exit...
 *
 * @author Robert Toomey
 */
public class ExitCommand extends WdssiiCommand {

    @Override
    public boolean execute() {
        int result = JOptionPane.showConfirmDialog(getRootComponent(), "Save changes to document before exiting?", "Exit WG2 Application", JOptionPane.YES_NO_CANCEL_OPTION);
        switch (result) {
            case JOptionPane.YES_OPTION:
                SaveCommand c = new SaveCommand(false);
                c.setConfirmReport(true, true, getRootComponent());
                CommandManager.getInstance().executeCommand(c, true);
                if (c.myReturn == JFileChooser.APPROVE_OPTION) {
                    System.exit(0);
                }
                break;
            case JOptionPane.NO_OPTION:
                System.exit(0);
            case JOptionPane.CANCEL_OPTION:
                return true;
        }
        return true;
    }
}
