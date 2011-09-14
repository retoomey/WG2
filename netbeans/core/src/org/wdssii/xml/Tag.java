package org.wdssii.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.openide.util.Exceptions;

/**
 * Tag is an html Tag object that handles a Stax parsing stream.
 * Even though we create a tree, it's our tree so we have some perks
 * like being able to directly stores values as proper field types, etc.
 * Also I'm hoping to eventually have the rest of display doing stuff like
 * creating radials or contours AS THEY LOAD...lol.
 * This basically halves the memory usage vs using a full Sax document instead.
 * 
 * @author Robert Toomey
 */
public abstract class Tag {

    public abstract String tag();

    /** Utility function to check for a new start tag */
    protected static String haveStartTag(XMLStreamReader p) {
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

    protected boolean atStart(XMLStreamReader p) {
        boolean atStart = false;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            String startTag = p.getLocalName();
            if (startTag.equals(tag())) {
                atStart = true;
            }
        }
        return atStart;
    }

    protected static boolean atStart(XMLStreamReader p, String tag) {
        boolean atStart = false;
        if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
            String startTag = p.getLocalName();
            if (startTag.equals(tag)) {
                atStart = true;
            }
        }
        return atStart;
    }

    /** Utility function to check for end tag */
    protected static boolean isEndTag(XMLStreamReader p, String end) {
        boolean isEndTag = false;
        if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
            String name = p.getLocalName();
            if (end.equals(name)) {
                isEndTag = true;
            }
        }
        return isEndTag;
    }

    protected boolean nextNotEnd(XMLStreamReader p) {

        // Move forward a tag....
        boolean end = false;
        try {
            if (p.hasNext()) {  // If we still more stuff...
                p.next();   // Move forward

                if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
                    String endTag = p.getLocalName();
                    if (endTag.equals(tag())) {
                        end = true;
                    }
                }

            }
        } catch (XMLStreamException ex) {
            // what to do?, just end so we don't loop forever
            end = true;
        }
        return !end;

    }

    /** Holder class for unit/value attributes */
    public static class UnitValuePair {

        public String unit;
        public String value;
    }

    protected static void readUnitValue(XMLStreamReader p, UnitValuePair buffer) {
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

    protected static void processAttributes(XMLStreamReader p, Map<String, String> buffer) {
        int count = p.getAttributeCount();
        for (int i = 0; i < count; i++) {
            QName attribute = p.getAttributeName(i);
            String name = attribute.toString();
            String value = p.getAttributeValue(i);
            buffer.put(name, value);
        }
    }

    /** Process our root tag returned by tag() */
    public boolean processTag(XMLStreamReader p) {

        boolean foundIt = false;
        if (atStart(p)) {  // We have to have our root tag
            foundIt = true;
            // Handle attributes of our start tag
            // <datatype name="Mesonet"
            int count = p.getAttributeCount();
            for (int i = 0; i < count; i++) {
                QName attribute = p.getAttributeName(i);
                String name = attribute.toString();
                String value = p.getAttributeValue(i);
                handleAttribute(name, value);
            }

            while (nextNotEnd(p)) {
                processChildren(p);
            }
        }
        return foundIt;
    }

    /** Process this tag as a document root.  Basically skip any information
     * until we get to our tag.  In STAX, the first event is not a start
     * tag typically.
     * @param p the stream to read from
     * @return true if tag was found and processed
     */
    public boolean processAsRoot(XMLStreamReader p) {
        boolean found = false;
        try {
            while (p.hasNext()) {
                int event = p.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        found = processTag(p);
                        break;
                    }
                }
            }
        } catch (XMLStreamException ex) {
        }
        return found;
    }

    /** Process document root from a given File */
    public boolean processAsRoot(File f) {
        boolean success = false;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            FileInputStream is = new FileInputStream(f);
            XMLStreamReader p = factory.createXMLStreamReader(is);
            success = processAsRoot(p);
        } catch (Exception ex) {
        }
        return success;

    }
    
    /** Process document root from a given URL */
    public boolean processAsRoot(URL aURL) {
        boolean success = false;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                aURL.openStream()));
            XMLStreamReader p = factory.createXMLStreamReader(in);
            success = processAsRoot(p);
        } catch (Exception ex) {
        }
        return success;

    }

    /** Process all child tabs within our tag */
    public void processChildren(XMLStreamReader p) {
    }

    /** Handle attributes by reflection.
     *  It looks for a matching field name exactly matching the xml attribute
     *  tag.  The type of the field is used to parse the xml string.
     * 
     * @param n
     * @param value 
     */
    public void handleAttribute(String name, String value) {

        try {
            Class<?> c = this.getClass();
            Field f = c.getDeclaredField(name);
            String theType = f.getType().getName();

            // Handle 'boolean' field type
            if (theType.equals("boolean")) {
                boolean flag = false;
                if (value.equals("yes")) { // todo other types of text
                    flag = true;
                }
                f.setBoolean(this, flag);
                
            // Handle 'int' field type
            } else if (theType.equals("int")) {
                try {
                    f.setInt(this, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // Could warn....
                }
            } else {
                // Handle 'string' type by default (or exception)
                f.set(this, value);
            }
        } catch (NoSuchFieldException x) {
            // Store generically in a map<String, String> if no field exists?
            // Not sure I like this since it could let developer avoid using
            // types.  We end up with code like:
            // String stuff = getStuff("tagname");
            // int i = Integer.parse(stuff);
            // error, error, etc...
        } catch (IllegalAccessException x) {
            // warn...
        }
    }
}