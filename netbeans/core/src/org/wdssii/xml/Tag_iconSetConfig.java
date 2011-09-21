package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <iconSetConfig track= trend=>
 *   (optional) <polygonTextConfig>
 * </iconSetConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_iconSetConfig extends Tag {

    // Reflection attributes
    public boolean track = false;
    public boolean trend = false;
    
    /** A <polygonTextConfig> tag */
    public Tag_polygonTextConfig polygonTextConfig = new Tag_polygonTextConfig();
    
    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
        polygonTextConfig.processTag(p);
    }
}
