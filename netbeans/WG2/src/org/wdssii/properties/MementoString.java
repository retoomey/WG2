package org.wdssii.properties;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * A memento using strings to lookup properties
 *
 *
 * @author Robert Toomey
 */
public class MementoString extends MementoTree<String> {

    private final static Logger LOG = LoggerFactory.getLogger(MementoString.class);
    /**
     * Our set of feature properties
     */
    private TreeMap<String, Property> myProperties = new TreeMap<String, Property>();

    public MementoString() {
    }

    /**
     * Create a full copy of another mememto
     */
    public MementoString(Memento m) {
        copyFromOther(m);
    }

    private void copyUsedFromOther(Memento o) {

        if (o instanceof MementoString) {
            MementoString other = (MementoString) (o);
            // Set the other momento fields to any in ours that are used
            Set<Entry<String, Property>> entries = other.myProperties.entrySet();
            for (Entry<String, Property> e : entries) {
            	Property v = e.getValue();
                if (v.use) {
                    this.initProperty(e.getKey(), e.getValue().value);
                }
            }
        }
    }

    private void copyFromOther(Memento o) {
        if (o instanceof MementoString) {
            MementoString other = (MementoString) (o);
            // Set the other momento fields to any in ours that are used
            Set<Entry<String, Property>> entries = other.myProperties.entrySet();
            for (Entry<String, Property> e : entries) {
            	Property v = e.getValue();
                this.initProperty(e.getKey(), e.getValue().value);
            }
        }
    }

    /**
     * Sync to another memento by only copying what is wanted to be changed.
     *
     * @param m
     */
    @Override
    public void syncToMemento(Memento m) {
        copyUsedFromOther(m);
    }

    @Override
    public void initProperty(String key, Object stuff) {
        myProperties.put(key, new Property(stuff));
    }

    @Override
    public void setProperty(Object okey, Object stuff) {
    	String key = (String)(okey);
    	Property f = myProperties.get(key);
        if (f == null) {
            LOG.error("Creating new property: " + key);
            myProperties.put(key, new Property(stuff, true));
        } else {
            myProperties.put(key, new Property(stuff, true));
        }
    }

    @Override
    public Property getProperty(String key) {
        return myProperties.get(key);
    }

    @Override
    public <T extends Object> T getPropertyValue(Object okey) {
    	String key = (String)(okey);
    	Property f = myProperties.get(key);
        if (f != null) {
            T r;
            try {
                @SuppressWarnings("unchecked")
                T p = (T) f.getValue();
                r = p;
            } catch (Exception e) {
                r = null;
            }

            return r;
        }
        return null;
    }
}