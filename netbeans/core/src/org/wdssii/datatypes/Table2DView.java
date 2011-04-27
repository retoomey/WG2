package org.wdssii.datatypes;

import org.wdssii.geom.Location;

/** The table 2D interface allows DataType to implement an 'excel' spreadsheet-like 2D table.
 * This is a different 'view' of the data.  The GUI uses this class to display an actual table.
 * 
 *  @author Robert Toomey
 */
public interface Table2DView {

    /** Locations for the getLocation method.  Each value represents the exact area of a data cell outline
     * in relation to the table. For example, TOP_LEFT for a radial set cell would be the range + the gate width
     * of the start azimuth..TOP_RIGHT the range + gate width + end azimuth.
     *
     * FIXME: add 'set' ability
     */
    public enum LocationType {

        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    };

    /** Used by the getCell method for output */
    public static class Cell {

        public int row;
        public int col;
        public boolean exactHit;
    }

    /** Get the number of columns in the table */
    int getNumCols();

    /** Get the number of rows in the table */
    int getNumRows();

    /** Get the row label for given row number */
    String getRowHeader(int row);

    /** Get the col label for given col number */
    String getColHeader(int col);

    /** Get the cell value as a float */
    float getCellValue(int row, int col);

    /** Get the 3D location of the given cell part, if capable.  Return false if unable.
     * The GUI can use this to display a 3D outline in space of a visible table
     */
    public boolean getLocation(LocationType type, int row, int col, Location output);

    /** For a given Location, get the hit row and col. */
    public boolean getCell(Location input, Cell output);
}
