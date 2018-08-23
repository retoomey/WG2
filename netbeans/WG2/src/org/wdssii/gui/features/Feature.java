package org.wdssii.gui.features;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.wdssii.core.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;
import org.wdssii.properties.MementoString;
import org.wdssii.properties.Mementor;

/**
 * Feature will be anything that we display in a window. A feature will be
 * unique per window. So 'visible' or 'color' will be at this level. Any
 * cached/shared information will be from contained objects. A Feature can
 * create a FeatureGUI for setting the properties of the feature. When the
 * feature is selected in the display, its GUI will be shown.
 *
 * @author Robert Toomey
 */
public class Feature implements Mementor {

    private static final Logger LOG = LoggerFactory.getLogger(Feature.class);
    /**
     * Storage for renderers based off of a passed in ID
     */
    private Map<String, ArrayList<FeatureRenderer>> myRenderers;

    /**
     * Used to get information back from a Feature factory to put into the
     * standard GUI table
     */
    public static class FeatureTableInfo {

        public boolean visible;
        public boolean onlyMode;
        public String visibleName;
        public String keyName;
        public String message;
        public Object created;
    }
    /**
     * Our feature group
     */
    private final String myFeatureGroup;
    /**
     * Our feature list we belong too
     */
    private final FeatureList myFeatureList;
    private FeatureMemento mySettings = new FeatureMemento();
    
    /**
     * What is our name?
     */
    private String myName = "";
    /**
     * What is our key?
     */
    private String myKey = "";
    /**
     * What is our message?
     */
    private String myMessage = "";
    /**
     * The GUI for this feature
     */
    private FeatureGUI myControls;
    /**
     * The 'rank' of feature. For example, point data has a higher rank value
     * than raster data, making it always stay higher on the render stack (so it
     * isn't covered by by the raster)
     */
    private int myRank = 0;
    public final static int RASTER = 0;
    public final static int POINT = 1;
    public final static int MAX_RANK = 1;

    /**
     * Create a feature with a default memento
     */
    public Feature(FeatureList f, String g) {
        this(f, g, new FeatureMemento());
    }

    /**
     * Typically called by subclass to add an enhanced memento with more
     * settings in it.
     *
     * @param g The group we're in
     * @param settings Memento from subclass
     */
    public Feature(FeatureList f, String g, FeatureMemento settings) {
        myFeatureList = f;
        myFeatureGroup = g;
        mySettings = settings;
    }

    /**
     * FACTORY for getting renderer for a view type, such as worldwind,
     * geotools, awiips2, etc.
     */
    public void addNewRendererItem(
            ArrayList<FeatureRenderer> list, String id, String packageName, String className) {
        FeatureRenderer r = createRenderer(id, packageName, className);
        if (r != null) {
            list.add(r);
        }
    }

    public ArrayList<FeatureRenderer> getNewRendererList(String type, String packageName) {
        return null; // Default we don't render anything
    }

    /**
     * Generically store a renderer list
     */
    public void storeRendererList(String id, ArrayList<FeatureRenderer> list) {
        if (myRenderers == null) {
            myRenderers = new TreeMap<String, ArrayList<FeatureRenderer>>();
        }
        myRenderers.put(id, list);
    }

    /**
     * Generically get back a renderer list
     */
    public ArrayList<FeatureRenderer> getRendererList(String id, String packageName) {
        if (myRenderers == null) {
            myRenderers = new TreeMap<String, ArrayList<FeatureRenderer>>();
        }
        ArrayList<FeatureRenderer> stuff = myRenderers.get(id);
        if (stuff == null) {
        	stuff = getNewRendererList(id, packageName);
            storeRendererList(id, stuff);
        }
        return stuff;
    }

    /**
     * Create a renderer by reflection. Decouples from class while allowing
     * others to use us for caching FeatureRenderers.
     */
    public FeatureRenderer createRenderer(String id, String packageName, String className) {

        // Example org.wdssii.
        Class<?> aClass = null;
        FeatureRenderer newRenderer = null;

        // "org.wdssii" + "." + "WW"+"MapRenderer" for example....
        // Worldwind id is WW
        // Geotools will be GT
        // Awiips will be AW2
      //  LOG.error("Package name is "+packageName+", and " +className);
    	//System.exit(1);
       // if (packageName.equals("org.wdssii.gui.worldwind")) {
        	LOG.error("*********************************************************");
        	LOG.error("Package name is "+packageName+", and " +className);
        	LOG.error("*********************************************************");

        //	System.exit(1);
       // }
        String make = packageName + "." + id + className;
        try {
            aClass = Class.forName(make);
            Constructor<?> c = aClass.getConstructor();
            if (c == null) {
                LOG.error("Tried to create: " + make);
            }
            newRenderer = (FeatureRenderer) c.newInstance();
            LOG.debug("Created renderer " + make);
        } catch (Exception e) {
            LOG.error("Couldn't create class by name '"
                    + make + "' because " + e.toString());
        }
        return newRenderer;
    }

    /**
     * All features belong to a FeatureList
     */
    public FeatureList list() {
        return myFeatureList;
    }

    /**
     * Get our feature group
     */
    public String getFeatureGroup() {
        return myFeatureGroup;
    }

    public void updateMemento(Memento m) {
        if (m != null) {
            mySettings.syncToMemento(m);
            if (myControls != null){
            	myControls.updateGUI(m);
            }
        }
    }

