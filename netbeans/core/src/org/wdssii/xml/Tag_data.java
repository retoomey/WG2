package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <data>
 * (1) <datacolumn>
 * </data>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_data extends Tag {
    
    /** Collected tag */
    public ArrayList<Tag_datacolumn> columns = new ArrayList<Tag_datacolumn>();
    
    private Tag_datacolumn col = new Tag_datacolumn();
    
    @Override
    public void processChildren(XMLStreamReader p) {
        if (col.processTag(p)) {
          columns.add(col);
          col = new Tag_datacolumn();
        }
    }
}
