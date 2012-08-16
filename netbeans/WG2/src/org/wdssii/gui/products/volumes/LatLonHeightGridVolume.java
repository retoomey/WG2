package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonHeightGrid;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.filters.DataFilter.DataValueRecord;

/**
 *
 * @author Robert Toomey
 */
public class LatLonHeightGridVolume extends ProductVolume {

    private final Object myLock = new Object();
    Product myVolume = null;

    /** Ok, looks like this will be called every time a product is requested???
     * Gonna have to do some sync work I'm thinking...
     * Chart and VSlice may call this at same time...
     */
    @Override
    public void initVirtual(Product init, boolean virtual) {

        // Create an array list of products and sort them...
        // This list can change over time due to autoupdates.
        // If these are NEW products, they will start background thread loading.
        StringBuilder newKey = new StringBuilder("");
        Product theVolume = null;
        // Only add products that are loaded to the volume....
        if (init.getRawDataType() != null) {
            theVolume = init;
            newKey.append(init.getCacheKey());
        }

        // Need to lock changing out products for new ones.  This is because
        // Someone might be calling getValueAt below
        synchronized (myLock) {
            myKey = newKey.toString();
            myVolume = theVolume;
            //System.out.println("Init volume called... we have "+myRadials.size()+" products ready ");
            //myRecords = newRecords;
        }
    }

    /** Generate a key that uniquely determines this volume based on product data */
    @Override
    public String getKey() {
        synchronized (myLock) {
            return myKey;
        }
    }

    /** Prepare filters for a batch value grab. */
    public void prepFilters(ArrayList<DataFilter> list) {
        if (list != null) {
            for (DataFilter d : list) {
                d.prepFilterForVolume(this);
            }
        }
    }

    /** Get filtered value of from a volume location, store into ColorMapOutput.
     * This function needs to be callable by multiple threads at once, example: 3D vslice render by GL thread, 2D table render by VSliceChart.
     * So synchronize if you 'share' any memory here
     */
    @Override
    public boolean getValueAt(Location loc, ColorMapOutput output, DataValueRecord out,
            FilterList list, boolean useFilters, VolumeValue v) {

        // Maybe this could be a filter in the color map...  You could clip anything by height heh heh..
        // It would make sense for it to be a filter
        if (loc.getHeightKms() < 0) {
            output.setColor(255, 255, 255, 255);
            //output.red = output.green = output.blue = output.alpha = 255;
            output.filteredValue = 0.0f;
            return false;
        }

       // output.location.init(lat, lon, heightM / 1000.0);

        // Smooth in the vertical direction....?? how
        // We would need a weight based on range
        float value = DataType.MissingData;

        LatLonHeightGrid.LatLonHeightGridQuery q = new LatLonHeightGrid.LatLonHeightGridQuery();
        q.inLocation = loc;
        q.outDataValue = DataType.MissingData;

        // Make sure the reading of data values is sync locked with updating in initProduct...
        synchronized (myLock) {

            // For each radial in the radial set....
            //RadialSet r = iter.next().getRadialSet();
            // DataType dt = iter.next().getRawDataType();
            DataType dt = myVolume.getRawDataType();
            LatLonHeightGrid grid = null;
            if (dt != null) {
                grid = (LatLonHeightGrid) (dt);
            }
            if (grid != null) {

                grid.queryData(q);
            }
            value = q.outDataValue;
        }

        q.inDataValue = value;
        q.outDataValue = value;

        list.fillColor(output, q, useFilters);
        return true;
    }
}
