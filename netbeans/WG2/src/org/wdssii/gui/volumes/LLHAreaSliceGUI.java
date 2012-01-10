/*
 * LLHAreaSliceGUI.java
 *
 * @author Robert Toomey
 * 
 */
package org.wdssii.gui.volumes;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.commands.LLHAreaChangeCommand;
import org.wdssii.gui.volumes.LLHArea.LLHAreaMemento;

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
public class LLHAreaSliceGUI extends javax.swing.JPanel {

    /** The VSlice object in 3D window we're linked to */
    private final LLHAreaSlice myOwner;
    /** Spinner for max height of slice */
    private JSpinner jMaxHeightKMS;
    /** Spinner for min height of slice */
    private JSpinner jMinHeightKMS;

    /** Creates new form LLHAreaSliceGUI */
    public LLHAreaSliceGUI(LLHAreaSlice owner) {
        myOwner = owner;
        setupComponents();
        //  String name = LLHAreaManager.getInstance().getVisibleName(owner);
        //   jNameLabel.setText(name);
    }

    /** General update call */
    public void updateGUI() {
        LLHAreaMemento m = myOwner.getMemento();
        jMaxHeightKMS.setValue(m.getMaxHeight());
        jMinHeightKMS.setValue(m.getMinHeight());
    }

    private void setupComponents() {

        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        LLHAreaMemento m = myOwner.getMemento();
        double maxHeight = m.getMaxHeight();
        double minHeight = m.getMinHeight();
        double min = m.getMinAllowedHeight();
        double max = m.getMaxAllowedHeight();
        double size = m.getMinAllowedSize();
        
        // Create max spinner
        jMaxHeightKMS = new JSpinner();
        jMaxHeightKMS.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jMaxHeightKMSStateChanged(evt);
            }
        });

        SpinnerModel model =
                new SpinnerNumberModel(maxHeight, //initial value
                min+size, // min of the max value
                max,      // max of the max value
                1); // 1 meter step.  Changable units might be nice
        jMaxHeightKMS.setModel(model);
        add(new JLabel("Top"));
        add(jMaxHeightKMS);
        add(new JLabel("Meters"), "wrap");
        

        // Create min spinner
        jMinHeightKMS = new JSpinner();
        jMinHeightKMS.addChangeListener(new javax.swing.event.ChangeListener() {

            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jMinHeightKMSStateChanged(evt);
            }
        });

        SpinnerModel model2 =
                new SpinnerNumberModel(minHeight, //initial value
                min, // min
                max-size, //max
                1); // 1 Meter step
        jMinHeightKMS.setModel(model2);
        add(new JLabel("Bottom"));
        add(jMinHeightKMS);
        add(new JLabel("Meters"), "wrap");
    }

    /** Change the maximum height of slice */
    private void jMaxHeightKMSStateChanged(ChangeEvent evt) {
        double value = (Double) jMaxHeightKMS.getValue();
        LLHAreaMemento m = myOwner.getMemento();
        m.setMaxHeight(value);
        LLHAreaChangeCommand c = new LLHAreaChangeCommand(myOwner, m);
        CommandManager.getInstance().executeCommand(c, true);
    }

    /** Change the minimum height of slice */
    private void jMinHeightKMSStateChanged(ChangeEvent evt) {
        double value = (Double) jMinHeightKMS.getValue();
        LLHAreaMemento m = myOwner.getMemento();
        m.setMinHeight(value);
        LLHAreaChangeCommand c = new LLHAreaChangeCommand(myOwner, m);
        CommandManager.getInstance().executeCommand(c, true);
    }
}
