package org.wdssii.xml.iconSetConfig;

import java.awt.Color;
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
    
    public static final int MERGE_NONE = 0;
    public static final int MERGE_CATEGORIES = 1;
    
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
        this.merge = s.merge;
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
        single.toSquare();
        single.color = Color.RED;
        single.pointsize = 20;
        this.single = new Single();
        this.single.setSymbol(single);

        this.categories = new Categories();

        // Make a 'dummy' category set so I can work on GUI reading it...
        // One step at a time..bleh.. MPing category icons...
        final int SIZE = 20;

        // Use this column for lookup...
        this.categories.column = "Type_id";

        PolygonSymbol newOne = new PolygonSymbol();
        newOne.color = Color.GREEN;
        newOne.ocolor = Color.BLACK;
        newOne.toCircle();
        newOne.pointsize = SIZE - 2;
        this.categories.addCategory(new Category("DZ", newOne));

        ArrowSymbol as = new ArrowSymbol();
        as.phaseangle = 0;
        as.color = Color.GREEN;
        as.ocolor = Color.BLACK;
        as.pointsize = SIZE+4;
        this.categories.addCategory(new Category("RA", as));
        
        /*newOne = new PolygonSymbol();
        newOne.toTriangle();
        newOne.color = Color.GREEN;
        newOne.ocolor = Color.BLACK;
        newOne.osize = 1;
        newOne.pointsize = SIZE + 4;
        this.categories.addCategory(new Category("RA", newOne));
*/
        newOne = new PolygonSymbol();
        newOne.color = new Color(255, 153, 153); // Pinkish
        newOne.ocolor = Color.BLACK;
        newOne.toCircle();
        newOne.pointsize = SIZE - 2;
        this.categories.addCategory(new Category("FD", newOne));

        newOne = new PolygonSymbol();
        newOne.toTriangle();
        newOne.color = new Color(255, 153, 153); // Pinkish
        newOne.ocolor = Color.BLACK;
        newOne.osize = 1;
        newOne.pointsize = SIZE + 4;
        this.categories.addCategory(new Category("FR", newOne));

        StarSymbol ss = new StarSymbol();
        ss.toAsterisk();
        ss.color = Color.GREEN;
        ss.ocolor = Color.BLACK;
        ss.osize = 1;
        ss.pointsize = SIZE;
        this.categories.addCategory(new Category("R/S", ss));

        newOne = new PolygonSymbol();
        newOne.color = new Color(153, 102, 255); // purple
        newOne.ocolor = Color.BLACK;
        newOne.toCircle();
        newOne.pointsize = SIZE - 2;
        this.categories.addCategory(new Category("IP", newOne));

        newOne = new PolygonSymbol();
        newOne.toSquare();
        newOne.color = new Color(153, 102, 255); // purple
        newOne.ocolor = Color.BLACK;
        newOne.osize = 1;
        newOne.pointsize = SIZE;
        this.categories.addCategory(new Category("GR", newOne));

        newOne = new PolygonSymbol();
        newOne.toTriangle();
        newOne.color = new Color(153, 102, 255); // purple
        newOne.ocolor = Color.BLACK;
        newOne.osize = 1;
        newOne.pointsize = SIZE + 4;
        this.categories.addCategory(new Category("R/IP", newOne));

        ss = new StarSymbol();
        ss.toAsterisk();
        ss.color = Color.WHITE;
        ss.ocolor = Color.BLACK;
        ss.osize = 1;
        ss.pointsize = SIZE;
        this.categories.addCategory(new Category("DS", ss));

        ss = new StarSymbol();
        ss.toAsterisk();
        ss.color = Color.BLUE;
        //newOne.useOutline = false;
        ss.ocolor = Color.BLACK;
        ss.osize = 1;
        ss.pointsize = SIZE;
        this.categories.addCategory(new Category("WS", ss));

        ss = new StarSymbol();
        ss.toAsterisk();
        ss.color = new Color(255, 153, 153); // Pinkish
        ss.ocolor = Color.BLACK;
        ss.osize = 1;
        ss.pointsize = SIZE;
        this.categories.addCategory(new Category("IP/S", ss));

        newOne = new PolygonSymbol();
        newOne.toSquare();
        newOne.color = Color.WHITE;
        newOne.ocolor = Color.BLACK;
        newOne.osize = 1;
        newOne.pointsize = SIZE;
        this.categories.addCategory(new Category("SL", newOne));

        /* Default fallback?  Maybe just use the 'single' 
         PolygonSymbol newOne = new PolygonSymbol();
         newOne.toCircle();
         newOne.color = Color.YELLOW;
         newOne.ocolor = Color.BLACK;
         newOne.osize = 1;
         newOne.pointsize = 6;
         rr = SymbolFactory.getSymbolRenderer(newOne);
         rr.setSymbol(newOne);
         */
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
    
    @XmlAttribute(name = "merge")
    public int merge = MERGE_CATEGORIES;
    
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

    public Category findCategory(String key) {
        Category c = null;
        if (categories != null) {
            c = categories.getCategory(key);
        }
        return c;
    }

    public Single getSingle() {
        if (single == null) {
            single = new Single();
        }
        return single;
    }
}
