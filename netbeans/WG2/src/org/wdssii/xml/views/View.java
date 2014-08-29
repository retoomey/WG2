package org.wdssii.xml.views;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * a JAXB memento class linking data to the infonode class This exists to allow
 * us to read/write to disk using JAXB XML
 * 
 * Add simple properties...
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "View")
public class View extends DockWindow {

    /**
     * Creation key for factory
     */
    public String key;
    public String title;
    
    @XmlElement
    public List<Property> properties;

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new ArrayList<Property>();
        }
        Property p = new Property();
        p.name = name;
        p.value = value;
        properties.add(p);
    }

    public String getProperty(String name) {
        Property p = null;
        if (properties != null) {
            for (Property p2 : properties) {
                if (p2.name.equals(name)) {
                    p = p2;
                    break;
                }
            }
        }
        if (p != null){
            return p.value;
        }
        return "";
    }
}
