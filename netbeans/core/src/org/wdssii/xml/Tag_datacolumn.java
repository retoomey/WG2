package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <datacolumn name=, units=>
 * (n) <item value=>
 * </datacolumn>
 * }
 * </pre>
 * 
 * Not gonna bother making an 'item' sub-tag.
 * 
 * @author Robert Toomey
 */
public class Tag_datacolumn extends Tag {

    public String name;
    public String units;

    private Tag_item aItem = new Tag_item();
    
    // Just store as values, save a little memory at least.  This will
    // change if Tag_item becomes more complex.
    public ArrayList<String> values = new ArrayList<String>();
    
    // Separate class?  Keeping here for now unless reused
    public static class Tag_item extends Tag {

        // Would be nice to have different types to save memory/parsing...
        public String value;
        public boolean valid = false;
        
        @Override
        public String tag() {
            return "item";
        }

        @Override
        public void handleAttribute(String n, String v) {
            if ("value".equals(n)) {
                value = v;
                valid = true;
            }
        }

        @Override
        public void processChildren(XMLStreamReader p) {
        }
    }

    @Override
    public String tag() {
        return "datacolumn";
    }

    /*
    @Override
    public void handleAttribute(String n, String value) {
        if ("name".equals(n)) {
            name = value;
        } else if ("units".equals(n)) {
            units = value;
        }
    }
*/
    @Override
    public void processChildren(XMLStreamReader p) {
         // If we have a colorDef tag, process it and add to list...
        if (aItem.processTag(p)) {
            if (aItem.valid){
                values.add(aItem.value);
                aItem.valid = false;
            }
        }
    }
}
