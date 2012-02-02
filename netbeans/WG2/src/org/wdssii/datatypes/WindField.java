package org.wdssii.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.Location;
import org.wdssii.storage.Array2Dfloat;

/**
 * Alpha version of WindField datatype.  Just a hook to get things started in GUI
 * 
 * @author Robert Toomey
 * 
 */
public class WindField extends DataType implements Table2DView {

    private static Logger log = LoggerFactory.getLogger(LatLonGrid.class);

    // Class passed in to pull back a value.
    public static class WindFieldDataPoint {

        public float u;
        public float v;
    };
    
    private final float deltaLat;
    private final float deltaLon;
    private Array2Dfloat uArray;
    private Array2Dfloat vArray;

    public static class WindFieldMemento extends DataTypeMemento {
        public float deltaLat;
        public float deltaLon;
        public Array2Dfloat uArray;
        public Array2Dfloat vArray;
    }
    
    public WindField(WindFieldMemento m){
        super(m);
        this.deltaLat = m.deltaLat;
        this.deltaLon = m.deltaLon;
        this.uArray = m.uArray;
        this.vArray = m.vArray;
    }

    /* For moment no manual creation...
    public WindField(Location nwCorner, Date time, String typeName, float deltaLat, float deltaLon, float[][] values){
    super(nwCorner, time, typeName);
    this.deltaLat = deltaLat;
    this.deltaLon = deltaLon;
    //this.uArray = values;
    }
     */
    /*
    public WindField(Location nwCorner, Date time, String typeName, float deltaLat, float deltaLon, int numLat, int numLon){
    this(nwCorner, time, typeName, deltaLat, deltaLon, new float[numLat][numLon] );
    }
     */
    // Gonna have to replace the getValues.  Allowing direct access to float array kills the whole
    // ability to change the internal data structure.  Maybe have an iterator get function?
	/*
    public float[][] getValues(){
    return values;
    }
     */
    public int getNumLat() {
        //return uArray.length;
        return uArray.getX();
    }

    public int getNumLon() {
        return uArray.getY();
        //if ( uArray.length == 0 ){
        //	return 0;
        //}
        //return uArray[0].length;
    }

    /** positive number that indicates the size of a pixel in lat-direction */
    public float getDeltaLat() {
        return deltaLat;
    }

    /** positive number that indicates the size of a pixel in lon-direction */
    public float getDeltaLon() {
        return deltaLon;
    }

    /** Converts all the values in this grid to the specified unit.
     *  @see UdUnits.parse()
     *  @param grid
     *  @param toUnit  Use UdUnits.parse() to create this Unit
     */
    /*
    public void convertToUnit(Unit toUnit) {
    if (this.getUnit() == null) {
    log.debug("Missing units in " + this.getTypeName() + " assuming "
    + toUnit);
    return;
    }
    Unit gridUnit = UdUnits.parse(this.getUnit());
    if (gridUnit.equals(toUnit)) {
    return;
    }
    try {
    Converter converter = gridUnit.getConverterTo(toUnit);
    float[][] values = this.getValues();
    for (int i = 0; i < values.length; ++i) {
    for (int j = 0; j < values.length; ++j) {
    values[i][j] = converter.convert(values[i][j]);
    }
    }
    } catch (ConversionException e) {
    log.error("Unit in " + this.getTypeName() + " is " + gridUnit
    + "; expected something compatible with " + toUnit);
    }
    }
     */
    /** Get the value array at Location.  The height is ignored and false is returned if values outside.
     * If looping, pre-create a WindFieldDataPoint and reuse it as a buffer */
    public boolean getValue(Location loc, WindFieldDataPoint output /** pre-created storage object */
            ) {
        try {
            // not reusing Pixel code since this is more efficient
            int i = (int) Math.rint((this.getLocation().getLatitude() - loc.getLatitude()) / this.deltaLat);
            int j = (int) Math.rint((loc.getLongitude() - this.getLocation().getLongitude()) / this.deltaLon);
            //output.u = uArray[i][j];
            //output.v = vArray[i][j];
            output.u = uArray.get(i, j);
            output.v = vArray.get(i, j);
            return true;
        } catch (Exception e) {
            //return DataType.MissingData;
            return false;
        }
    }

    /** Get the value array at 2d grid location.  */
    public boolean getValue(int d1, int d2, WindFieldDataPoint output /** pre-created storage object */
            ) {
        try {
            //output.u = uArray[d1][d2];
            //output.v = vArray[d1][d2];
            output.u = uArray.get(d1, d2);
            output.v = vArray.get(d1, d2);
            return true;
        } catch (Exception e) {
            //return DataType.MissingData;
            return false;
        }
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
    public boolean getCellValue(int row, int col, CellQuery output)
    {
        float value = 0;
        float u = uArray.get(row, col);
        float v = vArray.get(row, col);
        //float u = uArray[row][col];
        //float v = vArray[row][col];
        value = (float) Math.sqrt(((u * u) + (v * v)));  // As velocity
        output.value = value;
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

    @Override
    public boolean getCell(Location input, Cell output) {
        // TODO Auto-generated method stub
        return false;
    }
}
