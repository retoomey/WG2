package org.wdssii.gui.properties;

import javax.swing.JComponent;
import net.miginfocom.layout.CC;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;

/**
 * A GUI for setting a particular property of a momento
 *
 * @author Robert Toomey
 */
public class PropertyGUI {

    public PropertyGUI(Mementor f, Object p) {
        feature = f;
        property = p;
    }

    public void setTriple(JComponent l, JComponent v, JComponent u) {
        label = l;
        value = v;
        unit = u;
    }
    public JComponent label;
    public JComponent value;
    public JComponent unit;
    public Object property;
    public Mementor feature;

    /**
     * Util to add to a MigLayout
     */
    public void addToMigLayout(JComponent to) {
        to.add(label, new CC().growX());
        to.add(value, new CC().growX().width("min:pref:"));
        to.add(unit, new CC().growX().wrap());
    }

    public void update(Memento use) {
    }

    /**
     * Set the standard triple as enabled or not
     */
    public void setEnabled(boolean flag) {
        if (label != null) {
            label.setEnabled(flag);
        }
        if (value != null) {
            value.setEnabled(flag);
        }
        if (unit != null) {
            unit.setEnabled(flag);
        }
    }
}
