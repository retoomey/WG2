package org.wdssii.gui.renderers;

import com.jidesoft.swing.JideButton;
import java.awt.Color;
import javax.swing.JScrollPane;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.gui.renderers.PointSymbolGUI.PointSymbolMemento;
import org.wdssii.gui.renderers.PointSymbolGUI.PointSymbolMementor;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * GUI for editing a PolygonSymbol
 *
 * @author Robert Toomey
 */
public class PolygonSymbolGUI extends PointSymbolGUI {

    /**
     * The PolygonSymbol we are using
     */
    private PolygonSymbolMementor myMementor;

    public static class PolygonSymbolMemento extends PointSymbolMemento {

        // Properties
        public static final String NUMPOINTS = "numpoints";
        public static final String COLOR = "color";
        public static final String USEOUTLINE = "useoutline";
        public static final String OCOLOR = "ocolor";

        public PolygonSymbolMemento(PolygonSymbolMemento m) {
            super(m);
        }

        public PolygonSymbolMemento() {
            super();
            initProperty(NUMPOINTS, 4);
            initProperty(COLOR, Color.BLUE);
            initProperty(USEOUTLINE, true);
            initProperty(OCOLOR, Color.BLACK);
        }
    }

    /**
     * Provides the properties for a StarSymbol
     */
    private static class PolygonSymbolMementor extends PointSymbolMementor {

        private PolygonSymbol mySymbol;

        public PolygonSymbolMementor(PolygonSymbol data) {
            super(data);
            mySymbol = data;
        }

        public void toSquare() {
            mySymbol.toSquare();
        }

        public void toCircle() {
            mySymbol.toCircle();
        }

        public void toDiamond() {
            mySymbol.toDiamond();
        }

        public void toTriangle() {
            mySymbol.toTriangle();
        }

        @Override
        public void propertySetByGUI(String name, Memento m) {

            // Directly modify the StarSymbol object
            Integer v = ((Integer) m.getPropertyValue(PolygonSymbolMemento.NUMPOINTS));
            if (v != null) {
                mySymbol.numpoints = v.intValue();
            }
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
            super.propertySetByGUI(name, m);

        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            PolygonSymbolMemento m = new PolygonSymbolMemento((PolygonSymbolMemento) getMemento());
            return m;
        }

        @Override
        public void setMemento(Memento m) {
            super.setMemento(m);
            if (m instanceof PolygonSymbolMemento) {
                m.setProperty(PolygonSymbolMemento.NUMPOINTS, mySymbol.numpoints);
                m.setProperty(PolygonSymbolMemento.COLOR, mySymbol.color);
                m.setProperty(PolygonSymbolMemento.USEOUTLINE, mySymbol.useOutline);
                m.setProperty(PolygonSymbolMemento.OCOLOR, mySymbol.ocolor);
            }
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            PolygonSymbolMemento m = new PolygonSymbolMemento();
            setMemento(m);      
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

    public final void addPolygonSymbolComponents(Mementor m) {
        add(new IntegerGUI(myMementor, PolygonSymbolMemento.NUMPOINTS, "Sides", this,
                3, 20, 1, "points"));

        // Quick selects
        JideButton b;
        b = new JideButton("Square");
        b.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myMementor.toSquare();
                updateGUI();
            }
        });
        add(b, new CC());
        b = new JideButton("Circle");
        b.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myMementor.toCircle();
                updateGUI();
            }
        });
        add(b, new CC());
        b = new JideButton("Diamond");
        b.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myMementor.toDiamond();
                updateGUI();
            }
        });
        add(b, new CC());
        b = new JideButton("Triangle");
        b.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myMementor.toTriangle();
                updateGUI();
            }
        });
        add(b, new CC().wrap());

        // add(new IntegerGUI(myMementor, PolygonSymbolMemento.LINESIZE, "Linewidth", this,
        //         1, 10, 1, "points"));
        add(new ColorGUI(myMementor, PolygonSymbolMemento.COLOR, "Base Color", this));
        add(new BooleanGUI(myMementor, PolygonSymbolMemento.USEOUTLINE, "Use outline", this));
        add(new ColorGUI(myMementor, PolygonSymbolMemento.OCOLOR, "Outline Color", this));

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

        addPolygonSymbolComponents(myMementor);
    }
}
