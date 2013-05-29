package org.wdssii.properties;

/**
 * Mementor is what holds on to and uses a memento
 * 
 * @author Robert Toomey
 */
public interface Mementor {

	/** A GUI control has set a property within a memento */
	public void propertySetByGUI(Object name, Memento m);

	/**
	 * Get a new memento copy of our settings. 
	 * This is for setting a property and notification
	 */
	public Memento getNewMemento();

	/**
	 * Get the current settings. This is used to read a property
	 */
	public Memento getMemento();
}
