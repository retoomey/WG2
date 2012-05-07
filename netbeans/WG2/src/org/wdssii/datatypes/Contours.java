package org.wdssii.datatypes;

import java.net.URL;
import java.util.ArrayList;

import org.wdssii.geom.Location;
import org.wdssii.gui.GridVisibleArea;

/** A set of contour objects inside a DataType 
 * 
 * @author Robert Toomey
 */
public class Contours extends DataType implements Table2DView {

    private int myMaxLocations = 0;
    private final int NUM_FIELDS_PER_POINT = 2;

    /** Passed in by builder objects to use to initialize ourselves.
     * This allows us to have final field access from builders.
     */
    public static class ContoursMemento extends DataTypeMemento {
    };
    /** Our set of contours */
    protected ArrayList<Contour> myContours = new ArrayList<Contour>();

    public Contours(ContoursMemento memento) {
        super(memento);
    }

    public void addContour(Contour c) {
        myContours.add(c);
        // Keep track of maximum length for table column count....
        myMaxLocations = Math.max(c.getLocations().size(), myMaxLocations);
    }

    /** Get the number of unique contours in this contour */
    public int getNumberOfContours() {
        if (myContours != null) {
            return myContours.size();
        }
        return 0;
    }

    /** Get the array list of contour objects */
    public ArrayList<Contour> getContours() {
        return myContours;
    }

    @Override
    public int getNumCols() {
        return myMaxLocations * NUM_FIELDS_PER_POINT;
    }

    @Override
    public int getNumRows() {
        return getNumberOfContours();
    }

    @Override
    public String getRowHeader(int row) {
        return (String.format("Contour %d", row));
    }

    @Override
    public String getColHeader(int col) {
        String h = "";
        int base = col / NUM_FIELDS_PER_POINT;
        int subcol = col - (base * NUM_FIELDS_PER_POINT);

        switch (subcol) {
            case 0:
                h = "Lat";
                break;
            case 1:
                h = "Lon";
                break;
            case 2:
                h = "H";
                break;
            default:
                h = "?";
                break;
        }
        return h;
    }

    @Override
    public boolean getCellValue(int row, int col, CellQuery output) {
        float value = 0;
        if ((row > -1) && (row < getNumberOfContours())) {
            Contour c = myContours.get(row);
            ArrayList<Location> l = c.getLocations();
            if ((col > -1) && (col < l.size() * NUM_FIELDS_PER_POINT)) {
                int base = col / NUM_FIELDS_PER_POINT;
                int subcol = col - (base * NUM_FIELDS_PER_POINT);
                Location aLoc = l.get(base);
                switch (subcol) {
                    case 0:
                        value = (float) aLoc.getLatitude();
                        break;
                    case 1:
                        value = (float) aLoc.getLongitude();
                        break;
                    case 2:
                        value = (float) aLoc.getHeightKms();
                        break;
                    default:
                        value = 0;
                        break;
                }
            }
        }
        output.value = value;
        return true;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col,
            Location output) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getCell(Location input, Cell output) {
        // TODO Auto-generated method stub
        return false;
    }
	
    @Override
    public void exportToURL(URL aURL, GridVisibleArea g) {}
}
