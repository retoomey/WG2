package org.wdssii.xml;

import java.util.ArrayList;
import org.wdssii.xml.index.Tag_item;

/**
 * Root tag that processes a WDSS2 index
 * 
 *  <pre>
 * {@code
 *  <codeindex>
 * (n) <item>
 * </codeindex>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_codeindex extends Tag {
    // ----------------------------------------------------------------------
    // Reflection <colorMap upperBound= name=
    // Attributes
    public String type;
    public String dataset; 
    public ArrayList<Tag_item> items = new ArrayList<Tag_item>();
    // End reflection
}
