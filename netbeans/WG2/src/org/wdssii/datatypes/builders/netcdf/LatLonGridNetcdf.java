package org.wdssii.datatypes.builders.netcdf;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.datatypes.LatLonGrid.LatLonGridMemento;
import org.wdssii.datatypes.builders.netcdf.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.storage.Array2D;
import ucar.nc2.NetcdfFile;

/**
 *  Create a LatLonGrid from a Netcdf file
 * 
 * @author Robert Toomey
 */
public class LatLonGridNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private final static Logger LOG = LoggerFactory.getLogger(PPIRadialSetNetcdf.class);

    /** Try to create a LatLonGrid by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseLatLonGrid"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        LatLonGridMemento m = new LatLonGridMemento();
        m.datametric = LatLonGrid.createDataMetric();
        this.fillFromNetcdf(m, ncfile, sparse);
        return new LatLonGrid(m);
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

        if (m instanceof LatLonGridMemento) {
            LatLonGridMemento r = (LatLonGridMemento) (m);

            float latres = 0;
            float lonres = 0;
            Array2D<Float> grid = null;
            try {
                latres = ncfile.findGlobalAttribute("LatGridSpacing").getNumericValue().floatValue();
                lonres = ncfile.findGlobalAttribute("LonGridSpacing").getNumericValue().floatValue();
                try {
                    grid = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, r.typeName, r.datametric)
                            : NetcdfBuilder.readArray2Dfloat(ncfile, r.typeName, r.datametric);
                } catch (OutOfMemoryError mem) {
                    LOG.warn("Running out of ram trying to read in LatLonGrid data (FIXME)");
                }
            } catch (Exception e) {
                // FIXME: recover or throw instead, not 100% sure yet
                LOG.warn("LatLonGrid failing " + e.toString());
            }
            r.deltaLat = latres;
            r.deltaLon = lonres;
            r.values = grid;
        }
    }
}
