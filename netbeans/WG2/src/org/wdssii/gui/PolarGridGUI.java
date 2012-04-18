package org.wdssii.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.features.PolarGridFeature.PolarGridMemento;

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
    private JSpinner jLineThicknessSpinner;
    private JSpinner jNumRingsSpinner;
    private JButton jColorLabel;

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
        jLineThicknessSpinner.setValue(m.getLineThickness());
        jColorLabel.setBackground(m.getLineColor());
        jNumRingsSpinner.setValue(m.getNumRings());
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
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();

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

        // Create rings spinner
        jNumRingsSpinner = new JSpinner();
        jNumRingsSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jNumRingsStateChanged(evt); 
            }
        });
        model = new SpinnerNumberModel(m.getNumRings(), //initial value
                1, // min of the max value
                20, // max of the max value
                1); // 1 step.
        jNumRingsSpinner.setModel(model);
        add(new JLabel("Number of Rings"), "growx");
        add(jNumRingsSpinner, mid);
        add(new JLabel(""), "wrap");

    }

    private void jLineThicknessStateChanged(ChangeEvent evt) {
        int value = (Integer) jLineThicknessSpinner.getValue();
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
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
            PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            m.setLineColor(aLineColor);
            CommandManager.getInstance().executeCommand(c, true);
        }

    }

    private void jNumRingsStateChanged(ChangeEvent evt) {
        int value = (Integer) jNumRingsSpinner.getValue();
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
        if (m.getNumRings() != value) {
            m.setNumRings(value);
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /**
     * Load an individual file into the ManualLoadIndex
     */
    public static URL doSingleMapOpenDialog() {

        URL pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                // FIXME: need to get these from the Builders
                return (f.isDirectory() || t.endsWith(".shp"));
            }

            @Override
            public String getDescription() {
                return "ESRI Shapefile";
            }
        });
        chooser.setDialogTitle("Add single map");
        // rcp chooiser.setFilterPath("D:/") ?

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL();
            } catch (MalformedURLException ex) {
                // We assume that chooser knows not to return
                // malformed urls...
            }
        }
        return pickedFile;
    }
}
