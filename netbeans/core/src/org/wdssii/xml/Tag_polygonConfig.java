package org.wdssii.xml;

import javax.xml.stream.XMLStreamReader;

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
    @Override
    public void processChildren(XMLStreamReader p) {
        // colorMap.processTag(p);
        fillTagFieldsFromReflection(p);
    }
}
