package org.wdssii.datatypes.builders.netcdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.DataType.DataTypeMetric;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.PPIRadialSet.PPIRadialSetMemento;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.builders.netcdf.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.geom.Location;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array2D;
import org.wdssii.storage.Array2DfloatAsTiles;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Create a RadialSet from a CFRadial file
 * 
 * Hacked this together in a few hours, will need a ton of work..but
 * why not..might be cool.
 * 
 * @author Robert Toomey
 */
public class CFRadialNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private final static Logger LOG = LoggerFactory.getLogger(CFRadialNetcdf.class);

    /** Try to create a RadialSet by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        PPIRadialSetMemento m = new PPIRadialSetMemento();
        m.datametric = PPIRadialSet.createDataMetric();
        this.fillFromNetcdf(m, ncfile, sparse);
        return new PPIRadialSet(m);
    }

    @Override
    public void fillNetcdfFileInfo(NetcdfFile ncfile, NetcdfFileInfo info) {
        super.fillNetcdfFileInfo(ncfile, info);
        try {
            float elev = ncfile.findGlobalAttribute("Elevation").getNumericValue().floatValue();
            info.Choice = Float.toString(elev);
        } catch (Exception e) {
            info.Choice = "Missing";
        }
    }

    /** Return the DataType of this netcdf file */
    @Override
    public String getDatatypeNameFromNetcdf(NetcdfFile ncfile) {
        return "CFRadial";
    }

    /** Location from CFRadial can be a scalar for fixed location,
     * or a vector.  We assume scalar for now.
     * @param ncfile
     * @return a single Location
     */
    @Override
    public Location getLocationFromNetcdf(NetcdfFile ncfile) {
        Location loc = null; // FIXME: defaults if netcdf fails?
        try {
            double lat = 0.0, lon = 0.0, height = 0.0;
            // location and time
            Variable latVar = ncfile.findVariable("latitude");
            if (latVar.isScalar()) {  // only handling fixed location right now
                lat = latVar.readScalarDouble();
            }
            Variable lonVar = ncfile.findVariable("longitude");
            if (lonVar.isScalar()) {
                lon = lonVar.readScalarDouble();
            }
            Variable htVar = ncfile.findVariable("altitude");
            if (htVar.isScalar()) {
                height = htVar.readScalarDouble();
            }
            loc = new Location(lat, lon, height / 1000);
        } catch (Exception e) {
            LOG.warn("Couldn't read in location from netcdf file: "+e.toString());
        }

        return loc;
    }

    /** Hunt for Variables with Dimensions [time, range].  This should
     * find DBZ or VR, etc...
     * O(N) search of all Variables...is there a faster way?  I don't know
     * yet.
     * 
     * Would be nice if the CFRadial format had something like:
     * Moments = "DBZ VR" similar to the Conventions attribute.  Maybe it does?
     * 
     * @return a list of the variable names that matched.
     */
    public ArrayList<Variable> findDataMomentVariables(NetcdfFile ncfile) {
        ArrayList<Variable> theMoments = new ArrayList<Variable>();
        List<Variable> vars = ncfile.getVariables();
        Iterator<Variable> iter = vars.iterator();
        while (iter.hasNext()) {
            Variable v = iter.next();
            // If rank is 2, look for 'time' 'range'
            if (v.getRank() == 2) {
                String d = v.getDimensionsString();
                if ("time range".equals(d)) {
                    theMoments.add(v);
                }
            }
        }
        return theMoments;
    }

    public String getPlatformType(NetcdfFile ncfile) {
        // Read platform_type.  Docs say to assume fixed if missing
        String type;
        Variable pt = ncfile.findVariable("platform_type");
        if (pt == null) {
            type = "fixed";
        } else {
            try {
                type = pt.readScalarString();
            } catch (IOException ex) {
                LOG.warn("Couldn't read platform_type as scalar..??");
                type = "fixed";
            }
        }
        return type;
    }

   public  Array2D<Float> readArray2Dfloat(NetcdfFile ncfile, String typeName,
            DataTypeMetric optional,
            float scale_factor,
            float add_offset,
            boolean useScaleOffset)
            throws IOException {
        Variable data = ncfile.findVariable(typeName);
        Array gate_values = data.read();
        int num_radials = data.getDimension(0).getLength();
        int num_gates = data.getDimension(1).getLength();

        Array2D<Float> values = new Array2DfloatAsTiles(num_radials, num_gates, 0.0f);
        Index gate_index = gate_values.getIndex();
        if (optional != null) {
            optional.beginArray2D();
        }
        
        // Bleh according to docs, we use the multiplier and offset if
        // the type is integer
        // ncbyte 1 byte, scaled signed integer
        // short  2 byte, scaled signed integer
        // int    4 byte, scaled signed integer
        // float  4 byte, floating point
        // double 8 byte, floating point
        // float value = (integer value)*scale_factor + add_offset;
        Class c = gate_values.getElementType();
        int t = 0;
        if (c == short.class){
            t = 1;
        }else if (c == int.class){
            t = 2;
        }else if (c == float.class){
            t = 3;
        }else if (c == double.class){
            t = 4;
        }
        
        values.beginRowOrdered();
        for (int j = 0; j < num_gates; ++j) {
            for (int i = 0; i < num_radials; ++i) {
                gate_index.set(i, j);
                
                float fValue;
                switch(t){
                    case 0:  // ncbyte
                    case 2:  // int
                    default:{
                        int f = gate_values.getInt(gate_index);
                        fValue = f*scale_factor+add_offset;
                    }
                        break;
                    case 1:  // short
                    {
                        short f = gate_values.getShort(gate_index);
                        fValue = f*scale_factor+add_offset;
                    }
                        break;
                    case 3: // float
                        fValue = gate_values.getFloat(gate_index);
                        break;                
                    case 4: // double
                        fValue = (float)gate_values.getDouble(gate_index);         
                        break;
                }
  
                values.set(i, j, fValue);
                if (optional != null) {
                    optional.updateArray2D(i, j, fValue);
                }
            }
        }
        values.endRowOrdered();
        return values;
    }
        
    /** Fill a memento from Netcdf data. */
    @Override
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {

        /** Let super fill in the defaults */
        super.fillFromNetcdf(m, ncfile, sparse);

        if (m instanceof PPIRadialSetMemento) {
            PPIRadialSetMemento r = (PPIRadialSetMemento) (m);
            try {

                Variable tTimeVar = ncfile.findVariable("time");
                Variable tTimeRange = ncfile.findVariable("range");
                int tTime = tTimeVar.getDimension(0).getLength();
                int tRange = tTimeRange.getDimension(0).getLength();
                Dimension dt = ncfile.findDimension("time");
                Dimension dr = ncfile.findDimension("range");

                // if STATIONARY, these are scalars....
                Variable latitude = ncfile.findVariable("latitude");
                if (latitude.isScalar()) {
                    float lat = latitude.readScalarFloat();
                }
                Variable elev = ncfile.findVariable("elevation");
                //Variable az = ncfile.findVariable("azimuth");
                Array testElev = elev.read();
                //Array testAzimuth = az.read();

                String moment = "";
                float scale = 0;
                float offset = 0;
                boolean useScale = false;
                // Find the moments in the data file, can be more than one
                // for the moment (lol), will pick the first one to show
                ArrayList<Variable> theMoments = findDataMomentVariables(ncfile);
                if (theMoments.isEmpty()) {
                    return;
                } else {
                    Variable m1 = theMoments.get(0);
                    moment = m1.getFullName();
                    LOG.debug("FOUND moment data with name " + m1.getFullName());
                    List<Attribute> la = m1.getAttributes();
                    Iterator<Attribute> ia = la.iterator();
                    while(ia.hasNext()){
                      Attribute ab = ia.next();
                      LOG.debug("attribute "+ab.getName()+", "+ab.getStringValue());
                      if (ab.getName().equals("scale_factor")){
                          float f = ab.getNumericValue().floatValue();
                          LOG.debug("SCALE FACTOR "+f);
                          scale = f;
                          useScale = true;
                      }
                      if (ab.getName().equals("add_offset")){
                          float f = ab.getNumericValue().floatValue();
                          LOG.debug("ADD OFFSET "+f);
                          offset = f;
                          useScale = true;
                      }
                    }
                }

                // Read platform_type.  Docs say to assume fixed if missing
                LOG.debug("platform_type " + getPlatformType(ncfile));

                // 
                int sweepCount = ncfile.findDimension("sweep").getLength();

                // Data that uses sweep dimension...
                // fixed_angle (sweep)
                Variable vfixed_angle = ncfile.findVariable("fixed_angle");
                Array fixed_angle = vfixed_angle.read();
                // sweep_start_ray_index (sweep)
                Variable vsweep_start_ray_index = ncfile.findVariable("sweep_start_ray_index");
                Array sweep_start_ray_index = vsweep_start_ray_index.read();
                // sweep_end_ray_index (sweep)
                Variable vsweep_end_ray_index = ncfile.findVariable("sweep_end_ray_index");
                Array sweep_end_ray_index = vsweep_end_ray_index.read();

                // These will be the elevations of the volume, assuming
                // it is PPI mode (no RHI 'yet')
                int startRay = 0;
                int endRay = 0;
                for (int s = 0; s < sweepCount; s++) {
                    float current = fixed_angle.getFloat(s);
                    int start = sweep_start_ray_index.getInt(s);
                    int end = sweep_end_ray_index.getInt(s);
                    LOG.debug("SWEEP start ray: " + s + "  " + start + ", end ray: " + end + ",  angle " + current);
                    if (s == 0) {  // Gonna try to display first sweep of volume
                        r.fixedAngleDegs = current;
                        startRay = start;
                        endRay = end;
                    }
                }
                float distToFirstGate = 20; // Need this from file....
                r.rangeToFirstGate = distToFirstGate / 1000;

                // This reads the entire volume into a single array..
                // FIXME: need to handle the staggered case...bleh....
                Array2D<Float> values = null;
                int total_num_rays = 0;
                int total_num_gates = 0;
                try {
                    values = readArray2Dfloat(ncfile, moment, r.datametric, scale, offset, useScale);
                    total_num_rays = values.getX();
                    total_num_gates = values.getY();
                } catch (Exception e) {
                    // If we can't get the values for any reason, 
                    // just make a zero size radial set (nice recovery)
                    LOG.warn("Couldn't create radials of radial set, leaving as empty");
                }

                LOG.debug("Total " + total_num_rays + " rays " + total_num_gates + " gates");

                Variable rangeVar = ncfile.findVariable("range");
                Array ranges = rangeVar.read();
                
                float prevMeters = 0.0f;
                float totalDelta = 0.0f;
                for(int p=0;p<total_num_gates;p++){
                    float rangeMeters = ranges.getFloat(p);
                    if (p != 0){
                        float delta = rangeMeters-prevMeters;
                        totalDelta += delta;
                       // LOG.debug("Range is "+rangeMeters+" with delta "+delta);
                        prevMeters = rangeMeters;
                    }
                }
                
                // Ok since I don't have variable range, going to take the average
                // of all the ranges for the moment 'hack'
                float rangeDiffKM = (totalDelta/total_num_gates)/1000.0f;
                
                // Ok try to make a radialset....bleh.  We'll have to rework stuff make
                // our design a bit better I think...since volumes, etc can be in here
                // FIXME: check valid...
                
                Variable azVar = ncfile.findVariable("azimuth");
                Array azimuths = azVar.read();
                
                int num_radials = endRay - startRay + 1;
                r.radials = new Radial[num_radials];

                for (int i = 0; i < num_radials; ++i) {

                    // Cheating and it's wrong of course...assume it's squished into a 360 circle
                    // FIXME: get the actually stuff from netcdf data
                    // I'm just trying to get crap up on screen at moment
                    // Where's the beamwidth per ray in the CFRadial file?? eh?? eh???
                    float bw = 360.0f / ((float) (num_radials));
                   
                    float az = azimuths.getFloat(startRay+i);
                    float as = bw;
                    float gw = rangeDiffKM;  // assuming constant range (wrong technically)
                    float ny = 50;  // wrong

                    // This wraps around the column of the 2D array, _not_ a copy
                    Array1D<Float> col = values.getCol(i);
                    if (col != null) {
                        if (r.maxGateNumber < col.size()) {
                            r.maxGateNumber = col.size();
                        }
                    }
                    r.radials[i] = new Radial(az, bw, as, gw, ny, col, i);
                }

            } catch (Exception e) { // FIXME: what to do if anything?
                LOG.warn("Exception CFRadial is " + e.toString());
            }
        }
    }
}
