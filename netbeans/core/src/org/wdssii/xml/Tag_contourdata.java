package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;
import org.wdssii.xml.Tag_array.ArraySubtagFactory;

/**
 *  Tag which has the following format:
 *  contourdata is just a holder tag for an <array> of <contour>
 * 
 * <pre>
 * {@code
 * <contourdata>
 * (1) <array length="n">
 *  (n)   <contour>
 *       (1)   <datatype>
 *       (n)  <locationdata> 
 * </contourdata>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_contourdata extends Tag {

    /** Due to generics, make a factory to create a contour tag for
     * the Tag_array
     */
    private static class TagFactory extends ArraySubtagFactory {

        @Override // with covariance
        public Tag_contour make() {
            return new Tag_contour();
        }

        @Override
        public boolean processTag(Object o, XMLStreamReader p) {
            boolean handled = false;
            if (o instanceof Tag_contour){
                Tag_contour t = (Tag_contour)(o);
                handled = t.processTag(p);
            }
            return handled;
        }
    }
    
    /** Our array of contours */
    public Tag_array<Tag_contour> array = new Tag_array<Tag_contour>(new TagFactory());
    
    @Override
    public String tag() {
        return "contourdata";
    }
 
    @Override
    public void processChildren(XMLStreamReader p) {
       array.processTag(p); 
    }
}
