package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Root of all Symbology, this will include color maps and icon mapping, etc.
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "symbology")
@XmlAccessorType(XmlAccessType.NONE)
public class Symbology {
   
    /** Single symbol for all */
    public static final int SINGLE = 0;
    
    /** Categories unique values */
    public static final int CATEGORY_UNIQUE_VALUES = 1;
    
    /** One list string for each 'type' above */
    public static final String theListNames[] = {
          "Single Symbol",
          "Categories:Unique Values"
    };
    
    /** The Datatype this symbology is for */
    @XmlAttribute(name = "name")
    public String name;
    
    /** Just a number to tell which subset of symbology to use,
     * these numbers should only increase, never change */
    @XmlAttribute(name="use")
    public int use = 0;
    
    /** Category based symbology */
    @XmlElement(name = "categories", required=false)
    public Categories categories = null;
    
    public boolean hasCategories(){
        return (categories != null);
    }
    
    public Categories getCategories(){
        if (categories == null){ categories = new Categories(); }
        return categories;
    }
    
}
