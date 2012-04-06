package org.wdssii.gui.sources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A list of various sources.  Could be WDSSII Index, CFRadial file, etc....
 *  Shares code with FilterList, might make a superclass/interface...
 * 
 *  @author Robert Toomey
 */
public class SourceList {
    
    private static Logger log = LoggerFactory.getLogger(SourceList.class);

    /** A simple filter to return boolean for mass actions such as deletion */
    public static interface SourceFilter{
        boolean matches(Source f);
    }
    
     /**
     * A single Source list for entire display...
     */
    public static final SourceList theSources = new SourceList();
     
    /**
     * The sources we contain. Not adding a public interface here for
     * synchronization purposes
     */
    private ArrayList<Source> mySources = new ArrayList<Source>();

       /**
     * The top selected Source of all groups
     */
    private Source myTopSelectedSource = null;
    
    /**
     * Add a new Feature to our list
     */
    public void addSource(Source f) {
        mySources.add(f);
        setSelected(f);  // When created, select it
    }
    
        /**
     * Get the most recently selected Feature of ALL groups. The GUI has a uses
     * this for table selection where the selected Feature has a GUI available
     * for setting properties.
     *
     * @return
     */
    public Source getTopSelected() {
        return myTopSelectedSource;
    }

    /**
     * Selected this key for group
     */
    public void setSelected(String key) {
        Iterator<Source> i = mySources.iterator();
        while (i.hasNext()) {
            Source f = i.next();
            if (f.getKey().equals(key)) {
                setSelected(f);
                break;
            }
        }
    }
    
        /**
     * Set the selected Feature for the group that it is in. For example, you
     * can set the selected 'map' or '3d object' separately.
     *
     * @param f the Feature to select in its group
     */
    public void setSelected(Source f) {
        myTopSelectedSource = f;   
    }
    
    /**
     * Remove a Feature from our list
     */
    public void removeSource(String key) {
        Source f = getSource(key);
        if (f != null) {
            removeSource(f);
        }
    }

    /**
     * Remove a Feature by object
     */
    public void removeSource(Source f) {
        if (f != null) {

           // final String group = f.getFeatureGroup();
            Source selected = getTopSelected();
            mySources.remove(f);

            if (selected == f) {
                Source newSelection = getFirstSource("");
                if (newSelection == null) {
                } else {
                    setSelected(newSelection);
                }
            }
        }
    }
    
    /** Remove all features matching given filter */
    public void removeSources(SourceFilter filter){
        ArrayList<Source> toDelete = new ArrayList<Source>();
        for(Source f:mySources){
            if (filter.matches(f)){
                toDelete.add(f);
            }
        }
        for(Source f:toDelete){
            removeSource(f);  // safest, might be slow
        }
    }
    
    /**
     * Get a Feature matching a given key
     */
    public Source getSource(String key) {
        Iterator<Source> i = mySources.iterator();
        while (i.hasNext()) {
            Source f = i.next();
            if (f.getKey().equals(key)) {
                return f;
            }
        }
        return null;
    }
    
     /**
     * Used by GUI to pull out state information for table....
     *
     * @return
     */
    public List<Source> getSources() {
        return Collections.unmodifiableList(mySources);
    }
   
    public Source getFirstSource(String g) {
        Source first = null;
        Iterator<Source> i = mySources.iterator();
        while (i.hasNext()) {
            Source f = i.next();
            //if (f.getFeatureGroup().equals(g)) {
                first = f;
                break;
            //}
        }
        return first;
    }
}
