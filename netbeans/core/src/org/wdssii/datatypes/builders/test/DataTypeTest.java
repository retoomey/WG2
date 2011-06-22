package org.wdssii.datatypes.builders.test;

import java.util.HashMap;
import java.util.Map;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.geom.Location;
import org.wdssii.index.Index;
import org.wdssii.index.IndexRecord;

/**
 *  Root class of generators for test DataTypes.  Very alpha, will
 * need a lot of work still.
 * 
 * @author Robert Toomey
 */
public abstract class DataTypeTest {
    
    /** Try to create a DataType by reflection.  This is called from TestBuilder by reflection
     */
    public abstract DataType createTest(IndexRecord sourceRecord, boolean sparse);
    
    /** Fill a memento */
    public void fill(IndexRecord sourceRecord, DataTypeMemento m, boolean sparse) {
        // We can't set typename, since we're a test there's no data.
        // subclasses should set the typename explicitly
        // Hummm....
        
        // For now, center at OKC
        double lat = 35.333;
        double lon = -97.278;
        double ht = 384.0;
        Location loc = new Location(lat, lon, ht / 1000);
        m.originLocation = loc;    
        
        Map<String, String> attr = new HashMap<String, String>();
        attr.put("Unit", "dimensionless");
        m.attriNameToValue = attr;
    }

    public void createFakeRecords(Index index) {
    }
}
