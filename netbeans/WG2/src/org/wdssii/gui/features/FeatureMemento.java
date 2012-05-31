package org.wdssii.gui.features;

import org.wdssii.properties.Memento;

/**
 * Used by commands/GUI to send changes to a Feature
 * 
 * @author Robert Toomey
 */
public class FeatureMemento extends Memento {
	// Properties
	public final static String VISIBLE = "v";
	public final static String ONLY = "o";
	public final static String CAN_DELETE = "d";

	public FeatureMemento() {
		super();
		initProperty(VISIBLE, true);
		initProperty(ONLY, false);
		initProperty(CAN_DELETE, true);
	}

	/**
	 * Create a full copy of another mememto
	 */
	public FeatureMemento(Memento m) {
		super(m);
	}
}