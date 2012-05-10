package org.wdssii.datatypes;

import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wdssii.geom.Location;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.storage.Array2Dfloat;

/**
 *
 * A regularly spaced grid where the first dimension is latitude and the second
 * dimension is longitude. The (0,0) location is the north-west corner. In the
 * first dimension, latitude decreases (southward) and in the second dimension,
 * longitude increases (eastward).
 *
 * @author lakshman
 *
 */
public class LatLonGrid extends DataType implements Table2DView {

    private static Logger log = LoggerFactory.getLogger(LatLonGrid.class);
    /**
     * The change in lat distance per grid square
     */
    private final float deltaLat;
    /**
     * The change in lon distance per grid square
     */
    private final float deltaLon;
    /**
     * The array that stores our data
     */
    private Array2Dfloat values;

    /** The 'thickness' in KM of the LatLonGrid plane.  Used for
     * volumes.  Basically it's the equivalent of RadialSet's beamwidth.
     */
    private float myThicknessKms = .10f;
    
    public static class LatLonGridMemento extends DataTypeMemento {

        /**
         * The change in lat distance per grid square
         */
        public float deltaLat;
        /**
         * The change in lon distance per grid square
         */
        public float deltaLon;
        /**
         * The 2D array of data values.
         */
        public Array2Dfloat values;
    }

    /**
     * The query object for LatLonGrids.
     */
    public static class LatLonGridQuery extends DataTypeQuery {
    };

    public LatLonGrid(LatLonGridMemento m) {
        super(m);
        this.deltaLat = m.deltaLat;
        this.deltaLon = m.deltaLon;
        this.values = m.values;
    }

    /**
     * {@inheritDoc}
     */
    // public LatLonGrid(LatLonGrid master) {
    //     super(master);
    //      deltaLat = master.deltaLat;
    //     deltaLon = master.deltaLon;
    //      // FIXME: broken values = copyOf(master.values);
    //  }
    // public LatLonGrid(Location nwCorner, Date time, String typeName, float deltaLat, float deltaLon, Array2Dfloat values) {
    //     super(nwCorner, time, typeName);
    //     this.deltaLat = deltaLat;
    //     this.deltaLon = deltaLon;
    //     this.values = values;
    // }
    /**
     * All the values are initialized to zero
     */
    //  public LatLonGrid(Location nwCorner, Date time, String typeName, float deltaLat, float deltaLon, int numLat, int numLon) {
    //     this(nwCorner, time, typeName, deltaLat, deltaLon, new Array2DfloatAsTiles(numLat, numLon, 0.0f));
    // }
    public Array2Dfloat getValues() {
        return values;
    }

    public int getNumLat() {
        return values.getX();
    }

    public int getNumLon() {
        return values.getY();
    }

    /**
     * positive number that indicates the size of a pixel in lat-direction
     */
    public float getDeltaLat() {
        return deltaLat;
    }

    /**
     * positive number that indicates the size of a pixel in lon-direction
     */
    public float getDeltaLon() {
        return deltaLon;
    }

    /**
     * Get the value at a Location. The height is ignored and MissingData is
     * returned for values outside
     */
    public float getValue(Location loc) {
        try {
            int i = (int) Math.rint((this.getLocation().getLatitude() - loc.getLatitude()) / this.deltaLat);
            int j = (int) Math.rint((loc.getLongitude() - this.getLocation().getLongitude()) / this.deltaLon);
            if ((i >= 0) && (i < values.getX())
                    && (j >= 0) && (j < values.getY())) {
                return values.get(i, j);
            }
        } catch (Exception e) {
        }
        return DataType.MissingData;
    }

    /**
     * Can be used to set all the values of the grid at once.
     */
    public void setValues(Array2Dfloat values2) {
        values = values2;
    }

    // Table2D implementation --------------------------------------------------------
    @Override
    public int getNumCols() {
        return (getNumLon());
    }

    @Override
    public int getNumRows() {
        return (getNumLat());
    }

    @Override
    public String getRowHeader(int row) {
        return (String.format("[%d/%d]", row, getNumLat()));
    }

    @Override
    public String getColHeader(int col) {
        return (String.format("[%d/%d]", col, getNumLon()));
    }

    @Override
    public boolean getCellValue(int row, int col, CellQuery output) {
        output.value = values.get(row, col);
        return true;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col,
            Location output) {
        if ((col >= getNumCols()) || (row >= getNumRows())) {
            System.out.println("********* Out of bound : (" + col + "," + row + ") bounds [" + getNumCols() + "," + getNumRows());
            return false;
        }
        boolean success = false;
        Location loc = getLocation();
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        double dLat = getDeltaLat();
        double dLon = getDeltaLon();
        double height = loc.getHeightKms() + 10.0;
        int r = row;
        int c = col;

        switch (type) {
            case TOP_LEFT: {
                output.init(lat - (dLat * r), lon + (dLon * c), height);
            }
            break;
            case TOP_RIGHT: {
                output.init(lat - (dLat * r), lon + (dLon * (c + 1)), height);
            }
            break;
            case BOTTOM_LEFT: {
                output.init(lat - (dLat * r) - dLat, lon + (dLon * c), height);
            }
            break;

            case BOTTOM_RIGHT: {
                output.init(lat - (dLat * r) - dLat, lon + (dLon * (c + 1)), height);
            }
            break;
        }

        success = true;
        return success;

    }

    /**
     * Get the center location of the LatLonGrid..this is used by GUI for the
     * 'jump' function
     *
     * @return center of latlongrid
     */
    public Location getCenterLocation() {
        Location center = new Location(0, 0, 0);
        getLocation(LocationType.TOP_LEFT, getNumRows() / 2, getNumCols() / 2,
                center);
        return center;
    }

    @Override
    public boolean getCell(Location input, CellQuery output) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Not much to this query function...just pull from the grid
     */
    public void queryData(LatLonGridQuery q) {

        boolean haveLocation = false;

        if (q.inLocation != null) {
            haveLocation = true;
        }

        // We can query by a Location only...
        if (haveLocation) {
            float v = getValue(q.inLocation);          
             if (q.inUseHeight) {
                 
                 // Thickness test of plane
                 double h_diff = this.originLocation.getHeightKms()-q.inLocation.getHeightKms();
                 if (Math.abs(h_diff) > myThicknessKms) {
                     v = MissingData;
                 }
             }
             q.outDataValue = v;
        } else {
            q.outDataValue = MissingData;
        }
    }

    @Override
    public void exportToURL(URL aURL, GridVisibleArea g){}
}
