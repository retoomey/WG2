package org.wdssii.gui;

import java.util.ArrayList;
import java.util.HashMap;

import org.wdssii.gui.WdssiiXML.PerspectiveXMLDocument;

/** Holder class for the XML document  */
public class WdssiiXMLDocument {

    public ArrayList<PerspectiveXMLDocument> perspectives = new ArrayList<PerspectiveXMLDocument>();
    public String defaultPerspective = "org.wdssii.gui.rcp.perspectives.Basic";
    private HashMap<String, WdssiiXMLCollection> myCollections = new HashMap<String, WdssiiXMLCollection>();

    public void add(String name, WdssiiXMLCollection collection) {
        myCollections.put(name, collection);
    }

    public WdssiiXMLCollection get(String name) {
        return myCollections.get(name);
    }
}