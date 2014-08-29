package org.wdssii.xml.views;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * a JAXB memento class linking data to the infonode class This exists to allow
 * us to read/write to disk using JAXB XML
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "WindowBar")
public class WindowBar extends DockWindow {
    
}
