/*
 * LLHAreaSliceGUI.java
 *
 * @author Robert Toomey
 * 
 */
package org.wdssii.gui.volumes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.volumes.LLHArea.LLHAreaMemento;
import org.wdssii.gui.volumes.LLHAreaSlice.LLHAreaSliceMemento;

/**
 * LLHAreaSliceGUI
 * 
 * The gui that appears in the 3D Object list when a LLHAreaSlice
 * is selected.  This allows controls for a LLHAreaSlice.
 * All 'common' controls are in the LLHAreaManager list, so for now at
 * least we'll have to recreate height controls for any subclass, but that's
 * ok since we might want different layout anyway.
 * 
 * @author Robert Toomey
 */
public class LLHAreaSliceGUI extends javax.swing.JPanel implements FeatureGUI {

    /** The VSlice object in 3D window we're linked to */
    private final LLHAreaSlice myOwner;
    /** Spinner for max height of slice */
    private JSpinner jMaxHeightKMS;
    /** Spinner for min height of slice */
    private JSpinner jMinHeightKMS;
    /** Spinner for left latitude of slice */
    private JSpinner jLeftLatDegrees;
    /** Spinner for left longitude of slice */
    private JSpinner jLeftLonDegrees;
    /** Spinner for left latitude of slice */
    private JSpinner jRightLatDegrees;
    /** Spinner for left longitude of slice */
    private JSpinner jRightLonDegrees;

    /** Creates new form LLHAreaSliceGUI */
    public LLHAreaSliceGUI(LLHAreaSlice owner) {
        myOwner = owner;
        setupComponents();
        //  String name = LLHAreaManager.getInstance().getVisibleName(owner);
        //   jNameLabel.setText(name);
    }

    /** General update call */
    @Override
    public void updateGUI() {
        LLHAreaSliceMemento m = myOwner.getMemento();
        jMaxHeightKMS.setValue(m.getMaxHeight());
        jMinHeightKMS.setValue(m.getMinHeight());
        jLeftLatDegrees.setValue(m.getLeftLatDegrees());
        jLeftLonDegrees.setValue(m.getLeftLonDegrees());
        jRightLatDegrees.setValue(m.getRightLatDegrees());
        jRightLonDegrees.setValue(m.getRightLonDegrees());
    }
    
    @Override
    public void activateGUI(JComponent parent){             
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        doLayout();     
    }
    
    @Override
    public void deactivateGUI(JComponent parent){
        parent.remove(this);
    }

