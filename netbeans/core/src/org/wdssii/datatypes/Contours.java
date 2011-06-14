package org.wdssii.datatypes;

import java.util.ArrayList;

import org.wdssii.datatypes.builders.xml.DataTypeXML.DataTypeXMLHeader;
import org.wdssii.geom.Location;

/** A set of contour objects inside a DataType 
 * 
 * @author Robert Toomey
 */
public class Contours extends DataType implements Table2DView {

    /** A single contour of a contours set.
     * Could put in separate class, but it's pretty tiny.
     * @author Robert Toomey
     *
     */
    public static class Contour {

        /** Stock datatype header */
        private DataTypeXMLHeader datatypeHeader;
        /** Points of the contour */
        private ArrayList<Location> locations;

        /** Get the standard datatype header for this contour */
        public DataTypeXMLHeader getDataTypeHeader() {
            return datatypeHeader;
        }

        /** Set the standard datatype header for this contour */
        public void setDataTypeHeader(DataTypeXMLHeader h) {
            datatypeHeader = h;
        }

        /** Get the number of points in this contour */
        public int getSize() {
            if (locations != null) {
                return locations.size();
            }
            return 0;
        }

        /** Get the location list for the contour */
        public ArrayList<Location> getLocations() {
            return locations;
        }

        /** Set the location list for the contour */
        public void setLocations(ArrayList<Location> list) {
            locations = list;
        }
    }
    /** Our set of contours */
    protected ArrayList<Contour> myContours = new ArrayList<Contour>();

    /** Create a new, uninitialized Contours object.  Usually called by one of the XML/netcdf parsers */
    public Contours() {
    }

    public Contours(DataTypeXMLHeader header) {
        super(header.location, header.time, header.datatype);

        System.out.println("Contours constructor");
        System.out.println("Header is " + header);
        System.out.println("Header location " + header.location);
        System.out.println("Header time " + header.time);
        // FIXME: do the contours....
    }

    public void addContour(Contour c) {
        myContours.add(c);
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

    public void setDatatypeHeader(DataTypeXMLHeader header) {
        setLocation(header.location);
        setTime(header.time);
    }

    @Override
    public int getNumCols() {
        return getNumberOfContours();
    }

    @Override
    public int getNumRows() {
        return 1000000;  // test of large size..
    }

    @Override
    public String getRowHeader(int row) {
        return (String.format("%d", row));
    }

    @Override
    public String getColHeader(int col) {
        if (col < myContours.size()) {
            Contour a = myContours.get(col);
            return ("C(" + a.getSize() + ")");
        }
        // Default fall through...fill text in with row number
        return (String.format("%d", col));
    }

    @Override
    public float getCellValue(int row, int col) {
        // TODO Auto-generated method stub
        return 0;
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
}
