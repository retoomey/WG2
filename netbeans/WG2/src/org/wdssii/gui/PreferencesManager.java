package org.wdssii.gui;

import java.net.URL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.core.Singleton;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.core.StringUtil;
import org.wdssii.gui.commands.FeatureDeleteCommand.FeatureDeleteAllCommand;
import org.wdssii.gui.commands.OpenCommand;
import org.wdssii.gui.commands.SourceDeleteCommand;
import org.wdssii.gui.commands.SourceDeleteCommand.SourceDeleteAllCommand;
import org.wdssii.gui.views.ViewManager;
import org.wdssii.xml.Util;
import org.wdssii.xml.config.Source;
import org.wdssii.xml.config.W2Pref;
import org.wdssii.xml.wdssiiConfig.Tag_setup;

/**
 * Preference manager handles wdssii preferences. It has a PreferenceHandler
 * that is the platform specific preference manager (eclipse/netbeans/xml, etc)
 * that must be introduced to it.
 *
 * @author Robert Toomey
 *
 */
public class PreferencesManager implements Singleton {

    private final static Logger LOG = LoggerFactory.getLogger(PreferencesManager.class);
    /**
     * Direct xml JAXB tag for storing a old style w2pref file
     */
    private W2Pref theW2Prefs = null;
    private PreferenceHandler myPrefs = null;
    // Don't let it be read while it's reading...
    private final Object wdssiiReadLock = new Object();
    private Tag_setup theWdssiiXML;
    /**
     * Known URL location of preferences
     */
    private URL myURL = null;

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
    public static class PrefConstants {

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

    public static Singleton create() {
        instance = new PreferencesManager();
        return instance;
    }

    public static PreferencesManager getInstance() {
        if (instance == null) {
            LOG.debug("Preference Manager must be created by SingletonManager");
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
        loadRecentDocuments();
        loadConfig();
    }

    public Tag_setup getSetupXML() {
        synchronized (wdssiiReadLock) {
            return theWdssiiXML;
        }
    }

    public static void introduce(PreferenceHandler p) {
        if (instance == null) {
            LOG.debug("Preference Manager must be created by SingletonManager");
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
     * This is one of the 'static' files that is not part of layout/sources,
     * etc..
     */
    public void loadRecentDocuments() {
        OpenCommand.loadDocumentList();
    }

    /**
     * Not sure where to put this. The global w2config.xml file
     */
    public void loadConfig() {

       // String name = "wgpref.xml";
    }

    public void createNewDocument() {

        ViewManager.setNewLayout();

        // Remove all sources...
        CommandManager c = CommandManager.getInstance();
        SourceDeleteAllCommand dc = new SourceDeleteAllCommand();
        c.executeCommand(dc, false);
        FeatureDeleteAllCommand fc = new FeatureDeleteAllCommand();
        c.executeCommand(fc, false);
        
        setDocumentURL(null);
    }

    public void openDocument(URL aURL) {
        String name = aURL.getFile();
        try {
            W2Pref prefs = Util.load(aURL, W2Pref.class);
            theW2Prefs = prefs;
            if (prefs != null) {
                LOG.info("Preferences loaded from " + name);
            } else {
                LOG.info("Couldn't find preference file " + name);
            }
        } catch (Exception c) {
            LOG.error("Error loading preferences at " + name);
        }

        // First go to the new layout....
        ViewManager.setLayoutXML(theW2Prefs.rootwindow);

        // First remove all old sources from list
        // Would love to be 'smart' and merge old and new sources, only
        // remove sources that are not in new list, etc...maybe later
        CommandManager c = CommandManager.getInstance();
        SourceDeleteAllCommand dc = new SourceDeleteAllCommand();
        c.executeCommand(dc, false);
        FeatureDeleteAllCommand fc = new FeatureDeleteAllCommand();
        c.executeCommand(fc, false);

        if ((theW2Prefs != null) && (theW2Prefs.sources != null)) {

            for (Source s : theW2Prefs.sources.list) {
                try {
                    boolean connect = true;
                    LOG.info("Adding start source " + s.sourcename + " " + s.url);
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
        setDocumentURL(aURL);
    }

    public void saveAsDocument(URL aURL) {
        if (aURL != null) {
            if ((theW2Prefs != null)) {
                theW2Prefs.sources = SourceList.theSources.getSourceXML();
                theW2Prefs.rootwindow = ViewManager.getLayoutXML();

                String file = aURL.getFile();
                String error = Util.save(theW2Prefs, file, theW2Prefs.getClass());
                if (error.isEmpty()) {
                    setDocumentURL(aURL);
                }
            }
        }
    }

    public void saveDocument() {
        if (myURL != null) {
            saveAsDocument(myURL);
        }
    }

    public void setDocumentURL(URL aURL) {
        myURL = aURL;
        ViewManager.setConfigPath(myURL);
    }

    public URL getDocumentPath() {
        return myURL;
    }
}
