
package org.wdssii.xml.views;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * a JAXB memento class linking data to the infonode class
 * This exists to allow us to read/write to disk using JAXB XML
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "SplitWindow")
public class SplitWindow extends DockWindow {
    
    public boolean isHorizontal;
    public float dividerLocation;
}
