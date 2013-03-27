package org.wdssii.gui.renderers;

import java.awt.Color;
import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;

/**
 * GUI for editing a PolygonSymbol
 *
 * @author Robert Toomey
 */
public class PolygonSymbolGUI extends SymbolGUI {

    /**
     * The PolygonSymbol we are using
     */
    private PolygonSymbolMementor myMementor;

    private static class PolygonSymbolMemento extends Memento {

        // Properties
        public static final String NUMPOINTS = "numpoints";
      //  public static final String LINESIZE = "linesize";
        public static final String COLOR = "color";
        public static final String USEOUTLINE = "useoutline";
        public static final String OCOLOR = "ocolor";
        public static final String SIZE = "size";
        
        public PolygonSymbolMemento(PolygonSymbolMemento m) {
            super(m);
        }

        public PolygonSymbolMemento() {
            // Override initial feature delete to false
            initProperty(NUMPOINTS, 4);
           // initProperty(LINESIZE, 1);
            initProperty(COLOR, Color.BLUE);
            initProperty(USEOUTLINE, true);
            initProperty(OCOLOR, Color.BLACK);
            initProperty(SIZE, 16);
        }
    }

    /**
     * Provides the properties for a StarSymbol
     */
    private static class PolygonSymbolMementor implements Mementor {

        private PolygonSymbol mySymbol;

        private PolygonSymbolMementor() {
        }

        public PolygonSymbolMementor(PolygonSymbol data) {
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(String name, Memento m) {

            // Directly modify the StarSymbol object
            Integer v = ((Integer) m.getPropertyValue(PolygonSymbolMemento.NUMPOINTS));
            if (v != null) {
                mySymbol.numpoints = v.intValue();
            }
          //  v = ((Integer) m.getPropertyValue(PolygonSymbolMemento.LINESIZE));
          //  if (v != null) {
          //      mySymbol.lsize = v.intValue();
          //  }
            Color c = (Color) m.getPropertyValue(PolygonSymbolMemento.COLOR);
            if (c != null) {
                mySymbol.color = c;
            }
            Boolean f = (Boolean) m.getPropertyValue(PolygonSymbolMemento.USEOUTLINE);
            if (f != null) {
                mySymbol.useOutline = f;
            }
            c = (Color) m.getPropertyValue(PolygonSymbolMemento.OCOLOR);
            if (c != null) {
                mySymbol.ocolor = c;
            }
            v = ((Integer) m.getPropertyValue(PolygonSymbolMemento.SIZE));
            if (v != null) {
                mySymbol.pointsize = v.intValue();
            }
            AnimateManager.updateDuringRender();  // Bleh

        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            PolygonSymbolMemento m = new PolygonSymbolMemento((PolygonSymbolMemento) getMemento());
            return m;
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            PolygonSymbolMemento m = new PolygonSymbolMemento();
            m.setProperty(PolygonSymbolMemento.NUMPOINTS, mySymbol.numpoints);
           // m.setProperty(PolygonSymbolMemento.LINESIZE, mySymbol.lsize);
            m.setProperty(PolygonSymbolMemento.COLOR, mySymbol.color);
            m.setProperty(PolygonSymbolMemento.USEOUTLINE, mySymbol.useOutline);
            m.setProperty(PolygonSymbolMemento.OCOLOR, mySymbol.ocolor);
            m.setProperty(PolygonSymbolMemento.SIZE, mySymbol.pointsize);
            return m;
        }
    }

    /**
     * Creates new LegendGUI
     */
    public PolygonSymbolGUI(PolygonSymbol owner) {
        myMementor = new PolygonSymbolMementor(owner);
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        updateToMemento(myMementor.getNewMemento());
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
        add(new IntegerGUI(myMementor, PolygonSymbolMemento.NUMPOINTS, "Sides", this,
                3, 16, 1, "points"));
       // add(new IntegerGUI(myMementor, PolygonSymbolMemento.LINESIZE, "Linewidth", this,
       //         1, 10, 1, "points"));
        add(new ColorGUI(myMementor, PolygonSymbolMemento.COLOR, "Base Color", this));
        add(new BooleanGUI(myMementor, PolygonSymbolMemento.USEOUTLINE, "Use outline", this));
        add(new ColorGUI(myMementor, PolygonSymbolMemento.OCOLOR, "Outline Color", this));

         add(new IntegerGUI(myMementor, PolygonSymbolMemento.SIZE, "Size", this,
                5, 50, 1, "points"));

    }
}
