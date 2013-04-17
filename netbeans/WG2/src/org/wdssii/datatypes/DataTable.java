package org.wdssii.datatypes;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.wdssii.geom.Location;
import org.wdssii.core.GridVisibleArea;

/**
 * @author lakshman
 *
 */
public class DataTable extends DataType implements Table2DView {
   
    /** List of locations gathers from the rows of data */
    private ArrayList<Location> myLocations;
        
    /** Store an array list of columns */
    private ArrayList<Column> myColumns = new ArrayList<Column>();
    
    @Override
    public int getNumCols() {
        return myColumns.size();
    }

    @Override
    public int getNumRows() {
        int rows = 0;
        if (!myColumns.isEmpty()){
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
    public boolean getCell(Location input, CellQuery output) {
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
        
        public Iterator getIterator(){
            return values.iterator();
        }
        
        /** Get column value as a string */
        public String getValue(int row){
            return values.get(row);
        }
        
        /** Get column value as a float */
        public float getFloat(int row){
            float v = DataType.MissingData;
            String s = values.get(row);
            try{
                v = Float.parseFloat(s);
            }catch(NumberFormatException e){
                
            }
            return v;
        }

        public int getNumRows() {
            return values.size();
        }
    }


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
        updateLocations();
    }

    public ArrayList<Location> getLocations(){
        return myLocations;
    }
    
    public Location getLocation(int row){
        return myLocations.get(row);
    }
    
    // New way, called by GUI
    public ArrayList<Column> getNewColumns() {
        return myColumns;
    }
    
    public Column getColumnByName(String name){
        Column found = null;
        for(Column c:myColumns){   // O(n)
            if (c.name.equals(name)){
                found = c;
                break;
            }
        }
        return found;
    }

    /** Try to create a location array from the columns of data.
     * Currently pretty fragile, no handling of missing data, etc...
     */
    public final void updateLocations(){
        
        try{
        myLocations = new ArrayList<Location>();
        // Ok, should be three columns.  Latitude, Longitude, Height
        // For the moment, to have icons we HAVE to have the 3 columns
        Column f1 = getColumnByName("Latitude");
        if (f1 != null){
            Column f2 = getColumnByName("Longitude");
            if (f2 != null){
                Column f3 = getColumnByName("Height");
                List<String> heights = null;
                boolean haveHeights = false;
                if (f3 != null){ 
                    heights = f3.values;
                    haveHeights = true;
                }
                
                // Would be nice to have these already as values....
                List<String> lats = f1.values;
                List<String> lons = f2.values;
                int size = lats.size();
                
                for(int i=0;i<size;i++){
                    String latS = lats.get(i);
                    String lonS = lons.get(i);
                    double h;
                    if (haveHeights){
                        String hS = heights.get(i);
                        h = Double.parseDouble(hS);
                    }else{
                        h = 1.0;
                    }
                    double lat = Double.parseDouble(latS);
                    double lon = Double.parseDouble(lonS);
                    Location loc = new Location(lat, lon, h);
                    myLocations.add(loc);
                }
            }
        }   
        }catch(Exception e){
            
        }
    }

    @Override
    public void exportToURL(URL aURL, GridVisibleArea g){}
}
