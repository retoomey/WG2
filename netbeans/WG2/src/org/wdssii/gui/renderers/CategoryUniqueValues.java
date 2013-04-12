package org.wdssii.gui.renderers;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Panel for editing category unique values....
 *
 * @author Robert Toomey
 */
public class CategoryUniqueValues extends SymbologyGUI {

    @Override
    public int getType() {
        return Symbology.CATEGORY_UNIQUE_VALUES;
    }
    
    public CategoryUniqueValues() {
        setupComponents();
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
        JButton b = new JButton("Category");
        add(b, new CC());
    }

}
