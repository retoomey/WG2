package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <iconSetConfig track= trend=>
 *   (optional) <polygonTextConfig>
 * </iconSetConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_iconSetConfig extends Tag {

    public boolean track = false;
    public boolean trend = false;
    
    /** A <polygonTextConfig> tag */
    public Tag_polygonTextConfig polyText = new Tag_polygonTextConfig();
    
    @Override
    public String tag() {
        return "iconSetConfig";
    }
    
    /*
    @Override
    public void handleAttribute(String n, String value) {
        if ("track".equals(n)) { 
            // FIXME: better error handling here
            if (value.equalsIgnoreCase("yes")){
                 track = true;
            }
        }else if ("trend".equals(n)){
            // FIXME: better error handling here
            if (value.equalsIgnoreCase("yes")){
                 trend = true;
            } 
        }
    }
*/
    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
        polyText.processTag(p);
    }
}
