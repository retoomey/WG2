package org.wdssii.gui.features;

import javax.swing.JComponent;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.properties.gui.BooleanGUI;

/**
 * LegendGUI handles gui controls for colorkey
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

        /**
         * Completely control the layout within the scrollpane. Probably don't
         * want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC(), null, null));

        add(new BooleanGUI(myFeature, LegendMemento.SHOWLABELS, "Colorkey Labels", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWCOMPASS, "Compass", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWSCALE, "Scale Bar", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWWORLDINSET, "World Inset Map", this));
        add(new BooleanGUI(myFeature, LegendMemento.SHOWVIEWCONTROLS, "Navigation Controls", this));
    }
}
