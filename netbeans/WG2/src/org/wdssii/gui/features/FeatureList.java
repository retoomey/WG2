package org.wdssii.gui.features;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

import org.wdssii.core.W2Config;
import org.wdssii.gui.charts.DataView;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;

/**
 * FeatureList holds a list of the features of a particular window It will
 * eventually include 3d objects, products, maps, etc.
 *
 * FIXME: Starting to think composite pattern might be better for this
 *
 * @author Robert Toomey
 */
public class FeatureList {

    private final static Logger LOG = LoggerFactory.getLogger(FeatureList.class);
    private ArrayList<WeakReference<DataView>> myDataViews;
    private FeaturePosition myFeaturePosition = null;

    public static class FeaturePosition {

        public FeaturePosition(float lat, float lon, float elev) {
            latDegrees = lat;
            lonDegrees = lon;
            elevKM = elev;
        }
        public final float latDegrees;
        public final float lonDegrees;
        public final float elevKM;
    }

    /**
     * Set a shared tracking position for everything that uses this FeatureList.
     * FIXME: Might become a passed object later if needed
     */
    public void setTrackingPosition(FeaturePosition p) {
        // ?? Do we need to keep the tracking position...
        // Notify all views of tracking position...
        myFeaturePosition = p;
        cleanUpReferences();
        if (myDataViews != null) {
            for (WeakReference<DataView> a : myDataViews) {
                DataView v = a.get();
                if (v != null) {
                    v.setTrackingPosition(this, p);
                }
            }
        }
    }

    public FeaturePosition getTrackingPosition() {
        return myFeaturePosition;
    }

    /**
     * A simple filter to return boolean for mass actions such as deletion
     */
    public static interface FeatureFilter {

        boolean matches(Feature f);
    }
    /**
     * A single feature list for entire display, because I only have one window
     * at the moment.....
     */
    public static final FeatureList theFeatures = new FeatureList();
    /**
     * Every group can have a selected object
     */
    public TreeMap<String, Feature> mySelections = new TreeMap<String, Feature>();
    /**
     * The top selected Feature of all groups
     */
    private Feature myTopSelectedFeature = null;
    /**
     * We keep the 'stamp' which is hidden/made in the wdssii core
     */
    protected String mySimulationTimeStamp = "No time set";
    /**
     * The current 'time' this feature list is at, any feature can be time
     * dependent. Products are, for example
     */
    protected Date mySimulationTime = new Date();

    static {
    		
        Feature legend = LegendFeature.createLegend(FeatureList.theFeatures, 
        		"compass", "scale", "insert", "controls"); // FIXME: magic strings better way
        theFeatures.addFeature(legend);
        Feature loop = new LoopFeature(theFeatures);
        theFeatures.addFeature(loop);
    }
    /**
     * The features we contain. Not adding a public interface here for
     * synchronization purposes
     */
    private ArrayList<Feature> myFeatures = new ArrayList<Feature>();
    private final Object featureSync = new Object();

    public FeatureList() {
    }

    /**
     * Set the world wind view that we use This fails for multiview since more
     * than one view can share a feature list...
     */
    public void addDataView(DataView v) {
        if (myDataViews == null) {
            myDataViews = new ArrayList<WeakReference<DataView>>();
        }
        myDataViews.add(new WeakReference<DataView>(v));
    }

    private void cleanUpReferences() {
        if (myDataViews != null) {
            ArrayList<WeakReference<DataView>> cleanup = new ArrayList<WeakReference<DataView>>();

            for (WeakReference<DataView> a : myDataViews) {
                DataView v = a.get();
                if (v == null) {
                    cleanup.add(a);
                    LOG.debug("**************ADD TO CLEANUP " + a);
                }
            }
            myDataViews.removeAll(cleanup);
            if (myDataViews.size() == 0) {
                myDataViews = null;
                LOG.debug("**************DATAVIEW NULL");
            }
        }
    }

    /**
     * Update any graphical views that use this featurelist
     */
    public void updateOnMinTime() {
        cleanUpReferences();
        if (myDataViews != null) {
            for (WeakReference<DataView> a : myDataViews) {
                DataView v = a.get();
                if (v != null) {
                    v.updateOnMinTime();
                }
            }
        }
    }
     

