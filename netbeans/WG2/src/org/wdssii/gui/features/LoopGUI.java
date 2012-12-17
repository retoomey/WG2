package org.wdssii.gui.features;

import javax.swing.JComponent;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.LoopFeature.LoopMemento;
import org.wdssii.properties.Memento;
import org.wdssii.properties.gui.BooleanGUI;
import org.wdssii.properties.gui.IntegerGUI;

/**
 * LoopGUI handles gui controls for looping
 *
 * @author Robert Toomey
 */
public class LoopGUI extends FeatureGUI {

    /**
     * The LoopFeature we are using
     */
    private LoopFeature myFeature;

    /**
     * Creates new LoopGUI
     */
    public LoopGUI(LoopFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        Memento m = myFeature.getNewMemento();
        updateToMemento(m);
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

        // We 'could' automate the GUI construction even more,
        // but we would start to lose the ability to easily change/modify
        // it.  I think this gives enough control without adding too much code
        // (you have to create each control there's not a loop somewhere that turns
        // each property into a GUI)
        setLayout(new MigLayout(new LC(), null, null));

        add(new IntegerGUI(myFeature, LoopMemento.NUM_OF_FRAMES, "Number of Frames", this,
                2, 100, 1, ""));

        add(new IntegerGUI(myFeature, LoopMemento.FRAME_TIME_MS, "Frame Display Time", this,
                100, 5000, 1, "Milliseconds"));

        add(new IntegerGUI(myFeature, LoopMemento.FIRST_DWELL_SECS, "First Frame Dwell", this,
                0, 10, 1, "Seconds"));

        add(new IntegerGUI(myFeature, LoopMemento.LAST_DWELL_SECS, "First Frame Dwell", this,
                0, 10, 1, "Seconds"));

        add(new BooleanGUI(myFeature, LoopMemento.ROCK_LOOP, "Rock Loop", this));

    }
}
