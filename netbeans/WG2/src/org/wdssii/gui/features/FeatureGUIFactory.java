package org.wdssii.gui.features;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import net.miginfocom.layout.CC;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;

/**
 * A first attempt to generalize the creation of Feature setting change GUI
 * controls. I'm sure I can generalize even more, use reflection as well...
 * FIXME: should probably make a subpackage for all the control classes
 *
 * @author Robert Toomey
 */
public class FeatureGUIFactory {

	// Humm...first attempt, right?  Return an object with each
	// thingy. This is for layout..or should we just do layout as well..?
	public static class FeaturePropertyGUI {

		public FeaturePropertyGUI(Feature f, String p) {
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
		public Feature feature;

		/**
		 * Util to add to a MigLayout
		 */
		public void addToMigLayout(JComponent to) {
			to.add(label, new CC().growX());
			to.add(value, new CC().growX().width("min:pref:"));
			to.add(unit, new CC().growX().wrap());
		}

		public void update(FeatureMemento use) {
		}
	}

	/**
	 * Stock property change GUI for changing a Color
	 */
	public static class FeaturePropertyColorGUI extends FeaturePropertyGUI {

		public FeaturePropertyColorGUI(Feature f, String property, String plabel, JComponent dialogRoot) {
			super(f, property);
			// Create colored button...
			JButton b = new JButton("     ");
			b.setBackground((Color) f.getMemento().getProperty(property));

			// Humm is this ok?
			final JComponent myRoot = dialogRoot;
			final String myTitle = "Choose " + plabel;
			final Feature myF = f;
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
		public void update(FeatureMemento use) {
			value.setBackground((Color) use.getProperty(property));
		}
	}

	/**
	 * Handle a color button change by changing its property value to the
	 * new color
	 */
	private static void jColorButtonChanged(JComponent root, String title, Feature f, String property, ActionEvent evt) {

		JComponent j = (JComponent) evt.getSource();
		// Bring up color dialog with current color setting....
		Color aLineColor = JColorChooser.showDialog(root,
			title,
			j.getBackground());
		if (aLineColor != null) {
			FeatureMemento m = f.getNewMemento();
			m.setProperty(property, aLineColor);
			FeatureChangeCommand c = new FeatureChangeCommand(f, m);
			CommandManager.getInstance().executeCommand(c, true);
		}
	}

	/**
	 * Stock property change GUI for changing a Boolean
	 */
	public static class FeaturePropertyBooleanGUI extends FeaturePropertyGUI {

		public FeaturePropertyBooleanGUI(Feature f, String property, String plabel, JComponent dialogRoot) {
			super(f, property);
			// Create checked button...
			JCheckBox b = new JCheckBox();
			b.setSelected((Boolean) f.getMemento().getProperty(property));

			// Humm is this ok?
			final JComponent myRoot = dialogRoot;
			final Feature myF = f;
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
		public void update(FeatureMemento use) {
			JCheckBox v = (JCheckBox) (value);
			v.setSelected((Boolean) use.getProperty(property));
		}
	}

	/**
	 * Handle a color button change by changing its property value to the
	 * new color
	 */
	private static void jBooleanButtonChanged(JComponent root, Feature f, String property, ActionEvent evt) {

		JComponent j = (JComponent) evt.getSource();
		if (j instanceof JCheckBox) {
			JCheckBox b = (JCheckBox) (j);
			FeatureMemento m = f.getNewMemento();
			m.setProperty(property, b.isSelected());
			FeatureChangeCommand c = new FeatureChangeCommand(f, m);
			CommandManager.getInstance().executeCommand(c, true);
		}
	}
}