    public int getRank() {
        return myRank;
    }

    public void setRank(int r) {
        myRank = r;
    }

    /**
     * Called when property of our memento is changed
     */
    @Override
    public void propertySetByGUI(Object name, Memento m) {
        FeatureChangeCommand c = new FeatureChangeCommand(this, m);
        CommandManager.getInstance().executeCommand(c, true);
    }

    /** Experiment.  Memento with only update fields in it. */
    @Override
    public MementoString getUpdateMemento(){
    	return new MementoString();
    }
    
    /**
     * Get a new memento copy of our settings. This is for modifying and sending
     * back to us to change a setting
     *
     * @return
     */
    @Override
    public FeatureMemento getNewMemento() {
        FeatureMemento m = new FeatureMemento(mySettings);
        return m;
    }

    /**
     * Get our actual settings
     */
    @Override
    public FeatureMemento getMemento() {
        return mySettings;
    }

    /**
     * Get visible state
     */
    public Boolean getVisible() {
        return mySettings.get(FeatureMemento.VISIBLE, true);
    }

    /**
     * Get if can be deleted from FeatureList
     */
    public Boolean getDeletable() {
        return mySettings.get(FeatureMemento.CAN_DELETE, false);
    }

    /**
     * Set if can be deleted from FeatureList
     */
    public void setDeletable(boolean flag) {
        mySettings.setProperty(FeatureMemento.CAN_DELETE, flag);
    }

    /**
     * Set visible state
     */
    public void setVisible(boolean flag) {
        mySettings.setProperty(FeatureMemento.VISIBLE, flag);
    }

    /**
     * Get visible state
     */
    public Boolean getOnlyMode() {
        return mySettings.get(FeatureMemento.ONLY, false);
    }

    /**
     * Set visible state
     */
    public void setOnlyMode(boolean flag) {
        mySettings.setProperty(FeatureMemento.ONLY, flag);
    }

    /**
     * Get the name of this feature
     */
    public String getName() {
        return myName;
    }

    /**
     * Set the name of this feature
     */
    public void setName(String n) {
        myName = n;
    }

    /**
     * Get the key for this feature
     */
    public String getKey() {
        return myKey;
    }

    /**
     * Set the key for this feature
     */
    public void setKey(String n) {
        myKey = n;
    }

    /**
     * Get the name of this feature
     */
    public String getMessage() {
        return myMessage;
    }

    /**
     * Set the name of this feature
     */
    public void setMessage(String n) {
        myMessage = n;
    }

    /**
     * Sent from list to let us know we were selected
     */
    public void wasSelected() {
    }

    /*public void addRenderer(Feature3DRenderer f) {
     // Lazy create to save memory
     if (myRenderers == null) {
     myRenderers = new ArrayList<Feature3DRenderer>();
     }
     // Add if not already there...
     if (f != null) {
     if (!myRenderers.contains(f)) {
     myRenderers.add(f);
     }
     }
     }

     public void removeRenderer(Feature3DRenderer f) {
     if (myRenderers != null) {
     myRenderers.remove(f);
     if (myRenderers.isEmpty()) {
     myRenderers = null;
     }
     }
     }
     */
    /**
     * preRender a feature
     */
    //public void preRender(GLWorld w) {
    //    if (myRenderers != null) {
    //        FeatureMemento m = getMemento();
    //        for (Feature3DRenderer r : myRenderers) {
    //            r.preRender(w, m);
    //        }
    //    }
    //}
    /**
     * Render a feature
     */
    //public void render(GLWorld w) {
    //    if (myRenderers != null) {
    //        FeatureMemento m = getMemento();
    //        for (Feature3DRenderer r : myRenderers) {
    //            r.draw(w, m);
    //        }
    //    }
    //}
    /**
     * Pick a feature
     */
    //public void pick(GLWorld w, Point p) {
    //    if (myRenderers != null) {
    //        FeatureMemento m = getMemento();
    //        for (Feature3DRenderer r : myRenderers) {
    //            r.pick(w, p, m);
    //        }
    //    }
    //}
    /**
     * Would this feature render? This may be different than is visible or not,
     * for example a product might be 'visible' but won't draw because it is too
     * old in time
     */
    public boolean wouldRender() {
        return getVisible();  // Default is visible manual setting
    }

    public static class defaultGUI extends FeatureGUI {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void updateGUI() {
            // Set the layout and add our controls
            // source.setLayout(new java.awt.BorderLayout());
            //JTextField t = new JTextField();
            //t.setText("No controls for this object");
            //t.setEditable(false);
            //source.add(t, java.awt.BorderLayout.CENTER);
            //source.doLayout();
        }

        @Override
        public void activateGUI(JComponent parent) {
        }

        @Override
        public void deactivateGUI() {
        }
    }

    /**
     * Fill in two source GUIs. Return false if second not wanted
     */
    public void setUpEmptyGUI(JComponent source, JComponent source2) {
    }

    /**
     * Create a new GUI for this feature
     */
    public FeatureGUI createNewControls() {
        return new defaultGUI();
    }

    public FeatureGUI getControls() {
        if (myControls == null) {
            myControls = createNewControls();
        }
        return myControls;
    }

    public void sendMessage(String message) {
        if (myControls != null) {
            myControls.sendMessage(message);
        }
    }

	public void handlePickText(int pickId, boolean leftMouse) {
		// TODO Auto-generated method stub
		
	}
}
