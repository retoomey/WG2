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
    public ArrayList<Tag_datacolumn> datacolumn = new ArrayList<Tag_datacolumn>();
    
    private Tag_datacolumn col = new Tag_datacolumn();
    
    // FIXME: standard arraylist reflection not working here..what's up?
    @Override
    public void processChildren(XMLStreamReader p) {
        if (col.processTag(p)) {
          datacolumn.add(col);
          col = new Tag_datacolumn();
        }
    }
}
