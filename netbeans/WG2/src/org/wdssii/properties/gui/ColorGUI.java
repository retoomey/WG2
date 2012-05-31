package org.wdssii.properties.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.wdssii.properties.Memento;
import org.wdssii.properties.PropertyGUI;
import org.wdssii.properties.Mementor;

/**
 * Stock swing controls for choosing setting a Color property
 *
 * @author Robert Toomey
 */
public class ColorGUI extends PropertyGUI {

	public ColorGUI(Mementor f, String property, String plabel, JComponent dialogRoot) {
		super(f, property);
		// Create colored button...
		JButton b = new JButton("     ");
		b.setBackground((Color) f.getMemento().getProperty(property));

		// Humm is this ok?
		final JComponent myRoot = dialogRoot;
		final String myTitle = "Choose " + plabel;
		final Mementor myF = f;
		final String myP = property;

		// Dialog 
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				jColorButtonChanged(myRoot, myTitle, myF, myP, ae);
			}
		});

		setTriple(new JLabel(plabel), b, new JLabel("Color"));
	}

	@Override
	public void update(Memento use) {
		value.setBackground((Color) use.getProperty(property));
	}

	/**
	 * Handle a color button change by changing its property value to the
	 * new color
	 */
	private static void jColorButtonChanged(JComponent root, String title, Mementor f, String property, ActionEvent evt) {

		JComponent j = (JComponent) evt.getSource();
		// Bring up color dialog with current color setting....
		Color aLineColor = JColorChooser.showDialog(root,
			title,
			j.getBackground());
		if (aLineColor != null) {
			Memento m = f.getNewMemento();
			m.setProperty(property, aLineColor);
			f.propertySetByGUI(property, m);
		}
	}
}
