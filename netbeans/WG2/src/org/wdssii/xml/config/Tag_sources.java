package org.wdssii.xml.config;

import java.util.ArrayList;
import org.wdssii.xml.Tag;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <sources>
 * (n) <source name="name" url="url"/>
 * </sources>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_sources extends Tag {
    // ----------------------------------------------------------------------
    // Reflection <colorBin upperBound= name=
    // Attributes
    // Subtags
	/* Testing writer...
    public boolean testboolean = false;
    public boolean nullboolean;
    public float floattest1 = 5.6f;
    public float nullfloat;
    public double doubletest1 = 7.6d;
    public float nulldouble;
    public String testfield = "Fred";
    public String testnullstring;
    public Tag_source testSourceTag = new Tag_source();
    * 
    */
    public ArrayList<Tag_source> sources = new ArrayList<Tag_source>();
    // End Reflection
    // ----------------------------------------------------------------------	
}
