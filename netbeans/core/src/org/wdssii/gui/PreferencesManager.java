package org.wdssii.gui;

/** Preference manager handles wdssii preferences.  It has a PreferenceHandler that is 
 * the platform specific preference manager (eclipse/netbeans/xml, etc) that must be
 * introduced to it.
 * 
 * @author Robert Toomey
 *
 */
public class PreferencesManager implements Singleton {

    private PreferenceHandler myPrefs = null;

    /** Interface for a raw preference reader/writer */
    public static interface PreferenceHandler {

        public void setDefault(String name, boolean value);

       // public void setDefault(String name, String value);

        public void setDefault(String name, int value);

        public boolean getBoolean(String key);

        public void setBoolean(String key, boolean value);
        
        public int getInteger(String key);

        public void setInteger(String key, int i);

        public void setValue(String key, boolean value);

        public void setValue(String key, int value);
    }

    /** The constants for our display.  
     * FIXME: There might be a way with reflection to let all the strings be in the actual
     * classes.  This would keep this code from being modified everytime a new constant is needed.
     * However, we still need the ability to list all the constants in program and make sure that
     * none of them conflict.
     * Option 1: a pref class for each constant, dynamically read it in
     * Option 2: xml file string -> type, default...
     * 
     * Don't like this PrefConstants class because it's not CLOSED. (Every time you add a constant
     * you have to modify this code).  FIXME: Make a closed preference system that prevents
     * coding error of repeating a string name and does defaults.
     * 
     * FIXME: probably should have an xml file listing prefs and defaults...
     * 
     * @author Robert Toomey
     *
     */
    public class PrefConstants {

        public static final String PREF_BASE = "org.wdssii.gui.";
        public static final String PREF_showAddCommandDialog = PREF_BASE + "showAddCommandDialog";
        public static final String PREF_showDeleteCommandDialog = PREF_BASE + "deleteCommandDialog";
        public static final String PREF_showDeleteALLCommandDialog = PREF_BASE + "deleteAllCommandDialog";
        public static final String PREF_cacheSize = PREF_BASE + "productCacheSize";
        //public void gatherConstants(){
        // for all classes in org.wdssii....find fields starting with "PREF_BASE" and add them
        // to constants?
    }
    private static PreferencesManager instance = null;

    public static PreferencesManager getInstance() {
        if (instance == null) {
            instance = new PreferencesManager();
            SingletonManager.registerSingleton(instance);
        }
        return instance;
    }

    private void setupPrefs(PreferenceHandler p) {

        myPrefs = p;
        // These are the default values for all of our preferences.
        // Yuck. Add your preference default here.  So easy to forget too.
        myPrefs.setDefault(PrefConstants.PREF_showAddCommandDialog, true);
        myPrefs.setDefault(PrefConstants.PREF_showDeleteCommandDialog, true);
        myPrefs.setDefault(PrefConstants.PREF_showDeleteALLCommandDialog, true);
        myPrefs.setDefault(PrefConstants.PREF_cacheSize, 100);
    }

    @Override
    public void singletonManagerCallback() {
    }

    public static void introduce(PreferenceHandler p) {

        if (instance == null) {
            instance = new PreferencesManager();
            instance.setupPrefs(p);
            SingletonManager.registerSingleton(instance);
        } else {
            instance.setupPrefs(p);
        }
    }

    public boolean getBoolean(String key) {
        return (myPrefs.getBoolean(key));
    }

    public int getInteger(String key) {
        return (myPrefs.getInteger(key));
    }

    public void setValue(String key, boolean value) {
        myPrefs.setValue(key, value);
    }

    public void setValue(String key, int value) {
        myPrefs.setValue(key, value);
    }
}