    public void repaintViews() {
        cleanUpReferences();
        if (myDataViews != null) {
            for (WeakReference<DataView> a : myDataViews) {
                DataView v = a.get();
                if (v != null) {
                    v.repaint();

                }
            }
        }
    }

    public void addViewComponent(String name, Object component) {
        cleanUpReferences();
        if (myDataViews != null) {

            for (WeakReference<DataView> a : myDataViews) {
                DataView v = a.get();
                if (v != null) {
                    v.addViewComponent(name, component);
                }
            }
        }
    }

    /**
     * Update our memento to match the contents of given memento
     */
    public void updateMemento(String key, Memento m) {
        Feature f = getFeature(key);
        if (f != null) {
            f.updateMemento(m);
        }
    }

    /**
     * Add a new Feature to our list
     */
    public void addFeature(Feature f) {
        synchronized (featureSync) {
            myFeatures.add(f);
        }
        setSelected(f);  // When created, select it
    }

    /**
     * Remove a Feature from our list
     */
    public void removeFeature(String key) {
        Feature f = getFeature(key);
        if (f != null) {
            removeFeature(f);
        }
    }

    /**
     * Remove a 3DRenderer from any feature
     */
    /* public void remove3DRenderer(Feature3DRenderer r) {
     if (r != null) {
     synchronized (featureSync) {
     Iterator<Feature> iter = myFeatures.iterator();
     while (iter.hasNext()) {
     Feature f = iter.next();
     f.removeRenderer(r);
     }
     }
     }
     }
     */
    /**
     * Remove a Feature by object
     */
    public void removeFeature(Feature f) {
        if (f != null) {
            Boolean canDelete = f.getDeletable();

            if (canDelete) {
                final String group = f.getFeatureGroup();
                Feature selected = getSelected(group);
                synchronized (featureSync) {
                    myFeatures.remove(f);
                }

                if (selected == f) {
                    Feature newSelection = getFirstFeature(group);
                    // If deleted last of group, remove group...
                    if (newSelection == null) {
                        mySelections.remove(group);
                        synchronized (featureSync) {
                            if (!myFeatures.isEmpty()) {
                                newSelection = myFeatures.get(0);
                            }
                        }
                    }
                    // Select nothing or the new one of the same group...
                    // or the first of all features if group empty...
                    setSelected(newSelection);
                }
            }
        }
    }

    /**
     * Remove all features matching given filter
     */
    public void removeFeatures(FeatureFilter filter) {
        ArrayList<Feature> toDelete = new ArrayList<Feature>();
        synchronized (featureSync) {
            for (Feature f : myFeatures) {
                if (filter.matches(f)) {
                    toDelete.add(f);
                }
            }
        }
        for (Feature f : toDelete) {
            removeFeature(f);  // safest, might be slow
        }
    }

    /**
     * Get a Feature matching a given key
     */
    public Feature getFeature(String key) {
        synchronized (featureSync) {
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                if (f.getKey().equals(key)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Get the selected Feature of a given group
     */
    public Feature getSelected(String g) {
        Feature have = mySelections.get(g);
        return have;
    }

    public boolean isSelected(Feature f){
    	boolean selected = false;
    	if (f == getSelected(f.getFeatureGroup())){
    		selected = true;
    	}
    	return selected;
    }
    
    /**
     * Set the selected Feature for the group that it is in. For example, you
     * can set the selected 'map' or '3d object' separately.
     *
     * @param f the Feature to select in its group
     */
    public void setSelected(Feature f) {
        myTopSelectedFeature = f;

        if (f != null) {
            mySelections.put(f.getFeatureGroup(), f);
            synchronized (featureSync) {
                myFeatures.remove(f);
                myFeatures.add(f);
            }
            f.wasSelected();
        }
    }

    public void setDrawLast(Feature f) {
        if (f != null) {
            // Make sure this selected object draws last and over all others
            synchronized (featureSync) {
                myFeatures.remove(f);
                myFeatures.add(f);
            }
        }
    }

    /**
     * Get the most recently selected Feature of ALL groups. The GUI has a uses
     * this for table selection where the selected Feature has a GUI available
     * for setting properties.
     *
     * @return
     */
    public Feature getTopSelected() {
        return myTopSelectedFeature;
    }

    /**
     * Selected this key for group
     */
    public void setSelected(String key) {
        synchronized (featureSync) {
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                if (f.getKey().equals(key)) {
                    setSelected(f);
                    break;
                }
            }
        }
    }

    /**
     * Used by GUI to pull out state information for table....
     * LLHAreaCreateCommand
     *
     * @return
     */
    public List<Feature> getFeatures() {
        return Collections.unmodifiableList(myFeatures);
    }

    public Feature getFirstFeature(Class<?> c) {
        Feature first = null;
        synchronized (featureSync) {
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                if (f.getClass() == c) {
                    first = f;
                }
            }
        }
        return first;

    }

    public Feature getFirstFeature(String g) {
        Feature first = null;
        synchronized (featureSync) {
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                if (f.getFeatureGroup().equals(g)) {
                    first = f;
                }
            }
        }
        return first;
    }

