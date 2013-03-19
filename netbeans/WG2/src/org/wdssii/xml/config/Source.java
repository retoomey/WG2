package org.wdssii.xml.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 *  <source name= url= history=>
 *   
 *  </source>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "source")
public class Source {

    @XmlAttribute(name = "name")
    public String sourcename;
    @XmlAttribute(name = "url")
    public String url;
    @XmlAttribute(name = "history", required = false)
    public int history;
    
    public Source(){
        // Needed by JAXB
    }
    public Source(String s, String u, int h){
        sourcename = s;
        url = u;
        history = h;
    }
}
