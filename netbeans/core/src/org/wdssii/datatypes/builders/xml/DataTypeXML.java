package org.wdssii.datatypes.builders.xml;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.wdssii.datatypes.DataType;
import org.wdssii.geom.Location;

/** Base class for reading/writing a DataType with XML using STAX
 * Shared basic tags are here for subclasses to use.
 * Reading is much more complicated than writing.
 * 
 * Every datatype we write out will have a datatype header with meta-data
 * 
 * We return a new object only when we have to, otherwise we fill in
 * the fields of a passed object (this helps speed during parsing which makes the GUI more responsive)
 * 
 * @author Robert Toomey
 *
 */
public abstract class DataTypeXML {

    // Tags and fields we read/write.  Use constants to make sure input/output use exact same tags
    protected final String XML_DATATYPE = "datatype";
    protected final String XML_STREF = "stref";
    protected final String XML_ATTR = "attr";
    protected final String XML_ANGLE = "angle";
    protected final String XML_LOCATION = "location";
    protected final String XML_LAT = "lat";
    protected final String XML_LON = "lon";
    protected final String XML_HT = "ht";
    protected final String XML_TIME = "time";
    protected final String XML_BASIC_DATE = "secondsSinceEpoch";
    protected final String XML_ATTR_DATACOLUMN = "datacolumn";
    protected final String XML_ATTR_ITEM = "item";

    /** Holder class for the XML header (the 'datatype' tag information)  */
    public static class DataTypeXMLHeader {

        /** datatype name such as 'Mesonet' */
        public String datatype = null;
        /** The stref location */
        public Location location = new Location(0, 0, 0);
        /** The stref time */
        public Date time = null;
        /** A map of attr name to value */
        public Map<String, String> attriNameToValue = new HashMap<String, String>();
        /** A map of attr name to units */
        public Map<String, String> attriNameToUnits = new HashMap<String, String>();
    }

    /** Holder class for unit/value attributes */
    public static class UnitValueXML {

        public String unit;
        public String value;
    }
    /** Global buffer to avoid newing on the fly */
    protected UnitValueXML uvXML = new UnitValueXML();

    /** Create a DataType from an XMLStreamReader.  Subclasses should override with
     * covariance 
     * @param p	the XMLStreamReader with the open XML
     * @return the new DataType or null on failure
     * @throws XMLStreamException
     */
    public abstract DataType createFromXML(XMLStreamReader p) throws XMLStreamException;

