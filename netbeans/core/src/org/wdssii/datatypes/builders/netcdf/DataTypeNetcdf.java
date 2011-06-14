package org.wdssii.datatypes.builders.netcdf;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.wdssii.geom.Location;
import ucar.nc2.NetcdfFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.util.StringUtil;

/**
 * Root class for building a DataType from Netcdf data.
 * 
 * @author Robert Toomey
 */
public abstract class DataTypeNetcdf {

    private static Log log = LogFactory.getLog(DataTypeNetcdf.class);

     /** Try to create a DataType by reflection.  This is called from NetcdfBuilder by reflection
     * All netcdf builder classes have to implement this or what's the point?
     * 
     * @param ncfile	the netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    public abstract DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse);
    
    /** Fill a memento from netcdf data. */
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {
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
            m.originLocation = loc;
            m.startTime = dt;
            m.typeName = typeName;
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
        m.attributes = attr;
    }
}
