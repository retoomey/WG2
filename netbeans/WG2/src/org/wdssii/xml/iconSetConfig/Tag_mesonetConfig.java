package org.wdssii.xml.iconSetConfig;

import javax.xml.stream.XMLStreamReader;
import org.wdssii.xml.Tag;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <mesonetConfig>
 *  <dataColumn>
 * </mesonetConfig>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_mesonetConfig extends Tag {
    
    // ----------------------------------------------------------------------
    // Reflection <iconSetConfig track=, trend=
    // Attributes
    public Tag_dataColumn dataColumn = new Tag_dataColumn();
    public Tag_windBarb windBarb = new Tag_windBarb();
    public Tag_output output = new Tag_output();
    // End Reflection
    // ---------------------------------------------------------------------- 
   
    /** Process all child tabs within our tag */
    /*@Override
    public void processChildren(XMLStreamReader p) {
        fillTagFieldsFromReflection(p);
    } */
}
