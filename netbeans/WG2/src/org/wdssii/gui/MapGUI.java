package org.wdssii.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.MapFeature.MapMemento;

/**
 * MapGUI handles gui controls for a shapefile map....
 *
 * @author Robert Toomey
 */
public class MapGUI extends javax.swing.JPanel implements FeatureGUI {

    /**
     * The MapFeature we are using
     */
    private MapFeature myFeature;
    private JSpinner jLineThicknessSpinner;
    private JButton jColorLabel;

    /**
     * Creates new form LLHAreaSliceGUI
     */
    public MapGUI(MapFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        MapMemento m = (MapMemento) myFeature.getNewMemento();
        jLineThicknessSpinner.setValue(m.getLineThickness());
        jColorLabel.setBackground(m.getLineColor());
    }

    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        doLayout();
    }

    @Override
    public void deactivateGUI(JComponent parent) {
        parent.remove(this);
    }

    private void setupComponents() {

        /**
         * Completely control the layout within the scrollpane. Probably don't
         * want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC(), null, null));
        CC mid = new CC().growX().width("min:pref:");

        //"w min:pref:, growx");
        MapMemento m = (MapMemento) myFeature.getNewMemento();

        // Create max spinner
        jLineThicknessSpinner = new JSpinner();
        jLineThicknessSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jLineThicknessStateChanged(evt);
            }
        });
        SpinnerNumberModel model = new SpinnerNumberModel(m.getLineThickness(), //initial value
                1, // min of the max value
                15, // max of the max value
                1); // 1 step.
        jLineThicknessSpinner.setModel(model);
        add(new JLabel("Line Thickness"), "growx");
        add(jLineThicknessSpinner, mid);
        add(new JLabel("Pixels"), "wrap");

        // Create colored button...
        jColorLabel = new JButton("     ");
        jColorLabel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                jColorButtonChanged(ae);
            }
        });
        int h = jColorLabel.getHeight();
        jColorLabel.setBackground(m.getLineColor());
        add(new JLabel("Line Color"), "growx");
        add(jColorLabel, mid);
        add(new JLabel("Color"), "growx, wrap");

    }

    private void jLineThicknessStateChanged(ChangeEvent evt) {
        int value = (Integer) jLineThicknessSpinner.getValue();
        MapMemento m = (MapMemento) myFeature.getNewMemento();
        if (m.getLineThickness() != value) {
            m.setLineThickness(value);
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    private void jColorButtonChanged(ActionEvent evt) {
        // Bring up color dialog with current color setting....
        Color aLineColor = JColorChooser.showDialog(this,
                "Choose Map Line Color",
                jColorLabel.getBackground());
        if (aLineColor != null) {
            MapMemento m = (MapMemento) myFeature.getNewMemento();
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            m.setLineColor(aLineColor);
            CommandManager.getInstance().executeCommand(c, true);
        }

    }
}
