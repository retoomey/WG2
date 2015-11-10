package org.wdssii.properties;

/** A memento property.
 * Using an object, reference another object
 */
public class Property {

	public boolean use;
	public Object value;

	/**
	 * Created by initProperty
	 */
	public Property(Object o) {
		value = o;
	}

	/**
	 * Created by set property
	 */
	public Property(Object o, boolean u) {
		this(o);
		use = u;
	}

	/**
	 * Get the value stored inside us
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> T getValue() {
		return (T) value;
	}
}