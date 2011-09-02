package org.wdssii.xml;

import java.lang.reflect.Field;
import javax.xml.stream.XMLStreamReader;

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
    public int dcUnit;

    @Override
    public String tag() {
        return "textConfig";
    }
    
    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
       // polyText.processTag(p);
    }
}
