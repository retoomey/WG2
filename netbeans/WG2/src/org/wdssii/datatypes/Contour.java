package org.wdssii.datatypes;

import java.util.ArrayList;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.geom.Location;

/** Contour holds a single contour
 * 
 *
 * @author Robert Toomey
 */
public class Contour {

    /** Points of the contour */
    private ArrayList<Location> locations;

    private DataTypeMemento m;
    
    public Contour(ArrayList<Location> list, DataTypeMemento d){
        locations = list;
        m = d;
    }
    
    /** Get the number of points in this contour */
    public int getSize() {
        int size;
        if (locations != null) {
            size = locations.size();
        }else{
            size = 0;
        }
        return size;
    }

    /** Get the location list for the contour */
    public ArrayList<Location> getLocations() {
        return locations;
    }

    public String getAttribute(String name) {
        return m.attriNameToValue.get(name);
    }
    
    /** Set the location list for the contour */
    public void setLocations(ArrayList<Location> list) {
        locations = list;
    }
}
