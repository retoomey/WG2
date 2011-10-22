package org.wdssii.gui.swing;

import javax.swing.JTable;

/**
 *  Default table I use for the RowEntryTableModel, adds some convenience 
 * functions.
 *  
 * @author Robert Toomey
 */
public class RowEntryTable extends JTable {

    public RowEntryTable() {
    }
    
    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        // Disable cntl and shift behavior.  We want single row always
        // selected
        toggle = extend = false;
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }
}
