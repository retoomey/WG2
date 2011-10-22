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

    /** Collected tag */
    public Map<String, Tag_colorDef> colorDefs = new TreeMap<String, Tag_colorDef>();
    
    private Tag_colorDef aColor = new Tag_colorDef();

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
