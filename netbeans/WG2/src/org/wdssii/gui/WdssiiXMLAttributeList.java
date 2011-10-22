package org.wdssii.gui;

import java.util.ArrayList;

/** A generic list of attributes
 * 
 * @author Robert Toomey
 *
 */
public class WdssiiXMLAttributeList {

    private final String myName;

    public WdssiiXMLAttributeList(String name) {
        myName = name;
    }

    /** Root class of all stock attributes */
    public static class WdssiiXMLAttribute {

        public final String myName;

        public WdssiiXMLAttribute(String name) {
            myName = name;
        }

        public String getName() {
            return myName;
        }

        public boolean getBoolean() {
            return false;
        }

        public String getString() {
            return null;
        }
    }

    /** Store a boolean in an attribute */
    public static class ABoolean extends WdssiiXMLAttribute {

        public final boolean value;

        ABoolean(String name, boolean flag) {
            super(name);
            value = flag;
        }

        @Override
        public boolean getBoolean() {
            return value;
        }

        @Override
        public String getString() {
            return (value ? "1" : "0");
        }
    }

    /** Store a string in an attribute */
    public static class AString extends WdssiiXMLAttribute {

        public final String value;

        AString(String name, String theString) {
            super(name);
            value = theString;
        }

        @Override
        public boolean getBoolean() {
            return false;
        }

        @Override
        public String getString() {
            return value;
        }
    }
    /** The list of names attributes we hold onto */
    private ArrayList<WdssiiXMLAttribute> myList = new ArrayList<WdssiiXMLAttribute>();

    /** Add an attribute to this list */
    public void add(WdssiiXMLAttribute attr) {
        myList.add(attr);
    }

    /** Get an attribute from our list, if any */
    public WdssiiXMLAttribute get(String name) {
        // FIXME: maybe a map..this is O(n)
        for (WdssiiXMLAttribute a : myList) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public String getName() {
        return myName;
    }

    public ArrayList<WdssiiXMLAttribute> getList() {
        return myList;
    }
}