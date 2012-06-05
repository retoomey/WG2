package org.wdssii.xml.config;

import org.wdssii.xml.Tag;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 * <source name="name" url="url"/>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_source extends Tag {
    // ----------------------------------------------------------------------
    // Reflection <colorBin upperBound= name=
    // Attributes
    // Subtags
    /** The preferred source name */
    public String name;
    /** The full url to the source */
    public String url;
    // End Reflection
    // ----------------------------------------------------------------------	
}
