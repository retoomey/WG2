package org.wdssii.gui.properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;

/**
 * Stock GUI for changing a Integer property
 *
 * @author Robert Toomey
 */
public class IntegerGUI extends PropertyGUI {

    /**
     * Create a spinner by default
     */
    public IntegerGUI(Mementor f, Object property,
            String plabel, JComponent dialogRoot,
            int min, // Or do we store min/max values in the property??
            int max,
            int step,
            String units) {
        super(f, property);

        // Create spinner
        JSpinner s = new JSpinner();
        int vin = min;

        Memento m = f.getMemento();
        try {

            Integer v = ((Integer) m.getPropertyValue(property));
            if (v != null) {
                vin = v.intValue();
            }
        } catch (Exception e) {
            int a = 1;
            Object something = m.getPropertyValue(property);
        }

        vin = Math.max(Math.min(max, vin), min);
        SpinnerNumberModel model = new SpinnerNumberModel(
                vin, //initial value
                min, // min of the max value
                max, // max of the max value
                step); // 1 step.
        s.setModel(model);
        String format = String.format("(%d-%d) %s", min, max, units);
        JLabel range = new JLabel(format);

        // Set up change listener
        final JComponent myRoot = dialogRoot;
        final Mementor myF = f;
        final Object myP = property;
        s.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerStateChanged(myRoot, myF, myP, evt);
            }
        });

        // Set up in layout
        setTriple(new JLabel(plabel), s, range);
    }

    @Override
    public void update(Memento use) {
        JSpinner v = (JSpinner) (value);
        v.setValue(use.getPropertyValue(property));
    }

    /**
     * Handle a spinner change by changing its property value to the new integer
     */
    private static void jSpinnerStateChanged(JComponent root, Mementor f, Object property, ChangeEvent evt) {

        JComponent j = (JComponent) evt.getSource();
        if (j instanceof JSpinner) {
            JSpinner s = (JSpinner) (j);
            //Memento m = f.getNewMemento();
            Memento m = f.getUpdateMemento(); // blank memento
            m.setProperty(property, (Integer) s.getValue());
            f.propertySetByGUI(property, m);
        }
    }
}
