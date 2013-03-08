package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.xml.ColorDatabase;
import org.wdssii.xml.W2ColorMap;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 * <polygonConfig>
 *  <colorDatabase>
 * </polygonConfig>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "polygonconfig")
public class PolygonConfig {

    /**
     * Number of vertices
     */
    @XmlAttribute(name = "numvertices")
    public int numVertices = 4;
    /**
     * Phase angle of polygon
     */
    @XmlAttribute(name = "phaseangle")
    public int phaseAngle = 0;
    @XmlAttribute(name = "dccolumn")
    public String dcColumn;
    @XmlAttribute(name = "dcunit")
    public String dcUnit;
    
    /** Either we have a <colorMap> */
    @XmlElement(name="colormap", required=false)
    public W2ColorMap colorMap;
    
    /** Or we have a <colordatabase> */
    @XmlElement(name="colordatabase", required=false)
    public ColorDatabase colorDatabase;
}
