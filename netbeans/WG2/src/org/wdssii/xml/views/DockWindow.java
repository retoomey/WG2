package org.wdssii.xml.views;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * a JAXB memento class linking data to the infonode class
 * This exists to allow us to read/write to disk using JAXB XML
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "DockWindow")
@XmlSeeAlso({RootWindow.class , WindowBar.class, SplitWindow.class, TabWindow.class, View.class, FloatingWindow.class})
public class DockWindow {
           
    @XmlElement
    public List<DockWindow> window;
    
    public void addChild(DockWindow child){
        if (window == null){
            window = new ArrayList<DockWindow>();
        }
        window.add(child);
    }
}
