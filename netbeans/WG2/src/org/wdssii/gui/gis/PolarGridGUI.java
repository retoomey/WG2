package org.wdssii.gui.gis;

import javax.swing.*;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.gis.PolarGridFeature.PolarGridMemento;
import org.wdssii.properties.PropertyGUI;
import org.wdssii.properties.gui.ColorGUI;
import org.wdssii.properties.gui.IntegerGUI;

/**
 * PolarGridGUI handles gui controls for a PolarGrid overlay 
 *
 * @author Robert Toomey
 */
public class PolarGridGUI extends javax.swing.JPanel implements FeatureGUI {
    /**
     * The PolarGridFeature we are using
     */
    private PolarGridFeature myFeature;
    
    private IntegerGUI myLineThicknessGUI;
    private IntegerGUI myNumRingsGUI;
    private IntegerGUI myRangeGUI;
    private PropertyGUI myLineColorGUI;

    /**
     * Creates new form LLHAreaSliceGUI
     */
    public PolarGridGUI(PolarGridFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
        myLineThicknessGUI.update(m);
        myNumRingsGUI.update(m);
        myLineColorGUI.update(m);
        myRangeGUI.update(m);
    }

    @Override
    public void activateGUI(JComponent parent, JComponent secondary) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        doLayout();
    }

    @Override
    public void deactivateGUI(JComponent parent, JComponent secondary) {
        parent.remove(this);
    }

    private void setupComponents() {
        setLayout(new MigLayout(new LC(), null, null));

        // Create max spinner
        myLineThicknessGUI = new IntegerGUI(myFeature, PolarGridMemento.LINE_THICKNESS, "Line Thickness", this,
		1, 15, 1, "Pixels");
        myLineThicknessGUI.addToMigLayout(this);

        // Create line color
	myLineColorGUI = new ColorGUI(myFeature, PolarGridMemento.LINE_COLOR, "Line Color", this);
 	myLineColorGUI.addToMigLayout(this);
       
        // Create rings 
        myNumRingsGUI = new IntegerGUI(myFeature, PolarGridMemento.RING_COUNT, "Number of Rings", this,
                1, 20, 1, "");
 	myNumRingsGUI.addToMigLayout(this);
        
        // Create range
        myRangeGUI = new IntegerGUI(myFeature, PolarGridMemento.RING_RANGE, "Range per ring", this,
                1, 50000, 10, "Meters");
 	myRangeGUI.addToMigLayout(this);
        myRangeGUI.addToMigLayout(this);
     
    }
}
