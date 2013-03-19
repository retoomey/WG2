package org.wdssii.xml.iconSetConfig;


import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.wdssii.xml.Util;

/**
 *  Polygon symbol
 *  A solid polygon shape based on equal angle rotation.
 *  This can create squares, diamonds, circles...
 * 
 * @author Robert Toomey
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "star")
public class StarSymbol extends Symbol {
    
    /** Base color of the symbol */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "color")
    public Color color = Color.BLUE;
    public int lsize = 1;

    public boolean useOutline = true;
    /** Outline color of the symbol */
    @XmlJavaTypeAdapter(Util.ColorAdapter.class) // Use ColorAdapter
    @XmlAttribute(name = "ocolor")
    public Color ocolor = Color.BLACK;
    public int osize = 2;
    public int numpoints = 4;
     
    public void toX(){
        phaseangle = 45;
        numpoints = 4;
    }
    
    public void toAsterisk(){
        phaseangle = 30;
        numpoints = 6;
    };
}
