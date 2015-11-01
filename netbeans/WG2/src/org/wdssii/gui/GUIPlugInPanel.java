package org.wdssii.gui;

import javax.swing.JComponent;

import org.wdssii.properties.Memento;

/**
 * GUI Plug In Panel.
 * Basically I have lists of stuff in the display, when you list on a list item,
 * typically a GUI for that item is shown/swapped out on the fly within the
 * main display.  Features have GUIs that do this, Sources do, as well as
 * filters, etc.  This is the first attempt to more generalize that code
 * 
 * @author Robert Toomey
 */
public interface GUIPlugInPanel {
    /**
     * Update GUI
     */
    public void updateGUI();

    /** Update GUI to given memento changes */
    public void updateGUI(Memento m);
    
    /**
     * Activate the GUI into the given container
     */
    public void activateGUI(JComponent parent);

    /**
     * Deactivate the GUI.  It has already been removed.
     */
    public void deactivateGUI();	
}
