package org.wdssii.gui.renderers;

import javax.swing.JComponent;
import javax.swing.JPanel;
import org.wdssii.gui.GUIPlugInPanel;

/**
 * Panel for editing single symbol...
 * 
 * @author Robert Toomey
 */
public class SingleSymbol extends JPanel implements GUIPlugInPanel {
   protected JComponent myRoot = null;
    @Override
    public void updateGUI() {
       
    }

    @Override
    public void activateGUI(JComponent parent) {
        if (myRoot != null) {
            parent.setLayout(new java.awt.BorderLayout());
            parent.add(myRoot, java.awt.BorderLayout.CENTER);
            doLayout();
        }
    }

    @Override
    public void deactivateGUI() {
        
    }
    
}
