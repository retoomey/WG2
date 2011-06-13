package org.wdssii.datatypes;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.geom.Location;
import org.wdssii.util.StringUtil;

import ucar.nc2.NetcdfFile;

/**
 * Base class of all the data types that can be displayed.
 * 
 * @author Lakshman
 */
public class DataType {

    private static Log log = LogFactory.getLog(DataType.class);
    /** The origin location of the DataType, such as the radar center for RadialSets */
    protected Location originLocation;
    /** The start time of the data set */
    protected Date startTime;
    /** A more specific type name for this DataType...such as 'Mesonet' for a DataTable */
    protected String typeName;
    /** Name to value for attributes of a DataType:
     */
    protected Map<String, String> attributes = new HashMap<String, String>();
    /** XML stores the units for each attribute, while netcdf does not seem to.  If we have an attribute
     * in here then we have the units, otherwise it's dimensionless
     */
    protected Map<String, String> attrUnits = new HashMap<String, String>();
    // Note: Modify isRealDataValue() if adding any values to the set below
    public static final float MissingData = -99900;
    public static final float RangeFolded = -99901;
    public static final float DataUnavailable = -99903;
    protected DataTypeMetric myDataTypeMetric = new DataTypeMetric();

    /** The base type for a query.  Rather than just getting a 'double' value back from data,
     * this allows us to request/receive more detailed information.
     * There are input and output fields. You typically pre-create a query object outside of
     * a loop and reuse it in order to keep the speed (avoiding newing inside a loop)
     * For example, working through a radial set volume at a given location:
     * <code>
     * RadialSetQuery output = new RadialSetQuery();
     * output.inLocation = someLocation;
     * for(RadialSet r: myRadialVolume){
     *    r.queryData(output);
     *    // do something with output
     * }
     * </code>
     * @author Robert Toomey
     *
     */
    public static class DataTypeQuery {

        /** Do we take into consideration height when querying? */
        public boolean inUseHeight = true;
        /** Query by location (most if all DataTypes query this way) */
        public Location inLocation;
        /** Used by filters.  This is the original data value.  Each filter modifies the
         * outDataValue with its function, but the original data value is left alone.  However,
         * filters might need access to the original data value
         */
        public float inDataValue;
        
        /** Get weight for interpolation.  The weight is a 'distance' from
         * the closest data point.
         */
        public boolean inNeedInterpolationWeight = false;
        
        /** The distance from the 'true' value of the beam, to the location asked
         * for.  This will give us part of the weight for any linear interpolation
         */
        public float outDistanceHeight = 0.0f;
                
        /** The simple data value (most if all DataTypes have this).  Note a DataType such as WindField could have
         * multiple out values */
        public float outDataValue = DataType.DataUnavailable;
        
        /** Get the final filtered data value */
        public float getFinalValue() {
            return outDataValue;
        }

        /** Get the original data value without any filters */
        public float getOrgValue() {
            return inDataValue;
        }
    };

    /** Metrics gathered on data during read. Metrics are things like average value, min value, etc...
     * For example, the GUI uses the min/max values to generate color maps.
     * Subclasses could add to this in order to gather more data during reading.  The advantage of this
     * class is that it is called during the original loading of data, avoid the need to iterate later
     * over the entire dataset
     */
    public static class DataTypeMetric {

        private float minValue;

        public float getMinValue() {
            return minValue;
        }
        public float maxValue;

        public float getMaxValue() {
            return maxValue;
        }

        /** Do any init needed */
        public void beginArray2D() {
            minValue = 100000;
            maxValue = -90000;
        }

        ;
		
		/** Update for a data value */
		public void updateArray2D(int x, int y, float value) {
            if (DataType.isRealDataValue(value)) {
                if (value > maxValue) {
                    maxValue = value;
                }
                if (value < minValue) {
                    minValue = value;
                }
            }
        }
    }

    public DataType() {
        // Empty datatype
    }

    /** Most DataTypes can be optionally created from XML data.*/
    @SuppressWarnings("unused")
    public boolean fromXML(XMLStreamReader p) throws XMLStreamException {
        return true;
    }

    public void setOriginLocation(Location l) {
        originLocation = l;
    }

