package org.wdssii.gui.features;

import javax.swing.JComponent;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.properties.gui.BooleanGUI;

/**
 * LegendGUI handles gui controls for colorkey
 *
 * @author Robert Toomey
 */
public class LegendGUI extends javax.swing.JPanel implements FeatureGUI {

	/**
	 * The LegendFeature we are using
	 */
	private LegendFeature myFeature;
	//private JSpinner jLineThicknessSpinner;
	//private JButton jColorLabel;
	//private FeatureGUIFactory.FeaturePropertyGUI myShowLabelsGUI;
	private BooleanGUI myShowLabelsGUI;

	/**
	 * Creates new LegendGUI
	 */
	public LegendGUI(LegendFeature owner) {
		myFeature = owner;
		setupComponents();
	}

	/**
	 * General update call
	 */
	@Override
	public void updateGUI() {
		LegendMemento m = (LegendMemento) myFeature.getNewMemento();
		//Integer t = m.getProperty(MapMemento.LINE_THICKNESS);
		//Color c = m.getProperty(MapMemento.LINE_COLOR);
		//jLineThicknessSpinner.setValue(t);
		//jColorLabel.setBackground(c);
		myShowLabelsGUI.update(m);
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
		 * Completely control the layout within the scrollpane. Probably
		 * don't want to fill here, let the controls do default sizes
		 */
		setLayout(new MigLayout(new LC(), null, null));
		CC mid = new CC().growX().width("min:pref:");

//		myShowLabelsGUI = new FeaturePropertyBooleanGUI(myFeature, LegendMemento.SHOWLABELS, "Show Labels", this);
		myShowLabelsGUI = new BooleanGUI(myFeature, LegendMemento.SHOWLABELS, "Show Labels", this);
		myShowLabelsGUI.addToMigLayout(this);
		//"w min:pref:, growx");
	/*
		 * MapMemento m = (MapMemento) myFeature.getNewMemento();
		 *
		 * // Create max spinner jLineThicknessSpinner = new JSpinner();
		 * jLineThicknessSpinner.addChangeListener(new ChangeListener()
		 * {
		 *
		 * @Override public void
		 * stateChanged(javax.swing.event.ChangeEvent evt) {
		 * jLineThicknessStateChanged(evt); } }); Integer l =
		 * m.getProperty(MapMemento.LINE_THICKNESS); SpinnerNumberModel
		 * model = new SpinnerNumberModel(l.intValue(), //initial value
		 * 1, // min of the max value 15, // max of the max value 1); //
		 * 1 step. jLineThicknessSpinner.setModel(model); add(new
		 * JLabel("Line Thickness"), "growx");
		 * add(jLineThicknessSpinner, mid); add(new JLabel("Pixels"),
		 * "wrap");
		 *
		 * // Create colored button... jColorLabel = new JButton(" ");
		 * jColorLabel.addActionListener(new ActionListener() {
		 *
		 * @Override public void actionPerformed(ActionEvent ae) {
		 * jColorButtonChanged(ae); } }); int h =
		 * jColorLabel.getHeight(); Color lc =
		 * m.getProperty(MapMemento.LINE_COLOR);
		 * jColorLabel.setBackground(lc); add(new JLabel("Line Color"),
		 * "growx"); add(jColorLabel, mid); add(new JLabel("Color"),
		 * "growx, wrap");
		 *
		 */
	}
}
