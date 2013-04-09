package org.wdssii.gui.renderers;

import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * GUI for editing a ImageSymbol
 *
 * @author Robert Toomey
 */
public class ImageSymbolGUI extends SymbolGUI {

    /**
     * The ImageSymbol we are using
     */
    private ImageSymbolMementor myMementor;

    private static class ImageSymbolMemento extends SymbolMemento {

        public ImageSymbolMemento(ImageSymbolMemento m) {
            super(m);
        }

        public ImageSymbolMemento() {
            super();
        }
    }

    /**
     * Provides the properties for a StarSymbol
     */
    private static class ImageSymbolMementor extends SymbolMementor {

        private ImageSymbol mySymbol;

        public ImageSymbolMementor(ImageSymbol data) {
            super(data);
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(String name, Memento m) {

            super.propertySetByGUI(name, m);

        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            ImageSymbolMemento m = new ImageSymbolMemento((ImageSymbolMemento) getMemento());
            return m;
        }

        @Override
        public void setMemento(Memento m) {
            super.setMemento(m);
            if (m instanceof ImageSymbolMemento) {
            }
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            ImageSymbolMemento m = new ImageSymbolMemento();
            setMemento(m);
            return m;
        }
    }

    /**
     * Creates new LegendGUI
     */
    public ImageSymbolGUI(ImageSymbol owner) {
        myMementor = new ImageSymbolMementor(owner);
        setupComponents();  // FIXME: if not called here, no dangling this
        // and we can simplify the code.
    }
    
    @Override
    public Symbol getSymbol(){
        return myMementor.mySymbol;
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        updateToMemento(myMementor.getNewMemento());
    }

     public final void addImageSymbolComponents(Mementor m) {
         // Get the stock Symbol controls
        super.addSymbolComponents(myMementor);
     }
     
    /**
     * Set up the components. We haven't completely automated this because you
     * never know what little change you need that isn't supported.
     */
    private void setupComponents() {
        JScrollPane s = new JScrollPane();
        s.setViewportView(this);
        setRootComponent(s);
        setLayout(new MigLayout(new LC(), null, null));
        
        addImageSymbolComponents(myMementor);
    }
}
