package org.wdssii.datatypes.builders.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.wdssii.datatypes.Contour;
import org.wdssii.datatypes.Contours;
import org.wdssii.datatypes.Contours.ContoursMemento;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.geom.Location;
import org.wdssii.xml.Tag_contour;
import org.wdssii.xml.Tag_contours;

/** Parse read/write contour XML data files.
 * The XML for the contours is a bit dense, because of backward compatibility to read old data files
 * 
 * <contour>
 *   <datatype>
 *   <contourdata>
 *     <array length="n">
 *       <contour>
 *         <datatype...>
 *         <locationdata>
 *       
 * @author Robert Toomey
 *
 */
public class ContoursXML extends DataTypeXML {

    /** Initialize a data table from an XMLStreamReader 
     * @throws XMLStreamException
     * Overrides with covariance */
    @Override
    public Contours createFromXML(XMLStreamReader p) throws XMLStreamException {
        // Two top tags:
        // <contours>
        //  (1) <datatype>  which is metadata on the table
        //  (2) <coutourdata>
        // </contours>

        // Read in the document basically.
        Tag_contours t = new Tag_contours();
        t.processTag(p);
        
        // Create a memento and get data out of the tag we want...
        ContoursMemento c = new ContoursMemento();
        
        // Read top level datatype info into memento...
        TagToMemento(t.datatype, c);
        
        // Create the datatype
        Contours contours = new Contours(c);
        
        ArrayList<Tag_contour> theContours = t.contourdata.array.data;
        for(Tag_contour d:theContours){ 
            DataTypeMemento dtm = new DataTypeMemento();
            TagToMemento(d.datatype, dtm);
            ArrayList<Location> locs = d.locationdata.array.data;
            Contour n = new Contour(locs, dtm);
            contours.addContour(n);
        }
        return contours;
    }

}
