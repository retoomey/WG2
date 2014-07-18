package org.wdssii.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 *  <colorDatabase>
 * (n) <colorDef name="name" r="0" g="255" b="0" a="0>
 * </colorDatabase>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "colordatabase")
public class ColorDatabase {

    private final static Logger LOG = LoggerFactory.getLogger(ColorDatabase.class);
    @XmlElement(name = "colordef")
    public List<ColorDef> colorDefs = new ArrayList<ColorDef>();

    @XmlRootElement(name = "colordef")
    public static class ColorDef {

        @XmlAttribute(name = "name")
        public String name = "";
        
        // Either we store the three colors directly...
        @XmlAttribute
        public Integer r = 0;  // Have to use Integer to use our hex wrapper
        @XmlAttribute
        public Integer g = 0;
        @XmlAttribute
        public Integer b = 0;
        @XmlAttribute
        public Integer a = 255;
        
        // Or we have a dedicated color object (good grief)
        @XmlElement(name = "color", required = false)
        public W2Color color = null;

        public int red() {
            if (color != null) {
                return color.r;
            } else {
                return r;
            }
        }

        public int green() {
            if (color != null) {
                return color.g;
            } else {
                return g;
            }
        }

        public int blue() {
            if (color != null) {
                return color.b;
            } else {
                return b;
            }
        }
        
        public int alpha() {
            if (color != null) {
                return color.a;
            } else {
                return a;
            }
        }
    }
    private TreeMap<String, ColorDef> myLookup;

    public void makeLookup() {
        myLookup = new TreeMap<String, ColorDef>();
        for (ColorDef d : colorDefs) {
            myLookup.put(d.name, d);
            //LOG.debug("Created lookup " + d.name + " --> " + d.red() + ", " + d.green() + ", " + d.blue() + ", " + d.alpha());
        }
    }

    public ColorDef get(String name) {
        if (myLookup == null){
            makeLookup();
        }
        return myLookup.get(name);
    }
}
