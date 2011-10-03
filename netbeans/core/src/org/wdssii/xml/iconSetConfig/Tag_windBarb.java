package org.wdssii.xml.iconSetConfig;

import org.wdssii.xml.Tag;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <windBarb speedUnit= barbRadius= color= crossHairRadius= hMargin= vMargin=>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_windBarb extends Tag { 
    public String speedUnit;
    public int barbRadius = 10;
    public String color;
    public int crossHairRadius = 5;
    public int hMargin = 5;
    public int vMargin = 5;
    public Tag_superUnit superUnit = new Tag_superUnit();
    public Tag_baseUnit baseUnit = new Tag_baseUnit();
    public Tag_halfUnit halfUnit = new Tag_halfUnit();
    
    public static class Tag_superUnit extends Tag {
        public String value;
        public String tolerance;
    }
    
    public static class Tag_baseUnit extends Tag {
        public String value;
        public String tolerance;
    }    
    
    public static class Tag_halfUnit extends Tag {
        public String value;
        public String tolerance;
    }  
}
