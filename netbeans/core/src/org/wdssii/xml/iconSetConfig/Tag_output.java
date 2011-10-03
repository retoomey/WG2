package org.wdssii.xml.iconSetConfig;

import org.wdssii.xml.Tag;

/**
 *  Tag which has the following format:
 * 
 * A Really bad tag name for a subtag of mesonetConfig,
 * but I'm stuck with legacy data files...so here it is
 * 
 * <pre>
 * {@code
 * <output>
 * 
 * </output>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_output extends Tag {
   
    public Tag_windBarb windBarb;
    public Tag_airTemperature airTemperature;
    public Tag_dewTemperature dewTemperature;
    public Tag_precipitation precipitation;
    public Tag_label label;
    
    public static class Tag_airTemperature extends Tag {
        public String unit;
        public String color;
        public String format;
        public int textHeight;    
    }
    
    public static class Tag_dewTemperature extends Tag {
        public String unit;
        public String color;
        public String format;
        public int textHeight;    
    }
    
    public static class Tag_precipitation extends Tag {
        public String unit;
        public String color;
        public String format;
        public int textHeight;    
    }
    
    public static class Tag_label extends Tag {
        public String dcColumn;
        public String color;
        public String format;
        public int textHeight;    
    }
   
}
