package org.wdssii.gui;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Read the wdssii setup file.  This file does 'hard setup' of the display, such as if a perspective/filter is available or not.
 * More of a static configuration.
 * 
 * @author Robert Toomey
 *
 */
public class WdssiiXML {

    private static Log log = LogFactory.getLog(WdssiiXML.class);
    // Tags and fields we read/write.  Use constants to make sure input/output use exact same tags
    protected final static String XML_PERSPECTIVES = "perspectives";
    protected final static String XML_PERSPECTIVE = "perspective";
    protected final static String XML_CLASS = "class";
    protected final static String XML_CHARTS = "charts";
    protected final static String XML_CHART = "chart";
    protected final static String XML_ATTRIBUTE = "attr";

    public static class PerspectiveXMLDocument {

        public String className;
        public boolean loadOnStartup = false;
    }

    public static WdssiiXMLDocument readDocument(InputStream s) {

        WdssiiXMLDocument dt = null;
        InputStream is = s;
        try {
            //is = new FileInputStream(file);
            //if (file.getAbsolutePath().endsWith(".gz")) {
            //	is = new GZIPInputStream(is);
            //}

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(is);

            // get first 'real' tag (this could be a util)
            while (parser.hasNext()) {
                // Find the first tag matching the types we know:
                // 'datatable'
                // 'contours'
                // 'radialset' (not used)
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = parser.getLocalName();
                        if (name.equals("setup")) {

                            dt = readXML_setup(parser);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            parser.close();


        } catch (XMLStreamException e) {
            log.error("Got XMLStreamException parsing wdssii.xml" + e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                    // ok
                }
            }
        }
        log.info("Found and parsed wdssii.xml file successfully");
        return dt;
    }

    /** Read the standard datatype header tag.
     * Each datatype has a main one of these, but some products like Contours reuse it
     * for each individual contour */
    public static WdssiiXMLDocument readXML_setup(XMLStreamReader p) throws XMLStreamException {
        final String XML_SETUP = "setup";
        System.out.println("<setup>");

        WdssiiXMLDocument buffer = null;
        String tagCheck = p.getLocalName();
        if (XML_SETUP.equals(tagCheck)) {

            // <setup>
            // (1) <perspectives>
            buffer = new WdssiiXMLDocument();
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, tagCheck)) {
                    break;
                } else if (isStartTag(p)) {
                    readXML_perspectives(p, buffer);
                    readXML_Collection(p, buffer, "charts", "chart");
                }
            }

        }

        return buffer;
    }

    /** Read the standard perspectives header tag. */
    public static boolean readXML_perspectives(XMLStreamReader p, WdssiiXMLDocument buffer) throws XMLStreamException {

        boolean success = false;
        String datatypeTag = p.getLocalName();

        if (XML_PERSPECTIVES.equals(datatypeTag)) {

            // <perspectives default="..."
            int count = p.getAttributeCount();
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);
                if ("show".equals(name)) {
                    buffer.defaultPerspective = "org.wdssii.gui.rcp.perspectives." + value;
                }
            }

            // <perspectives>
            // (n)  <perspective>
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, datatypeTag)) { // </datatype>
                    break;
                } else if (isStartTag(p)) {
                    readXML_perspective(p, buffer);
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
    public static void readXML_perspective(XMLStreamReader p, WdssiiXMLDocument header) {
        String tagCheck = p.getLocalName();
        String className = "";
        boolean loadOnStartUp = false;

        if (XML_PERSPECTIVE.equals(tagCheck)) {

            // <perspective name="Mesonet"
            int count = p.getAttributeCount();
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);
                if ("class".equals(name)) {
                    className = "org.wdssii.gui.rcp.perspectives." + value;
                }
                if ("loadOnStartup".equals(name)) {
                    loadOnStartUp = readBoolean(false, value);
                }
            }

            PerspectiveXMLDocument per = new PerspectiveXMLDocument();
            per.className = className;
            per.loadOnStartup = loadOnStartUp;
            header.perspectives.add(per);

        }
    }

    // UTILITY FUNCTIONS ************************************************************************
    /** Read and add to a WdssiiXMLCollection.  A generic way of storing information.
     * Example:
     * <collectionName>  // WdssiiXMLCollection
     *   <individualName name="Robert">   // WdssiiXMLAttributeList
     *      <attr name="flag" type="boolean" value="1"/>
     *   </individualName>
     *   <individualName name="Fred">
     *      <attr name="flag" type="boolean" value="0"/>
     *   </individualName>
     * </collectionName>
     *  */
    public static boolean readXML_Collection(XMLStreamReader p, WdssiiXMLDocument buffer, String collectionName,
            String individualName) throws XMLStreamException {

        boolean success = false;
        String datatypeTag = p.getLocalName();

        if (collectionName.equals(datatypeTag)) {

            // Get the top fields..
            // <charts default="" >
            int count = p.getAttributeCount();
            String defaultName = null;
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);

                if ("default".equals(name)) {
                    defaultName = value;
                }
            }

            // <name>
            // (n)  <stuff>
            WdssiiXMLCollection collection = new WdssiiXMLCollection(collectionName, defaultName);
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, datatypeTag)) { // </charts>
                    break;
                } else if (isStartTag(p)) {
                    readXML_AttributeList(p, collection, individualName);
                }
            }
            buffer.add(collectionName, collection);
            success = true;
        }

        return success;
    }

    /** 
     * Read a generic attribute list of the format:
     * <tag name="listname">
     *   <attribute....>
     *   <attribute...>
     * </tag>
     */
    public static boolean readXML_AttributeList(XMLStreamReader p, WdssiiXMLCollection collection,
            String individualName) throws XMLStreamException {

        boolean success = false;
        String tagCheck = p.getLocalName();
        if (individualName.equals(tagCheck)) {

            // Get the name of this AttributeList..each one MUST have a name to reference it
            int count = p.getAttributeCount();
            boolean foundName = false;
            String theName = null;
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);

                if ("name".equals(name)) {
                    theName = value;
                    foundName = true;
                }
            }

            if (foundName) {

                success = true;
                WdssiiXMLAttributeList list = new WdssiiXMLAttributeList(theName);
                while (p.hasNext()) {
                    p.next();
                    if (isEndTag(p, tagCheck)) {  // Look for last '</tag>'
                        break;
                    } else if (isStartTag(p)) {
                        readXML_attribute(p, list);
                    }
                }
                collection.add(theName, list);
            } else {
                log.error("XML collection read error.  Missing name attribute in '" + individualName + "' tag, skipping");
            }
        }
        return success;
    }

    /** Read an attribute tag into a list.  Tags are deliberately short to shrink the memory
     * requirement for larger lists */
    public static boolean readXML_attribute(XMLStreamReader p, WdssiiXMLAttributeList buffer) {

        boolean success = false;
        String datatypeTag = p.getLocalName();

        if (XML_ATTRIBUTE.equals(datatypeTag)) {

            // <attribute name="..." type="boolean" value="yes
            int count = p.getAttributeCount();
            boolean foundName = false;
            String theName = null;
            boolean foundValue = false;
            String theValue = null;
            boolean foundType = false;
            String theType = null;
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);

                if ("name".equals(name)) {
                    theName = value;
                    foundName = true;
                }
                if ("type".equals(name)) {
                    theType = value;
                    foundType = true;
                }
                if ("val".equals(name)) {
                    theValue = value;
                    foundValue = true;
                }
            }

            if (foundName && foundType && foundValue) {
                WdssiiXMLAttributeList.WdssiiXMLAttribute attr;
                // 'factory' for attribute types...
                if (theType.equalsIgnoreCase("boolean")) {
                    attr = new WdssiiXMLAttributeList.ABoolean(theName, readBoolean(false, theValue));
                } else {
                    attr = new WdssiiXMLAttributeList.AString(theName, theValue);
                }
                buffer.add(attr);
                log.info("ATTR: " + theName + "(" + theType + ")=" + theValue);

            }
            success = true;
        }

        return success;
    }

    /** Convert value to boolean */
    public static boolean readBoolean(boolean missingValue, String parse) {
        boolean value = missingValue;
        if (parse != null) {
            if ("yes".equalsIgnoreCase(parse)) {
                value = true;
            } else if ("1".equalsIgnoreCase(parse)) {
                value = true;
            } else if ("true".equalsIgnoreCase(parse)) {
                value = true;
            } else if ("no".equalsIgnoreCase(parse)) {
                value = false;
            } else if ("0".equalsIgnoreCase(parse)) {
                value = false;
            } else if ("false".equalsIgnoreCase(parse)) {
                value = false;
            }
        }
        return value;
    }

    /** Utility function to check for end tag */
    public static boolean isEndTag(XMLStreamReader p, String end) {
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
    public static String haveStartTag(XMLStreamReader p) {
        String startTag = null;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            startTag = p.getLocalName();
        }
        return startTag;
    }

    /** Utility function to check for a new start tag */
    public static boolean isStartTag(XMLStreamReader p) {
        boolean haveStart = false;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            haveStart = true;
        }
        return haveStart;
    }
}
