package org.wdssii.gui.features;

import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.gui.properties.BooleanGUI;

/**
 * LegendGUI handles gui controls for fixed floating 2d overlays
 *
 * @author Robert Toomey
 */
public class LegendGUI extends FeatureGUI {

    /**
     * The LegendFeature we are using
     */
    private LegendFeature myFeature;

    /**
     * Creates new LegendGUI
     */
    public LegendGUI(LegendFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        LegendMemento m = (LegendMemento) myFeature.getNewMemento();
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
        add(new BooleanGUI(myFeature, LegendMemento.SHOWLABELS, "Colorkey Labels", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWCOMPASS, "Compass", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWSCALE, "Scale Bar", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWWORLDINSET, "World Inset Map", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWVIEWCONTROLS, "Navigation Controls", this));
    }
}
