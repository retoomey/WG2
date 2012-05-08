package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * Feature will be anything that we display in a window. A feature will be
 * unique per window. So 'visible' or 'color' will be at this level. Any
 * cached/shared information will be from contained objects. A Feature can
 * create a FeatureGUI for setting the properties of the feature. When the
 * feature is selected in the display, its GUI will be shown.
 *
 * @author Robert Toomey
 */
public class Feature {

	/**
	 * Used to get information back from a Feature factory to put into the
	 * standard GUI table
	 */
	public static class FeatureTableInfo {

		public boolean visible;
		public boolean onlyMode;
		public String visibleName;
		public String keyName;
		public String message;
		public Object created;
	}
	/**
	 * Our feature group
	 */
	private final String myFeatureGroup;
	/**
	 * Our feature list we belong too
	 */
	private final FeatureList myFeatureList;
	private FeatureMemento mySettings = new FeatureMemento(true, false);
	/**
	 * What is our name?
	 */
	private String myName = "";
	/**
	 * What is our key?
	 */
	private String myKey = "";
	/**
	 * What is our message?
	 */
	private String myMessage = "";
	/** Generic Feature3DRenderers */
	ArrayList<Feature3DRenderer> myRenderers;

	/** Create a feature with a default memento */
	public Feature(FeatureList f, String g) {
		this(f, g, new FeatureMemento(true, false));
	}

	/**
	 * Typically called by subclass to add an enhanced memento with more
	 * settings in it.
	 *
	 * @param g The group we're in
	 * @param settings Memento from subclass
	 */
	public Feature(FeatureList f, String g, FeatureMemento settings) {
		myFeatureList = f;
		myFeatureGroup = g;
		mySettings = settings;
	}

	/** All features belong to a FeatureList */
	public FeatureList list() {
		return myFeatureList;
	}

	/**
	 * Get our feature group
	 */
	public String getFeatureGroup() {
		return myFeatureGroup;
	}

	public void setMemento(FeatureMemento m) {
		if (m != null) {
			mySettings.syncToMemento(m);
		}
	}

	/**
	 * Get a new memento copy of our settings. This is for modifying and sending
	 * back to us to change a setting
	 *
	 * @return
	 */
	public FeatureMemento getNewMemento() {
		FeatureMemento m = new FeatureMemento(mySettings);
		return m;
	}

	/**
	 * Get our actual settings
	 */
	public FeatureMemento getMemento() {
		return mySettings;
	}

	/**
	 * Get visible state
	 */
	public boolean getVisible() {
		return mySettings.getVisible();
	}

	/**
	 * Set visible state
	 */
	public void setVisible(boolean flag) {
		mySettings.setVisible(flag);
	}

	/**
	 * Get visible state
	 */
	public boolean getOnlyMode() {
		return mySettings.getOnly();
	}

	/**
	 * Set visible state
	 */
	public void setOnlyMode(boolean flag) {
		mySettings.setOnly(flag);
	}

	/**
	 * Get the name of this feature
	 */
	public String getName() {
		return myName;
	}

	/**
	 * Set the name of this feature
	 */
	public void setName(String n) {
		myName = n;
	}

	/**
	 * Get the key for this feature
	 */
	public String getKey() {
		return myKey;
	}

	/**
	 * Set the key for this feature
	 */
	public void setKey(String n) {
		myKey = n;
	}

	/**
	 * Get the name of this feature
	 */
	public String getMessage() {
		return myMessage;
	}

	/**
	 * Set the name of this feature
	 */
	public void setMessage(String n) {
		myMessage = n;
	}

	/** Sent from list to let us know we were selected */
	public void wasSelected() {
	}

	public void addRenderer(Feature3DRenderer f) {
		// Lazy create to save memory
		if (myRenderers == null) {
			myRenderers = new ArrayList<Feature3DRenderer>();
		}
		// Add if not already there...
		if (f != null) {
			if (!myRenderers.contains(f)) {
				myRenderers.add(f);
			}
		}
	}

	public void removeRenderer(Feature3DRenderer f) {
		if (myRenderers != null) {
			myRenderers.remove(f);
		}
	}

	/**
	 * Render a feature
	 */
	public void render(DrawContext dc) {
		if (myRenderers != null) {
			FeatureMemento m = getMemento();
			for (Feature3DRenderer r : myRenderers) {
				r.draw(dc, m);
			}
		}
	}

	/** Would this feature render?  This may be different than is visible or not,
	 * for example a product might be 'visible' but won't draw because it is
	 * too old in time
	 */
	public boolean wouldRender() {
		return getVisible();  // Default is visible manual setting
	}

	public void setupFeatureGUI(JComponent source) {

		// Set the layout and add our controls
		source.setLayout(new java.awt.BorderLayout());
		JTextField t = new JTextField();
		t.setText("No controls for this object");
		t.setEditable(false);
		source.add(t, java.awt.BorderLayout.CENTER);
		source.doLayout();

		updateGUI();
	}

	public void updateGUI() {
	}
}
