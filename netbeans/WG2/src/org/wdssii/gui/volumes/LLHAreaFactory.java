package org.wdssii.gui.volumes;

import org.wdssii.gui.features.Feature.FeatureTableInfo;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;

/** The root class for creating a LLHArea.  This creates the drawing part and the editor part for a slice, box, stick, etc. 
 * Slices are used for VSlices in the display, Boxes for isosurfaces, Sticks for time height trends...and future stuff.
 * @author Robert Toomey
 * */
public abstract class LLHAreaFactory {

    /** Information record about this class Unused at moment since name==displayed name */
    public static class LLHAreaFactoryInfo {

        public String myGUIName;
        public String myKeyName;
        public String myToolTip;
    };

    /** The internal name of this object.  Displayed in a GUI list */
    private String myName = null;

    /** Get the displayed type of object this creates (This is for GUI listing of factories) */
    public abstract String getFactoryNameDisplay();

    /** Create an airspace and airspace editor in the given world */
   // public abstract boolean create(WorldWindow wwd, LLHAreaFeature f, FeatureTableInfo info, Object params);
    public abstract boolean create(LLHAreaFeature f, FeatureTableInfo info, Object params);
   
    /** Create a GUI for an LLHArea */
    public FeatureGUI createGUI(LLHAreaFeature f, LLHArea a){
        return null;
    }
    
    /** Get the displayed name of this object */
    //public String getName(){ return myName; }
    /** Set the displayed name of this object */
    public void setName(String name) {
        setMyName(name);
    }

    public void setMyName(String myName) {
        this.myName = myName;
    }

    public String getMyName() {
        return myName;
    }
};
