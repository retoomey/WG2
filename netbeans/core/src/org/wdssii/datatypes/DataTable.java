package org.wdssii.datatypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.wdssii.geom.Location;

/**
 * @author lakshman
 *
 */
public class DataTable extends DataType {

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

        /** copies from the master. The master can change without affecting this object. */
        public Column(Column master) {
            this.name = master.name;
            this.unit = master.unit;
            this.values = new ArrayList<String>(master.values); // String is immutable
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
    public DataTable(Location originLoc, Date startTime, String typeName, Column[] columns) {
        super(originLoc, startTime, typeName);
        this.columns = columns;
    }

    // Build from XML
    public DataTable(DataTypeMemento header, ArrayList<Column> columns) {
        super(header.originLocation, header.startTime, header.typeName);
        this.columns = null;
        this.setAttributes(header.attriNameToValue);
        this.setUnitsForAttributes(header.attriNameToUnits);
        this.myColumns = columns;
    }

    ;
	
	/**
	 * copies all the attributes, etc. from the master. The master can change
	 * without affecting this object
	 */
	public DataTable(DataTable master) {
        super(master);
        this.columns = new Column[master.columns.length];
        for (int i = 0; i < columns.length; ++i) {
            this.columns[i] = new Column(master.columns[i]);
        }
    }

    // Old way, lightning at least uses it..will have to modify it...
    public Column[] getColumns() {
        return columns;
    }

    // New way, called by GUI
    public ArrayList<Column> getNewColumns() {
        return myColumns;
    }

    /** Make sure to provide the column values in the right order! */
    public void addRow(String[] values) {
        if (values.length != columns.length) {
            throw new IllegalArgumentException("The row should have " + columns.length + " columns");
        }
        for (int i = 0; i < values.length; ++i) {
            columns[i].addValue(values[i]);
        }
    }

    public int getNumRows() {
        return (columns.length > 0) ? columns[0].getNumRows() : 0;
    }
}