    private void setupComponents() {

        /** Completely control the layout within the scrollpane.  Probably
         * don't want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC(), null, null));
        CC mid = new CC().growX().width("min:pref:");
        
        //"w min:pref:, growx");
        LLHAreaMemento m = myOwner.getMemento();
        double maxHeight = m.getMaxHeight();
        double minHeight = m.getMinHeight();
        double min = m.getMinAllowedHeight();
        double max = m.getMaxAllowedHeight();
        double size = m.getMinAllowedSize();
        SpinnerModel model;

        // Create max spinner
        jMaxHeightKMS = new JSpinner();
        jMaxHeightKMS.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jMaxHeightKMSStateChanged(evt);
            }
        });

        model =
                new SpinnerNumberModel(maxHeight, //initial value
                min + size, // min of the max value
                max, // max of the max value
                1); // 1 meter step.  Changable units might be nice
        jMaxHeightKMS.setModel(model);
        add(new JLabel("Top"));
        add(jMaxHeightKMS, mid);
        add(new JLabel("Meters"), "wrap");

        // Create min spinner
        jMinHeightKMS = new JSpinner();
        jMinHeightKMS.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jMinHeightKMSStateChanged(evt);
            }
        });

        model = new SpinnerNumberModel(minHeight, //initial value
                min, // min
                max - size, //max
                1); // 1 Meter step
        jMinHeightKMS.setModel(model);
        add(new JLabel("Bottom"));
        add(jMinHeightKMS, mid);
        add(new JLabel("Meters"), "wrap");

        // Create left latitude spinner
        jLeftLatDegrees = new JSpinner();
        jLeftLatDegrees.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jLeftLatDegreesStateChanged(evt);
            }
        });

        model = new SpinnerNumberModel(32, //initial value
                -80, // min -90 is the pole
                80, //max
                .1); // .11 degree step
        jLeftLatDegrees.setModel(model);
        add(new JLabel("LeftLat"));
        add(jLeftLatDegrees, mid);
        add(new JLabel("Degrees"), "wrap");

        // Create left longitude spinner
        jLeftLonDegrees = new JSpinner();
        jLeftLonDegrees.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jLeftLonDegreesStateChanged(evt);
            }
        });

        model = new SpinnerNumberModel(32, //initial value
                -179, // min 180 is the line
                179, //max
                .1); // .1 degree step
        jLeftLonDegrees.setModel(model);
        add(new JLabel("LeftLon"));
        add(jLeftLonDegrees, mid);
        add(new JLabel("Degrees"), "wrap");

        // Create right latitude spinner
        jRightLatDegrees = new JSpinner();
        jRightLatDegrees.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jRightLatDegreesStateChanged(evt);
            }
        });

        model = new SpinnerNumberModel(32, //initial value
                -80, // min -90 is the pole
                80, //max
                .1); // .11 degree step
        jRightLatDegrees.setModel(model);
        add(new JLabel("RightLat"));
        add(jRightLatDegrees, mid);
        add(new JLabel("Degrees"), "wrap");

        // Create right longitude spinner
        jRightLonDegrees = new JSpinner();
        jRightLonDegrees.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jRightLonDegreesStateChanged(evt);
            }
        });

        model = new SpinnerNumberModel(32, //initial value
                -179, // min 180 is the line
                179, //max
                .1); // .1 degree step
        jRightLonDegrees.setModel(model);
        add(new JLabel("RightLon"));
        add(jRightLonDegrees, mid);
        add(new JLabel("Degrees"), "wrap");

    }

    /** Change the maximum height of slice */
    private void jMaxHeightKMSStateChanged(ChangeEvent evt) {
        double value = (Double) jMaxHeightKMS.getValue();
        LLHAreaMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getMaxHeight() != value) {
            m.setMaxHeight(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** Change the minimum height of slice */
    private void jMinHeightKMSStateChanged(ChangeEvent evt) {
        double value = (Double) jMinHeightKMS.getValue();
        LLHAreaMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getMinHeight() != value) {
            m.setMinHeight(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** Change the left latitude of the slice */
    private void jLeftLatDegreesStateChanged(ChangeEvent evt) {
        double value = (Double) jLeftLatDegrees.getValue();
        LLHAreaSliceMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getLeftLatDegrees() != value) {
            m.setLeftLatDegrees(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** Change the left longitude of the slice */
    private void jLeftLonDegreesStateChanged(ChangeEvent evt) {
        double value = (Double) jLeftLonDegrees.getValue();
        LLHAreaSliceMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getLeftLonDegrees() != value) {
            m.setLeftLonDegrees(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** Change the right latitude of the slice */
    private void jRightLatDegreesStateChanged(ChangeEvent evt) {
        double value = (Double) jRightLatDegrees.getValue();
        LLHAreaSliceMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getRightLatDegrees() != value) {
            m.setRightLatDegrees(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** Change the right longitude of the slice */
    private void jRightLonDegreesStateChanged(ChangeEvent evt) {
        double value = (Double) jRightLonDegrees.getValue();
        LLHAreaSliceMemento m = myOwner.getMemento();
        LLHAreaFeature f = myOwner.getFeature();
        if (m.getRightLonDegrees() != value) {
            m.setRightLonDegrees(value);
            FeatureChangeCommand c = new FeatureChangeCommand(f, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }
}
