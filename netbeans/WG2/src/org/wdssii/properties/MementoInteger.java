package org.wdssii.properties;

import java.util.ArrayList;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * A memento that used integers as lookup keys. It assumes all keys are added
 * during construction. In other words, you can't add key number 1500 without
 * adding 1-1499 first.
 *
 * @author Robert Toomey
 */
public class MementoInteger extends MementoTree<Integer> {

    private final static Logger LOG = LoggerFactory.getLogger(MementoInteger.class);
    /**
     * Our list of properties
     */
    public ArrayList<Property> myProperties;

    public MementoInteger(int size) {
        myProperties = new ArrayList<Property>(size);
    }

    public MementoInteger() {
        myProperties = new ArrayList<Property>();
    }

    /**
     * Create a full copy of another memento. Type should match
     */
    public MementoInteger(Memento m) {
        copyFromOther(m);
    }

    private void copyUsedFromOther(Memento other) {

        if (other instanceof MementoInteger) {
            MementoInteger mi = (MementoInteger) (other);

            int length = mi.myProperties.size();
            for (int i = 0; i < length; i++) {
            	Property v = mi.getProperty(i);
                if (v.use) {
                    this.initProperty(i, v.value);
                }
            }
            // Set the other momento fields to any in ours that are used
		/*Set<	Entry<String, MementoInt.Property>> entries = other.myProperties.entrySet();
             for (Entry<String, MementoInt.Property> e : entries) {
             MementoInt.Property v = e.getValue();
             if (v.use) {
             this.initProperty(e.getKey(), e.getValue().value);
             }
             }*/
        }
    }

    private void copyFromOther(Memento other) {
        // Set the other momento fields to any in ours that are used
		/*Set<	Entry<String, MementoInt.Property>> entries = other.myProperties.entrySet();
         for (Entry<String, MementoInt.Property> e : entries) {
         MementoInt.Property v = e.getValue();
         this.initProperty(e.getKey(), e.getValue().value);
         }
         * */
        if (other instanceof MementoInteger) {
            MementoInteger mi = (MementoInteger) (other);
            int length = mi.myProperties.size();
            myProperties = new ArrayList<Property>(length);
            for (int i = 0; i < length; i++) {
            	Property v = mi.getProperty(i);
                this.initProperty(i, v.value);
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

    public static void ensureSize(ArrayList<?> list, int size) {
        // Prevent excessive copying while we're adding
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }

    @Override
    public void initProperty(Integer key, Object stuff) {
        if (key >= myProperties.size()) {
            ensureSize(myProperties, key+1);
        }
        myProperties.set(key, new Property(stuff));
    }

    @Override
    public void setProperty(Object okey, Object stuff) {
    	Integer key = (Integer)(okey);
    	Property f = myProperties.get(key);
        if (f == null) {
            // You need to call initProperty on the memento before setting
            LOG.error("Tried to set uninitialized property: " + key);
        } else {
            // FIXME: Might be able to modify the original, this makes
            // a copy...
            myProperties.set(key, new Property(stuff, true));
        }
    }

    @Override
    public Property getProperty(Integer key) {
        return myProperties.get(key);
    }

    @Override
    public <T extends Object> T getPropertyValue(Object okey) {
    	Integer key = (Integer)(okey);
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