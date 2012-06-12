package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.gis.MapFeature;

/**
 * FeatureList holds a list of the features of a particular window It will
 * eventually include 3d objects, products, maps, etc.
 *
 * @author Robert Toomey
 */
public class FeatureList {

	private static Logger log = LoggerFactory.getLogger(FeatureList.class);

	/**
	 * A simple filter to return boolean for mass actions such as deletion
	 */
	public static interface FeatureFilter {

		boolean matches(Feature f);
	}
	/**
	 * A single feature list for entire display, because I only have one
	 * window at the moment.....
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
	protected Date mySimulationTime;

	static {
		Feature testOne = new MapFeature(theFeatures, "shapefiles/usa/ok/okcnty.shp");
		theFeatures.addFeature(testOne);
		Feature testTwo = new MapFeature(theFeatures, "shapefiles/usa/tx/txcnty.shp");
		theFeatures.addFeature(testTwo);
		Feature legend = new LegendFeature(theFeatures);
		theFeatures.addFeature(legend);
	}
	/**
	 * The features we contain. Not adding a public interface here for
	 * synchronization purposes
	 */
	private ArrayList<Feature> myFeatures = new ArrayList<Feature>();

	public FeatureList() {
	}

	public void setMemento(String key, FeatureMemento m) {
		Feature f = getFeature(key);
		if (f != null) {
			f.setMemento(m);
		}
	}

	/**
	 * Add a new Feature to our list
	 */
	public void addFeature(Feature f) {
		myFeatures.add(f);
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
	public void remove3DRenderer(Feature3DRenderer r) {
		if (r != null) {
			Iterator<Feature> iter = myFeatures.iterator();
			while (iter.hasNext()) {
				Feature f = iter.next();
				f.removeRenderer(r);
			}
		}
	}

	/**
	 * Remove a Feature by object
	 */
	public void removeFeature(Feature f) {
		if (f != null) {
			Boolean canDelete = f.getDeletable();

			if (canDelete) {
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
			} else {
				log.error("Tried to delete a feature that is not deletable");
			}
		}
	}

	/**
	 * Remove all features matching given filter
	 */
	public void removeFeatures(FeatureFilter filter) {
		ArrayList<Feature> toDelete = new ArrayList<Feature>();
		for (Feature f : myFeatures) {
			if (filter.matches(f)) {
				toDelete.add(f);
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
		Iterator<Feature> i = myFeatures.iterator();
		while (i.hasNext()) {
			Feature f = i.next();
			if (f.getKey().equals(key)) {
				return f;
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

	/**
	 * Set the selected Feature for the group that it is in. For example,
	 * you can set the selected 'map' or '3d object' separately.
	 *
	 * @param f the Feature to select in its group
	 */
	public void setSelected(Feature f) {
		myTopSelectedFeature = f;

		if (f != null) {
			mySelections.put(f.getFeatureGroup(), f);
			myFeatures.remove(f);
			myFeatures.add(f);
			f.wasSelected();
		}
	}

	public void setDrawLast(Feature f) {
		if (f != null) {
			// Make sure this selected object draws last and over all others
			myFeatures.remove(f);
			myFeatures.add(f);
		}
	}

	/**
	 * Get the most recently selected Feature of ALL groups. The GUI has a
	 * uses this for table selection where the selected Feature has a GUI
	 * available for setting properties.
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
	 * Get all visible/onlymode features in a group. All that should be
	 * shown.
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

	public <T> ArrayList<T> getFeatureGroup(Class c) {
		ArrayList<T> holder = new ArrayList<T>();
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
		return holder;
	}

	/**
	 * Get 'top' match of a group (selected items move to top, or end of the
	 * list, since last rendered is on 'top' of other items
	 */
	public <T> T getTopMatch(FeatureFilter matcher) {
		T holder = null;
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
}
