package org.wdssii.gui.features;

import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.properties.Memento;
import org.wdssii.properties.PropertyGUI;

/**
 * Used to create a swing based GUI for controlling the properties of a Feature
 *
 * @author Robert Toomey
 */
public class FeatureGUI extends JPanel implements GUIPlugInPanel {

    /**
     * Stock PropertyGUI items to update
     */
    protected ArrayList<PropertyGUI> myStockGUIItems;

    public void add(PropertyGUI g) {
        if (myStockGUIItems == null) {
            myStockGUIItems = new ArrayList<PropertyGUI>();
        }
        myStockGUIItems.add(g);
        layout(g);
    }
    
    /** Layout a newly added propety gui item */
    public void layout(PropertyGUI g){
        /* Default layout is mig */
        g.addToMigLayout(this);
    }

    /** Called with proper memento to update all of our stock propertygui items */
    public void updateToMemento(Memento m) {
        if (myStockGUIItems != null) {
            for (PropertyGUI p : myStockGUIItems) {
                p.update(m);
            }
        }
    }

    @Override
    public void updateGUI() {
    }

    @Override
    public void activateGUI(JComponent parent, JComponent secondary) {
    }

    @Override
    public void deactivateGUI(JComponent parent, JComponent secondary) {
    }
}