    /** Most DataTypes can be optionally created from netcdf data.  This function tries 
     * to fill in our fields from a NetCdfFile.  Subclasses should create a constructor
     * to parse more stuff (first calling super).  This is called by reflection from the NetcdfBuilder
     * @param ncfile the netcdf file to read from
     * @param sparse was the netcdf file a sparse datatype
     */
    public DataType(NetcdfFile ncfile, boolean sparse) {

        String typeName = "DataType";
        Location loc = null; // FIXME: defaults if netcdf fails?
        Date dt = null;

        // These are the shared attributes that are gathered from a netcdf
        // file for any subclass of DataType.  Note that this never has to be
        // called.
        try {
            typeName = ncfile.findGlobalAttribute("TypeName").getStringValue();

            // location and time
            double lat = ncfile.findGlobalAttribute("Latitude").getNumericValue().doubleValue();
            double lon = ncfile.findGlobalAttribute("Longitude").getNumericValue().doubleValue();
            double ht = ncfile.findGlobalAttribute("Height").getNumericValue().doubleValue();
            long tm = ncfile.findGlobalAttribute("Time").getNumericValue().longValue();

            loc = new Location(lat, lon, ht / 1000);
            long tm_long = 1000 * tm;

            try {
                double ftm = ncfile.findGlobalAttribute("FractionalTime").getNumericValue().doubleValue();
                tm_long += (int) Math.round(1000 * ftm);
            } catch (Exception e) {
            } // okay if no fractional
            dt = new Date(tm_long);

        } catch (Exception e) {
            log.warn("Couldn't read in location/time/type shared attibutes from netcdf file", e);
        } finally {
            this.originLocation = loc;
            this.startTime = dt;
            this.typeName = typeName;
        }

        // The global list of attributes directly from the netcdf
        Map<String, String> attr = new HashMap<String, String>();
        try {
            List<String> attrNames = StringUtil.split(ncfile.findGlobalAttribute("attributes").getStringValue());
            for (int i = 0; i < attrNames.size(); ++i) {
                String name = attrNames.get(i);
                if (name.equals("") == false) {
                    String val = ncfile.findGlobalAttribute(name + "-value").getStringValue();
                    attr.put(name, val);
                }
            }
        } catch (Exception e) {
            // If attr fails, just keep what we have so far
            log.warn("While extracting attributes.", e);
        }
        setAttributes(attr);
    }

    /** Convenience test for if a data value is 'real' or one of the special
     * values such as MissingData.  Modify this function if you add any additional
     * special types
     * @param value value to check
     * @return true is data value is real
     */
    public static boolean isRealDataValue(float value) {
        return (value > MissingData);
    }

    /** Return a double used to sort a volume of this DataType.  For example,
     * for RadialSets this would be the elevation value.
     * @return value in volume
     */
    public double sortInVolume() {
        return 0.0;
    }

    public DataType(Location originLoc, Date startTime, String typeName) {
        this.originLocation = originLoc;
        this.startTime = startTime;
        this.typeName = typeName;
    }

    /**
     * copies all the attributes, etc. from the master. The master can change
     * without affecting this object
     */
    public DataType(DataType master) {
        this.originLocation = master.originLocation;
        this.startTime = master.startTime;
        this.typeName = master.typeName;
        Iterator<Entry<String, String>> entries = master.attributes.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, String> entry = entries.next();
            this.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /** copies all the attributes over */
    public void addAttributes(DataType example) {
        Iterator<Entry<String, String>> entries = example.attributes.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, String> entry = entries.next();
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public void setAttributes(Map<String, String> attr) {
        attributes = attr;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /** Get the start time of the data */
    public Date getTime() {
        return startTime;
    }

    /** Set the start time of the data */
    public void setTime(Date d) {
        this.startTime = d;
    }

    /**Get the origin of the data type. */
    public Location getLocation() {
        return originLocation;
    }

    /** Get the location in the GUI to 'jump' to the data.  Default is the origin location */
    public Location getJumpToLocation() {
        Location loc = new Location(originLocation.getLatitude(), originLocation.getLongitude(),
                100);
        return loc;
    }

    /** Set the origin of the data type */
    public void setLocation(Location l) {
        this.originLocation = l;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setUnitsForAttributes(Map<String, String> units) {
        attrUnits = units;
    }

    public String getUnitForAttribute(String name) {
        // Return the attribute unit type or dimensionless if not found
        String unit = attrUnits.get(name);
        if (unit != null) {
            return unit;
        }
        return "dimensionless";
    }

    public String getUnit() {
        String unit = attributes.get("Unit");
        if (unit == null) {
            return "dimensionless";
        }
        return unit;
    }

    public String getUTC() {
        java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(startTime) + " UTC";
    }

    public String toStringDB() {
        String s = "datatype: at " + getUTC() + " for " + originLocation + " ";
        Iterator<Map.Entry<String, String>> entries = getAttributes().entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            s += entry.getKey() + "=" + entry.getValue() + " ";
        }
        return s;
    }

    // Default convert datatype single value to string.  FIXME: datatypes with multiple values,
    // such as windfield?  Also, needs to become non-static so subclasses can override.
    public static String valueToString(float value) {
        String text = null;
        if (DataType.isRealDataValue(value)) {
            if (value < .05) {
                text = String.format("%5.5f", value);
            } else {
                text = String.format("%5.2f", value);
            }

        } else {
            text = ".";
        }
        return text;
    }

    public DataTypeMetric getDataTypeMetric() {
        return myDataTypeMetric;
    }
}
