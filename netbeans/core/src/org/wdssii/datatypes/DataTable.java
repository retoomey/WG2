package org.wdssii.datatypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.wdssii.geom.Location;

/**
 * @author lakshman
 *
 */
public class DataTable extends DataType implements Table2DView {

    @Override
    public int getNumCols() {
        return myColumns.size();
    }

    @Override
    public int getNumRows() {
        int rows = 0;
        if (myColumns.size() > 0){
            rows = myColumns.get(0).getNumRows();
        }
        return rows;
    }

    @Override
    public String getRowHeader(int row) {
        return Integer.toString(row);
    }

    @Override
    public String getColHeader(int col) {
        if (col < myColumns.size()) {
            Column c = myColumns.get(col);
            return c.name;
        }
        return "";
    }

    @Override
    public boolean getCellValue(int row, int col, CellQuery output) {
        boolean success = false;
        if (col < myColumns.size()) {
            Column c = myColumns.get(col);
            if (row < c.values.size()) {
                output.text = c.getValues().get(row);
                success = true;
            }
        }
        return success;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col, Location output) {
        return false;
    }

    @Override
    public boolean getCell(Location input, Cell output) {
        return false;
    }

    /** Passed in by builder objects to use to initialize ourselves.
     * This allows us to have final field access from builders.
     */
    public static class DataTableMemento extends DataTypeMemento {

        public ArrayList<Column> columns;
    };

    /** A single column of strings in the DataTable.  Each column has a name and a units */
    public static class Column {

        private final String name;
        private final String unit;
        private final List<String> values;

        public Column(String name, String unit, List<String> values) {
            this.name = name;
            this.unit = unit;
            this.values = values;
        }

        public Column(String name, String unit) {
            this.name = name;
            this.unit = unit;
            this.values = new ArrayList<String>();
        }

        public void addValue(String value) {
            values.add(value);
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public List<String> getValues() {
            return values;
        }

        public int getNumRows() {
            return values.size();
        }
    }
    /** Since Column is an object not a primitive, we don't save much
     * having columns as an array. 
     * @deprecated
     */
    private final Column[] columns;
    /** Store an array list of columns */
    private ArrayList<Column> myColumns = new ArrayList<Column>();

    /** 
     * A tabular product.
     * 
     * @param originLoc  An arbitrary location representative of this table. All items in
     *                   this table are assumed to be at this location unless there is
     *                   a column named Latitude and/or Longitude and/or Height
     * @param startTime  An arbitrary time representative of this table. All items in this
     *                   table are assumed to be at this time.  To include items at different
     *                   times, create different tables.
     * @param typeName
     * @param columns    Provide the populated columns here or start off using the Column(name,unit)
     *                   constructor, then call addRow() multiple times.
     *                   If providing populated columns, make sure they're all the same size!
     */
    //  public DataTable(Location originLoc, Date startTime, String typeName, Column[] columns) {
    //      super(originLoc, startTime, typeName);
    //      this.columns = columns;
    //  }
    // Build from XML
    public DataTable(DataTableMemento memento) {
        super(memento);
        myColumns = memento.columns;
        columns = null; // bleh
    }

    ;
	


    // Old way, lightning at least uses it..will have to modify it...
    //public Column[] getColumns() {
   //     return columns;
  //  }

    // New way, called by GUI
    public ArrayList<Column> getNewColumns() {
        return myColumns;
    }

    /** Make sure to provide the column values in the right order! */
  //  public void addRow(String[] values) {
   //     if (values.length != columns.length) {
  //          throw new IllegalArgumentException("The row should have " + columns.length + " columns");
  //      }
  //      for (int i = 0; i < values.length; ++i) {
  //          columns[i].addValue(values[i]);
  //      }
  //  }
}
