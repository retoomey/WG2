package org.wdssii.gui.renderers;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * One of the 'list' of Symbology subpanel types
 *
 * @author Robert Toomey
 */
public abstract class SymbologyGUI extends SwingGUIPlugInPanel {
    
    public SymbologyGUI() {
        setupComponents();
    }

    /** Get the symbology.use type that we edit for, each will have a unique number */
    public abstract int getType();
    
    /** Return the string used to define us in a list */
    public String getDisplayName() {
        return Symbology.theListNames[getType()];
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
       // JButton b = new JButton("Symbology");
        //add(b, new CC());
    }

}
