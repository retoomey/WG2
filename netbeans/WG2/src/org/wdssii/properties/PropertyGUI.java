package org.wdssii.properties;

import javax.swing.JComponent;
import net.miginfocom.layout.CC;

/**
 * A GUI for setting a particular property of a momento
 *
 * @author Robert Toomey
 */
public class PropertyGUI {

	public PropertyGUI(Mementor f, String p) {
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
	public String property;
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
}
