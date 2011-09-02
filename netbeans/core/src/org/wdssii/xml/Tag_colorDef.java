package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <colorDef name="name" r="0" g="255" b="0" a="0>
 * or
 * <colorDef name="name">
 *   <color name="databasename"/>
 * </colorDef>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorDef extends Tag {

    // Reflection filled fields
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

    /*
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
     */
    // Just pull the "color" info out directly...
    @Override
    public void processChildren(XMLStreamReader p) {
    }
}
