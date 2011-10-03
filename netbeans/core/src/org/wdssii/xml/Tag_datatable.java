package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <datatable>
 * (1) <datatype>
 * (1) <data>
 * </datatable>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_datatable extends Tag {

    /** The <datatype> tag within us */
    public Tag_datatype datatype = new Tag_datatype();
    /** The <data> tag within us */
    public Tag_data data = new Tag_data();

   /* @Override
    public void processChildren(XMLStreamReader p) {
        // Assume one of each tag in any order...
        datatype.processTag(p);
        data.processTag(p);
    }*/
}
