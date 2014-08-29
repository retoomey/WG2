package org.wdssii.xml.views;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * a JAXB memento class linking data to the infonode class This exists to allow
 * us to read/write to disk using JAXB XML
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "RootWindow")
public class RootWindow extends DockWindow {
    public int x = 10;
    public int y = 10;
    public int width= 1000;
    public int height = 500;
}
