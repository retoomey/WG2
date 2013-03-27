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
@XmlRootElement(name = "Symbol")
@XmlAccessorType(XmlAccessType.NONE)
// Allow these subclasses also when unmarshalling
// NOTE: You have to add this for each subclass to tell JAXB about it
// This allows List<Symbol> to contain different subclasses
@XmlSeeAlso({PolygonSymbol.class, StarSymbol.class})
public class Symbol {

    /**
     * The size in points
     */
    @XmlAttribute(name = "points")
    public int pointsize = 16;
    /**
     * The X offset from point of this symbol
     */
    @XmlAttribute(name = "xoffset")
    public int xoffset = 0;
    /**
     * The Y offset from point of this symbol
     */
    @XmlAttribute(name = "yoffset")
    public int yoffset = 0;
    /**
     * The phase (rotation) angle of the symbol
     */
    @XmlAttribute(name = "angle")
    public double phaseangle = 0;
}
