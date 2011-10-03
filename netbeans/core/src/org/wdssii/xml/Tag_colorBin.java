package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

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
    
   /* @Override
    public void processChildren(XMLStreamReader p) {
   
        // This will put the Tag_colors into our 'colors' field
        fillArrayListFieldsFromReflection(p);
        
        // This will put Tag_unit in
      //  fillTagFieldsFromReflection(p);
    }
     * 
     */
}
