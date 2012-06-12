package org.wdssii.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Prototype factory registers a map of names to classes.
 * <pre>
 * {@code
 * <prototypes>
 * <class name="xml" proto="org.wdssii.index.XMLIndex"/>
 * <class name="fam" proto="org.wdssii.index.FamIndex"/>
 * </prototypes>
 * }
 * </pre>
 *
 * @author lakshman
 *
 */
public class PrototypeFactory<X extends Object> {

	private Logger log = LoggerFactory.getLogger(PrototypeFactory.class);
	private Map<String, X> myMap = new HashMap<String, X>();

	/**
	 * Add the object for given name
	 */
	private void register(String name, X prototype) {
		myMap.put(name, prototype);
	}

	/**
	 * Add a default class to the list, iff it is not there.
	 */
	public void addDefault(String name, String className) {
		if (!myMap.containsKey(name)) {
			X theClass;
			try {
				@SuppressWarnings("unchecked")
				X tryIt = (X) Class.forName(className).newInstance();
				theClass = tryIt;
				register(name, theClass);
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
	}

	public Iterator<Entry<String, X>> iterator() {
		return myMap.entrySet().iterator();
	}

	/**
	 * Get the object for a given name, if available
	 *
	 * @return null if no such prototype was registered
	 */
	public synchronized X getPrototypeMaster(String name) {
		return myMap.get(name);
	}

	@SuppressWarnings("unchecked")
	public PrototypeFactory(String configpath) {
		try {
			Element e = W2Config.getElement(configpath);
			if (e == null) {
				log.warn("did not find " + configpath + ", using built-in defaults");
			} else {
				NodeList nodes = e.getElementsByTagName("class");
				for (int i = 0; i < nodes.getLength(); ++i) {
					Element c = (Element) nodes.item(i);
					String name = c.getAttribute("name");
					String proto = c.getAttribute("proto");
					log.info("*********************REGISTERED " + name + " " + proto);

					register(name, (X) Class.forName(proto).newInstance());
					if (log.isDebugEnabled()) {
						log.debug("Registered " + proto);
					}
				}
			}
		} catch (Exception e) {
			log.error(e.toString());
		}
	}
}
