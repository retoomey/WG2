package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonGrid;
import org.wdssii.datatypes.LatLonGrid.LatLonGridQuery;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.filters.DataFilter.DataValueRecord;

/**
 * A ProductVolume consisting entirely of RadialSetProducts
 *
 * @author Robert Toomey
 *
 */
public class LatLonGridVolume extends IndexRecordVolume {

    private final static Logger LOG = LoggerFactory.getLogger(LatLonGridVolume.class);
    // Synchronize access to these...
    private final Object myProductLock = new Object();
    /**
     * The set of products
     */
    private ArrayList<Product> myProducts = new ArrayList<Product>();

    /**
     * Ok, looks like this will be called every time a product is requested???
     * Gonna have to do some sync work I'm thinking... Chart and VSlice may call
     * this at same time...
     */
    @Override
    public void initVirtual(Product init, boolean virtual) {

        super.initVirtual(init, virtual);
        // Create an array list of products and sort them...
        // This list can change over time due to autoupdates.
        // If these are NEW products, they will start background thread loading.
        StringBuilder newKey = new StringBuilder("");
        ArrayList<Product> newProducts = new ArrayList<Product>();
        Product first = init;
        ArrayList<Product> p = loadVolumeProducts(first.getIndexKey(), first.getRecord(), virtual);
        Iterator<Product> iter = p.iterator();
        while (iter.hasNext()) {
            Product product = iter.next();
            // Only add products that are loaded to the volume....
            // Note: some may still be loading...
            if (product.getRawDataType() != null) {
               // newProducts.add(product);
                newKey.append(product.getCacheKey());
            }
        }
        first.sortVolumeProducts(newProducts);

        // Need to lock changing out products for new ones.  This is because
        // Someone might be calling getValueAt below
        synchronized (myProductLock) {
            myKey = newKey.toString();
            myProducts = newProducts;
            //System.out.println("Init volume called... we have "+myRadials.size()+" products ready ");
            //myRecords = newRecords;
        }
        // LOG.debug("VOLUME COUNT IS "+p.size());
    }

    /**
     * Generate a key that uniquely determines this volume based on product data
     */
    @Override
    public String getKey() {
        synchronized (myProductLock) {
            return myKey;
        }
    }

    /**
     * Prepare filters for a batch value grab.
     */
    public void prepFilters(ArrayList<DataFilter> list) {
        if (list != null) {
            for (DataFilter d : list) {
                d.prepFilterForVolume(this);
            }
        }
    }
    public final static boolean myExperiment = false;

    /**
     * Get filtered value of from a volume location, store into ColorMapOutput.
     * This function needs to be callable by multiple threads at once, example:
     * 3D vslice render by GL thread, 2D table render by VSliceChart. So
     * synchronize if you 'share' any memory here
     */
    @Override
    public boolean getValueAt(Location loc, ColorMapOutput output, DataValueRecord out,
            FilterList list, boolean useFilters, VolumeValue v) {

        // Bleh this gonna slow us down...
        synchronized (myProductLock) {
            for (Product p : myProducts) {
                p.startLoading(); // Start loading if not already...
            }
        }
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
        float weightAtValue;

        LatLonGridQuery q = new LatLonGridQuery();
        q.inLocation = loc;
        q.inUseHeight = true;
        // Testing interpolation ability (alpha experiment)
        // This tells query to return a distance from closest point...
        // for each of the Lat, Lon, Height 'axis'
        if (myExperiment) {
            q.inNeedInterpolationWeight = true;
        }

        q.outDataValue = DataType.MissingData;

        int productNumber = 0;
        // Make sure the reading of data values is sync locked with updating in initProduct...
        synchronized (myProductLock) {

            for (int i = 0; i < myProducts.size(); i++) {
                DataType dt = myProducts.get(i).getRawDataType();

                LatLonGrid r = null;
                if (dt != null) {
                    r = (LatLonGrid) (dt);
                }
                if (r != null) {
                    r.queryData(q);
                    // Cheapest...first 'hit' gives us value
                    value = q.outDataValue;
                    if (DataType.isRealDataValue(value)) {
                        break;
                    }
                }
                productNumber++;
            }
        }

        q.inDataValue = value;
        q.outDataValue = value;
        //  q.outRadialSetNumber = productNumber;
        out.hWeight = q.outDistanceHeight;
        list.fillColor(output, q, useFilters);

        // Find a location value in our radial set collection...
        return true;
    }
}
