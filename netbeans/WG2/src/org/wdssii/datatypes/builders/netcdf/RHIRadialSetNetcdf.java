package org.wdssii.datatypes.builders.netcdf;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.RHIRadialSet;
import org.wdssii.datatypes.RHIRadialSet.RHIRadialSetMemento;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.datatypes.builders.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array2D;
import org.wdssii.storage.Array2DfloatAsTiles;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Create a RadialSet from a Netcdf file
 * 
 * This is a RadialSet which handles RHI, or 
 * Range Height Indicator radial sets, which have fixed azimuth and
 * changing elevation.
 * 
 * @author Robert Toomey
 */
public class RHIRadialSetNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private final static Logger LOG = LoggerFactory.getLogger(RHIRadialSetNetcdf.class);

    /** Try to create a RadialSet by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        RHIRadialSetMemento m = new RHIRadialSetMemento();
        m.datametric = RHIRadialSet.createDataMetric();
        this.fillFromNetcdf(m, ncfile, sparse);
        return new RHIRadialSet(m);
    }

    @Override
    public void fillNetcdfFileInfo(NetcdfFile ncfile, NetcdfFileInfo info){
        super.fillNetcdfFileInfo(ncfile, info);
        try{
            float elev = ncfile.findGlobalAttribute("Azimuth").getNumericValue().floatValue();
            info.Choice = Float.toString(elev);
        }catch(Exception e){
            info.Choice="Missing";
        }
    }
    
    public static Array2D<Float> makeArray2Dfloat(NetcdfFile ncfile,
            DataType.DataTypeMetric optional, int num_radials, int num_gates)
            throws IOException {

        Array2D<Float> values = new Array2DfloatAsTiles(num_radials, num_gates, 0.0f);
        if (optional != null) {
            optional.beginArray2D();
        }
        values.beginRowOrdered();
	float value = 0;
	boolean toggle = false;
        for (int j = 0; j < num_gates; ++j) {
            for (int i = 0; i < num_radials; ++i) {
		toggle = !toggle;
		if (toggle){
			value = 50.0f;
		}else{
			value = 80.0f;
		}
                values.set(i, j, value);
                if (optional != null) {
                    optional.updateArray2D(i, j, value);
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

        if (m instanceof RHIRadialSetMemento) {
            RHIRadialSetMemento r = (RHIRadialSetMemento) (m);
            try {
                
                //Variable v_elev = ncfile.findVariable("Elevation");
                Variable v_elev = ncfile.findVariable("Azimuth");
                Variable v_bw = ncfile.findVariable("BeamWidth");
                //Variable v_as = ncfile.findVariable("ElevationalSpacing");
                Variable v_as = ncfile.findVariable("AzimuthalSpacing");
                Variable v_gw = ncfile.findVariable("GateWidth");
                Variable v_ny = ncfile.findVariable("NyquistVelocity");
                //float azimuth = ncfile.findGlobalAttribute("Azimuth").getNumericValue().floatValue();
                float azimuth = ncfile.findGlobalAttribute("Elevation").getNumericValue().floatValue();
                float distToFirstGate = ncfile.findGlobalAttribute("RangeToFirstGate").getNumericValue().floatValue();
                float nyquist = DataType.MissingData;

                if (r.attriNameToValue != null) {
                    if (r.attriNameToValue.containsKey("Nyquist_Vel")) {
                        nyquist = Float.parseFloat(r.attriNameToValue.get("Nyquist_Vel"));
                    }
                }
                Array elev_values = v_elev.read();
                Array bw_values = v_bw.read();
                Array gw_values = v_gw.read();

                // optional
                Array as_values = null;
                if (v_as != null) {
                    as_values = v_as.read();
                }
                Array ny_values = null;
                if (v_ny != null) {
                    ny_values = v_ny.read();
                }

                // Valid for all info but the radials
		// Bleh constant angle..
		float az = 90.0f;
                r.fixedAngleDegs = az;
                r.rangeToFirstGate = distToFirstGate / 1000;
                // set up the co-ordinate system
                r.radarLocation = r.originLocation.getCPoint();
                r.myUz = r.radarLocation.minus(new CPoint(0, 0, 0)).unit();
                r.myUx = new CVector(0, 0, 1).crossProduct(r.myUz).unit();
                r.myUy = r.myUz.crossProduct(r.myUx);

                Array2D<Float> values = null;
                int num_radials = 0;
		//num_radials = 50;
		//int num_gates = 100;
                try {
                    values = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, r.typeName, r.datametric)
                            : NetcdfBuilder.readArray2Dfloat(ncfile, r.typeName, r.datametric);
		//	values = makeArray2Dfloat(ncfile, r.datametric, num_radials, num_gates);
                    num_radials = values.getX();
                } catch (Exception e) {
                    // If we can't get the values for any reason, 
                    // just make a zero size radial set (nice recovery)
                    LOG.warn("Couldn't create radials of radial set, leaving as empty");
                }

		num_radials=18; // 90/5

                r.radials = new Radial[num_radials];
                //Index radial_index = az_values.getIndex();
		float startelev = 0.0f;
		float deltaelev = 5.0f;
		float elev = startelev;

                for (int i = 0; i < num_radials; ++i) {
                    float bw = bw_values.getFloat(i);
                   // float as = (as_values == null) ? bw : as_values.getFloat(i);
		    float as = 5.0f; // 5 deg
                    float gw = gw_values.getFloat(i) / 1000; // meters to kms
                    float ny = (ny_values == null) ? nyquist : ny_values.getFloat(i);
                    
                    // This wraps around the column of the 2D array, _not_ a copy
                    Array1D<Float> col = values.getCol(i);
                    if (col != null) {
                        if (r.maxGateNumber < col.size()) {
                            r.maxGateNumber = col.size();
                        }
                    }
                    r.radials[i] = new Radial(elev, bw, as, gw, ny, col, i);
		    elev += deltaelev;
                }
            } catch (Exception e) { // FIXME: what to do if anything?
                LOG.warn("Couldn't create radial set from netcdf file");
            }
        }
    }
}
