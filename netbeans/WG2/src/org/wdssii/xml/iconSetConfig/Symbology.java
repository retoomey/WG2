package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Root of all Symbology, this will include color maps and icon mapping, etc.
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "symbology")
@XmlAccessorType(XmlAccessType.NONE)
public class Symbology {

    /**
     * Single symbol for all
     */
    public static final int SINGLE = 0;
    /**
     * Categories unique values
     */
    public static final int CATEGORY_UNIQUE_VALUES = 1;

    /**
     * The symbology types. Points lead to PointSymbols, for example...
     */
    public static enum Type {

        UNINITIALIZED,
        POINT,
        POLYGON,
        LINE
    }
    /**
     * One list string for each 'type' above
     */
    private static final String theListNames[] = {
        "Single Symbol",
        "Categories:Unique Values"
    };

    public Symbology() {
    }

    /**
     * The GUI can edit Symbology, but another thread openGL can be drawing
     * using Symbology. We could synchronize all field access but instead we
     * just make a copy. Since only the GUI changes fields, this make it
     * immutable for the drawing thread. The downside is that you need
     * EVERYTHING. The good thing is that we only copy this when we change a GUI
     * setting on a symbol, so it's not noticeable slow.
     *
     * @param s
     */
    public Symbology(Symbology s) {
        this.name = s.name;
        this.use = s.use;
        if (s.single != null) {
            this.single = new Single(s.single);
        }
        if (s.categories != null) {
            this.categories = new Categories(s.categories);
        }
    }

    /**
     * Set ourselves to default point data configuration
     */
    public void toDefaultForPointData() {
        this.use = SINGLE;
        //this.type = POINT;
        PolygonSymbol single = new PolygonSymbol();
        single.toCircle();
        single.pointsize = 20;
        this.single = new Single();
        this.single.setSymbol(single);
    }
    /**
     * The Datatype this symbology is for
     */
    @XmlAttribute(name = "name")
    public String name;
    /**
     * Just a number to tell which subset of symbology to use, these numbers
     * should only increase, never change
     */
    @XmlAttribute(name = "use")
    public int use = SINGLE;
    /**
     * Single based symbology
     */
    @XmlElement(name = "single", required = false)
    public Single single = null;
    /**
     * Category based symbology
     */
    @XmlElement(name = "categories", required = false)
    public Categories categories = null;

    public int getUse() {
        return use;
    }

    public void setUse(int f) {
        use = f;
    }

    public static String getType(int i) {
        return theListNames[i];
    }

    public static int getTypeLength() {
        return theListNames.length;
    }

    public Categories getCategories() {
        if (categories == null) {
            categories = new Categories();
        }
        return categories;
    }

    public Single getSingle() {
        if (single == null) {
            single = new Single();
        }
        return single;
    }
}
