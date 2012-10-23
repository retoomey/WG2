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
	private BooleanGUI myShowLabelsGUI;
	private BooleanGUI myShowCompassGUI;
	private BooleanGUI myShowScaleGUI;
	private BooleanGUI myShowInsetGUI;
	private BooleanGUI myShowControlsGUI;

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
		myShowLabelsGUI.update(m);
		myShowCompassGUI.update(m);
		myShowScaleGUI.update(m);
		myShowInsetGUI.update(m);
		myShowControlsGUI.update(m);
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
		 * Completely control the layout within the scrollpane. Probably
		 * don't want to fill here, let the controls do default sizes
		 */
		setLayout(new MigLayout(new LC(), null, null));
		CC mid = new CC().growX().width("min:pref:");

		myShowLabelsGUI = new BooleanGUI(myFeature, LegendMemento.SHOWLABELS, "Colorkey Labels", this);
		myShowLabelsGUI.addToMigLayout(this);

		myShowCompassGUI = new BooleanGUI(myFeature, LegendMemento.SHOWCOMPASS, "Compass", this);
		myShowCompassGUI.addToMigLayout(this);

		myShowScaleGUI = new BooleanGUI(myFeature, LegendMemento.SHOWSCALE, "Scale Bar", this);
		myShowScaleGUI.addToMigLayout(this);

		myShowInsetGUI = new BooleanGUI(myFeature, LegendMemento.SHOWWORLDINSET, "World Inset Map", this);
		myShowInsetGUI.addToMigLayout(this);

		myShowControlsGUI = new BooleanGUI(myFeature, LegendMemento.SHOWVIEWCONTROLS, "Navigation Controls", this);
		myShowControlsGUI.addToMigLayout(this);
	}
}
