package org.wdssii.gui;

import javax.swing.JComponent;

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

    /**
     * Activate the GUI into the given container
     */
    public void activateGUI(JComponent parent, JComponent secondary);

    /**
     * Deactivate the GUI from given container
     */
    public void deactivateGUI(JComponent parent, JComponent secondary);	
}
