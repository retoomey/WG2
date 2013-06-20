package org.wdssii.gui.features;

import java.awt.Point;
import java.util.ArrayList;
import javax.swing.JComponent;
import org.wdssii.core.CommandManager;
import org.wdssii.geom.GLWorld;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.properties.Memento;
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
     * Generic Feature3DRenderers
     */
    ArrayList<Feature3DRenderer> myRenderers;
    /**
     * The GUI for this feature
     */
    private FeatureGUI myControls;

    /** The 'rank' of feature.  For example, point data has
     * a higher rank value than raster data, making it always stay
     * higher on the render stack (so it isn't covered by by the raster)
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

    public void setMemento(FeatureMemento m) {
        if (m != null) {
            mySettings.syncToMemento(m);
        }
    }

    public int getRank(){
        return myRank;
    }
    
    public void setRank(int r){
        myRank = r;
    }
    
    /**
     * Called when property of our memento is changed
     */
    @Override
    public void propertySetByGUI(Object name, Memento m) {
        FeatureMemento fm = (FeatureMemento) (m); // Check it
        FeatureChangeCommand c = new FeatureChangeCommand(this, fm);
        CommandManager.getInstance().executeCommand(c, true);
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
        return mySettings.getPropertyValue(FeatureMemento.VISIBLE);
    }

    /**
     * Get if can be deleted from FeatureList
     */
    public Boolean getDeletable() {
        return mySettings.getPropertyValue(FeatureMemento.CAN_DELETE);
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
        return mySettings.getPropertyValue(FeatureMemento.ONLY);
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

    public void addRenderer(Feature3DRenderer f) {
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

    /**
     * preRender a feature
     */
    public void preRender(GLWorld w) {
        if (myRenderers != null) {
            FeatureMemento m = getMemento();
            for (Feature3DRenderer r : myRenderers) {
                r.preRender(w, m);
            }
        }
    }

    /**
     * Render a feature
     */
    public void render(GLWorld w) {
        if (myRenderers != null) {
            FeatureMemento m = getMemento();
            for (Feature3DRenderer r : myRenderers) {
                r.draw(w, m);
            }
        }
    }

    /**
     * Pick a feature
     */
    public void pick(GLWorld w, Point p) {
        if (myRenderers != null) {
            FeatureMemento m = getMemento();
            for (Feature3DRenderer r : myRenderers) {
                r.pick(w, p, m);
            }
        }
    }

    /**
     * Would this feature render? This may be different than is visible or not,
     * for example a product might be 'visible' but won't draw because it is too
     * old in time
     */
    public boolean wouldRender() {
        return getVisible();  // Default is visible manual setting
    }

    public static class defaultGUI extends FeatureGUI {

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

    public FeatureGUI getControls(){
        if (myControls == null){
          myControls = createNewControls();
        }
        return myControls;
    }
    
    public void sendMessage(String message){
        if (myControls != null){
            myControls.sendMessage(message);
        }
    }
}
