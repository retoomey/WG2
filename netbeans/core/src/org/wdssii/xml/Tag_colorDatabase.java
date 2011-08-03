package org.wdssii.xml;

import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <colorDatabase>
 * (n) <colorDef name="name" r="0" g="255" b="0" a="0>
 * </colorDatabase>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorDatabase extends Tag {

    // Separate class?  Keeping here for now unless reused
    public static class Tag_colorDef extends Tag {

        public String name;
        /** Store from 0 to 255 */
        public int r = 0;
        public int g = 0;
        public int b = 0;
        public int a = 255;

        @Override
        public String tag() {
            return "colorDef";
        }

        @Override
        public void handleAttribute(String n, String value) {
            try {
                if ("name".equals(n)) {
                    name = value;
                } else if ("r".equals(n)) {
                    r = Integer.parseInt(value);
                } else if ("g".equals(n)) {
                    g = Integer.parseInt(value);
                } else if ("b".equals(n)) {
                    b = Integer.parseInt(value);
                } else if ("a".equals(n)) {
                    a = Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                // Could warn....
            }
        }

        // Just pull the "color" info out directly...
        @Override
        public void processChildren(XMLStreamReader p) {
        }
    }
    /** Collected tag */
    public Map<String, Tag_colorDef> colorDefs = new TreeMap<String, Tag_colorDef>();
    private Tag_colorDef aColor = new Tag_colorDef();

    @Override
    public String tag() {
        return "colorDatabase";
    }

    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {

        // If we have a colorDef tag, process it and add to list...
        if (aColor.processTag(p)) {
            colorDefs.put(aColor.name, aColor);
            aColor = new Tag_colorDef();
        }
    }
}
