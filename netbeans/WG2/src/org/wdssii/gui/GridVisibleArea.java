package org.wdssii.gui;

/** Object representing a square section of visible table area.
 * Used to render product outlines for readout in the 3D world, also to render table
 * tracking.
 * 
 *  @author Robert Toomey
 *  
 */
public class GridVisibleArea {

    public int numRows;
    public int numCols;
    public int startCol; // Index of first visible column on screen
    public int lastFullColumn; // Index of last fully (non-clipped column on
    // screen)
    public int lastPartialColumn; // Index of last clipped column on screen
    public int startRow;
    public int lastFullRow;
    public int lastPartialRow;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(numRows);
        builder.append(",");
        builder.append(numCols);
        builder.append(") Col:");
        builder.append(startCol);
        builder.append(", ");
        builder.append(lastFullColumn);
        builder.append(", ");
        builder.append(lastPartialColumn);
        builder.append(", Row:");
        builder.append(startRow);
        builder.append(", ");
        builder.append(lastFullRow);
        builder.append(", ");
        builder.append(lastPartialRow);

        return builder.toString();
    }
}