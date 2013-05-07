package org.wdssii.gui.symbology;

import javax.swing.JPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.products.SymbolPanel;
import org.wdssii.gui.products.SymbolPanel.SymbolPanelListener;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Panel for editing single symbol...
 *
 * @author Robert Toomey
 */
public class SingleSymbol extends SymbologyGUI implements SymbolPanelListener {

    @Override
    public int getType() {
        return Symbology.SINGLE;
    }

    public SingleSymbol() {
    }

    /**
     * Set up the components. We haven't completely automated this because you
     * never know what little change you need that isn't supported.
     */
    @Override
    public void setupComponents() {

         // Fill the space we have....
        setLayout(new MigLayout("",
                 "[grow, fill]",
                 "[pref!][grow, fill]"));
        
         // Why does this break growing? I can't ever get Mig to work as
         // expected through functions instead of strings.  Grrrrrr
        // setLayout(new MigLayout(new LC(),
        //        new AC().grow(0).fill(0),
        //        new AC().size("pref!", 0).grow(1).fill(1)));

        // Create type panel...
        JPanel typeHolder = new JPanel();
        add(typeHolder, new CC().growX().wrap());
        
        // Create SymbolPane inside a scrollbar
        JPanel symbolHolder = new JPanel();
        add(symbolHolder, new CC().growX().growY());

        setRootComponent(this);
        // Default symbol (first run)  Eventually will need to pass
        // this in for line, polygon, etc...
        Symbol first = getSymbol();
        if (first == null) {
            PolygonSymbol p = new PolygonSymbol();
            p.toCircle();
            first = p;
        }
        SymbolPanel p = new SymbolPanel(first, typeHolder, symbolHolder);
        p.addListener(this);
        if (first != getSymbol()) {
            symbolChanged(first);
        }
    }

    @Override
    public void useSymbology(Symbology symbology) {
        super.useSymbology(symbology);

        // If we're using it, make sure it's set to our mode....
        if (mySymbology.use != Symbology.SINGLE) {
            mySymbology.use = Symbology.SINGLE;
            notifyChanged();
        }
    }

    public Symbol getSymbol() {
        Symbol s = null;
        if (mySymbology != null) {
            s = mySymbology.getSingle().getSymbol();  // Just one for now
        }
        return s;
    }

    @Override
    public void symbolChanged(Symbol s) {
        if (mySymbology != null) {
            mySymbology.getSingle().setSymbol(s);  // Just one for now
            notifyChanged();
        }
    }
}
