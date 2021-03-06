package org.wdssii.xml.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.xml.views.RootWindow;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 *  <w2pref>
 *    <sources>
 *    </sources>
 *    <layout>
 *    </layout>
 *  </w2pref>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "w2pref")
public class W2Pref {
    @XmlElement(name = "sources")
    public Sources sources;
    
    @XmlElement(name = "layout")
    public RootWindow rootwindow;
}
