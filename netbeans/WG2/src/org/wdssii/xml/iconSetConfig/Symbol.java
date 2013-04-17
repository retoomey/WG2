package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Symbol stores information for drawing a particular icon
 *
 * @author Robert Toomey
 */
// Allow these subclasses also when unmarshalling
// NOTE: You have to add this for each subclass to tell JAXB about it
// This allows List<Symbol> to contain different subclasses
@XmlRootElement(name = "symbol")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({PolygonSymbol.class, StarSymbol.class, ImageSymbol.class})
public class Symbol {

    /**
     * The size in points
     */
    @XmlAttribute(name = "points", required=false)
    public int pointsize = 16;
    /**
     * The X offset from point of this symbol
     */
    @XmlAttribute(name = "xoffset", required=false)
    public int xoffset = 0;
    /**
     * The Y offset from point of this symbol
     */
    @XmlAttribute(name = "yoffset", required=false)
    public int yoffset = 0;
    /**
     * The phase (rotation) angle of the symbol
     */
    @XmlAttribute(name = "angle", required=false)
    public int phaseangle = 0;
    
    public Symbol copy(){
        Symbol s = new Symbol();
        s.copyFrom(this);
        return s;
    }
    
    public void copyFrom(Symbol s){
        pointsize = s.pointsize;
        xoffset = s.xoffset;
        yoffset = s.yoffset;
        phaseangle = s.phaseangle;
    }
}