    /** Read the standard datatype header tag.
     * Each datatype has a main one of these, but some products like Contours reuse it
     * for each individual contour */
    protected DataTypeXMLHeader readXML_datatype(XMLStreamReader p) throws XMLStreamException {

        DataTypeXMLHeader buffer = null;
        String tagCheck = p.getLocalName();
        if (XML_DATATYPE.equals(tagCheck)) {
            System.out.println("<Datatype> tag reading....");

            String dataTypeName = "UNKNOWN";

            // <datatype name="Mesonet"
            int count = p.getAttributeCount();
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);
                if ("name".equals(name)) {
                    dataTypeName = value;
                }
            }
            System.out.println("datatype reading: name set to " + dataTypeName);
            // <datatype>
            // (1) <stref>
            // (n)  <attr>
            buffer = new DataTypeXMLHeader();
            String startTag = p.getLocalName();
            while (p.hasNext()) {
                p.next();
                String tag = null;
                if (isEndTag(p, startTag)) {
                    break;
                } else if ((tag = haveStartTag(p)) != null) {
                    if (XML_STREF.equals(tag)) {
                        readXML_stref(p, buffer);
                    } else if (XML_ATTR.equals(tag)) {
                        readXML_attr(p, buffer);
                    }
                }
            }
            buffer.datatype = dataTypeName;
            System.out.println("<Datatype> tag READ");

        }
        return buffer;
    }

    /** Read the standard datatype header tag.
     * Each datatype has a main one of these, but some products like Contours reuse it
     * for each individual contour */
    protected boolean readXML_datatype(DataTypeXMLHeader buffer, XMLStreamReader p) throws XMLStreamException {

        boolean success = false;
        String datatypeTag = p.getLocalName();
        if (XML_DATATYPE.equals(datatypeTag)) {
            String dataTypeName = "UNKNOWN";

            // <datatype name="Mesonet"
            int count = p.getAttributeCount();
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);
                if ("name".equals(name)) {
                    dataTypeName = value;
                }
            }
            buffer.datatype = dataTypeName;
            System.out.println("datatype reading: name set to " + dataTypeName);

            // <datatype>
            // (1) <stref>
            // (n)  <attr>
            while (p.hasNext()) {
                p.next();
                String tag = null;
                if (isEndTag(p, datatypeTag)) { // </datatype>
                    break;
                } else if ((tag = haveStartTag(p)) != null) {
                    if (XML_STREF.equals(tag)) {
                        readXML_stref(p, buffer);
                    } else if (XML_ATTR.equals(tag)) {
                        readXML_attr(p, buffer);
                    }
                }
            }
            success = true;
        }
        return success;
    }

    /** Read a stref tag into a SpaceTimeRefXML
     * <stref>
     *   <location..>
     *   <time..>
     */
    protected void readXML_stref(XMLStreamReader p, DataTypeXMLHeader header) throws XMLStreamException {
        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next();
            String tag = null;
            if (isEndTag(p, startTag)) {
                break;
            } else if ((tag = haveStartTag(p)) != null) {
                if (XML_LOCATION.equals(tag)) {
                    readXML_location(p, header.location);
                } else if (XML_TIME.equals(tag)) {
                    header.time = readXML_time(p);
                }
            }
        }
    }

    /** Read a attribute tag into a SpaceTimeRefXML
     * Looks like an attr just has a datacolumn with a SINGLE item. 
     * Currently we'll treat this datacolumn xml as different from DataTableXML which expects (N)
     * values in the column.  Most likely it was just shared code in the c++ version.
     * <attr>
     *   <datacolumn..>
     *     <item ...>
     */
    protected void readXML_attr(XMLStreamReader p, DataTypeXMLHeader stref) throws XMLStreamException {
        String startTag = p.getLocalName();
        String name = null;
        String value = null;
        String units = null;
        int hitCount = 0;

        // Could read 'name' from the attr tag and make sure it matches
        // the datacolumn.
        while (p.hasNext()) {
            p.next();
            String tag = null;
            if (isEndTag(p, startTag)) { // </attr>
                break;
            } else if ((tag = haveStartTag(p)) != null) {
                if (XML_ATTR_DATACOLUMN.equals(tag)) {  // <datacolumn>

                    // Find the DataColumn name and units....
                    int count = p.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        QName attribute = p.getAttributeName(i);
                        String attrName = attribute.toString();
                        String attrValue = p.getAttributeValue(i);
                        if ("name".equals(attrName)) {
                            name = attrValue;
                            hitCount++;
                        } else if ("units".equals(attrName)) {
                            units = attrValue;
                            hitCount++;
                        }
                    }
                    while (p.hasNext()) {
                        p.next();
                        if (isEndTag(p, XML_ATTR_DATACOLUMN)) { // </datacolumn>
                            break;
                        } else if ((tag = haveStartTag(p)) != null) {
                            if (XML_ATTR_ITEM.equals(tag)) {			// <item value=
                                int count2 = p.getAttributeCount();
                                for (int i = 0; i < count2; i++) {
                                    QName attribute = p.getAttributeName(i);
                                    String attrName = attribute.toString();
                                    String attrValue = p.getAttributeValue(i);
                                    if ("value".equals(attrName)) {
                                        value = attrValue;
                                        hitCount++;
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }
        if (hitCount > 2) {
            stref.attriNameToValue.put(name, value);  // temp for testing
            stref.attriNameToUnits.put(name, units);
        } else {
            // warn?
        }
    }

    /*
    Element attr = doc.createElement("attr");
    attr.setAttribute("name", entry.getKey());
    Element dc = doc.createElement("datacolumn");
    dc.setAttribute("name", entry.getKey());
    dc.setAttribute("units", "dimensionless");
    Element value = doc.createElement("item");
    value.setAttribute("value", entry.getValue());
    dc.appendChild(value);
    attr.appendChild(dc);
    datatype.appendChild(attr);
     */
    /** Pull a Location from XML into a buffer
     * <location>
     *   <lat></lat>
     *   <lon></lon>
     *   <ht></ht>
     * @param p stream already at the location tag
     * @return new location or null
     * @throws XMLStreamException
     */
    protected boolean readXML_location(XMLStreamReader p, Location buffer) throws XMLStreamException {
        float lat = Float.NaN;
        float lon = Float.NaN;
        float ht = Float.NaN;
        boolean success = false;

        String locationTag = p.getLocalName();
        if (XML_LOCATION.equals(locationTag)) {
            String currentTag = null;
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, locationTag)) {
                    break; // Finish up
                } else if ((currentTag = haveStartTag(p)) != null) {
                    if (XML_LAT.equals(currentTag)) {
                        lat = readWrappedAngle(p);
                    } else if (XML_LON.equals(currentTag)) {
                        lon = readWrappedAngle(p);
                    } else if (XML_HT.equals(currentTag)) {
                        ht = readWrappedLength(p);
                    }
                }
            }

            // Put location together
            // FIXME: is comparing NaN here too slow or wrong?
            if ((lat != Float.NaN) && (lon != Float.NaN) && (ht != Float.NaN)) {
                System.out.println("Read in " + lat + ", " + lon + ", " + ht);
                //loc = new Location(lat, lon, ht/1000.0); // can throw if lon/lat/ht out of range
                buffer.init(lat, lon, ht / 1000.0);
                success = true;
            }
        }
        return success;
    }

    /** Read an angle wrapped by a single named tag 
     * <lat>
     *   <angle ... >
     * </lat>
     * @throws XMLStreamException 
     */
    protected float readWrappedAngle(XMLStreamReader p) throws XMLStreamException {
        float angle = Float.NaN;
        String tag = null;

        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next();
            if (isEndTag(p, startTag)) {
                break; // Finish up
            } else if ((tag = haveStartTag(p)) != null) {
                if ("angle".equals(tag)) {
                    angle = readXML_angle(p);
                }
            }
        }
        return angle;
    }

    /** Read an length wrapped by a single named tag 
     * <ht>
     *   <length ... >
     * </ht>
     * @throws XMLStreamException 
     */
    protected float readWrappedLength(XMLStreamReader p) throws XMLStreamException {
        float length = Float.NaN;
        String tag = null;

        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next();
            if (isEndTag(p, startTag)) {
                break; // Finish up
            } else if ((tag = haveStartTag(p)) != null) {
                if ("length".equals(tag)) {
                    length = readXML_length(p);
                }
            }
        }
        return length;
    }

    // END NODES ********************************************************************************
    /** Return an angle in DEGREES, or NaN on failure..
     * This is a node, no subtags
     * <angle units="Degrees" value="34.123123"/> */
    protected float readXML_angle(XMLStreamReader p) {
        float angleDegrees = Float.NaN;
        // <angle units="Degrees" value="34.12323"/>
        readUnitValue(p, this.uvXML);
        String units = uvXML.unit;
        String number = uvXML.value;

        // Fill in angle from data...
        try {
            float v = Float.parseFloat(number);

            // FIXME: do unit conversion in future if wanted
            // Two types of angle that we handle
            if ("Degrees".equalsIgnoreCase(units)) {				// Degrees
                angleDegrees = v;
            } else if ("Radians".equalsIgnoreCase(units)) {		// Radians
                v = (float) Math.toDegrees(v);
                angleDegrees = v;
            }
        } catch (Exception e) { // Recover
        }

        return angleDegrees;
    }

    /** Return an length in Meters, or NaN on failure..
     * <length units="Meters" value="34.123123"/> */
    protected float readXML_length(XMLStreamReader p) {
        float lengthMeters = Float.NaN;
        // <length units="Meters" value="34.12323"/>
        readUnitValue(p, this.uvXML);
        String units = uvXML.unit;
        String number = uvXML.value;
        // Fill in angle from data...
        try {
            float v = Float.parseFloat(number);

            // Handle different length types					// Meters
            if ("Meters".equalsIgnoreCase(units)) {
                lengthMeters = v;
            }
        } catch (Exception e) {  // Recover
        }

        return lengthMeters;
    }

    /** Return a date in Meters, or NaN on failure..
     * <time units="Meters" value="34.123123"/> */
    protected Date readXML_time(XMLStreamReader p) {
        Date theDate = null;

        readUnitValue(p, this.uvXML);
        String units = uvXML.unit;
        String number = uvXML.value;

        try {
            if (units.equalsIgnoreCase(XML_BASIC_DATE)) {
                Calendar calendar = Calendar.getInstance();
                long milli = Long.parseLong(number) * 1000;
                System.out.println("Seconds was " + milli);
                calendar.setTimeInMillis(milli);
                theDate = calendar.getTime();
            }
        } catch (Exception e) {  // Recover
            System.out.println("XML Format time tag read exception " + e.toString());
        }
        System.out.println("Time tag read in " + theDate);
        return theDate;
    }

    // UTILITY FUNCTIONS ************************************************************************
    /** Utility function to check for end tag */
    protected boolean isEndTag(XMLStreamReader p, String end) {
        boolean isEndTag = false;
        if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
            String name = p.getLocalName();
            if (end.equals(name)) {
                isEndTag = true;
            }
        }
        return isEndTag;
    }

    /** Utility function to check for a new start tag */
    protected String haveStartTag(XMLStreamReader p) {
        String startTag = null;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            startTag = p.getLocalName();
        }
        return startTag;
    }

    /** Utility function to check for a new start tag */
    protected boolean isStartTag(XMLStreamReader p) {
        boolean haveStart = false;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            haveStart = true;
        }
        return haveStart;
    }

    /** A Utility fast snag of Unit/Value attributes which occur a lot within the XML
     * This is just to reduce code for common case
     * @param buffer
     */
    protected void readUnitValue(XMLStreamReader p, UnitValueXML buffer) {
        int count = p.getAttributeCount();
        buffer.unit = null;
        buffer.value = null;
        for (int i = 0; i < count; i++) {
            QName attribute = p.getAttributeName(i);
            String name = attribute.toString();
            String value = p.getAttributeValue(i);
            if ("units".equals(name)) {
                buffer.unit = value;
            } else if ("value".equals(name)) {
                buffer.value = value;
            }
        }
    }
}
