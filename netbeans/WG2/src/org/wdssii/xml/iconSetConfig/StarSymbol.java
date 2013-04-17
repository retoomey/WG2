package org.wdssii.xml.iconSetConfig;

import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.wdssii.xml.Util;

/**
 * Polygon symbol A solid polygon shape based on equal angle rotation. This can
 * create squares, diamonds, circles...
 *
 * @author Robert Toomey
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "starsymbol")
public class StarSymbol extends PointSymbol {

    /**
     * Base color of the symbol
     */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "color")
    /**
     * Fill color of the star
     */
    public Color color = Color.BLUE;
    /**
     * Line size for drawing star
     */
    public int lsize = 1;
    /**
     * Set to true if we use the outline
     */
    public boolean useOutline = true;
    /**
     * Outline color of the symbol
     */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "ocolor")
    /**
     * Outline color of the star, if any
     */
    public Color ocolor = Color.BLACK;
    /**
     * Outline line size, if outline is used
     */
    public int osize = 2;
    /**
     * Number of points in the star, multiple of 2
     */
    public int numpoints = 4;

    public void toX() {
        phaseangle = 45;
        numpoints = 4;
    }

    public void toAsterisk() {
        phaseangle = 30;
        numpoints = 6;
    }

    @Override
    public Symbol copy() {
        StarSymbol s = new StarSymbol();
        s.copyFrom(this);
        return s;
    }

    @Override
    public void copyFrom(Symbol s) {
        super.copyFrom(s);
        if (s instanceof StarSymbol) {
            StarSymbol o = (StarSymbol) s;
            color = o.color;
            lsize = o.lsize;
            useOutline = o.useOutline;
            ocolor = o.ocolor;
            osize = o.osize;
            numpoints = o.numpoints;
        }
    }
}
