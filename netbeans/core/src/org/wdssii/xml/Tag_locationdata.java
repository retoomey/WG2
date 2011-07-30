package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;
import org.wdssii.geom.Location;
import org.wdssii.xml.Tag_array.ArraySubtagFactory;

/**
 *  Tag which has the following format:
 *  locationdata is just a holder tag for an <array> of <location>
 * 
 * <pre>
 * {@code
 * <locationdata>
 * (1) <array length="n">
 *  (n)   <location>
 * </locationdata>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_locationdata extends Tag {

    /** Due to generics, make a factory to create a Location for
     * the Tag_array of Locations
     */
    private static class TagFactory extends ArraySubtagFactory {

        @Override // with covariance
        public Location make() {
            return new Location(0, 0, 0);
        }

        @Override
        public boolean processTag(Object o, XMLStreamReader p) {
            boolean success = false;
            if (o instanceof Location) {
                Location l = (Location) (o);
                if (Tag_stref.processLocation(p, l)) {
                    success = true;

                }
            }
            return success;
        }
    }
    /** Our array of Locations */
    public Tag_array<Location> array = new Tag_array<Location>(new TagFactory());

    @Override
    public String tag() {
        return "locationdata";
    }

    @Override
    public void processChildren(XMLStreamReader p) {
        array.processTag(p);
    }
}