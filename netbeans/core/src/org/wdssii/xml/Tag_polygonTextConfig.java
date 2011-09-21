package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <polygonTextConfig>
 *  (optional 1) <textConfig>
 *  (optional 1) <polygonConfig>
 *  (optional 1) <borderConfig>
 * </polygonTextConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_polygonTextConfig extends Tag {

    /** A <polygonConfig> tag */
    public Tag_polygonConfig polygonConfig = new Tag_polygonConfig();
    
    /** A <textConfig> tag */
    public Tag_textConfig textConfig = new Tag_textConfig();
    
    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
       textConfig.processTag(p);
       polygonConfig.processTag(p);
    }
}
