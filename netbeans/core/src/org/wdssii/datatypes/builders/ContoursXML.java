package org.wdssii.datatypes.builders;

import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.wdssii.datatypes.Contours;
import org.wdssii.datatypes.Contours.Contour;
import org.wdssii.geom.Location;

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

    protected final String XML_CONTOURDATA = "contourdata";
    protected final String XML_CONTOUR = "contour";
    protected final String XML_ARRAY = "array";
    protected final String XML_LOCATIONDATA = "locationdata";

    /** Initialize a data table from an XMLStreamReader 
     * @throws XMLStreamException
     * Overrides with covariance */
    @Override
    public Contours createFromXML(XMLStreamReader p) throws XMLStreamException {
        // Two top tags:
        // <contours>
        //  (1) <datatype>  which is metadata on the table
        //  
        // </contours>
        Contours contours = new Contours();
        DataTypeXMLHeader header = null;
        try {
            String tag = null;
            String startTag = p.getLocalName();   // <contour>
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, startTag)) {
                    System.out.println("End tag " + startTag);
                    break;
                } else if ((tag = haveStartTag(p)) != null) {
                    if (XML_DATATYPE.equals(tag)) {             // <datatype>
                        header = readXML_datatype(p);
                        //contours.setDatatypeHeader(header);
                        contours.setLocation(header.location);
                        contours.setTime(header.time);
                        contours.setAttributes(header.attriNameToValue);
                        contours.setUnitsForAttributes(header.attriNameToUnits);
                    } else {
                        readXML_Contourdata(contours, p);          // <contourdata>
                    }
                }
            }
        } catch (Exception e) {
            // Recover on any exception by returning a null table
            System.out.println("XML PARSING ERROR " + e.toString());
        }

        return contours;
    }

    /** Read a contourdata tag.  Basically a wrapper for the contourdata_array below*/
    protected boolean readXML_Contourdata(Contours c, XMLStreamReader p) throws XMLStreamException {

        // <contourdata>
        // (1) <array>
        boolean success = false;
        String contourdataTag = p.getLocalName();
        if (XML_CONTOURDATA.equals(contourdataTag)) { // <contourdata>

            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, contourdataTag)) { // </contourdata>
                    System.out.println("End tag " + contourdataTag);
                    break;
                } else if (isStartTag(p)) {

                    // Handle each possible subtag.  Note these can be out of order and repeat.
                    if (readXML_contourdata_array(c, p)) { // <array> (break because we only want one)
                        success = true;
                        break;  // All we expect is ONE array subtag, probably could read multiple no problem
                    }
                }
            }
        }
        return success;
    }

    /** Read an array of contour(s)
     * <array length="n">
     * (n) <contour>
     */
    protected boolean readXML_contourdata_array(Contours c, XMLStreamReader p) throws XMLStreamException {
        boolean success = false;
        int contourCount = 0;
        String arrayTag = p.getLocalName();
        if (XML_ARRAY.equals(arrayTag)) { // <array>

            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, arrayTag)) { // </arrayTag>
                    System.out.println("End tag " + arrayTag);

                    break;
                } else if (isStartTag(p)) {

                    // Handle each possible sub-tag.  Note these can be out of order and repeat.
                    if (readXML_Contour(c, p)) {
                        contourCount++;
                    }
                }
            }
        }
        success = (contourCount > 0);
        System.out.println("***********WE GOT THIS MANY CONTOURS " + contourCount);
        return success;
    }

    /** Read a single contour 
     * <contour>
     *   <datatype>
     *   <locationdata>
     */
    protected boolean readXML_Contour(Contours c, XMLStreamReader p) throws XMLStreamException {

        // <contour>
        // (1)  <datatype>  header with spacetime ref/attributes
        // (1) <locationdata> 
        //        <array>
        //       (n) <location>
        boolean success = false;
        String contourTag = p.getLocalName();
        if (XML_CONTOUR.equals(contourTag)) { // <contour>

            // Create a new contour to fill in...
            Contour c1 = new Contour();

            // Create a header for contour.
            DataTypeXMLHeader header = new DataTypeXMLHeader();
            boolean foundHeader = false;

            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, contourTag)) { // </contour>
                    System.out.println("End tag " + contourTag);

                    break;
                } else if (isStartTag(p)) {

                    if (readXML_datatype(header, p)) {	// <datatype>
                        foundHeader = true;
                    } else // <locationdata>
                    if (readXML_locationdata(c1, p)) {
                    }
                }
            }

            if (foundHeader) {
                c1.setDataTypeHeader(header);
                c.addContour(c1);
                success = true;
            } else {
                // Warn that no header was found.....
            }
        }
        return success;

    }

    /** Read an array of location(s)
     * <locationdata>
     *   <array length="n">
     *   (n) <location>
     */
    protected boolean readXML_locationdata(Contour c, XMLStreamReader p) throws XMLStreamException {
        boolean success = false;
        String locationdataTag = p.getLocalName();
        if (XML_LOCATIONDATA.equals(locationdataTag)) {

            ArrayList<Location> list = new ArrayList<Location>();
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, locationdataTag)) { // </contour>
                    System.out.println("End tag " + locationdataTag);

                    break;
                } else if (isStartTag(p)) {
                    // Handle each possible subtag.  Note these can be out of order and repeat.
                    if (readXML_locationdata_array(list, p)) { // <array> (break because we only want one)
                        success = true;
                        break;  // All we expect is ONE array subtag, probably could read multiple no problem
                    }
                }
                success = true;
                c.setLocations(list);
            }
        }
        return success;
    }

    /** Read an array of locations(s)
     * <array length="n">
     * (n) <location>
     * This is a node, so it returns null or a new object on success
     */
    protected boolean readXML_locationdata_array(ArrayList<Location> list, XMLStreamReader p) throws XMLStreamException {
        boolean success = false;
        int locationCount = 0;
        String arrayTag = p.getLocalName();
        if (XML_ARRAY.equals(arrayTag)) { // <array>

            // Create a location buffer for each
            Location loc = new Location(0, 0, 0);

            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, arrayTag)) { // <//array>
                    System.out.println("End tag " + arrayTag);

                    break;
                } else if (isStartTag(p)) {

                    // Handle each possible sub-tag.  Note these can be out of order and repeat.
                    if (readXML_location(p, loc)) {
                        // add location to the location data array and create a new buffer
                        list.add(loc);
                        locationCount++;

                        // Recreate a new object reference for next pass
                        loc = new Location(0, 0, 0);
                    }
                }
            }
            success = true;
        }
        success = (locationCount > 0);
        System.out.println("***********WE GOT THIS MANY LOCATIONS " + locationCount);
        return success;
    }
}
