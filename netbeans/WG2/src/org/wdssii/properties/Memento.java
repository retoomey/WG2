package org.wdssii.properties;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of properties that belong together.
 * 
 * Could call this a PropertySet as well.
 * I'm reinventing the wheel here, but haven't found a library I like
 * that does everything I want.
 *
 * @author Robert Toomey
 */
public class Memento {

	private static Logger log = LoggerFactory.getLogger(Memento.class);

	public static class Property {

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
	}
	/**
	 * Our set of feature properties
	 */
	private TreeMap<String, Property> myProperties = new TreeMap<String, Property>();

	public Memento() {
	}

	/**
	 * Create a full copy of another mememto
	 */
	public Memento(Memento m) {
		copyFromOther(m);
	}

	private void copyUsedFromOther(Memento other) {
		// Set the other momento fields to any in ours that are used
		Set<	Entry<String, Property>> entries = other.myProperties.entrySet();
		for (Entry<String, Property> e : entries) {
			Property v = e.getValue();
			if (v.use) {
				this.initProperty(e.getKey(), e.getValue().value);
			}
		}
	}

	private void copyFromOther(Memento other) {
		// Set the other momento fields to any in ours that are used
		Set<	Entry<String, Property>> entries = other.myProperties.entrySet();
		for (Entry<String, Property> e : entries) {
			Property v = e.getValue();
			this.initProperty(e.getKey(), e.getValue().value);
		}
	}

	/**
	 * Sync to another memento by only copying what is wanted to be changed.
	 *
	 * @param m
	 */
	public void syncToMemento(Memento m) {
		copyUsedFromOther(m);
	}

	protected final void initProperty(String key, Object stuff) {
		myProperties.put(key, new Property(stuff));
	}

	public final void setProperty(String key, Object stuff) {
		// FIXME: check class?
		Property f = myProperties.get(key);
		if (f == null) {
			// You need to call initProperty on the memento before setting
			log.error("Tried to set uninitialized property: " + key);
		} else {
			// FIXME: Might be able to modify the original, this makes
			// a copy...
			myProperties.put(key, new Property(stuff, true));
		}
	}

	public <T extends Object> T getProperty(String key) {
		Property f = myProperties.get(key);
		if (f != null) {
			// Can't test here because java loses generic information at
			// compile time...caller is able to test it. I don't consider
			// it a big deal since the debugger will catch it and it would
			// be an easy fix.
			@SuppressWarnings("unchecked")
			T r = (T) f.value;

			return r;
		}
		return null;
	}

	
}