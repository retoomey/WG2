package org.wdssii.gui.products.filters;

import java.util.ArrayList;

import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GUISetting;
import org.wdssii.gui.commands.ProductChangeCommand;
import org.wdssii.gui.products.volumes.ProductVolume;

/**
 * Filter a value, return something based on the value
 * A ProductHandler has a filter list wrapped around its current product that
 * filters the data value by some rules.  We'll be able to dynamically create/add
 * to the stock set of filters with the GUI

 * @author Robert Toomey
 * 
 */
public abstract class DataFilter {

    /** The info passed through filters.  Subclasses can override to add more features, such as
     * radial, range.
     * @author Robert Toomey
     *
     */
    public static class DataValueRecord {

        public double orgValue;
        public double outValue;
        public double lat;
        public double lon;
        public double height;
        public double hWeight;
    }
    /** Default enabled setting.  This is the global toggle in the GUI for this filter */
    private boolean myEnabled = false;
    /** Displayed name of this filter.  This is the text shown in the GUI */
    private String myDisplayedName = "Unnamed";
    public ArrayList<GUISetting> myGUISettings = new ArrayList<GUISetting>();

    /** Prep filter for a volume... */
    public void prepFilterForVolume(ProductVolume v) {
    }

    ;
	
	/** Return filtered thing for a value */
	public abstract void f(DataTypeQuery q);

    /** Return the display name of this filter, for GUI */
    public abstract String getKey();

    /** Create the GUI box for changing our filter settings.  This can be called
     * whenever the filter is chosen, so settings should be kept inside the filter object
     * (In other words, the box from last time is disposed)
     */
    public abstract ArrayList<GUISetting> getGUISettings();

    /** Create the gui for this filter */
    public abstract Object getNewGUIBox(Object parent);

    public boolean isEnabled() {
        return myEnabled;
    }

    /** Check box toggle from the GUI */
    public void setGUIEnabled(boolean flag) {
        setEnabled(flag);
        fireFilterChangedEvent();
    }

    /** Set if this filter is currently enabled */
    public void setEnabled(boolean flag) {
        myEnabled = flag;
    }

    /** Get the name of filter displayed in the GUI */
    public String getDisplayedName() {
        return myDisplayedName;
    }

    /** Set the name of the filter displayed in the GUI */
    public void setDisplayedName(String name) {
        myDisplayedName = name;
    }

    /** Simple wdssii command firing for filter change event */
    public void fireFilterChangedEvent() {
      //  CommandManager.getInstance().executeCommand(new ProductChangeCommand.ProductFilterCommand(), true);
    }
}
