package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <contours>
 * (1) <datatype>
 * (1) <contourdata>
 * </contours>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_contours extends Tag {
    
    /** The <datatype> tag within us */
    public Tag_datatype datatype = new Tag_datatype();
    
    /** The <contourdata> tag within us */
    public Tag_contourdata contourdata = new Tag_contourdata();

    @Override
    public void processChildren(XMLStreamReader p) {
        // Assume one of each tag in any order...
        datatype.processTag(p);
        contourdata.processTag(p);
    }
}
