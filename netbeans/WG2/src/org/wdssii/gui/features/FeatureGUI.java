package org.wdssii.gui.features;

import javax.swing.JComponent;

/**
 * Used to create a swing based GUI for controlling the properties of a Feature
 *
 * @author Robert Toomey
 */
public interface FeatureGUI {

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