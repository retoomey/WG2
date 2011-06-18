package org.wdssii.datatypes.builders.netcdf;

import java.util.ArrayList;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.LatLonHeightGrid;
import org.wdssii.datatypes.LatLonHeightGrid.LatLonHeightGridMemento;
import ucar.nc2.NetcdfFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.storage.Array3Dfloat;
import org.wdssii.storage.Array3DfloatRAM;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Variable;

/**
 * Create a LatLonHeightGrid from a Netcdf file
 * 
 * @author Robert Toomey
 */
public class LatLonHeightGridNetcdf extends DataTypeNetcdf {

    /** The log for errors */
    private static Log log = LogFactory.getLog(RadialSetNetcdf.class);

    /** Try to create a LatLonHeightGrid by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the Netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    @Override
    public DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse) {

        LatLonHeightGridMemento m = new LatLonHeightGridMemento();
        m.datametric = LatLonHeightGrid.createDataMetric();
        m.datametric.maxValue = 100;
        m.datametric.minValue = -10;
        this.fillFromNetcdf(m, ncfile, sparse);
        return new LatLonHeightGrid(m);
    }

    /** Fill a memento from Netcdf data. */
    @Override
    public void fillFromNetcdf(DataTypeMemento m, NetcdfFile ncfile, boolean sparse) {

        /** Let super fill in the defaults */
        super.fillFromNetcdf(m, ncfile, sparse);

        if (m instanceof LatLonHeightGridMemento) {
            LatLonHeightGridMemento llgm = (LatLonHeightGridMemento) (m);

            float latres = 0;
            float lonres = 0;
            int numLats = 0;
            int numLons = 0;
            Array3Dfloat grid = null;
            ArrayList<Float> theHeights = new ArrayList<Float>();

            try {
                // Angles in degrees
                latres = ncfile.findGlobalAttribute("LatGridSpacing").getNumericValue().floatValue();
                lonres = ncfile.findGlobalAttribute("LonGridSpacing").getNumericValue().floatValue();

                // Try to read in the heights...
                Variable heightVariable = ncfile.findVariable("Height");
                int numHeights = ncfile.getDimensions().get(0).getLength();
                Array heights = heightVariable.read();
                Index heightIndex = heights.getIndex();
                for (int i = 0; i < numHeights; i++) {
                    heightIndex.set(i);
                    theHeights.add(heights.getFloat(heightIndex));
                }

                numLats = ncfile.getDimensions().get(1).getLength();
                numLons = ncfile.getDimensions().get(2).getLength();

                try {
                    // Read the 3D grid in...
                    // grid = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, r.typeName, r.datametric)
                    //         : NetcdfBuilder.readArray2Dfloat(ncfile, r.typeName, r.datametric);
                    grid = new Array3DfloatRAM(numHeights, numLats, numLons, DataType.MissingData);


                    // FIXME: This is a quick mess to get it working...
                    // Need to generalize this stuff
                    if (sparse) {
                        Variable data = ncfile.findVariable(llgm.typeName);
                        //float backgroundValue = data.findAttribute("BackgroundValue").getNumericValue().floatValue();
                        Variable pixelxVariable = ncfile.findVariable("pixel_x"); // height?
                        Variable pixelyVariable = ncfile.findVariable("pixel_y"); // numlat?
                        Variable pixelzVariable = ncfile.findVariable("pixel_z"); // numlon?
                        Variable countVariable = null;
                        try {
                            countVariable = ncfile.findVariable("pixel_count");
                        } catch (Exception e) {
                            // ok the length will be 1 then
                        }
                        Array data_values = data.read();
                        Array pixelx_values = pixelxVariable.read();
                        Array pixely_values = pixelyVariable.read();
                        Array pixelz_values = pixelzVariable.read();
                        Array count_values = (countVariable == null) ? null : countVariable.read();
                        int num_pixels = pixelxVariable.getDimension(0).getLength();
                        Index pixel_index = data_values.getIndex();

                        int actualData = 0;
                        for (int i = 0; i < num_pixels; ++i) {

                            // Get each value at pixel...
                            pixel_index.set(i);
                            float value = data_values.getFloat(pixel_index);
                            short x = pixelx_values.getShort(pixel_index);  // lat values
                            short y = pixely_values.getShort(pixel_index); // lon values
                            short z = pixelz_values.getShort(pixel_index);  // height values
                            int count = (count_values == null) ? 1 : count_values.getInt(pixel_index);
                            for (int j = 0; j < count; ++j, ++y) {
                                if (y == numLons) {
                                    y = 0;
                                    ++x;
                                }
                                if (x == numLats) {
                                    x = 0;
                                    ++z;
                                }
                                grid.set(z, x, y, value);
                                actualData++;
                            }

                        }
                    }

                } catch (OutOfMemoryError mem) {
                    log.warn("Running out of ram trying to read in LatLonHeightGrid data (FIXME)");
                }

            } catch (Exception e) { // FIXME: what to do if anything?
                log.warn("Couldn't create radial set from netcdf file");
            }

            // Fill in memento for LatLonHeightGrid constructor
            llgm.heightsMeters = theHeights;
            llgm.data = grid;
            llgm.latResDegs = latres;
            llgm.lonResDegs = lonres;
            llgm.numLats = numLats;
            llgm.numLons = numLons;
        }
    }
}
