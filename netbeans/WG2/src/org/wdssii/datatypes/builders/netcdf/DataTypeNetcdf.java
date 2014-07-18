package org.wdssii.datatypes.builders.netcdf;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.wdssii.geom.Location;
import ucar.nc2.NetcdfFile;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.builders.netcdf.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.core.StringUtil;
import ucar.nc2.Attribute;

/**
 * Root class for building a DataType from Netcdf data.
 * 
 * @author Robert Toomey
 */
public abstract class DataTypeNetcdf {

    private final static Logger LOG = LoggerFactory.getLogger(DataTypeNetcdf.class);

    /** Try to create a DataType by reflection.  This is called from NetcdfBuilder by reflection
     * All netcdf builder classes have to implement this or what's the point?
     * 
     * @param ncfile	the netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    public abstract DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse);

    /** Fill basic info for file.  This is used by GUI to get selections
     * of a manual file.  Typically we need:
     * TypeName such as 'Reflectivity'
     * Choice such as "0.50"  This depends on DataType.
     * Time of the product.
     * 
     * @param ncfile 
     */
    public void fillNetcdfFileInfo(NetcdfFile ncfile, NetcdfFileInfo info) {
        info.TypeName = getDatatypeNameFromNetcdf(ncfile);
        info.Time = getTimeFromNetcdf(ncfile);
    }

    /** Return the DataType of this netcdf file */
    public String getDatatypeNameFromNetcdf(NetcdfFile ncfile){
        String typeName;
        Attribute a = ncfile.findGlobalAttribute("TypeName");
        if (a == null){
            typeName = "Missing";
        }else{
            typeName = a.getStringValue();
        }
        return typeName;
    }
    
    /** Read the Time and FractionalTime attributes from a netcdf file */
    public Date getTimeFromNetcdf(NetcdfFile ncfile) {
        Date dt;
        long tm = 0;
        try {
            tm = ncfile.findGlobalAttribute("Time").getNumericValue().longValue();
        } catch (Exception e2) {
            // No time attribute or wrong format, use current time
            LOG.warn("Missing Time attribute in netcdf file, using current time");
            dt = new Date();
            return dt;
        }
        long tm_long = 1000 * tm;

        // Try to get fractional part as well
        try {
            double ftm = ncfile.findGlobalAttribute("FractionalTime").getNumericValue().doubleValue();
            tm_long += (int) Math.round(1000 * ftm);
        } catch (Exception e) {
        } // okay if no fractional
        dt = new Date(tm_long);
        return dt;
    }

    public Location getLocationFromNetcdf(NetcdfFile ncfile) {
        Location loc = null; // FIXME: defaults if netcdf fails?
        try {
            // location and time
            double lat = ncfile.findGlobalAttribute("Latitude").getNumericValue().doubleValue();
            double lon = ncfile.findGlobalAttribute("Longitude").getNumericValue().doubleValue();
            double ht = ncfile.findGlobalAttribute("Height").getNumericValue().doubleValue();
            loc = new Location(lat, lon, ht / 1000);
        } catch (Exception e) {
            LOG.warn("Couldn't read in location from netcdf file: "+e.toString());
        }

        return loc;
    }

    /** Our attributes in a WDSSII style file, not in regular netcdf */
    public Map<String, String> getWGAttributes(NetcdfFile ncfile){
         // The global list of attributes directly from the netcdf
        Map<String, String> attr = new HashMap<String, String>();
        
        Attribute a = ncfile.findGlobalAttribute("attributes");
        if (a != null){
            List<String> attrNames = StringUtil.split(a.getStringValue());
            for (int i = 0; i < attrNames.size(); ++i) {
                String name = attrNames.get(i);
                if (!name.isEmpty()){
                    Attribute v = ncfile.findGlobalAttribute(name + "-value");
                    if (v != null){
                        String val = v.getStringValue();
                        attr.put(name, val);
                    }else{
                        LOG.warn ("Wdssii netcdf file had attribute "+name+" that was missing value");
                    }
                }
            }
        }
        return attr;
    }
    /** Fill a memento from netcdf data. */
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {
        
        m.typeName = getDatatypeNameFromNetcdf(ncfile);
        m.originLocation = getLocationFromNetcdf(ncfile);
        m.startTime = getTimeFromNetcdf(ncfile);
        m.attriNameToValue = getWGAttributes(ncfile);
    }
}
