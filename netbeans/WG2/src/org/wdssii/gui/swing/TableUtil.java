package org.wdssii.gui.swing;

import java.awt.Component;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A table that allows icon headers that we use a lot for
 * layers, navigation lists, etc...
 * 
 * 
 * @author Robert Toomey
 */
public class TableUtil {

    /** Code taken from open source example on web:
     * http://tips4java.wordpress.com/2009/02/27/default-table-header-cell-renderer/
     * 
     * This might move into IconFactory
     */
    public static Icon getIcon(JTable table, int column) {
        SortKey sortKey = getSortKey(table, column);
        if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
            switch (sortKey.getSortOrder()) {
                case ASCENDING:
                    return UIManager.getIcon("Table.ascendingSortIcon");
                case DESCENDING:
                    return UIManager.getIcon("Table.descendingSortIcon");
            }
        }
        return null;
    }

    /** For 'icon' columns we add a text + or - for sorting direction */
    public static String getSortText(JTable table, int column) {
        SortKey sortKey = getSortKey(table, column);
        if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
            switch (sortKey.getSortOrder()) {
                case ASCENDING:
                    return "-";
                case DESCENDING:
                    return "+";
            }
        }
        return "";
    }

    public static SortKey getSortKey(JTable table, int column) {
        RowSorter rowSorter = table.getRowSorter();
        if (rowSorter == null) {
            return null;
        }

        List sortedColumns = rowSorter.getSortKeys();
        if (!sortedColumns.isEmpty()) {
            return (SortKey) sortedColumns.get(0);
        }
        return null;
    }

    /** A renderer that shows an icon in a table header.  The very
     * annoying issue is that the theme/UI of java doesn't let us snag
     * the default behavior in any easy way I can yet see.  
     * By drawing an icon we have bypassed any L and F. Bad design, IMO
     * 
     * So to make it look consistent can replace ALL table headers with
     * our personal one, I guess.
     */
    public static class IconHeaderRenderer extends DefaultTableCellRenderer {

        public static class IconHeaderInfo {

            private Icon icon;
            private String text;
            private String iconName;

            public Icon getIcon() {
                return icon;
            }

            public void setIcon(Icon i) {
                icon = i;
            }

            public String getText() {
                return text;
            }

            public void setText(String s) {
                text = s;
            }

            public void update() {
                if (icon == null) {
                    setIcon(SwingIconFactory.getIconByName(iconName));
                }
            }

            public IconHeaderInfo(String iName) {
                iconName = iName;
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // First, let super set defaults...
            super.getTableCellRendererComponent(table, "", isSelected, cellHasFocus, row, col);

            // Basic settings for a 'header'
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(BOTTOM);
            setHorizontalTextPosition(LEFT);
            setOpaque(false);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));

            // Draw our stuff..
            if (value instanceof IconHeaderInfo) {
                IconHeaderInfo info = (IconHeaderInfo) (value);
                info.update();
                if (info.icon != null) {
                    setIcon(info.icon);
                }
                setText(getSortText(table, col));
            } else {
                setText((String) (value));
                setIcon(TableUtil.getIcon(table, col));
            }
            return this;
        }
    }

    /** A table I'll end up using for all tables in the display I think
     * in order to be consistent
     */
    public static class WG2Table extends JTable {
        
    }
    
    /** A renderer with a bit more power */
    public static class WG2TableCellRenderer extends DefaultTableCellRenderer {

        /** A shared JCheckBox for rendering every check box in the list */
        private JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {
            setIcon(null);
            return super.getTableCellRendererComponent(table, value, isSelected, cellHasFocus, row, col);
        }

        /** Render this cell as a checkbox */
        public Component getJCheckBox(JTable table, boolean checked,
                boolean isSelected, boolean cellHasFocus, int row, int col) {
            // We have to make sure we set EVERYTHING we use everytime,
            // since we are just using a single checkbox.        
            checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            checkbox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            checkbox.setEnabled(isEnabled());

            checkbox.setSelected(checked);

            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
                    : noFocusBorder);
            return checkbox;
        }

        /** Render this cell as an icon that toggles two pictures by state */
        public Component getJCheckBoxIcon(JTable table, boolean checked,
                String iconOn, String iconOff,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Superclass is a JLabel, set the defaults, no text though
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);
            Icon i = null;
            if (checked) {
                i = SwingIconFactory.getIconByName(iconOn);
            } else {
                i = SwingIconFactory.getIconByName(iconOff);
            }
            setIcon(i);
            return this;
        }
        
        /** Render this cell as an icon */
        public Component getIcon(JTable table, String iconName,   
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Superclass is a JLabel, set the defaults, no text though
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);
            Icon i = SwingIconFactory.getIconByName(iconName);
            setIcon(i);
            return this;
        }
    }
}
