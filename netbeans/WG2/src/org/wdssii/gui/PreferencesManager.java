package org.wdssii.gui;

import java.io.File;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.util.StringUtil;
import org.wdssii.xml.Util;
import org.wdssii.xml.config.Source;
import org.wdssii.xml.config.W2Pref;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Preference manager handles wdssii preferences. It has a PreferenceHandler
 * that is the platform specific preference manager (eclipse/netbeans/xml, etc)
 * that must be introduced to it.
 *
 * @author Robert Toomey
 *
 */
public class PreferencesManager implements Singleton {

    private static Logger log = LoggerFactory.getLogger(PreferencesManager.class);
    /**
     * Direct xml JAXB tag for storing a old style w2pref file
     */
    private W2Pref theW2Prefs = null;
    private PreferenceHandler myPrefs = null;

    /**
     * Interface for a raw preference reader/writer
     */
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

    /**
     * The constants for our display. FIXME: There might be a way with
     * reflection to let all the strings be in the actual classes. This would
     * keep this code from being modified everytime a new constant is needed.
     * However, we still need the ability to list all the constants in program
     * and make sure that none of them conflict. Option 1: a pref class for each
     * constant, dynamically read it in Option 2: xml file string -> type,
     * default...
     *
     * Don't like this PrefConstants class because it's not CLOSED. (Every time
     * you add a constant you have to modify this code). FIXME: Make a closed
     * preference system that prevents coding error of repeating a string name
     * and does defaults.
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
        loadConfig();
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

    /**
     * Not sure where to put this. The global w2config.xml file
     */
    public void loadConfig() {

        String name = "wgpref.xml";

        try {
            W2Pref prefs = Util.load(name, W2Pref.class);
            theW2Prefs = prefs;
            if (prefs != null) {
                log.info("Preferences loaded from " + name);
            } else {
                log.info("Couldn't find preference file " + name);
            }
        } catch (Exception c) {
            log.error("Error loading preferences at " + name);
        }

        // Ok, try to load each initial source....
        // not sure where this belongs quite yet...
        // We really should make a dialog of something here I think....
        if ((theW2Prefs != null) && (theW2Prefs.sources != null)) {

            for (Source s : theW2Prefs.sources.list) {
                try {
                    CommandManager c = CommandManager.getInstance();
                    boolean connect = true;
                    log.info("Adding start source " + s.sourcename + " " + s.url);
                    // Old school...everything is an index in old w2config files.
                    // Eventually we need different source types, such as the nasa wms source, which is not an IndexSource
                    // This at least will read old files...
                    s.url = StringUtil.convertToLabDomain(s.url);
                    SourceAddCommand.IndexSourceAddParams p = new SourceAddCommand.IndexSourceAddParams(s.sourcename, s.url, false, connect, s.history);
                    c.executeCommand(new SourceAddCommand(p), false);
                } catch (Exception e) {
                    // Recover
                }
            }

        }
    }

    public void saveConfig(URL aURL) {
        if ((theW2Prefs != null)) {
            theW2Prefs.sources = SourceList.theSources.getSourceXML();
            String file = aURL.getFile();
            Util.save(theW2Prefs, file, theW2Prefs.getClass());
            /*SimpleSymbol test = new SimpleSymbol();      // is JAXB smart enough?  
            Util.save(test, file, test.getClass());

            try {
                File f = new File(file);
                URL u = f.toURI().toURL();
                Symbol readback = Util.loadURL(u, Symbol.class);
                log.debug("Class back is "+readback.getClass().getSimpleName());
            } catch (Exception c) {
                log.error("Error load back"+file+", "+c.toString());
            }*/
        }
    }
}
