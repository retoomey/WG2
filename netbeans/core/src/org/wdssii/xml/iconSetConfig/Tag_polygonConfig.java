package org.wdssii.xml.iconSetConfig;

import javax.xml.stream.XMLStreamReader;
import org.wdssii.xml.Tag;
import org.wdssii.xml.Tag_colorMap;
import org.wdssii.xml.Tag_unit;

/**
 *  Tag which has the following format:
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
public class Tag_polygonConfig extends Tag {

    /** Number of vertices */
    public int numVertices = 4;
    /** Phase angle of polygon */
    public int phaseAngle = 0;
    public String dcColumn;
    public String dcUnit;
    public Tag_colorMap colorMap = new Tag_colorMap();

    /** Process all child tabs within our tag */
   /* @Override
    public void processChildren(XMLStreamReader p) {
        // colorMap.processTag(p);
        fillTagFieldsFromReflection(p);
    }
     * 
     */
    @Override
    public void validateTag(){
        // Check for units in colorMap tag...if missing, use our dcUnit
        // so that color map renderer will draw it.
        try{
            if (colorMap.unit == null){
                colorMap.unit = new Tag_unit();    
            }
            if (colorMap.unit.name == null){
                colorMap.unit.name = dcUnit;
            }
        }finally{}
    }
}
