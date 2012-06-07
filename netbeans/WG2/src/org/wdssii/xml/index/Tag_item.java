package org.wdssii.xml.index;

import java.util.ArrayList;
import org.wdssii.xml.Tag;

/**
 *
 * @author Robert Toomey
 */
public class Tag_item extends Tag {
    // ----------------------------------------------------------------------
    // Reflection Attributes
    public Tag_time time;
    public ArrayList<Tag_params> paramss  = new ArrayList<Tag_params>();
    public Tag_selections selections;
}
