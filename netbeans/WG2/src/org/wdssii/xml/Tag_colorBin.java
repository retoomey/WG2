package org.wdssii.xml;

import java.util.ArrayList;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <colorBin>
 * (n) <color>
 * </colorBin>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorBin extends Tag {

    // ----------------------------------------------------------------------
    // Reflection <colorBin upperBound= name=
    // Attributes
    public float upperBound;
    public String name;
    // Subtags
    public ArrayList<Tag_color> colors = new ArrayList<Tag_color>();
    // End Reflection
    // ----------------------------------------------------------------------
}
