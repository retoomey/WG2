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
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;

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

    private static class ImageSymbolMemento extends Memento {

        // Properties
       // public static final String NUMPOINTS = "numpoints";
      //  public static final String LINESIZE = "linesize";
       // public static final String COLOR = "color";
      //  public static final String USEOUTLINE = "useoutline";
       // public static final String OCOLOR = "ocolor";
        public static final String SIZE = "size";
        
        public ImageSymbolMemento(ImageSymbolMemento m) {
            super(m);
        }

        public ImageSymbolMemento() {
            // Override initial feature delete to false
           // initProperty(NUMPOINTS, 4);
           // initProperty(LINESIZE, 1);
           // initProperty(COLOR, Color.BLUE);
           // initProperty(USEOUTLINE, true);
           // initProperty(OCOLOR, Color.BLACK);
            initProperty(SIZE, 16);
        }
    }

    /**
     * Provides the properties for a StarSymbol
     */
    private static class ImageSymbolMementor implements Mementor {

        private ImageSymbol mySymbol;

        private ImageSymbolMementor() {
        }

        public ImageSymbolMementor(ImageSymbol data) {
            mySymbol = data;
        }

        @Override
        public void propertySetByGUI(String name, Memento m) {

            // Directly modify the StarSymbol object
           // Integer v = ((Integer) m.getPropertyValue(ImageSymbolMemento.NUMPOINTS));
           // if (v != null) {
          //      mySymbol.numpoints = v.intValue();
           // }
          //  v = ((Integer) m.getPropertyValue(ImageSymbolMemento.LINESIZE));
          //  if (v != null) {
          //      mySymbol.lsize = v.intValue();
          //  }
           /* Color c = (Color) m.getPropertyValue(ImageSymbolMemento.COLOR);
            if (c != null) {
                mySymbol.color = c;
            }
            Boolean f = (Boolean) m.getPropertyValue(ImageSymbolMemento.USEOUTLINE);
            if (f != null) {
                mySymbol.useOutline = f;
            }
            c = (Color) m.getPropertyValue(ImageSymbolMemento.OCOLOR);
            if (c != null) {
                mySymbol.ocolor = c;
            }*/
            Integer v = ((Integer) m.getPropertyValue(ImageSymbolMemento.SIZE));
            if (v != null) {
                mySymbol.pointsize = v.intValue();
            }
            AnimateManager.updateDuringRender();  // Bleh

        }

        @Override
        public Memento getNewMemento() {
            // Get brand new mementor with default settings
            ImageSymbolMemento m = new ImageSymbolMemento((ImageSymbolMemento) getMemento());
            return m;
        }

        @Override
        public Memento getMemento() {
            // Get the current settings...patch from StarSymbol...
            ImageSymbolMemento m = new ImageSymbolMemento();
          //  m.setProperty(ImageSymbolMemento.NUMPOINTS, mySymbol.numpoints);
           // m.setProperty(ImageSymbolMemento.LINESIZE, mySymbol.lsize);
           // m.setProperty(ImageSymbolMemento.COLOR, mySymbol.color);
           // m.setProperty(ImageSymbolMemento.USEOUTLINE, mySymbol.useOutline);
           //// m.setProperty(ImageSymbolMemento.OCOLOR, mySymbol.ocolor);
            m.setProperty(ImageSymbolMemento.SIZE, mySymbol.pointsize);
            return m;
        }
    }

    /**
     * Creates new LegendGUI
     */
    public ImageSymbolGUI(ImageSymbol owner) {
        myMementor = new ImageSymbolMementor(owner);
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
      //  add(new IntegerGUI(myMementor, ImageSymbolMemento.NUMPOINTS, "Count", this,
       //         2, 16, 2, "points"));
       // add(new IntegerGUI(myMementor, ImageSymbolMemento.LINESIZE, "Linewidth", this,
       //         1, 10, 1, "points"));
      //  add(new ColorGUI(myMementor, ImageSymbolMemento.COLOR, "Base Color", this));
      //  add(new BooleanGUI(myMementor, ImageSymbolMemento.USEOUTLINE, "Use outline", this));
       // add(new ColorGUI(myMementor, ImageSymbolMemento.OCOLOR, "Outline Color", this));

         add(new IntegerGUI(myMementor, ImageSymbolMemento.SIZE, "Size", this,
                5, 50, 1, "points"));

    }
}
