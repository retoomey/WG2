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
   
    /** The Datatype this symbology is for */
    @XmlAttribute(name = "name")
    public String name;
    
    /** Category based symbology */
    @XmlElement(name = "categories", required=false)
    public Categories categories = null;
    
    public boolean hasCategories(){
        return (categories != null);
    }
}
