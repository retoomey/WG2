package org.wdssii.gui.renderers;

import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.properties.Memento;
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
    public static class SymbolMemento extends Memento {

        public SymbolMemento(SymbolMemento m) {
            super(m);
        }

        public SymbolMemento() {
        }
    }

    public abstract class SymbolMementor implements Mementor {

        @Override
        public void propertySetByGUI(String name, Memento m) {
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
