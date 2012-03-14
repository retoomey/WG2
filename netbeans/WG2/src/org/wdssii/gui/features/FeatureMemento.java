package org.wdssii.gui.features;

import java.awt.Color;
import java.util.TreeMap;

/**
 * Used by commands/GUI to send changes to a Feature
 * 
 * @author Robert Toomey
 */
public class FeatureMemento {

    private boolean visible;
    private boolean useVisible = false;
    private boolean only;
    private boolean useOnly = false;

    private TreeMap<String, Object> myProperties = new TreeMap<String, Object>();
    private final String V = "visible";
    
    /** Create a full copy of another mememto */
    public FeatureMemento(FeatureMemento m){
        visible = m.visible;
        only = m.only;
    }
    
    /** Sync to another memento by only copying what is wanted
     * to be changed.
     * @param m 
     */
    public void syncToMemento(FeatureMemento m){
        if (m.useVisible){
            visible = m.visible;
        }
        if (m.useOnly){
            only = m.only;
        }
    }
    
    public FeatureMemento(boolean v, boolean o) {
        visible = v;
        only = o;
    }

    public boolean getVisible() {
      // return visible;
        setProperty(V, visible);
       // Boolean test = getProperty(V);
        //Integer shouldBeError = getProperty(V);
        return visible;
    }

    public void setVisible(boolean f) {
        visible = f;
        useVisible = true;
    }

    public boolean getOnly() {
        return only;
    }

    public void setOnly(boolean f) {
        only = f;
        useOnly = true;
    }
    
    public void setProperty(String key, Object stuff){
        myProperties.put(key, stuff);
        // creating NEW is the issue...
        // how to deep copy for example generically?

    }
    
   /* public <T> T getProperty(String key){
       // T newOne = (T) c.newInstance();
        try{
            return (T) myProperties.get(key);
        }catch(ClassCastException e){
            return null;
        }
    }*/
}