package org.wdssii.core;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 *  Stores a Least Recently Used Cache where access makes it less likely
 * to be deleted.  Generic...for something like 'Float' the cache key is the
 * toString function.  More complicated objects can interface LRUCacheItem
 * to return the cache key.
 * 
 * FIXME: not sure the cache key even needed really...the object should be
 * enough..
 * 
 * @author Robert Toomey
 */
public class LRUCache<T> {

    /** Return the key used to look up this item, all 'T' objects should
     * implement this, otherwise we'll use toString
     */
    public static interface LRUCacheItem {

        public String getCacheKey();
    }

    /** Interface to return true for all objects in cache wanting
     * to be deleted
     */
    public static interface LRUTrimComparator<T> {

        public boolean shouldDelete(T test);
    }
    /** The lookup map from a 'key' to the object wanted */
    private TreeMap<String, T> myLRUCache = new TreeMap<String, T>();
    private ArrayList<T> myLRUStack = new ArrayList<T>();
    private int myMinCacheSize = 50;
    private int myMaxCacheSize = 500;
    private int myCacheSize = myMinCacheSize;

    /** Get an item given a key.  Getting an item MOVES it up in the LRU stack,
    as it has been referenced and is now more important than older entries
     */
    public T get(String key) {
        T theThing = myLRUCache.get(key);
        raiseToTop(theThing);
        return theThing;
    }

    
    public void put(String key, T putMe){
        
        // Make room for the item if needed..
        trimCache(myCacheSize - 1);
        myLRUCache.put(key, putMe);
        myLRUStack.add(putMe); 
    }
    
    /** Kinda defeats the point, but get an object without raising it within
     * the LRU stack....normally you would just call get
     * @param key
     * @return 
     */
    public T getWithoutRaising(String key) {
        return myLRUCache.get(key);
    }

    /** Remove an item from the stack and raise it to the top.  This makes
     * it the newest item and last to be deleted
     * @param raiseMe 
     */
    private void raiseToTop(T raiseMe) {
        if (raiseMe != null) {
            myLRUStack.remove(raiseMe);
            myLRUStack.add(raiseMe);
        }
    }

    /** Clear all entries from the cache */
    public void clear() {
        myLRUCache.clear();
        myLRUStack.clear();
    }

    /** Set the minimum size of the cache.  This is the size we trim too */
    public void setMinCacheSize(int min) {
        myMinCacheSize = min;
        if (myMaxCacheSize < myMinCacheSize) {
            myMaxCacheSize = myMinCacheSize;
        }
    }

    public void setMaxCacheSize(int max) {
        myMaxCacheSize = max;
        if (myMinCacheSize > myMaxCacheSize) {
            myMinCacheSize = myMaxCacheSize;
        }
    }

    /** Set the current size of the cache */
    public void setCacheSize(int size) {
        if ((size >= myMinCacheSize) && (size <= myMaxCacheSize)) {
            myCacheSize = size;
            trimCache(myCacheSize);
        }
    }

    /** Get the current cache size */
    public int getCacheSize() {
        return myCacheSize;
    }

    /** Get the cache key for a cached item. */
    private String getCacheKey(T forMe) {
        String key;
        if (forMe instanceof LRUCacheItem) {
            key = ((LRUCacheItem) forMe).getCacheKey();
        } else {
            key = forMe.toString();
        }
        return key;
    }

    /** Trim cache down to the MIN_CACHE_SIZE */
    public void trimCache(int toSize) {

        // Don't trim less than zero
        if (toSize < 0) {
            toSize = 0;
        }
        try {
            while (true) {
                // Drop oldest from stack until we've got space...
                if (myLRUStack.size() > toSize) {
                    T oldest = myLRUStack.get(0); // Oldest
                    myLRUStack.remove(0);
                    String key = getCacheKey(oldest);
                    myLRUCache.remove(key);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception purging cache element " + e.toString());
        }
    }

    /** Trim all objects matching a LRUTrimComparator */
    public int trimCacheMatching(LRUTrimComparator<T> compare) {
        int removed = 0;
        try {
            ArrayList<T> toDelete = new ArrayList<T>();
            for (T p : myLRUStack) {
                if (compare.shouldDelete(p)) {
                    toDelete.add(p);
                    myLRUCache.remove(getCacheKey(p));
                    removed++;
                }
            }
            myLRUStack.removeAll(toDelete);
        } catch (Exception e) {
            System.out.println("Exception purging cache for index " + e.toString());
        }
        return removed;
    }
}
