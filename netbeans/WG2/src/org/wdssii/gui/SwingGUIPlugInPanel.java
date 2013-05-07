package org.wdssii.gui;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.properties.PropertyGUI;
import org.wdssii.properties.Memento;

/**
 * GUIPlugInPanel in Swing. Just a JPanel this will cover 99.9% if not all cases
 *
 * @author Robert Toomey
 */
public class SwingGUIPlugInPanel extends JPanel implements GUIPlugInPanel {

    /**
     * We can have a list of stock PropertyGUI items to update
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
     * Layout a newly added property gui item
     */
    public void layout(PropertyGUI g) {
        /* Default layout is mig */
        g.addToMigLayout(this);
    }

    /**
     * Called with proper memento to update all of our stock property gui items
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
           // parent.setLayout(new java.awt.BorderLayout());
           // parent.add(myRoot, java.awt.BorderLayout.CENTER);
            parent.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
            parent.add(myRoot, new CC().growX().growY());
            doLayout();
        }
    }

    @Override
    public void deactivateGUI() {
    }

    public static boolean swapToPanel(JComponent panel, SwingGUIPlugInPanel oldOne, SwingGUIPlugInPanel newOne) {

        boolean didSwap = false;
        if (oldOne != newOne) {
            panel.removeAll();
            if (newOne != null) {
                newOne.activateGUI(panel);
                newOne.updateGUI();
            }
            panel.validate();
            panel.repaint();
            if ((oldOne != null)) {  // and != newOne already checked
                oldOne.deactivateGUI();
            }
            didSwap = true;
        }
        return didSwap;
    }
    
    public static boolean install(JComponent panel, SwingGUIPlugInPanel newOne) {

        // Find old panel and deactivate it...
        SwingGUIPlugInPanel oldOne = null;
        int childCount = panel.getComponentCount();
        if (childCount > 0){
             Component aChild = panel.getComponent(0);
             if (aChild instanceof SwingGUIPlugInPanel){
                 SwingGUIPlugInPanel p = (SwingGUIPlugInPanel)(aChild);
                 oldOne = p;
             }
        }
       return swapToPanel(panel, oldOne, newOne);
       
    }
}
