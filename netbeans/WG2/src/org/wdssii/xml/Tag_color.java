package org.wdssii.xml;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <color r= g= b= a= name=>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_color extends Tag {
    
    // Reflection attributes... 
    public int r;
    public int g;
    public int b;
    public int a;
    public String name;
}
