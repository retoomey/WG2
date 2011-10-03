package org.wdssii.xml.iconSetConfig;

import org.wdssii.xml.Tag;

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
public class Tag_dataColumn extends Tag {
    
    // ----------------------------------------------------------------------
    // Reflection <iconSetConfig track=, trend=
    // Attributes
    public String speedCol;
    public String directionCol;
    public String airTemperatureCol;
    public String relativeHumidCol;
    public String dewTemperatureCol;
    public String pressureCol;
    public String precipitationCol;
    // End Reflection
    // ---------------------------------------------------------------------- 
    
}
