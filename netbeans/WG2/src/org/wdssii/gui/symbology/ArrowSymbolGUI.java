package org.wdssii.gui.symbology;

import java.awt.Color;
import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.gui.symbology.PointSymbolGUI.PointSymbolMemento;
import org.wdssii.gui.symbology.PointSymbolGUI.PointSymbolMementor;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.ArrowSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * GUI for editing a ArrowSymbol
 *
 * @author Robert Toomey
 */
public class ArrowSymbolGUI extends PointSymbolGUI {

    /**
     * The ArrowSymbol we are using
     */
    private ArrowSymbolMementor myMementor;

    public static class ArrowSymbolMemento extends PointSymbolMemento {

        // Properties
        public static final int WIDTH = PointSymbolMemento.LAST;
        public static final int TAIL = WIDTH + 1;
        public static final int TAILW = TAIL + 1;
        public static final int COLOR = TAILW + 1;
        public static final int USEOUTLINE = COLOR + 1;
        public static final int OCOLOR = USEOUTLINE + 1;
        public static final int LAST = OCOLOR + 1;

        public ArrowSymbolMemento(ArrowSymbolMemento m) {
            super(m);
        }

        public ArrowSymbolMemento() {
            super();
            initProperty(WIDTH, 20);
            initProperty(TAIL, 15);
            initProperty(TAILW, 4);
            initProperty(COLOR, Color.BLUE);
            initProperty(USEOUTLINE, true);
            initProperty(OCOLOR, Color.BLACK);
        }
    }

    /**
     * Provides the properties for a StarSymbol
     */
    private class ArrowSymbolMementor extends PointSymbolMementor {

        private ArrowSymbol mySymbol;

        public ArrowSymbolMementor(ArrowSymbol data) {
            super(data);
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(Object name, Memento m2) {

            if (m2 instanceof ArrowSymbolMemento) {
                ArrowSymbolMemento m = (ArrowSymbolMemento) (m2);

                // Directly modify the ArrowSymbol object
                Integer v = ((Integer) m.getPropertyValue(ArrowSymbolMemento.WIDTH));
                if (v != null) {
                    mySymbol.width = v.intValue();
                }
                v = ((Integer) m.getPropertyValue(ArrowSymbolMemento.TAIL));
                if (v != null) {
                    mySymbol.taillength = v.intValue();
                }
                v = ((Integer) m.getPropertyValue(ArrowSymbolMemento.TAILW));
                if (v != null) {
                    mySymbol.tailthick = v.intValue();
                }
                Color c = (Color) m.getPropertyValue(ArrowSymbolMemento.COLOR);
                if (c != null) {
                    mySymbol.color = c;
                }
                Boolean f = (Boolean) m.getPropertyValue(ArrowSymbolMemento.USEOUTLINE);
                if (f != null) {
                    mySymbol.useOutline = f;
                }
                c = (Color) m.getPropertyValue(ArrowSymbolMemento.OCOLOR);
                if (c != null) {
                    mySymbol.ocolor = c;
                }
            }
            super.propertySetByGUI(name, m2);

        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            ArrowSymbolMemento m = new ArrowSymbolMemento((ArrowSymbolMemento) getMemento());
            return m;
        }

        @Override
        public void setMemento(Memento m2) {
            super.setMemento(m2);
            if (m2 instanceof ArrowSymbolMemento) {
                ArrowSymbolMemento m = (ArrowSymbolMemento) (m2);
                m.setProperty(ArrowSymbolMemento.WIDTH, mySymbol.width);
                m.setProperty(ArrowSymbolMemento.TAIL, mySymbol.taillength);
                m.setProperty(ArrowSymbolMemento.TAILW, mySymbol.tailthick);
                m.setProperty(ArrowSymbolMemento.COLOR, mySymbol.color);
                m.setProperty(ArrowSymbolMemento.USEOUTLINE, mySymbol.useOutline);
                m.setProperty(ArrowSymbolMemento.OCOLOR, mySymbol.ocolor);
            }
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            ArrowSymbolMemento m = new ArrowSymbolMemento();
            setMemento(m);
            return m;
        }
    }

    /**
     * Creates new LegendGUI
     */
    public ArrowSymbolGUI(ArrowSymbol owner) {
        myMementor = new ArrowSymbolMementor(owner);
        setupComponents();
    }

    @Override
    public Symbol getSymbol() {
        return myMementor.mySymbol;
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        updateToMemento(myMementor.getNewMemento());
    }

    public final void addArrowSymbolComponents(Mementor m) {
        add(new IntegerGUI(myMementor, ArrowSymbolMemento.WIDTH, "Width", this,
                3, 100, 1, "points"));
        add(new IntegerGUI(myMementor, ArrowSymbolMemento.TAIL, "Tail Length", this,
                0, 100, 1, "points"));
        add(new IntegerGUI(myMementor, ArrowSymbolMemento.TAILW, "Tail Width", this,
                3, 100, 1, "points"));
        add(new ColorGUI(myMementor, ArrowSymbolMemento.COLOR, "Base Color", this));
        add(new BooleanGUI(myMementor, ArrowSymbolMemento.USEOUTLINE, "Use outline", this));
        add(new ColorGUI(myMementor, ArrowSymbolMemento.OCOLOR, "Outline Color", this));

        // Get the stock Symbol controls
        super.addPointSymbolComponents(myMementor);
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

        addArrowSymbolComponents(myMementor);
    }
}
