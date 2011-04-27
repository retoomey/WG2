package org.wdssii.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author lakshman
 * 
 */
public class PrototypeFactory<X extends Object> {

    private Log log = LogFactory.getLog(PrototypeFactory.class);
    private Map<String, X> myMap = new HashMap<String, X>();

    private void register(String name, X prototype) {
        myMap.put(name, prototype);
    }

    /**
     * Get the master
     * 
     * @return null if no such prototype was registered
     */
    public synchronized X getPrototypeMaster(String name) {
        return myMap.get(name);
    }

    @SuppressWarnings("unchecked")
    public PrototypeFactory(String configpath) {
        try {
            Element e = W2Config.getFileElement(configpath);
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
        } catch (Exception e) {
            log.error(e);
        }
    }
}
