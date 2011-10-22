package org.wdssii.gui;

import java.util.ArrayList;
import java.util.HashMap;

/** Holder of a named attribute collection 
 * There's probably some standard library thing that will do all this, but it's quick to code
 * and gives me some experience with STAX so I'll make it for now until I find a good replacement, if any.
 * This is something like this:
 * <charts default="chart2">  (the collection)
 *   <chart name = "chart1">  (Attribute list with name 'chart1')
 *     <attr....> <attr...>
 *   <chart name = "chart2">  (Attribute list with name 'chart2')
 *     <attr...> <attr...>
 * </charts>
 * 
 * @author Robert Toomey
 * */
public class WdssiiXMLCollection {

    /** The name of this collection, such as 'charts' */
    private final String myName;
    /** The name of the XMLAttributeList that is the 'default' */
    private final String myDefaultItem;
    /** A mapping from names to each XMLAttributeList in our collection */
    private HashMap<String, WdssiiXMLAttributeList> myCollection = new HashMap<String, WdssiiXMLAttributeList>();

    /** Create a new collection with name */
    public WdssiiXMLCollection(String name, String defaultItem) {
        myName = name;
        myDefaultItem = defaultItem;
    }

    /** Get the name of this collection */
    public String getName() {
        return myName;
    }

    /** Get the default item name of this collection, can be null */
    public String getDefaultListName() {
        return myDefaultItem;
    }

    /** Add list with given name to collection */
    public void add(String name, WdssiiXMLAttributeList list) {
        System.out.println("collection object " + myCollection);
        myCollection.put(name, list);
    }

    /** Get the default item, if any */
    public WdssiiXMLAttributeList getDefaultList() {
        if (myDefaultItem != null) {
            return myCollection.get(myDefaultItem);
        }
        return null;
    }

    /** Get the list with given name, if any */
    public WdssiiXMLAttributeList get(String name) {
        if (name != null) {
            return myCollection.get(name);
        }
        return null;
    }

    /** Get the full hash map */
    public HashMap<String, WdssiiXMLAttributeList> getCollection() {
        return myCollection;
    }

    /** Get list of attribute lists by name */
    public ArrayList<String> getNames() {
        ArrayList<String> names = new ArrayList<String>(myCollection.keySet());
        return names;
    }
}
