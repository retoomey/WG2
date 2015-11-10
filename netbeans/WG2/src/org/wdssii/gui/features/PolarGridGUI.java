package org.wdssii.gui.features;

import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.PolarGridFeature.PolarGridMemento;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;

/**
 * PolarGridGUI handles gui controls for a PolarGrid overlay
 *
 * @author Robert Toomey
 */
public class PolarGridGUI extends FeatureGUI {

    /**
     * The PolarGridFeature we are using
     */
    private PolarGridFeature myFeature;

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
        updateToMemento(m);
    }

    private void setupComponents() {
	JScrollPane s = new JScrollPane();
	s.setViewportView(this);
	setRootComponent(s);

        setLayout(new MigLayout(new LC(), null, null));

        // Create max spinner
        add(new IntegerGUI(myFeature, PolarGridMemento.LINE_THICKNESS, "Line Thickness", this,
                1, 15, 1, "Pixels"));

        // Create line color
        add(new ColorGUI(myFeature, PolarGridMemento.LINE_COLOR, "Line Color", this));

        // Create rings 
        add(new IntegerGUI(myFeature, PolarGridMemento.RING_COUNT, "Number of Rings", this,
                1, 100, 1, ""));
        // Create range
        add(new IntegerGUI(myFeature, PolarGridMemento.RING_RANGE, "Range per ring", this,
                1, 50000, 10, "Meters"));

    }
}
