package org.wdssii.xml.iconSetConfig;

import org.wdssii.xml.Tag;

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
public class Tag_iconSetConfig extends Tag {

    // ----------------------------------------------------------------------
    // Reflection <iconSetConfig track=, trend=
    // Attributes
    public boolean track = false;
    public boolean trend = false;
    // Subtags
    public Tag_polygonTextConfig polygonTextConfig;
    public Tag_mesonetConfig mesonetConfig;
    // End Reflection
    // ---------------------------------------------------------------------- 
}
