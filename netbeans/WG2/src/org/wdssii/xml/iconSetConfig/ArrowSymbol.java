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
@XmlRootElement(name = "arrowsymbol")
public class ArrowSymbol extends PointSymbol {

    public boolean useOutline = true;
    /**
     * Outline color of the symbol
     */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "ocolor")
    public Color ocolor = Color.BLACK;
    
    public int osize = 2;
    public int numpoints = 4;

    public int width = 10;
    
    public int taillength = 10;
    public int tailthick = 4;
    
    @Override
    public Symbol copy() {
        ArrowSymbol p = new ArrowSymbol();
        p.copyFrom(this);
        return p;
    }

    @Override
    public void copyFrom(Symbol s) {
        super.copyFrom(s);
        if (s instanceof ArrowSymbol) {
            ArrowSymbol o = (ArrowSymbol) s;
            color = o.color;
            useOutline = o.useOutline;
            ocolor = o.ocolor;
            numpoints = o.numpoints;
            osize = o.osize;
            width = o.width;
            taillength = o.taillength;
            tailthick = o.tailthick;
        }
    }
}
