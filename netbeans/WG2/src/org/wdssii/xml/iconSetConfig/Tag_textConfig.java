package org.wdssii.xml.iconSetConfig;

import javax.xml.stream.XMLStreamReader;
import org.wdssii.xml.Tag;
import org.wdssii.xml.Tag_colorMap;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <textConfig textField=, dcColumn=, dcUnit=>
 * </textConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_textConfig extends Tag {

    // Reflection attributes
    public String textField;
    public String dcColumn;
    public String dcUnit;
    public Tag_colorMap colorMap = new Tag_colorMap();
    
    /** Process all child tabs within our tag */
   /* @Override
    public void processChildren(XMLStreamReader p) {
      // colorMap.processTag(p);
        fillTagFieldsFromReflection(p);
    }*/
}
