package org.wdssii.gui.animators;

import java.util.ArrayList;

import org.wdssii.gui.GUISetting;

/**
 * Animates the display somehow dealing with wdssi products/gui
 * FIXME: shares a lot with filters.  A 'list' of items
 * that have GUI settings.  Would be nice if we created a PreferencePage wrapper
 * around this class as well...
 * 
 * @author Robert Toomey
 * 
 */
public abstract class Animator {

    /** Default enabled setting.  This is the global toggle in the GUI for this filter */
    private boolean myEnabled = false;
    private long myLastAnimateTime = 0;
    private long myDwell = 1000;
    /** Displayed name of this filter.  This is the text shown in the GUI */
    private String myDisplayedName = "Unnamed";
    public ArrayList<GUISetting> myGUISettings = new ArrayList<GUISetting>();

    /** Return the display name of this filter, for GUI */
    public abstract String getKey();

    /** Create the GUI box for changing our filter settings.  This can be called
     * whenever the filter is chosen, so settings should be kept inside the filter object
     * (In other words, the box from last time is disposed)
     */
    public abstract ArrayList<GUISetting> getGUISettings();

    /** Create the gui for this filter */
    public abstract Object getNewGUIBox(Object parent);

    /** Return if the features of this animator are enabled */
    public boolean isEnabled() {
        return myEnabled;
    }

    /** Check box toggle from the GUI */
    public void setGUIEnabled(boolean flag) {
        setEnabled(flag);
        //CommandManager.getInstance().executeCommand(new ProductChangeCommand.ProductFilterCommand(), true);
    }

    /** Make this enabled */
    public void setEnabled(boolean flag) {
        myEnabled = flag;
    }

    public String getDisplayedName() {
        return myDisplayedName;
    }

    public void setDisplayedName(String name) {
        myDisplayedName = name;
    }

    public int getMinDwellMS() {
        return 1000;
    }

    /** Called from job thread, this asks us to do our stuff...
     * This isn't a perfect timing loop, probably needs more work. */
    public boolean needToAnimate(int dwellIncrease) {
        boolean needAnimate = false;
        long current = System.currentTimeMillis(); // let's try this way...

        long wait = current - myLastAnimateTime; // Note on first run, wait is pretty big
        if (wait > myDwell) {
            needAnimate = true;
            myLastAnimateTime = current;  // FIXME: what about time waited already?
        }
        return needAnimate;
    }

    public int animate() {
        return 1000;  // Return the new min dwell...
    }

    public void setDwellTime(long ms) {
        myDwell = ms;
    }
}