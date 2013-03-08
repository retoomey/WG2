package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <iconSetConfig track= trend=>
 *   (optional) <polygonTextConfig>
 * </iconSetConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "iconsetconfig")
public class IconSetConfig {

    @XmlAttribute
    public boolean track = false;
    @XmlAttribute
    public boolean trend = false;
    @XmlElement(name = "polygontextconfig", required=false)
    public PolygonTextConfig polygonTextConfig;
    @XmlElement(name = "mesonetconfig", required=false)
    public MesonetConfig mesonetConfig;
    // CASA 3D var
    //@XmlElement(name = "fieldarrowiconconfig", required=false)
    //public FieldArrowIconConfig fieldArrowIconConfig;
    // rankConfig....
}