    /**
     * Get all visible/onlymode features in a group. All that should be shown.
     *
     */
    public List<Feature> getActiveFeatureGroup(String g) {
        ArrayList<Feature> holder = new ArrayList<Feature>();

        Feature selected = getSelected(g);

        // In only mode the selected volume is the only possible visible one
        // for its group
        if ((selected != null) && (selected.getOnlyMode())) {
            if (selected.getVisible()) {
                holder.add(selected);
            }
        } else {
            synchronized (featureSync) {
                Iterator<Feature> i = myFeatures.iterator();
                while (i.hasNext()) {
                    Feature f = i.next();
                    if (f.getVisible() && (f.getFeatureGroup().equals(g))) {
                        holder.add(f);
                    }
                }
            }
        }

        return holder;
    }
    
    /**
     * Get all features in a group by name.
     *
     */
    public List<Feature> getFeatureGroup(String g) {
    	ArrayList<Feature> holder = new ArrayList<Feature>();

    	synchronized (featureSync) {
    		Iterator<Feature> i = myFeatures.iterator();
    		while (i.hasNext()) {
    			Feature f = i.next();
    			if (f.getFeatureGroup().equals(g)) {
    				holder.add(f);
    			}
    		}
    	}
    	return holder;
    }


    public <T> ArrayList<T> getFeatureGroup(Class<?> c) {
        ArrayList<T> holder = new ArrayList<T>();
        synchronized (featureSync) {
            Iterator<Feature> iter = myFeatures.iterator();
            while (iter.hasNext()) {
                Feature f = iter.next();
                if (f.getClass() == c) {  // Humm subclass won't work, right?
                    try {
                        @SuppressWarnings("unchecked")
                        T tryIt = (T) f;
                        holder.add(tryIt);
                    } catch (Exception e) {
                        // Allow it....
                    }
                }
            }
        }
        return holder;
    }

    /**
     * Get 'top' match of a group (selected items move to top, or end of the
     * list, since last rendered is on 'top' of other items
     */
    public <T> T getTopMatch(FeatureFilter matcher) {
        T holder = null;
        synchronized (featureSync) {
            ListIterator<Feature> itr = myFeatures.listIterator(myFeatures.size());
            while (itr.hasPrevious()) {
                Feature f = itr.previous();
                if (matcher.matches(f)) {
                    try {
                        @SuppressWarnings("unchecked")
                        T tryCast = (T) f;
                        holder = tryCast;
                    } catch (Exception e) {
                        // Allow it...just return null
                    }
                    break;
                }
            }
        }

        return holder;
    }

    /**
     * Get the simulation time of this FeatureList
     */
    public Date getSimulationTime() {
        return mySimulationTime;
    }

    public void setSimulationTime(Date d) {
        mySimulationTime = d;
    }

    /**
     * Get the simulation timestamp string of this FeatureList
     */
    public String getSimulationTimeStamp() {
        return mySimulationTimeStamp;
    }

    public void setSimulationTimeStamp(String s) {
        mySimulationTimeStamp = s;
    }

    /**
     * Get the string displayed by the GUI on our status
     */
    public String getGUIInfoString() {
        return mySimulationTimeStamp;
    }

    /**
     * Send a string message to all of our features
     */
    public void sendMessage(String message) {
        synchronized (featureSync) {
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                f.sendMessage(message);
            }
        }
    }
}
