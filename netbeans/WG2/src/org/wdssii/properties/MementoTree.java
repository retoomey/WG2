package org.wdssii.properties;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * A collection of properties that belong together.
 * Each property is referenced by a Key object 'K'
 * 
 * @author Robert Toomey
 */
public abstract class MementoTree<K> extends Memento {

	private final static Logger LOG = LoggerFactory.getLogger(MementoTree.class);

	public MementoTree() {
	}

	/**
	 * Create a full copy of another mememto
	 */
	public MementoTree(Memento m) {
	}


	
	public String getString2(Object key, String defValue){
		K k2 = (K)(key);
		String v = getPropertyValue(k2);
		if (v != null) {
			return v;
		}
		return defValue;
	}
	
	

	/**
	 * Sync to another memento by only copying what is wanted to be changed.
	 *
	 * @param m
	 */
	public abstract void syncToMemento(Memento m);

	public abstract void initProperty(K key, Object stuff);

	//public abstract void setProperty(K key, Object stuff);

	public abstract Property getProperty(K key);

	//public abstract <T extends Object> T getPropertyValue(K key);
}