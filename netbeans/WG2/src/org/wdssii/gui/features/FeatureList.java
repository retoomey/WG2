package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FeatureList holds a list of the features of a particular window It will
 * eventually include 3d objects, products, maps, etc.
 *
 * @author Robert Toomey
 */
public class FeatureList {

    private static Logger log = LoggerFactory.getLogger(FeatureList.class);
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

    static {
        Feature testOne = new MapFeature("shapefiles/usa/ok/okcnty.shp");
        theFeatures.addFeature(testOne);
        Feature testTwo = new MapFeature("shapefiles/usa/tx/txcnty.shp");
        theFeatures.addFeature(testTwo);
    }
    /**
     * The features we contain. Not adding a public interface here for
     * synchronization purposes
     */
    private ArrayList<Feature> myFeatures = new ArrayList<Feature>();

    public FeatureList() {
    }

    public void addFeature(Feature f) {
        myFeatures.add(f);
        setSelected(f);  // When created, select it
    }

    public void removeFeature(String key) {
        Feature f = getFeature(key);
        if (f != null) {
            removeFeature(f);
        }
    }

    public Feature getFeature(String key) {
        Iterator<Feature> i = myFeatures.iterator();
        while (i.hasNext()) {
            Feature f = i.next();
            if (f.getKey().equals(key)) {
                return f;
            }
        }
        return null;
    }

    public void setMemento(String key, FeatureMemento m) {
        Feature f = getFeature(key);
        if (f != null) {
            f.setMemento(m);
        }
    }

    public void removeFeature(Feature f) {
        if (f != null) {

            final String group = f.getFeatureGroup();
            Feature selected = getSelected(group);
            myFeatures.remove(f);

            if (selected == f) {
                Feature newSelection = getFirstFeature(group);
                if (newSelection == null) {
                    mySelections.remove(group);
                } else {
                    setSelected(newSelection);
                }
            }
        }
    }

    public Feature getSelected(String g) {
        Feature have = mySelections.get(g);
        return have;
    }

    /**
     * Set the selected Feature for the group that it is in. For example, you
     * can set the selected 'map' or '3d object' separately.
     *
     * @param f the Feature to select in its group
     */
    public void setSelected(Feature f) {
        if (f != null) {
            mySelections.put(f.getFeatureGroup(), f);
        }
        myTopSelectedFeature = f;
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
     * Selected this key for group FIXME: This is assuming no two Features can
     * have same key
     */
    public void setSelected(String key) {
        Iterator<Feature> i = myFeatures.iterator();
        while (i.hasNext()) {
            Feature f = i.next();
            if (f.getKey().equals(key)) {
                setSelected(f);
                break;
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

    public Feature getFirstFeature(Class c) {
        Feature first = null;
        Iterator<Feature> i = myFeatures.iterator();
        while (i.hasNext()) {
            Feature f = i.next();
            if (f.getClass() == c) {
                first = f;
            }
        }
        return first;
    }

    public Feature getFirstFeature(String g) {
        Feature first = null;
        Iterator<Feature> i = myFeatures.iterator();
        while (i.hasNext()) {
            Feature f = i.next();
            if (f.getFeatureGroup().equals(g)) {
                first = f;
            }
        }
        return first;
    }

    /**
     * Render all features that are in the given group
     */
    public void renderFeatureGroup(DrawContext dc, String g) {

        List<Feature> list = getActiveFeatureGroup(g);
        Iterator<Feature> iter = list.iterator();
        while (iter.hasNext()) {
            Feature f = iter.next();
            f.render(dc);
        }
    }

    /**
     * Get all visible/onlymode features in a group. All that should be shown.
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
            Iterator<Feature> i = myFeatures.iterator();
            while (i.hasNext()) {
                Feature f = i.next();
                if (f.getVisible() && (f.getFeatureGroup().equals(g))) {
                    holder.add(f);
                }
            }
        }

        return holder;
    }
}
