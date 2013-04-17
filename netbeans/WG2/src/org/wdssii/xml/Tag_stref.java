package org.wdssii.xml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.wdssii.geom.Location;

/**
 *  Our stref tag, which is a SpaceTime reference consisting of a Location
 * and a Date
 * 
 * This is considered a terminal tag in that there are no tags made below
 * this one.  The Location and Date are pulled as the actual objects.
 * FIXME: might make angle a tag or location
 * 
 * <pre>
 * {@code
 * <stref>
 *   <location>
 *    <lat>
 *      <angle units="Degrees" value="29.910000"/>
 *    </lat>
 *    <lon>
 *       <angle units="Degrees" value="-90.270000"/>
 *    </lon>
 *    <ht>
 *      <length units="Meters" value="0.000000"/>
 *    </ht>
 *   </location><time units="secondsSinceEpoch" value="1311802183"/>
 * </stref>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_stref extends Tag {

    public static class Angle {

        float degrees;
        float radians;
    }

    public static class Length {

        float meters;
    }
    /** The location we have if any */
    public Location location = new Location(0, 0, 0);
    /** The date time, if any  */
    public Date time;
    /** <stref name=> */
    public String name;

    /** Called within a parsing loop */
    @Override
    public void processChildren(XMLStreamReader p) {
        Date d = processTime(p);
        if (d!= null){ time = d; }
        processLocation(p, location);
    }

    /** Return a date in Meters, or NaN on failure..
     * <time units="Meters" value="34.123123"/> */
    public static Date processTime(XMLStreamReader p) {
        boolean success = false;
        Date theDate = null;
        if (atStart(p, "time")) {

            try {
                UnitValuePair uv = new UnitValuePair();
                readUnitValue(p, uv);
                String units = uv.unit;
                String number = uv.value;
                if (units.equalsIgnoreCase("secondsSinceEpoch")) {
                    Calendar calendar = Calendar.getInstance();
                    long milli = Long.parseLong(number) * 1000;
                    calendar.setTimeInMillis(milli);
                    theDate = calendar.getTime();
                }
                success = true;
            } catch (Exception e) {  // Recover
                theDate = new Date(); // Use current time
                // warn?
            }
        }
        return theDate;
    }

    /** Pull a Location from XML into a buffer
     * <pre>
     * {@code
     * <location>
     *   <lat></lat>
     *   <lon></lon>
     *   <ht></ht>
     * }
     * </pre>
     * @param p stream already at the location tag
     * @return new location or null
     * @throws XMLStreamException
     */
    public static boolean processLocation(XMLStreamReader p, Location buffer) {

        boolean success = false;

        if (atStart(p, "location")) {
            float lat = Float.NaN;
            float lon = Float.NaN;
            float ht = Float.NaN;
            String currentTag = null;
            try {
                while (p.hasNext()) {
                    p.next();
                    if (isEndTag(p, "location")) {
                        break; // Finish up
                    } else if ((currentTag = haveStartTag(p)) != null) {
                        if ("lat".equals(currentTag)) {
                            lat = readWrappedAngle(p);
                        } else if ("lon".equals(currentTag)) {
                            lon = readWrappedAngle(p);
                        } else if ("ht".equals(currentTag)) {
                            ht = readWrappedLength(p);
                        }
                    }
                }
            } catch (XMLStreamException ex) {
                // Warn?
            }

            // Put location together
            // FIXME: is comparing NaN here too slow?
            if (!Float.isNaN(lat) && !Float.isNaN(lon) && !Float.isNaN(ht)){
                //loc = new Location(lat, lon, ht/1000.0); // can throw if lon/lat/ht out of range
                buffer.init(lat, lon, ht / 1000.0);
                success = true;
            }
        }
        return success;
    }

    /** Pull a Location from XML into a buffer
     * <pre>
     * {@code
     * <array length="n"
     *   <location>
     * }
     * </pre>
     * @param p stream already at the location tag
     * @return new location or null
     * @throws XMLStreamException
     */
   /* public static boolean processLocationArray(XMLStreamReader p, ArrayList<Location> buffer) {

        boolean success = false;

        try {
            Location l = new Location(0,0,0);
            if (atStart(p, "array")) {

                while (p.hasNext()) {
                    p.next();
                    if (isEndTag(p, "array")) {
                        break; // Finish up
                    } else {
                        // Handle all subtags.
                       if (processLocation(p, l)){
                           buffer.add(l);
                           success = true; // true if we got at least one
                       }
                    }
                }

            }
        } catch (XMLStreamException ex) {
            // Warn?
        }
        return success;
    }
*/
    /** Read an angle wrapped by a single named tag 
     * <lat>
     *   <angle ... >
     * </lat>
     * @throws XMLStreamException 
     */
    private static float readWrappedAngle(XMLStreamReader p) throws XMLStreamException {
        float angle = Float.NaN;
        String tag = null;

        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next(); // Skip the 'lat' or 'lon' tag
            if (isEndTag(p, startTag)) {
                break; // Finish up
            } else if ((tag = haveStartTag(p)) != null) {
                if ("angle".equals(tag)) {
                    Angle a = new Angle();
                    processAngle(p, a);
                    angle = a.degrees;
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
    private static float readWrappedLength(XMLStreamReader p) throws XMLStreamException {
        float length = Float.NaN;
        String tag = null;

        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next(); // skip the 'ht' tag
            if (isEndTag(p, startTag)) {
                break; // Finish up
            } else if ((tag = haveStartTag(p)) != null) {
                if ("length".equals(tag)) {
                    Length l = new Length();
                    processLength(p, l);
                    length = l.meters;
                }
            }
        }
        return length;
    }

    /** Return an angle 
     * This is a node, no subtags
     * <angle units="Degrees" value="34.123123"/> */
    public static boolean processAngle(XMLStreamReader p, Angle a) {
        boolean success = false;

        if (atStart(p, "angle")) {

            try {
                UnitValuePair uv = new UnitValuePair();
                readUnitValue(p, uv);
                String units = uv.unit;
                String number = uv.value;

                float v = Float.parseFloat(number);

                // FIXME: do unit conversion in future if wanted
                // Two types of angle that we handle
                if ("Degrees".equalsIgnoreCase(units)) {                // Degrees
                    a.degrees = v;
                    a.radians = (float) Math.toRadians(v);
                } else if ("Radians".equalsIgnoreCase(units)) {		// Radians
                    a.degrees = (float) Math.toDegrees(v);
                    a.radians = v;
                }
                success = true;
            } catch (Exception e) { // Recover how?
            }
        }
        return success;
    }

    /** Return an length in Meters, or NaN on failure..
     * <length units="Meters" value="34.123123"/> */
    public static boolean processLength(XMLStreamReader p, Length a) {
        boolean success = false;

        if (atStart(p, "length")) {

            try {
                UnitValuePair uv = new UnitValuePair();
                readUnitValue(p, uv);
                String units = uv.unit;
                String number = uv.value;

                float v = Float.parseFloat(number);

                // Handle different length types
                if ("Meters".equalsIgnoreCase(units)) {
                    a.meters = v;
                    success = true;
                }
            } catch (Exception e) {  // Recover
            }

        }
        return success;
    }
}
