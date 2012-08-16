package org.wdssii.datatypes.builders.netcdf;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.storage.Array2Dfloat;
import ucar.nc2.NetcdfFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.WindField;
import org.wdssii.datatypes.WindField.WindFieldMemento;
import org.wdssii.datatypes.builders.NetcdfBuilder.NetcdfFileInfo;

/**
 *  Create a WindField from a Netcdf file
 * 
 * @author Robert Toomey
 */
public class WindFieldNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private static Logger log = LoggerFactory.getLogger(PPIRadialSetNetcdf.class);

    /** Try to create a LatLonGrid by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseLatLonGrid"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        WindFieldMemento m = new WindFieldMemento();
        m.datametric = WindField.createDataMetric();
        this.fillFromNetcdf(m, ncfile, sparse);
        return new WindField(m);
    }

    @Override
    public void fillNetcdfFileInfo(NetcdfFile ncfile, NetcdfFileInfo info) {
        super.fillNetcdfFileInfo(ncfile, info);
        info.Choice = "Missing"; // FIXME
    }
        
    /** Fill a memento from Netcdf data. */
    @Override
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {

        /** Let super fill in the defaults */
        super.fillFromNetcdf(m, ncfile, sparse);

        if (m instanceof WindFieldMemento) {
            WindFieldMemento w = (WindFieldMemento) (m);

            float latres = 0;
            float lonres = 0;
            Array2Dfloat gridu = null;
            Array2Dfloat gridv = null;

            try {
                // These exist in windfield as well
                latres = ncfile.findGlobalAttribute("LatGridSpacing").getNumericValue().floatValue();
                lonres = ncfile.findGlobalAttribute("LonGridSpacing").getNumericValue().floatValue();

                try {
                    gridu = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, "uArray", m.datametric)
                            : NetcdfBuilder.readArray2Dfloat(ncfile, "uArray", m.datametric);
                    gridv = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, "vArray", null)
                            : NetcdfBuilder.readArray2Dfloat(ncfile, "vArray", null);

                } catch (OutOfMemoryError mem) {
                    // Windfield is currently a LOT smaller than conus..shouldn't see this
                    log.warn("Running out of ram trying to read in WindField data (FIXME)");
                }
            } catch (Exception e) {
                // FIXME: recover or throw instead, not 100% sure yet
                log.warn("WindField failing " + e.toString());
            }
            w.deltaLat = latres;
            w.deltaLon = lonres;
            w.uArray = gridu;
            w.vArray = gridv;

            if (gridu != null) {
                log.info("Windfield managed to read in uArray of " + gridu.size());
            } else {
                log.warn("Windfield didn't get the uArray");
            }
            if (gridv != null) {
                log.info("Windfield managed to read in vArray of " + gridv.size());
            } else {
                log.warn("Windfield didn't get the vArray");
            }
        }
    }
}
