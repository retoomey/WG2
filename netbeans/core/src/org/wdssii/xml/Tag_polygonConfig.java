package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <polygonConfig>
 *  <colorDatabase>
 * </polygonConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_polygonConfig extends Tag {

    @Override
    public String tag() {
        return "polygonConfig";
    }
    
}
