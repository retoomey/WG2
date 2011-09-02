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
    public Tag_polygonConfig polygon = new Tag_polygonConfig();
    
    /** A <textConfig> tag */
    public Tag_textConfig text = new Tag_textConfig();
    
    @Override
    public String tag() {
        return "polygonTextConfig";
    }
    
    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
       text.processTag(p);
    }
}
