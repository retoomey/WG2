package org.wdssii.gui.sources;

import javax.swing.JComponent;

/**
 * Source GUI shows the controls/gui options for a particular source.
 * For example, for an IndexSource it would show the products within that
 * source.
 * 
 * @author Robert Toomey
 */
public interface SourceGUI {
     /**
     * Update GUI to the properties of the Feature
     */
    public void updateGUI();

    /**
     * Activate the GUI into the given container
     */
    public void activateGUI(JComponent parent);

    /**
     * Deactivate the GUI from given container
     */
    public void deactivateGUI(JComponent parent);
}
