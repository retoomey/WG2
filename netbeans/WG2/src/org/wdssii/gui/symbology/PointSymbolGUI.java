package org.wdssii.gui.symbology;

import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * The root of all PointSymbol editors
 *
 * @author Robert Toomey
 */
public abstract class PointSymbolGUI extends SymbolGUI {

    public static class PointSymbolMemento extends SymbolMemento {

        // Properties
        public static final int SIZE = SymbolMemento.LAST;
        public static final int PHASE = SIZE + 1;
        public static final int XOFFSET = PHASE + 1;
        public static final int YOFFSET = XOFFSET + 1;
        public static final int LAST = YOFFSET + 1;

        public PointSymbolMemento(PointSymbolMemento m) {
            super(m);
        }

        public PointSymbolMemento() {
            // Override initial feature delete to false
            initProperty(SIZE, 16);
            initProperty(PHASE, 0);
            initProperty(XOFFSET, 0);
            initProperty(YOFFSET, 0);
        }
    }

    @Override
    public abstract Symbol getSymbol();

    public class PointSymbolMementor extends SymbolMementor {

        private Symbol mySymbol;

        private PointSymbolMementor() {
        }

        public PointSymbolMementor(Symbol data) {
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(Object name, Memento m) {
            Integer v = ((Integer) m.getPropertyValue(PointSymbolMemento.SIZE));
            if (v != null) {
                mySymbol.pointsize = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(PointSymbolMemento.PHASE));
            if (v != null) {
                mySymbol.phaseangle = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(PointSymbolMemento.XOFFSET));
            if (v != null) {
                mySymbol.xoffset = v.intValue();
            }
            v = ((Integer) m.getPropertyValue(PointSymbolMemento.YOFFSET));
            if (v != null) {
                mySymbol.yoffset = v.intValue();
            }
            super.propertySetByGUI(name, m);
        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            PointSymbolMemento m = new PointSymbolMemento((PointSymbolMemento) getMemento());
            return m;
        }

        public void setMemento(Memento m) {
            if (m instanceof PointSymbolMemento) {
                m.setProperty(PointSymbolMemento.SIZE, mySymbol.pointsize);
                m.setProperty(PointSymbolMemento.PHASE, mySymbol.phaseangle);
                m.setProperty(PointSymbolMemento.XOFFSET, mySymbol.xoffset);
                m.setProperty(PointSymbolMemento.YOFFSET, mySymbol.yoffset);
            }
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            PointSymbolMemento m = new PointSymbolGUI.PointSymbolMemento();
            setMemento(m);
            return m;
        }
    }

    public final void addPointSymbolComponents(Mementor m) {
        add(new IntegerGUI(m, PointSymbolMemento.SIZE, "Size", this,
                5, 50, 1, "points"));
        add(new IntegerGUI(m, PointSymbolMemento.PHASE, "Angle", this,
                0, 359, 1, "degrees"));
        add(new IntegerGUI(m, PointSymbolMemento.XOFFSET, "XOffset", this,
                -200, 200, 1, "points"));
        add(new IntegerGUI(m, PointSymbolMemento.YOFFSET, "YOffset", this,
                -200, 200, 1, "points"));
    }
}
