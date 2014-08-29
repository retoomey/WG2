package org.wdssii.xml.views;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * a JAXB memento class linking data to the infonode class This exists to allow
 * us to read/write to disk using JAXB XML
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "FloatingWindow")
public class FloatingWindow extends DockWindow {

    public int x;
    public int y;
    public int width;
    public int height;
}
