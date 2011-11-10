package org.wdssii.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Tag is an html Tag object that handles a Stax parsing stream.
 * Even though we create a tree, it's our tree so we have some perks
 * like being able to directly stores values as proper field types, etc.
 * Also I'm hoping to eventually have the rest of display doing stuff like
 * creating radials or contours AS THEY LOAD...lol.
 * This basically halves the memory usage vs using a full Sax document instead.
 * 
 * FIXME: bug where if duplicate tag occurs within an unhandled child then
 * it will overwrite the tag.
 * 
 * @author Robert Toomey
 */
public abstract class Tag {

    private String cacheTagName;
    private boolean haveTag = false;

    /** Set to true iff tag was found and processed */
    private boolean processedTag = false;
    
    /* Default tag method returns the part of the classname
     * without the "Tag_" part.  This is why this class is abstract.
     */
    public final String tag() {
        if (!haveTag) {
            Class<?> c = this.getClass();
            String s = c.getSimpleName();
            s = s.replaceAll("Tag_", "");
            // FIXME: how to check for errors here?
            cacheTagName = s;
            haveTag = true;
        }
        return cacheTagName;
    }

    /** Return true iff tag was read from xml */
    public boolean wasRead(){
        return processedTag;
    }
    
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

    protected boolean atEnd(XMLStreamReader p) {
        boolean atEnd = false;
        if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
            String endTag = p.getLocalName();
            if (endTag.equals(tag())) {
                atEnd = true;
            }
        }
        return atEnd;
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
            processedTag = true;
            validateTag();
        }
        return foundIt;
    }

    public void validateTag() {
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

    /** Process just this tag and STOP.  Normally don't do this.  GUI uses
     * this to process a 'header' of a URL/file to gather info.
     * The tag must match the given.
     */
    public boolean processOneAndStop(XMLStreamReader p){
        boolean found = false;
        boolean done = false;
        try {
            while (!done && p.hasNext()) {
                int event = p.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        found = processTag(p);
                        done = true;// even if not the tag we wanted....
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

        // Default snags Tags and ArrayList<Tags>
        fillTagFieldsFromReflection(p);
        fillArrayListFieldsFromReflection(p);
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
            parseFieldString(f, value);

        } catch (NoSuchFieldException x) {
            // Store generically in a map<String, String> if no field exists?
            // Not sure I like this since it could let developer avoid using
            // types.  We end up with code like:
            // String stuff = getStuff("tagname");
            // int i = Integer.parse(stuff);
            // error, error, etc...
        }
    }

    /** Parse a field and value from reflection. Return true if handled.
     * Subclasses can override to add more types if needed.
     * We handle int, boolean, float and string by default
     * 
     * @param f theField we are to set
     * @param value the string of the xml text to parse
     * @return true if we handled it
     */
    public boolean parseFieldString(Field f, String value) {
        boolean handled = false;
        try {
            String theType = f.getType().getName();

            // ---------------------------------------------------------------
            // Handle 'boolean' field type
            // <tag fieldBoolean={yes, no, 1, no }
            if (theType.equals("boolean")) {
                boolean flag = false;
                if (value.equalsIgnoreCase("yes")) {
                    flag = true;
                }
                if (value.equals("1")) {
                    flag = true;
                }
                f.setBoolean(this, flag);
                handled = true;
            // ---------------------------------------------------------------
            // Handle 'int' field type
            // <tag fieldInteger={0xHex, number }
            } else if (theType.equals("int")) {
                try {
                    int anInt = 0;
                    // Handle '0x' as hex number....
                    if (value.toLowerCase().startsWith("0x")) {
                        value = value.substring(2);
                        anInt = Integer.parseInt(value, 16);
                    } else {
                        anInt = Integer.parseInt(value);
                    }
                    f.setInt(this, anInt);
                    handled = true;
                } catch (NumberFormatException e) {
                    // Could warn....
                }

            // ---------------------------------------------------------------
            // Handle 'float' field type
            // <tag fieldInteger={+-infinity, +-inf, float
            } else if (theType.equals("float")) {

                try {
                    float aFloat = Float.NaN;
                    if (value.equalsIgnoreCase("infinity")) {
                        aFloat = Float.POSITIVE_INFINITY;
                    } else if (value.equalsIgnoreCase("-infinity")) {
                        aFloat = Float.NEGATIVE_INFINITY;
                    } else {
                        aFloat = Float.parseFloat(value);
                    }
                    f.setFloat(this, aFloat);
                    handled = true;
                } catch (NumberFormatException e) {
                    // Could warn....
                }
            // ---------------------------------------------------------------
            // Handle 'string' field type (which is just the xml text)
            // <tag fieldInteger=xmltext
            } else {
                f.set(this, value);
                handled = true;
            }
        } catch (IllegalAccessException e) {
            // FIXME: notify programmer of bad access
        }
        return handled;
    }

    /** Fill in ArrayList fields from reflection.  For example:
     * in xml we have "<color " tag.  This will look for
     * public ArrayList<Tag_color> colors;
     * and add by reflection each Tag_color
     * 
     * @param p 
     */
    public void fillArrayListFieldsFromReflection(XMLStreamReader p) {

        String tag = null;
        if ((tag = haveStartTag(p)) != null) {

            try {

                // For tag <color >  -->
                // public ArrayList<Tag_color> colors;
                Class<?> c = this.getClass();

                // ---------------------------------------------------------------------------
                // Only allow ArrayList for now....
                Field f = c.getDeclaredField(tag + "s");
                String theType = f.getType().getName();
                if (theType.equals("java.util.ArrayList")) {

                    // Anything of form ClassType<Class, Class, ...>
                    Type type = f.getGenericType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) (type);
                        //Type rawType = pt.getRawType();
                        // String raw = rawType.toString(); //arrayList?

                        // We just want one thing inside the <>...
                        Type[] types = pt.getActualTypeArguments();
                        if (types.length == 1) {
                            Type insideType = types[0];
                            // From pre-generics Type/Class merging...
                            if (insideType instanceof Class<?>) {
                                Class<?> theClass = (Class<?>) (insideType);

                                // This class should be a subclass of Tag...
                                Class<?>[] argTypes = new Class[]{XMLStreamReader.class};
                                Object[] args = new Object[]{p}; // Actual args
                                Object classInstance = theClass.newInstance();
                                Method aMethod = theClass.getMethod("processTag", argTypes);
                                Object result = aMethod.invoke(classInstance, args);

                                if ((Boolean) (result) == true) {
                                    // Add this tag to the ArrayList...
                                    ArrayList<Object> currentArray = (ArrayList<Object>) (f.get(this));
                                    currentArray.add(classInstance);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // We don't know how to handle this tag...ignore it...
            } finally {
            }
        }
    }

    /** Fill in Tag_ fields from reflection.  For example:
     * in xml we have "<color " tag.  This will look for
     * public Tag_Color color;
     * and add by reflection
     * 
     * @param p 
     */
    public void fillTagFieldsFromReflection(XMLStreamReader p) {

        String tag = null;
        if ((tag = haveStartTag(p)) != null) {

            try {

                // For tag <color >  -->
                // public Tag_color colors;
                Class<?> c = this.getClass();

                // ---------------------------------------------------------------------------
                // Only allow ArrayList for now....
                Field f = c.getDeclaredField(tag);
                Type t = f.getType();
                // FIXME: how to check for Tag_name?
                Class<?> toMake = null;
                if (t instanceof Class<?>) {
                    toMake = (Class<?>) (t);

                    // This class should be a subclass of Tag...
                    Class<?>[] argTypes = new Class[]{XMLStreamReader.class};
                    Object[] args = new Object[]{p}; // Actual args
                    Object classInstance = toMake.newInstance();
                    Method aMethod = toMake.getMethod("processTag", argTypes);
                    Object result = aMethod.invoke(classInstance, args);

                    if ((Boolean) (result) == true) {
                        f.set(this, classInstance);
                        // Add this tag to the ArrayList...
                        // ArrayList<Object> currentArray = (ArrayList<Object>) (f.get(this));
                        //  currentArray.add(classInstance);
                    }
                }

            } catch (Exception e) {
                // We don't know how to handle this tag...ignore it...
            } finally {
            }
        }
    }
}