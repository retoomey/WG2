package org.wdssii.gui.gis;

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
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.gis.PolarGridFeature.PolarGridMemento;
import org.wdssii.properties.PropertyGUI;
import org.wdssii.properties.gui.ColorGUI;

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
    private JSpinner jRangeSpinner;
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
        jLineThicknessSpinner.setValue((Integer)m.getPropertyValue(PolarGridMemento.LINE_THICKNESS));
        myLineColorGUI.update(m);
        jNumRingsSpinner.setValue((Integer)m.getPropertyValue(PolarGridMemento.RING_COUNT));
        jRangeSpinner.setValue((Integer)m.getPropertyValue(PolarGridMemento.RING_RANGE));
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
	Integer t = m.getPropertyValue(PolarGridMemento.LINE_THICKNESS);
        SpinnerNumberModel model = new SpinnerNumberModel(t.intValue(), //initial value
                1, // min of the max value
                15, // max of the max value
                1); // 1 step.
        jLineThicknessSpinner.setModel(model);
        add(new JLabel("Line Thickness"), "growx");
        add(jLineThicknessSpinner, mid);
        add(new JLabel("Pixels"), "wrap");

	myLineColorGUI = new ColorGUI(myFeature, PolarGridMemento.LINE_COLOR, "Line Color", this);
 	myLineColorGUI.addToMigLayout(this);

        // Create rings spinner
        jNumRingsSpinner = new JSpinner();
        jNumRingsSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jNumRingsStateChanged(evt); 
            }
        });
	int numrings = (Integer)(m.getPropertyValue(PolarGridMemento.RING_COUNT));
        model = new SpinnerNumberModel(numrings, //initial value
                1, // min of the max value
                20, // max of the max value
                1); // 1 step.
        jNumRingsSpinner.setModel(model);
        add(new JLabel("Number of Rings"), "growx");
        add(jNumRingsSpinner, mid);
        add(new JLabel(""), "wrap");

				// Create range ring spinner
        // Create rings spinner
        jRangeSpinner = new JSpinner();
        jRangeSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jRangeStateChanged(evt); 
            }
        });
	int range = (Integer)(m.getPropertyValue(PolarGridMemento.RING_RANGE));
        model = new SpinnerNumberModel(range, //initial value
                1, // min of the max value
                50000, // max of the max value
                10); // 1 step.
        jRangeSpinner.setModel(model);
        add(new JLabel("Range per ring"), "growx");
        add(jRangeSpinner, mid);
        add(new JLabel(""), "wrap");

    }

    private void jLineThicknessStateChanged(ChangeEvent evt) {
        int value = (Integer) jLineThicknessSpinner.getValue();
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
	Integer t = m.getPropertyValue(PolarGridMemento.LINE_THICKNESS);
        if (t != value) {
	    m.setProperty(PolarGridMemento.LINE_THICKNESS, value);
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    private void jNumRingsStateChanged(ChangeEvent evt) {
        int value = (Integer) jNumRingsSpinner.getValue();
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
	Integer r = m.getPropertyValue(PolarGridMemento.RING_COUNT);
        if (r != value) {
	    m.setProperty(PolarGridMemento.RING_COUNT, value);
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    private void jRangeStateChanged(ChangeEvent evt) {
        int value = (Integer) jRangeSpinner.getValue();
        PolarGridMemento m = (PolarGridMemento) myFeature.getNewMemento();
	Integer r = m.getPropertyValue(PolarGridMemento.RING_RANGE);
        if (r != value) {
	    m.setProperty(PolarGridMemento.RING_RANGE, value);
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
