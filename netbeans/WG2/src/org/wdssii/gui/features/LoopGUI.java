package org.wdssii.gui.features;

import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.LoopFeature.LoopMemento;
import org.wdssii.properties.Memento;
import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.IntegerGUI;

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

    /** Set up the components.  We haven't completely automated this because
     * you never know what little change you need that isn't supported.
     */
    private void setupComponents() {
	JScrollPane s = new JScrollPane();
	s.setViewportView(this);
	setRootComponent(s);

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
