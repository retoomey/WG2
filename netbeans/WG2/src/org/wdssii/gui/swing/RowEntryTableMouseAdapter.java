package org.wdssii.gui.swing;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

/**
 *  A MouseAdapter for a table using a RowEntryTableModel.  Handles
 * standard stuff like a popup menu and figuring out model object per
 * table line.
 *
 * @author Robert Toomey
 */
/** A mouse adapter that assume we are using a model where there is
 * a single object per line of the table
 */
public class RowEntryTableMouseAdapter extends MouseAdapter {

    /** The table we're handling clicks for */
    private final RowEntryTable myTable;
    /** The model. */
    private final RowEntryTableModel myModel;

    public RowEntryTableMouseAdapter(RowEntryTable table, RowEntryTableModel model) {
        myTable = table;
        myModel = model;  // Guess we 'could' pull model from table
    }

    /** Get the menu items for our popup, if any */
    public JMenuItem[] getMenuItems() {
        return null;
    }

    public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {
        return null;
    }

    /** Handle click on object at row and column.  This is used for
    special columns such as 'visible' or 'only' toggles, etc.
     */
    public void handleClick(Object line, int row, int column) {
    }
    
    /** Handle click on object at row and column.  This is used for
    special columns such as 'visible' or 'only' toggles, etc.
     */
    public void handleDoubleClick(Object line, int row, int column) {
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getComponent().isEnabled()
                && e.getButton() == MouseEvent.BUTTON1/* && e.getClickCount() == 1*/) {
            Point p = e.getPoint();
            int row = myTable.rowAtPoint(p);
            int column = myTable.columnAtPoint(p);

            if ((row > -1) && (column > -1)) {
                int orgColumn = myTable.convertColumnIndexToModel(column);
                int orgRow = myTable.convertRowIndexToModel(row);
                Object stuff = myModel.getValueAt(orgRow, orgColumn);
                if (e.getClickCount() == 1){
                     handleClick(stuff, orgRow, orgColumn);
                }else{
                    handleDoubleClick(stuff, orgRow, orgColumn);
                }
            }
        }
    }

    /** Select the table row under mouse */
    public void selectRowUnderPoint(Point p) {

        // get the row index that contains that coordinate
        int rowNumber = myTable.rowAtPoint(p);

        // Get the ListSelectionModel of the JTable
        ListSelectionModel model = myTable.getSelectionModel();

        // set the selected interval of rows. Using the "rowNumber"
        // variable for the beginning and end selects only that one row.
        model.setSelectionInterval(rowNumber, rowNumber);
    }

    @Override
    public void mousePressed(MouseEvent e) {

        // On right down, we select that row...so that any popupmenu
        // will refer to the row under the mouse....
        if (e.getComponent().isEnabled()
                && e.getButton() == MouseEvent.BUTTON3) {
            selectRowUnderPoint(e.getPoint());
        }
        showPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Why again?  We did it above in mousePressed.  The reason is that
        // if user right clicks twice, the second mousePress will get rid of the
        // popup menu and then only this gets called (The popup takes the
        // mousePressed event).  We want the line to
        // change and a new popup to form over the new line.
        if (e.getComponent().isEnabled()
                && e.getButton() == MouseEvent.BUTTON3) {
            selectRowUnderPoint(e.getPoint());
        }
        showPopup(e);
    }

    /** Show the popup menu when the trigger exists.  Guess the trigger is OS
     * dependent.
     * 
     * @param e 
     */
    private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {

            Point p = e.getPoint();
            int row = myTable.rowAtPoint(p);
            int column = myTable.columnAtPoint(p);

            if ((row > -1) && (column > -1)) {
                int orgColumn = myTable.convertColumnIndexToModel(column);
                int orgRow = myTable.convertRowIndexToModel(row);
                Object stuff = myModel.getValueAt(orgRow, orgColumn);

                JPopupMenu popupmenu = getDynamicPopupMenu(stuff, orgRow, orgColumn);
                if (popupmenu != null){
                    popupmenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }
}
