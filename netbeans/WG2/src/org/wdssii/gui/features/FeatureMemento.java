package org.wdssii.gui.features;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used by commands/GUI to send changes to a Feature
 * 
 * @author Robert Toomey
 */
public class FeatureMemento {

        private static Logger log = LoggerFactory.getLogger(FeatureMemento.class);

	public static class FeatureProperty {

		public boolean use;
		public Object value;

		/** Created by initProperty */
		public FeatureProperty(Object o) {
			value = o;
		}

		/** Created by set property */
		public FeatureProperty(Object o, boolean u) {
			this(o);
			use = u;
		}
	}

	/** Our set of feature properties */
	private TreeMap<String, FeatureProperty> myProperties = new TreeMap<String, FeatureProperty>();

	// Properties
	public final static String VISIBLE = "v";
	public final static String ONLY = "o";
	public final static String CAN_DELETE = "d";

	/** Create a full copy of another mememto */
	public FeatureMemento(FeatureMemento m) {
		copyFromOther(m);
	}

	private void copyUsedFromOther(FeatureMemento other) {
		// Set the other momento fields to any in ours that are used
		Set<	Entry<String, FeatureProperty>> entries = other.myProperties.entrySet();
		for (Entry<String, FeatureProperty> e : entries) {
			FeatureProperty v = e.getValue();
			if (v.use) {
				this.initProperty(e.getKey(), e.getValue().value);
			}
		}
	}

	private void copyFromOther(FeatureMemento other) {
		// Set the other momento fields to any in ours that are used
		Set<	Entry<String, FeatureProperty>> entries = other.myProperties.entrySet();
		for (Entry<String, FeatureProperty> e : entries) {
			FeatureProperty v = e.getValue();
			this.initProperty(e.getKey(), e.getValue().value);
		}
	}

	/** Sync to another memento by only copying what is wanted
	 * to be changed.
	 * @param m 
	 */
	public void syncToMemento(FeatureMemento m) {
		copyUsedFromOther(m);
	}

	public FeatureMemento() {
		initProperty(VISIBLE, true);
		initProperty(ONLY, false);
		initProperty(CAN_DELETE, true);
	}

	protected final void initProperty(String key, Object stuff) {
		myProperties.put(key, new FeatureProperty(stuff));
	}

	public final void setProperty(String key, Object stuff) {
		// FIXME: check class?
		FeatureProperty f = myProperties.get(key);
		if (f == null){
			// You need to call initProperty on the memento before setting
			log.error("Tried to set uninitialized property: "+key);
		}else{
	           // FIXME: Might be able to modify the original, this makes
	           // a copy...
		   myProperties.put(key, new FeatureProperty(stuff, true));
		}
	}

	public <T extends Object> T getProperty(String key) {
		FeatureProperty f = myProperties.get(key);
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