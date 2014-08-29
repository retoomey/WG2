package org.wdssii.xml.views;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "Property")
public class Property {
    public String name;
    public String value;
}
