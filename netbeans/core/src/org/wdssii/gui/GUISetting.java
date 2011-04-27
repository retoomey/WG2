package org.wdssii.gui;

/** Not sure what to call this yet.  It's an object that defines
 * a user setting.  Currently used by the DataFilter objects to define
 * GUI settings in the FilterView
 * This will have swt methods...should it be in the gui.rcp class...
 * @author Robert Toomey
 *
 */
public class GUISetting {

    public String myName = "None";

    public String getAttributeName() {
        return myName;
    }

    /** This defines a checkbox toggle */
    public static class ToggleBoolean extends GUISetting {

        public boolean myValue = false;
    }
}
