package org.wdssii.gui.features;

import javax.swing.JScrollPane;

import org.wdssii.gui.features.EarthBallFeature.EarthBallMemento;
import org.wdssii.gui.properties.IntegerGUI;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

public class EarthBallGUI extends FeatureGUI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The EarthBallFeature we are using
	 */
	private EarthBallFeature myFeature;

	/**
	 * General update call
	 */
	@Override
	public void updateGUI() {
		EarthBallMemento m = (EarthBallMemento) myFeature.getNewMemento();
		updateToMemento(m);
	}

	/**
	 * Creates new
	 */
	public EarthBallGUI(EarthBallFeature owner) {
		myFeature = owner;
		setupComponents();
	}

	private void setupComponents() {
		JScrollPane s = new JScrollPane();
		s.setViewportView(this);
		setRootComponent(s);

		setLayout(new MigLayout(new LC(), null, null));
        add(new IntegerGUI(myFeature, EarthBallMemento.BALL_DENSITY, "GLU ball density", this,
                50, 400, 50, "units"));
		// add(new IntegerGUI(myFeature, MapMemento.LINE_THICKNESS, "Line Thickness",
		// this,
		// 1, 15, 1, "Pixels"));
		// add(new ColorGUI(myFeature, MapMemento.LINE_COLOR, "Line Color", this));
	}
}
