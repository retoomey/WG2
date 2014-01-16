package org.wdssii.gui.symbology;

import org.wdssii.gui.swing.SwingGUIPlugInPanel;
import org.wdssii.properties.Memento;
import org.wdssii.properties.MementoInteger;
import org.wdssii.properties.MementoString;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Root GUI for editing a Symbol
 *
 * @author Robert Toomey
 */
public abstract class SymbolGUI extends SwingGUIPlugInPanel {

    public abstract Symbol getSymbol();
    private SymbolGUIListener myListener;

    public static abstract interface SymbolGUIListener {

        public void symbolChanged();
    }

    public void addListener(SymbolGUIListener l) {
        myListener = l;
    }

    public void symbolChanged() {
        if (myListener != null) {
            myListener.symbolChanged();
        }
    }

    /**
     * Holds all the flags of StarSymbol
     */
    public static class SymbolMemento extends MementoInteger {

        public static final int LAST = 0;
        
        public SymbolMemento(SymbolMemento m) {
            super(m);
        }

        public SymbolMemento() {
        }
    }

    public abstract class SymbolMementor implements Mementor {

        @Override
        public void propertySetByGUI(Object name, Memento m) {
            SymbolGUI.this.symbolChanged();
        }
        //  @Override
        //  public Memento getNewMemento() {
        // }
        // @Override
        // public Memento getMemento() {
        ///}
    }
}
