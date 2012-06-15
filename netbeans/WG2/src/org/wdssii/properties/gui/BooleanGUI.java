package org.wdssii.properties.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.properties.PropertyGUI;

/**
 * Stock GUI for changing a boolean setting
 *
 * @author Robert Toomey
 */
public class BooleanGUI extends PropertyGUI {

	public BooleanGUI(Mementor f, String property, String plabel, JComponent dialogRoot) {
		super(f, property);
		// Create checked button...
		JCheckBox b = new JCheckBox();
		b.setSelected((Boolean) f.getMemento().getPropertyValue(property));

		// Humm is this ok?
		final JComponent myRoot = dialogRoot;
		final Mementor myF = f;
		final String myP = property;

		// Dialog 
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				jBooleanButtonChanged(myRoot, myF, myP, ae);
			}
		});

		setTriple(new JLabel(plabel), b, new JLabel(""));
	}

	@Override
	public void update(Memento use) {
		JCheckBox v = (JCheckBox) (value);
		v.setSelected((Boolean) use.getPropertyValue(property));
	}

	/**
	 * Handle a color button change by changing its property value to the
	 * new color
	 */
	private static void jBooleanButtonChanged(JComponent root, Mementor f, String property, ActionEvent evt) {

		JComponent j = (JComponent) evt.getSource();
		if (j instanceof JCheckBox) {
			JCheckBox b = (JCheckBox) (j);
			Memento m = f.getNewMemento();
			m.setProperty(property, b.isSelected());
			f.propertySetByGUI(property, m);
		}
	}
}
