package org.wdssii.gui.gis;

import java.awt.Color;
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
import org.wdssii.gui.gis.MapFeature.MapMemento;
import org.wdssii.properties.PropertyGUI;
import org.wdssii.properties.gui.ColorGUI;

/**
 * MapGUI handles gui controls for a shapefile map....
 *
 * @author Robert Toomey
 */
public class MapGUI extends FeatureGUI {

    /**
     * The MapFeature we are using
     */
    private MapFeature myFeature;
    private JSpinner jLineThicknessSpinner;
    //private JButton jColorLabel;
    private PropertyGUI myLineColorGUI;

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
	Integer t = m.getPropertyValue(MapMemento.LINE_THICKNESS);
	Color c = m.getPropertyValue(MapMemento.LINE_COLOR);
        jLineThicknessSpinner.setValue(t);
        myLineColorGUI.update(m);
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
	Integer l = m.getPropertyValue(MapMemento.LINE_THICKNESS);
        SpinnerNumberModel model = new SpinnerNumberModel(l.intValue(), //initial value
                1, // min of the max value
                15, // max of the max value
                1); // 1 step.
        jLineThicknessSpinner.setModel(model);
        add(new JLabel("Line Thickness"), "growx");
        add(jLineThicknessSpinner, mid);
        add(new JLabel("Pixels"), "wrap");

	// Line color
	myLineColorGUI = new ColorGUI(myFeature, MapMemento.LINE_COLOR, "Line Color", this);
 	myLineColorGUI.addToMigLayout(this);

	/*
        // Create colored button...
        jColorLabel = new JButton("     ");
        jColorLabel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                jColorButtonChanged(ae);
            }
        });
        int h = jColorLabel.getHeight();
	Color lc = m.getProperty(MapMemento.LINE_COLOR);
        jColorLabel.setBackground(lc);
        add(new JLabel("Line Color"), "growx");
        add(jColorLabel, mid);
        add(new JLabel("Color"), "growx, wrap");
	*/

    }

    private void jLineThicknessStateChanged(ChangeEvent evt) {
        int value = (Integer) jLineThicknessSpinner.getValue();
        MapMemento m = (MapMemento) myFeature.getNewMemento();
	Integer r = m.getPropertyValue(MapMemento.LINE_THICKNESS);
        if (r != value) {
	    m.setProperty(MapMemento.LINE_THICKNESS, value);
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /*
    private void jColorButtonChanged(ActionEvent evt) {
        // Bring up color dialog with current color setting....
        Color aLineColor = JColorChooser.showDialog(this,
                "Choose Map Line Color",
                jColorLabel.getBackground());
        if (aLineColor != null) {
            MapMemento m = (MapMemento) myFeature.getNewMemento();
            FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
	    m.setProperty(MapMemento.LINE_COLOR, aLineColor);
            CommandManager.getInstance().executeCommand(c, true);
        }

    }
    * 
    */

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
