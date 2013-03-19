package org.wdssii.gui.renderers;

import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Create a symbol renderer from symbol xml...
 * 
 * @author Robert Toomey
 */
public class SymbolFactory {
    
    public static SymbolRenderer getSymbolRenderer(Symbol s){
        
        // silly for now..FIXME: reflection?
        SymbolRenderer r = null;
        String name = s.getClass().getSimpleName();
        if (name.equals("PolygonSymbol")){
            r = new PolygonSymbolRenderer();
        }
        if (name.equals("StarSymbol")){
            r = new StarSymbolRenderer();
        }
                if (name.equals("ImageSymbol")){
            r = new ImageSymbolRenderer();
        }
        return r;
    }
}
