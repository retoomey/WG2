package org.wdssii.gui.sources;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.xml.config.Sources;

/**
 * A list of various sources. Could be WDSSII Index, CFRadial file, etc....
 * Shares code with FilterList, might make a superclass/interface...
 *
 * @author Robert Toomey
 */
public class SourceList {

    private final static Logger LOG = LoggerFactory.getLogger(SourceList.class);

    /**
     * A simple filter to return boolean for mass actions such as deletion
     */
    public static interface SourceFilter {

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
                // if (newSelection == null) {
                // } else {
                setSelected(newSelection);
                // }
            }
        }
    }

    /**
     * Remove all features matching given filter
     */
    public void removeSources(SourceFilter filter) {
        ArrayList<Source> toDelete = new ArrayList<Source>();
        for (Source f : mySources) {
            if (filter.matches(f)) {
                toDelete.add(f);
            }
        }
        for (Source f : toDelete) {
            removeSource(f);  // safest, might be slow
        }
    }

    /**
     * Get a Source matching a given key
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
     * Get a Source matching a given URL
     */
    public Source getSourceURL(URL aURL) {
        Iterator<Source> i = mySources.iterator();
        while (i.hasNext()) {
            Source f = i.next();
            if (f.getURL().equals(aURL)) {
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

    /**
     * Used by delete routine
     */
    public List<Source> getSourcesCopy() {
        return new ArrayList<Source>(mySources);
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

    /**
     * We generate a Tag for this source list
     */
    /*public Tag getTag() {
        Tag_sources sources = new Tag_sources();
        for (Source s : mySources) {
            Tag_source t = new Tag_source();
            t.name = s.getVisibleName();
            t.url = s.getURLString();
            sources.sources.add(t);
        }
        return sources;
    }*/

    public org.wdssii.xml.config.Sources getSourceXML() {
        Sources sxml = new Sources();
        for (Source s : mySources) {                 
            sxml.addSource(s.getVisibleName(), s.getURLString(), s.getHistory());
        }
        return sxml;
    }

    /**
     * Get the visible name of this source
     */
    public String getVisibleName(String key) {
        Source s = getSource(key);
        if (s != null) {
            return s.getVisibleName();
        }
        return "";
    }

    /**
     * About to try to connect to a source
     */
    public boolean aboutToConnect(String key, boolean start) {
        Source s = getSource(key);
        if (s != null) {
            return s.aboutToConnect(start);
        }
        return false;

    }

    /**
     * Connect to a source
     */
    public boolean connectSource(String key) {
        Source s = getSource(key);
        if (s != null) {
            return s.connect();
        }
        return false;
    }

    /**
     * Disconnect from a source
     */
    public void disconnectSource(String key) {
        Source s = getSource(key);
        if (s != null) {
            s.disconnect();
        }
    }
}
