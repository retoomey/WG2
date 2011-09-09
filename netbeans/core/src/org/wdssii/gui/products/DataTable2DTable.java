package org.wdssii.gui.products;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Table2DView.CellQuery;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.gui.swing.ProductTableModel;
import org.wdssii.gui.swing.SimpleTable;

/**
 *
 * @author Robert Toomey
 */
public class DataTable2DTable extends Product2DTable {

    // The default 2D table for a product uses the virtual table for
    // really large datasets.  This is default for RadialSets, etc.
    // There's not much to this table other than it is purely virtual and
    // has no O(n) column/row stuff.
    private DataTableModel myTableModel;
    private JTable jDataTableSwingTable;
    
    /** Model that wraps around a DataTable */
    private class DataTableModel extends AbstractTableModel {

        /** The column headers */
        private DataTable myDataTable;
        private CellQuery myBuffer = new CellQuery();

        public DataTableModel(DataTable d) {
            myDataTable = d;
        }

        @Override
        public int getColumnCount() {
            int size = 0;
            if (myDataTable != null) {
                size = myDataTable.getNumCols();
            }
            return size;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myDataTable != null) {
                size = myDataTable.getNumRows();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            if (myDataTable != null) {
                return  myDataTable.getColHeader(column);
            }
            return null;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (myDataTable != null) {
                myDataTable.getCellValue(row, column, myBuffer);
                return myBuffer.text;
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        public void setDataTable(DataTable n) {
            myDataTable = n;
            this.fireTableDataChanged();
        }
    }

    /* Create a brand new table in given scrollpane.  Humm, we might
     * want to save some information...or maybe not.
     */
    @Override
    public void createInScrollPane(JScrollPane scrollPane, Product p) {

        // By now this is loaded or we wouldn't be here.
        DataType dt = p.getRawDataType();
        // Should always be true.  Can't create DataTable2DTable by reflection
        // without DataTable being loaded.
        if (dt instanceof DataTable){ 
            DataTable data = (DataTable)(dt);
        
            // Default is to make a virtual table..
            myTableModel = new DataTableModel(data);
                        //myTableModel.setDataTable(data);

            jDataTableSwingTable = new JTable();
            //jDataTableSwingTable.setupScrollPane(scrollPane);
            jDataTableSwingTable.setModel(myTableModel);
            scrollPane.setViewportView(jDataTableSwingTable);
            scrollPane.revalidate();
            scrollPane.repaint();
            //jProductDataTable.revalidate();
            // jProductDataTable.repaint();
        }
    }

    /** Return a visible grid.  This is used to draw the outline of the
     * displayed table within the 3D world ball view 
     */
    @Override
    public GridVisibleArea getCurrentVisibleGrid() {

        // DataTables are lists of icons usually...what would be an outline?
        return null;
    }
}