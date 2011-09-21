package org.wdssii.xml;

import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <colorBin>
 * (n) <color>
 * </colorBin>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorBin extends Tag {
    public String upperBound;
    public String name;
    
}
