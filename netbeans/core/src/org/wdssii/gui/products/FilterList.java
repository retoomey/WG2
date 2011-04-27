package org.wdssii.gui.products;

import java.util.ArrayList;

import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.filters.LowCutFilter;
import org.wdssii.gui.products.filters.StormRMFilter;

/** This object is a ColorMap plus a list of DataFilters 
 * FIXME: a ColorMap should become just another Filter.
 * Filter should have ability to change colors...say I want a filter that turns off 'blue' for example...
 * 
 * @author Robert Toomey
 *
 */
public class FilterList {

    private ColorMap myColorMap = null;
    ArrayList<DataFilter> myFilters = new ArrayList<DataFilter>();

    public FilterList() {
    }

    public void createFiltersForProduct(Product p) {
        // Add proof of concept filters:  FIXME: need generic interface
        myFilters.clear();
        myFilters.add(new StormRMFilter());
        myFilters.add(new LowCutFilter());
        if (p != null) {
            setColorMap(p.getColorMap());  // hummm circular...
        }
    }

    public void setColorMap(ColorMap c) {
        myColorMap = c;
    }

    public ArrayList<DataFilter> getFilterList() {
        return myFilters;
    }

    /** Allow filters to do any prep work for a volume generation.
     * This is for speed during things like vslice rendering or isosurfaces
     * @param v
     */
    public void prepForVolume(ProductVolume v) {
        for (DataFilter d : myFilters) {
            d.prepFilterForVolume(v);
        }
    }

    /** Fill a ColorMapOutput given original input */
    public void fillColor(ColorMapOutput out, DataTypeQuery q, boolean filter) {

        // Note that colormap is going to become a filter
        if (filter) {
            myColorMap.fillColor(out, f(q));
        } else {
            myColorMap.fillColor(out, q.inDataValue);
        }
    }

    /** Apply the filters */
    public float f(DataTypeQuery q) {
        float value = q.inDataValue;
        for (DataFilter d : myFilters) {
            d.f(q);
        }
        value = q.outDataValue;
        return value;
    }

    /** Get a unique key for the state of the filter list */
    public String getFilterKey(boolean useFilters) {
        String filterKey = "";
        if (useFilters) {
            for (DataFilter d : myFilters) {
                filterKey += d.getKey();
            }
        }
        return filterKey;
    }
}
