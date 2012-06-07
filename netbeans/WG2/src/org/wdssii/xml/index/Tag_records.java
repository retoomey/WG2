package org.wdssii.xml.index;

import java.util.ArrayList;
import org.wdssii.xml.Tag;

/**
 * Root tag that processes a WDSS2 index
 * This is used for webindex
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
public class Tag_records extends Tag {
    // ----------------------------------------------------------------------
    // Reflection <colorMap upperBound= name=
    // Attributes
    public String source;
    public int lastRead; 
    public int listsize; 
    public ArrayList<Tag_item> items = new ArrayList<Tag_item>();
    // End reflection
}