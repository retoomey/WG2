package org.wdssii.gui.renderers;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.wdssii.gui.symbology.SymbolFactory;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Our custom renderer for drawing a java Symbol in a table cell using its
 * Icon interface
 */
public class SymbolCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean cellHasFocus, int row, int col) {

        // Let super set all the defaults...
        super.getTableCellRendererComponent(table, "",
                isSelected, cellHasFocus, row, col);

        if (value instanceof Symbol) {
            Symbol s = (Symbol) value;
            SymbolRenderer render = SymbolFactory.getSymbolRenderer(s);
            render.setSymbol(s);
            if (render instanceof Icon) {
                setIcon((Icon) (render));
            } else {
                setText("?");
            }
        }
        return this;
    }
}