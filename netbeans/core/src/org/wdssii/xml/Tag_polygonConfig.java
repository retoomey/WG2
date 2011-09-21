package org.wdssii.xml;

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
    
    // FIXME: this contains a colorMap tag...or colorDatabase?
    
}
