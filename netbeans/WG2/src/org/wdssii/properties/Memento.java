package org.wdssii.properties;

/** The base class of memento.  Store properties.
 * @author Robert Toomey
 *  */
public class Memento {
	
	private Object eventSource = null;
	
	
	public void setEventSource(Object o){
		eventSource = o;
	}
	
	public Object getEventSource(){
		return eventSource;
	}
	
	/** Set the property given the given key.  Subclasses
	 * store this how they want */
	public  void setProperty(Object key, Object stuff){
		
	};
	
	/** Lookup anything, return null if not found.  
	 * Usually, you'd use the regular 'get' for convenience,
	 * keeps you from tons of null checks in your code.
	 */
	public <T extends Object> T getPropertyValue(Object key){
		return null;
	};
	
	/** Lookup anything with a default return value.
	 * Basically, pass in the default value of the object, it is
	 * used if the value is not found in our property true.  Also,
	 * the default tells us the object type.  Kinda neat.
	 * 
	 * Integer test = get(thekey, 6);
	 * --> test will be the value, or 6 if not found...
	 * String test = get(thekey, "defaultvalue")
	 * Color c = get(thekey, Color.White);
	 *  **/
	public <Z extends Object> Z get (Object key, Z defValue){
		Z v = getPropertyValue(key);
		if (v != null) {
			return v;
		}
		return defValue;
	}
	
}
