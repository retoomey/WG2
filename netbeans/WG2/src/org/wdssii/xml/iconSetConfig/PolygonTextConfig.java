package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 * <polygonTextConfig>
 *  (optional 1) <textConfig>
 *  (optional 1) <polygonConfig>
 *  (optional 1) <borderConfig>
 * </polygonTextConfig>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name="polygontextconfig")
public class PolygonTextConfig {
    
    @XmlElement(name = "textconfig", required=false)
    public TextConfig textConfig = new TextConfig();
    
    @XmlElement(name = "polygonconfig", required=false)
    public PolygonConfig polygonConfig = new PolygonConfig();
    
    // @XmlElement(name = "borderconfig", required=false)
    //public BorderConfig borderConfig = new BorderConfig();
}
