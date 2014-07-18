package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.filters.DataFilter.DataValueRecord;

/**
 * A ProductVolume consisting entirely of RadialSetProducts
 *
 * RadialSetVolume can be shared, so state such as VolumeValue has to be passed
 * in.
 *
 * @author Robert Toomey
 *
 */
public class PPIRadialSetVolume extends IndexRecordVolume {

    private final static Logger LOG = LoggerFactory.getLogger(PPIRadialSetVolume.class);
    /**
     * Synchronize access to myRadials
     */
    private final Object myRadialLock = new Object();
    /**
     * The set of radials
     */
    private ArrayList<Product> myRadials = new ArrayList<Product>();
    /**
     * The volume value objects for RadialSets
     */
    private final static ArrayList<VolumeValue> myVolumeValues;
    /**
     * Globally try to remember the name of the VolumeValue we are set to JUST
     * for RadialSets only.... all volume classes should do this.
     */
    private static String myGlobalVolumeValueChoice;

    // Post constructor stuff...set up all the ways of looking at the
    // volume data
    static {
        myVolumeValues = new ArrayList<VolumeValue>();
        VolumeValue normalOne = new RadialSetNormalValue();
        myVolumeValues.add(normalOne);
        myVolumeValues.add(new RadialSetLIValue());
        myVolumeValues.add(new RadialSetBIValue());
        myGlobalVolumeValueChoice = normalOne.getName();
    }

    /**
     * The menu list of VolumeValue choices. This lets user choose between
     * different types of interpolation, for example
     */
    @Override
    public List<String> getValueNameList() {
        ArrayList<String> stuff = new ArrayList<String>();
        for (VolumeValue v : myVolumeValues) {
            stuff.add(v.getName());
        }
        return stuff;
    }

    /**
     * Get the list name of the current volume value object, if any
     */
    @Override
    public VolumeValue getVolumeValue(String name) {
        ArrayList<String> stuff = new ArrayList<String>();
        for (VolumeValue v : myVolumeValues) {
            if (v.getName().equals(name)) {
                return v;
            }
        }
        return myVolumeValues.get(0);
    }

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
        ArrayList<Product> newRadials = new ArrayList<Product>();
        Product first = init;
        ArrayList<Product> p = loadVolumeProducts(first.getIndexKey(), first.getRecord(), virtual);
        Iterator<Product> iter = p.iterator();
        while (iter.hasNext()) {
            Product product = iter.next();
            
            // Always add radial to collection..might grow as autoupdate happens
            newRadials.add(product);
            
            // Only add products that are loaded to the volume key...
            // Note: some may still be loading...
            if (product.getRawDataType() != null) {
                newKey.append(product.getCacheKey());
            }
        }
        first.sortVolumeProducts(newRadials);

        // Need to lock changing out products for new ones.  This is because
        // Someone might be calling getValueAt below
        // Always update myRadials because autoUpdate could have brought a new
        // one in...
        synchronized (myRadialLock) {
            myKey = newKey.toString();
            myRadials = newRadials;
            //System.out.println("Init volume called... we have "+myRadials.size()+" products ready ");
            //myRecords = newRecords;
        }
    }

    /**
     * Generate a key that uniquely determines this volume based on product data
     */
    @Override
    public String getKey() {
        synchronized (myRadialLock) {
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
    public static boolean myExperiment = false;

    /**
     * Get filtered value of from a volume location, store into ColorMapOutput.
     * This function needs to be callable by multiple threads at once, example:
     * 3D vslice render by GL thread, 2D table render by VSliceChart. So
     * synchronize if you 'share' any memory here
     */
    @Override
    public boolean getValueAt(Location loc, ColorMapOutput output, DataValueRecord out,
            FilterList list, boolean useFilters, VolumeValue v) {
        if (v == null) {
            v = myVolumeValues.get(0);
        }
        // Bleh each time....could call a function before getValueAt
        synchronized (myRadialLock) {
            for (Product r : myRadials) {
                r.startLoading();
            }
        }
        return v.getValueAt(myRadialLock, myRadials, loc, output, out, list, useFilters);
    }

    /**
     * Get the Storm Relative Motion deltas for each product in our set.
     */
    public ArrayList<ArrayList<Float>> getSRMDeltas(float speed, float degrees) {

        ArrayList<ArrayList<Float>> list = new ArrayList<ArrayList<Float>>();
        synchronized (myRadialLock) {
            for (Product r : myRadials) {
                DataType d = r.getRawDataType();
                if (d != null) {
                    if (d instanceof PPIRadialSet) {
                        PPIRadialSet radial = (PPIRadialSet) (d);
                        ArrayList<Float> values = radial.createSRMDeltas(speed, degrees);
                        list.add(values);
                    }
                } else {
                    list.add(null); // filler
                }
            }
        }
        return list;
    }
}
