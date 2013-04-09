package org.wdssii.gui.renderers;

import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.gui.properties.PropertyGUI;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Root GUI for editing a Symbol
 *
 * @author Robert Toomey
 */
public abstract class SymbolGUI extends JPanel implements GUIPlugInPanel {

    public static class SymbolMemento extends Memento {

        // Properties
        public static final String SIZE = "pointsize";
        public static final String PHASE = "phaseangle";
        public static final String XOFFSET = "xoffset";
        public static final String YOFFSET = "yoffset";

        public SymbolMemento(SymbolMemento m) {
            super(m);
        }

        public SymbolMemento() {
            // Override initial feature delete to false
            initProperty(SIZE, 16);
            initProperty(PHASE, 0);
            initProperty(XOFFSET, 0);
            initProperty(YOFFSET, 0);
        }
    }

    public abstract Symbol getSymbol();

    public static class SymbolMementor implements Mementor {

        private Symbol mySymbol;

        private SymbolMementor() {
        }

        public SymbolMementor(Symbol data) {
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(String name, Memento m) {
            Integer v = ((Integer) m.getPropertyValue(SymbolMemento.SIZE));
            if (v != null) {
                mySymbol.pointsize = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(SymbolMemento.PHASE));
            if (v != null) {
                mySymbol.phaseangle = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(SymbolMemento.XOFFSET));
            if (v != null) {
                mySymbol.xoffset = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(SymbolMemento.YOFFSET));
            if (v != null) {
                mySymbol.yoffset = v.intValue();
            }
            AnimateManager.updateDuringRender();  // Bleh
        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            SymbolMemento m = new SymbolMemento((SymbolMemento) getMemento());
            return m;
        }

        public void setMemento(Memento m) {
            if (m instanceof SymbolMemento) {
                m.setProperty(SymbolMemento.SIZE, mySymbol.pointsize);
                m.setProperty(SymbolMemento.PHASE, mySymbol.phaseangle);
                m.setProperty(SymbolMemento.XOFFSET, mySymbol.xoffset);
                m.setProperty(SymbolMemento.YOFFSET, mySymbol.yoffset);
            }
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            SymbolMemento m = new SymbolMemento();
            setMemento(m);
            return m;
        }
    }

    public final void addSymbolComponents(Mementor m) {
        add(new IntegerGUI(m, SymbolMemento.SIZE, "Size", this,
                5, 50, 1, "points"));
        add(new IntegerGUI(m, SymbolMemento.PHASE, "Angle", this,
                0, 359, 1, "degrees"));
        add(new IntegerGUI(m, SymbolMemento.XOFFSET, "XOffset", this,
                -200, 200, 1, "points"));
        add(new IntegerGUI(m, SymbolMemento.YOFFSET, "YOffset", this,
                -200, 200, 1, "points"));
    }
    // FIXME: All this code copies the FeatureGUI...get it all in one place
    /**
     * Stock PropertyGUI items to update
     */
    protected ArrayList<PropertyGUI> myStockGUIItems;
    /**
     * The root component, this is added/removed on the fly
     */
    protected JComponent myRoot = null;

    public void add(PropertyGUI g) {
        if (myStockGUIItems == null) {
            myStockGUIItems = new ArrayList<PropertyGUI>();
        }
        myStockGUIItems.add(g);
        layout(g);
    }

    /**
     * Layout a newly added propety gui item
     */
    public void layout(PropertyGUI g) {
        /* Default layout is mig */
        g.addToMigLayout(this);
    }

    /**
     * Called with proper memento to update all of our stock propertygui items
     */
    public void updateToMemento(Memento m) {
        if (myStockGUIItems != null) {
            for (PropertyGUI p : myStockGUIItems) {
                p.update(m);
            }
        }
    }

    /**
     * Set the root container component
     */
    public void setRootComponent(JComponent r) {
        myRoot = r;
    }

    @Override
    public void updateGUI() {
    }

    @Override
    public void activateGUI(JComponent parent) {
        if (myRoot != null) {
            parent.setLayout(new java.awt.BorderLayout());
            parent.add(myRoot, java.awt.BorderLayout.CENTER);
            doLayout();
        }
    }

    @Override
    public void deactivateGUI() {
    }
}
