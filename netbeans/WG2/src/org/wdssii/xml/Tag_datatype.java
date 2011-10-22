package org.wdssii.xml;

import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamReader;

/**
 *  Our <datatype> tag which has the following format:
 * 
 * <pre>
 * {@code
 * <datatype name="datatype">
 * (1) <stref>...</stref>
 * (n) <attr>...</attr>
 * </datatype>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_datatype extends Tag {

    // Reflection attributes
    public String name;

    /** A <stref> tag */
    public Tag_stref stref = new Tag_stref();
    /** A map from name to values */
    public Map<String, String> attrValues = new TreeMap<String, String>();
    /** a map from name to units, usually the same size as the name map */
    public Map<String, String> attrUnits = new TreeMap<String, String>();

    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
        stref.processTag(p);
        processAttr(p, attrValues, attrUnits);
    }

    /** Process this nasty legacy attr tag that has way too much XML for such
     * little information:
     * <pre>
     * {@code    
     *<attr name="LineWidth">
     *  <datacolumn name="LineWidth" units="dimensionless">
     *     <item value="2"/>
     *  </datacolumn>
     * </attr>
     * }
     * </pre>
     * 
     * Note values and units are separate because units is optional
     * FIXME: Would be nice to have simple like:
     * <pre>
     * {@code    
     *<attr name="LineWidth" units="dimensionless" value="2"/>
     * }
     * </pre>
     */
    public static boolean processAttr(XMLStreamReader p, Map<String, String> valueMap,
            Map<String, String> unitMap) {
        boolean success = false;

        try {

            // <attr loop
            if (atStart(p, "attr")) {
                Map aMap = new TreeMap<String, String>();
                processAttributes(p, aMap); // <attr name=value
                while (p.hasNext()) {
                    p.next();
                    if (isEndTag(p, "attr")) {
                        break; // end of attr
                    } else {

                        // <datacolumn loop
                        if (atStart(p, "datacolumn")) {
                            processAttributes(p, aMap); // <datacolumn name=
                            while (p.hasNext()) {
                                p.next();
                                if (isEndTag(p, "datacolumn")) {
                                    break; // end of datacolumn
                                } else {

                                    if (atStart(p, "item")) {
                                        processAttributes(p, aMap); // <item value=

                                        // Now fill in stuff and end it...
                                        Object n = aMap.get("name");
                                        if (n != null) {
                                            String name = (String) (n);
                                            String value = "";
                                            String units = "dimensionless";
                                            Object v = aMap.get("value");
                                            if (v != null) {
                                                value = (String) (v);
                                            }
                                            Object u = aMap.get("units");
                                            if (u != null) {
                                                units = (String) (u);
                                            }
                                            valueMap.put(name, value);
                                            unitMap.put(name, units);
                                            success = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                return success;
            }
        } catch (Exception e) {
        }
        return success;
    }
}