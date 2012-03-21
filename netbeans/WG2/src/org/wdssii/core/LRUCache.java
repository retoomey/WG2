package org.wdssii.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *   Stores a Least Recently Used Cache where access makes it less likely to be
 * deleted.
 *
 * The LRUCache uses a unique key to reference each item.
 *
 *  The cache uses a stack, where the 0th item is the oldest, n-1 the newest.
 *
 *  @author Robert Toomey
 */
public class LRUCache<K extends Comparable, T extends LRUCache.LRUCacheItem> {
    private static Logger log = LoggerFactory.getLogger(LRUCache.class);

    /**
     * Return the key used to look up this item, all 'T' objects should
     * implement this, otherwise we'll use toString
     */
    public static interface LRUCacheItem<K extends Comparable> {

        public K getCacheKey();
        
        /** Called on the item when it is trimmed out from the LRU cache */
        public void trimmed();
    }

    /**
     * Interface to return true for all objects in cache wanting to be deleted
     */
    public static interface LRUTrimComparator<T> {

        public boolean shouldDelete(T test);
    }
    /**
     * The lock for dealing with myLRUCache and myLRUStack
     */
    private final Object myLRULock = new Object();
    /**
     * The lookup map from a 'key' to the object wanted
     */
    private TreeMap<K, T> myLRUCache = new TreeMap<K, T>();
    /**
     * The LRU stack of objects
     */
    private ArrayList<T> myLRUStack = new ArrayList<T>();
    /**
     * The smallest setting for the cache size
     */
    private int myMinCacheSize;
    /**
     * The largest setting for the cache size
     */
    private int myMaxCacheSize;
    /**
     * The current full size of the cache. Could have fewer than this many items
     * in the cache
     */
    private int myCacheSize;
    
    public LRUCache(int min, int current, int max){
        myMinCacheSize = min;
        myMaxCacheSize = max;
        myCacheSize = current;
    }
    
    /**
     * Get an item given a key. Getting an item MOVES it up in the LRU stack, as
     * it has been referenced and is now more important than older entries
     */
    public T get(K key) {
        T theThing = null;
        synchronized (myLRULock) {
            theThing = myLRUCache.get(key);
        }
        raiseToTop(theThing);
        return theThing;
    }

    /**
     * Get an item given a key and remove it from our management
     */
    public T pop(K key) {
        T theThing;
        synchronized (myLRULock) {
            theThing = myLRUCache.get(key);
            if (theThing != null) {
                myLRUStack.remove(theThing);
                myLRUCache.remove(key);
            }
        }
        return theThing;
    }

    /**
     * Get an item by given index i.
     *
     *  @param i
     *  @return item at i or null
     */
    public T getStackItemNumber(int i) {
        synchronized (myLRULock) {
            if (i < myLRUStack.size()) {
                return myLRUStack.get(i);
            }
        }
        return null;
    }

    /**
     * Make a copy of the current stack. Used by GUI for synchronized access to
     * our T objects. Note that the individual T objects if modified will cause
     * sync issues, but the whole point of a cache to to keep sets of repeated
     * non-modified objects
     */
    public ArrayList<T> getStackCopy() {
        ArrayList<T> aList = null;
        synchronized (myLRULock) {  // Make sure not changing while copied
            aList = new ArrayList<T>(myLRUStack);
        }
        return aList;
    }

    public void put(K key, T putMe) {

        // Make room for the item if needed..
        trimCache(myCacheSize - 1);
        synchronized (myLRULock) {
            myLRUCache.put(key, putMe);
           // log.error("-------> KEY "+key);
            myLRUStack.add(putMe);
        }
    }

    /**
     * Kinda defeats the point, but get an object without raising it within the
     * LRU stack....normally you would just call get
     *
     *  @param key
     *  @return
     */
    public T getWithoutRaising(K key) {
        return myLRUCache.get(key);
    }

    /**
     * Remove an item from the stack and raise it to the top. This makes it the
     * newest item and last to be deleted
     *
     *  @param raiseMe
     */
    private void raiseToTop(T raiseMe) {
        if (raiseMe != null) {
            synchronized (myLRULock) {
                myLRUStack.remove(raiseMe);
                myLRUStack.add(raiseMe);
            }
        }
    }

    /**
     * Clear all entries from the cache
     */
    public void clear() {
        synchronized (myLRULock) {
            /** Let old tile objects clear out */
            Iterator<T> i = myLRUStack.iterator();
            while(i.hasNext()){
                T aT = i.next();
                aT.trimmed();
            }
            myLRUCache.clear();
            myLRUStack.clear();
        }
    }

    /**
     * Set the minimum size of the cache. This is the size we trim too
     */
    public void setMinCacheSize(int min) {
        myMinCacheSize = min;
        if (myMaxCacheSize < myMinCacheSize) {
            myMaxCacheSize = myMinCacheSize;
        }
        if (myCacheSize < myMinCacheSize) {
            setCacheSize(myMinCacheSize);
        }
    }

    public void setMaxCacheSize(int max) {
        myMaxCacheSize = max;
        if (myMinCacheSize > myMaxCacheSize) {
            myMinCacheSize = myMaxCacheSize;
        }
        if (myCacheSize > myMaxCacheSize) {
            setCacheSize(myMaxCacheSize);
        }
    }

    /**
     * Set the current size of the cache
     */
    public void setCacheSize(int size) {
        if ((size >= myMinCacheSize) && (size <= myMaxCacheSize)) {
            myCacheSize = size;
            trimCache(myCacheSize);
        }
    }

    /**
     * Get the current cache size
     */
    public int getCacheSize() {
        return myCacheSize;
    }

    /**
     * Get the current filled cache size
     */
    public int getCacheFilledSize() {
        synchronized (myLRULock) {
            return myLRUStack.size();
        }
    }

    /**
     * Get the cache key for a cached item.
     */
    private K getCacheKey(T forMe) {
        K key;
        key = (K)(forMe.getCacheKey());
        return key;
    }

    /**
     * Trim cache down to the MIN_CACHE_SIZE
     */
    public void trimCache(int toSize) {

        // Don't trim less than zero
        if (toSize < 0) {
            toSize = 0;
        }
        try {
            while (true) {
                // Drop oldest from stack until we've got space...
                synchronized (myLRULock) {
                    if (myLRUStack.size() > toSize) {
                        T oldest = myLRUStack.get(0); // Oldest
                        myLRUStack.remove(0);
                        K key = getCacheKey(oldest);
                        myLRUCache.remove(key);
                        oldest.trimmed();
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception purging cache element " + e.toString());
        }
    }

    /**
     * Trim all objects matching a LRUTrimComparator
     */
    public int trimCacheMatching(LRUTrimComparator<T> compare) {
        int removed = 0;
        try {
            ArrayList<T> toDelete = new ArrayList<T>();
            synchronized (myLRULock) {
                for (T p : myLRUStack) {
                    if (compare.shouldDelete(p)) {
                        toDelete.add(p);
                        myLRUCache.remove(getCacheKey(p));
                        removed++;
                    }
                }
                myLRUStack.removeAll(toDelete);
            }
        } catch (Exception e) {
            log.error("Exception purging cache for index " + e.toString());
        }
        return removed;
    }
}
