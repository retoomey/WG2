package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *  MesonetConfig subTag which has the following format:
 * 
 * <pre>
 * {@code
 * <dataColumn
 *   speedCol
 *   directionCol
 *   airTemperatureCol
 *   relativeHumidCol
 *   dewTemperatureCol
 *   pressureCol
 *   precipitationCol
 * 
 * >
 * </dataColumn>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "datacolumn")
public class DataColumn {
    
    @XmlAttribute(name="speedcol")
    public String speedCol;
    @XmlAttribute(name="directioncol")
    public String directionCol;
    @XmlAttribute(name="airtemperaturecol")
    public String airTemperatureCol;
    @XmlAttribute(name="relativehumidcol")
    public String relativeHumidCol;
    @XmlAttribute(name="pressurecol")
    public String pressureCol;
    @XmlAttribute(name="precipitationcol")
    public String precipitationCol;
    @XmlAttribute(name="dewpointcol")
    public String dewpointCol; 
}
