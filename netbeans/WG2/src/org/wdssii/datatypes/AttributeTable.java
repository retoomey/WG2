package org.wdssii.datatypes;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Interface for defining attribute table. FIXME: Or do we want this to be a
 * class...? With attribute tables, RadialSet should return a count for each
 * pixel type like any raster in ArcMap. DataTable should return its regular
 * data..pretty much the same thing. Maps need to return their data as well so
 * not sure on design yet.
 *
 * @author Robert Toomey
 */
public interface AttributeTable {

    /**
     * Return a list of attribute columns by name
     */
    public List<String> getAttributeColumns();

    /** Return a named column of table */
    public AttributeColumn getAttributeColumn(String key);
    
    /**
     * Define a generic attribute column FIXME: support
     * strings/numbers/locations as base types? Also do we need more/fewer
     * functions here...
     */
    public static interface AttributeColumn {

        /**
         * Return name of this column
         */
        public String getName();

        public String getUnit();

        public List<String> getValues();

        public Iterator getIterator();

        /**
         * Get column value as a string
         */
        public String getValue(int row);

        /**
         * Get column value as a float
         */
        public float getFloat(int row);

        public int getNumRows();

        public Map<String, Integer> summerize();
    }
}
