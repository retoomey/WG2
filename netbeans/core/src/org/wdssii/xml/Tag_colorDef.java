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

    // Just pull the "color" info out directly...
    @Override
    public void processChildren(XMLStreamReader p) {
    }
}
