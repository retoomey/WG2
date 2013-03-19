package org.wdssii.xml.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 *  <sources>
 *
 *  </sources>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "sources")
public class Sources {
    @XmlElement(name = "source")
    public List<Source> list = new ArrayList<Source>();
    
    public void addSource(String n, String u, int h){
        Source s = new Source(n, u, h);
        list.add(s);
    }
}
