package org.wdssii.gui.symbology;

import org.wdssii.gui.symbology.SymbolGUI;
import org.wdssii.gui.symbology.PolygonSymbolGUI;
import org.wdssii.gui.symbology.StarSymbolGUI;
import org.wdssii.gui.symbology.ImageSymbolGUI;
import java.util.ArrayList;
import org.wdssii.gui.renderers.ImageSymbolRenderer;
import org.wdssii.gui.renderers.PolygonSymbolRenderer;
import org.wdssii.gui.renderers.StarSymbolRenderer;
import org.wdssii.gui.renderers.SymbolRenderer;
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Create a symbol renderer from symbol xml...
 *  Right now just patch directly...should become more general to avoid
 *  adding each time we make a new symbol type.  Then again, probably not
 *  gonna add that many symbol types...
 * 
 * @author Robert Toomey
 */
public class SymbolFactory {

    public static SymbolGUI getSymbolGUI(Symbol s) {

        // silly for now..FIXME: reflection?
        SymbolGUI r = null;
        String name = s.getClass().getSimpleName();
        if (name.equals("PolygonSymbol")) {
            r = new PolygonSymbolGUI((PolygonSymbol) s);
        }
        if (name.equals("StarSymbol")) {
            r = new StarSymbolGUI((StarSymbol) s);
        }
        if (name.equals("ImageSymbol")) {
            r = new ImageSymbolGUI((ImageSymbol) s);
        }
        return r;
    }

    public static SymbolRenderer getSymbolRenderer(Symbol s) {

        // silly for now..FIXME: reflection?
        SymbolRenderer r = null;
        String name = s.getClass().getSimpleName();
        if (name.equals("PolygonSymbol")) {
            r = new PolygonSymbolRenderer();
        }
        if (name.equals("StarSymbol")) {
            r = new StarSymbolRenderer();
        }
        if (name.equals("ImageSymbol")) {
            r = new ImageSymbolRenderer();
        }
        return r;
    }
    
    public static Symbol getSymbolByName(String name, Symbol oldSymbol){
        Symbol s = null;
         if (name.equals("Star")) {
            s = new StarSymbol();
        }
        if (name.equals("Polygon")) {
           s = new PolygonSymbol();
        }
        if (name.equals("Image")) {
            s = new ImageSymbol();
        }
        if ((s != null) && (oldSymbol != null)){
            s.copyFrom(oldSymbol);
        }
        return s;
    }

    /**
     * List of possible Symbols we can use
     */
    public static ArrayList<String> getSymbolNameList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Star");
        list.add("Polygon");
        list.add("Image");
        return list;
    }

    /**
     * The string in the combo dialog for choosing this type
     */
    public static String getSymbolTypeString(Symbol s) {
        String name = s.getClass().getSimpleName();
        if (name.endsWith("Symbol")) {
            String prefix = name.substring(0, name.indexOf("Symbol"));
            return prefix;
        }
        return name;
    }
}
