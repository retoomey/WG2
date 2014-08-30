package org.wdssii.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import org.wdssii.core.CommandManager;
import org.wdssii.core.WdssiiCommand;

/**
 * Create Swing GUI items for WdssiiCommand
 *
 * @author Robert Toomey
 */
public class WdssiiCommandGUI {

    public static class CommandMenuItem extends JMenuItem implements WdssiiCommand.CommandItem {

        public String option;

        public CommandMenuItem(String text, String o) {
            super(text);
            option = o;
        }

        @Override
        public String getOption() {
            return option;
        }
    }

    public static class CommandCheckBoxMenuItem extends JCheckBoxMenuItem implements WdssiiCommand.CommandItem {

        public String option;

        public CommandCheckBoxMenuItem(String text, String o) {
            super(text);
            option = o;
        }

        @Override
        public String getOption() {
            return option;
        }
    }

    public static void fillMenuFor(JPopupMenu menu, WdssiiCommand l) {
        menu.removeAll();
        ArrayList<WdssiiCommand.CommandOption> list = l.getCommandOptions();
        final WdssiiCommand myCommand = l;
        ActionListener menuAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Using same command for all options...shouldn't have
                // a thread issue (only one command should fire for any
                // given menu in a particular window, for instance)
                Object s = e.getSource();
                if (s instanceof WdssiiCommand.CommandItem) {
                    WdssiiCommand.CommandItem theItem = (WdssiiCommand.CommandItem) (s);
                    myCommand.setParameter(WdssiiCommand.option, theItem.getOption());
                    CommandManager.getInstance().executeCommand(myCommand, true);
                }
            }
        };
        for (WdssiiCommand.CommandOption m : list) {
            // item = new CommandMenuItem(m.visibleText, m.commandText);
            if (m.visibleText.isEmpty()) {
                menu.add(new JSeparator());
            } else {
                CommandMenuItem item = new CommandMenuItem(m.visibleText, m.commandText);
                // JMenuItem item = (JMenuItem) i.createWidget(m.visibleText);
                menu.add(item);
                item.addActionListener(menuAction);
            }
        }
    }

    /**
     * Generate a dynamic popup menu from a given menu list command
     */
    public static JPopupMenu getSwingMenuFor(WdssiiCommand l) {
        JPopupMenu menu = new JPopupMenu();
        fillMenuFor(menu, l);
        return menu;
    }

    public static void fillCheckMenuFor(JPopupMenu menu, WdssiiCommand l) {
        menu.removeAll();
        ButtonGroup group = new ButtonGroup();
        String current = l.getSelectedOption();
        ArrayList<WdssiiCommand.CommandOption> list = l.getCommandOptions();
        final WdssiiCommand myCommand = l;
        ActionListener menuAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Using same command for all options...shouldn't have
                // a thread issue (only one command should fire for any
                // given menu in a particular window, for instance)
                Object s = e.getSource();
                if (s instanceof WdssiiCommand.CommandItem) {
                    WdssiiCommand.CommandItem theItem = (WdssiiCommand.CommandItem) (s);
                    myCommand.setParameter(WdssiiCommand.option, theItem.getOption());
                    CommandManager.getInstance().executeCommand(myCommand, true);
                }
            }
        };
        for (WdssiiCommand.CommandOption m : list) {

            CommandCheckBoxMenuItem item = new CommandCheckBoxMenuItem(m.visibleText, m.commandText);
            menu.add(item);
            group.add(item);
            if (m.commandText.equals(current)) {
                item.setSelected(true);
            }
            item.addActionListener(menuAction);
        }
    }

    /**
     * Generate a dynamic popup menu from a given menu list command
     */
    public static JPopupMenu getSwingCheckMenuFor(WdssiiCommand l) {
        JPopupMenu menu = new JPopupMenu();
        fillCheckMenuFor(menu, l);
        return menu;
    }
}
