package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <colorMap>
 * (n) <colorBin>
 * </colorMap>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorMap extends Tag {

    // ----------------------------------------------------------------------
    // Reflection <colorMap upperBound= name=
    // Attributes
    public ArrayList<Tag_colorBin> colorBins = new ArrayList<Tag_colorBin>();
    public Tag_unit unit = new Tag_unit();
    // End Reflection
    // ----------------------------------------------------------------------
 
  /*  @Override
    public void processChildren(XMLStreamReader p) {

        // Experimenting:  This will be probably become the default
        // way of working once tested more.
        
        // This will put the Tag_colors into our 'colors' field
        fillArrayListFieldsFromReflection(p);
        
        // This will put Tag_unit in
        fillTagFieldsFromReflection(p);
    }*/
}
/*
public class Tag_colorMap extends Tag_array<Tag_colorBin> {

private static class ColorBinTagFactory extends ArraySubtagFactory {

@Override // with covariance
public Tag_colorBin make() {
return new Tag_colorBin();
}

@Override
public boolean processTag(Object o, XMLStreamReader p) {
boolean handled = false;
if (o instanceof Tag_colorBin){
Tag_colorBin t = (Tag_colorBin)(o);
handled = t.processTag(p);
}
return handled;
}
}

public Tag_colorMap(){
super(new ColorBinTagFactory());     
}


}
 * 
 */
