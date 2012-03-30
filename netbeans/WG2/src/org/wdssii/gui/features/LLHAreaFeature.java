package org.wdssii.gui.features;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.render.DrawContext;
import java.util.TreeMap;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.gui.volumes.*;

/**
 * A LLHArea feature draws a 3D shape in the window that can be used by charts
 * or display for rendering stuff.
 *
 * @author Robert Toomey
 */
public class LLHAreaFeature extends Feature {

    private static Logger log = LoggerFactory.getLogger(LLHAreaFeature.class);
    public static final String LLHAreaGroup = "3D";
    // Factories shared by all LLHAreaFeatures anywhere
    // Probably should make this more configurable...
    private static final TreeMap<String, LLHAreaFactory> myFactoryList = new TreeMap<String, LLHAreaFactory>();
    /**
     * The factory we were created with
     */
    private LLHAreaFactory myFactory;
    
    /** The GUI for this feature */
    private FeatureGUI myControls;

    static {
        myFactoryList.put("Slice", new LLHAreaSliceFactory());
        myFactoryList.put("Box", new LLHAreaBoxFactory());
        myFactoryList.put("Stick", new LLHAreaHeightStickFactory());
    }
    /**
     * The LLHArea object we render
     */
    private LLHArea myLLHArea;

    public LLHAreaFeature(FeatureList f) {
        super(f, LLHAreaGroup);
    }

    public boolean createLLHArea(String factory) {
        boolean success = false;
        if (myFactory == null) {
            myFactory = myFactoryList.get(factory);

            if (myFactory != null) {
                FeatureTableInfo theData = new FeatureTableInfo();
                success = myFactory.create(getWorld(), this, theData);
                if (success) {
                    myLLHArea = (LLHArea) theData.created;
                    setVisible(true);
                    setOnlyMode(false);
                    setName(theData.visibleName);
                    setKey(theData.keyName);
                    setMessage(theData.message);
                }

            } else {
                setName("Error");
                setMessage("Not created");
            }
        }
        return success;
    }

    @Override
    public void setMemento(FeatureMemento m) {
        super.setMemento(m);
        if (myLLHArea != null) {
            myLLHArea.setFromMemento(m);
        }
    }

    /**
     * Get the world we use. Currently only one earthball. This should probably
     * come from 'above' somehow. Should a feature know the FeatureList it is
     * in?
     */
    private WorldWindow getWorld() {
        WorldWindow world = null;

        WorldWindView v = CommandManager.getInstance().getEarthBall();
        if (v != null) {
            world = v.getWwd();
        }

        return world;
    }

    public LLHArea getLLHArea() {
        return myLLHArea;
    }

    /**
     * Render a LLHAreaFeature. Currently the LLHAreaLayer gets our LLHArea
     * directly and does fancy rendering...
     */
    @Override
    public void render(DrawContext dc) {
    }

    /**
     * Activate our GUI within the given JComponent. The JComponent is assumed
     * empty. We should assign the layout we want to it. The caller is trusting
     * us to handle this properly.
     *
     * @param source
     */
    @Override
    public void setupFeatureGUI(JComponent source) {

        // FIXME: general FeatureFactory..move code up into Feature
        boolean success = false;
        if (myFactory != null) {

            if (myControls == null) {
                myControls = myFactory.createGUI(myLLHArea, source);
            }

            // Set the layout and add our controls
            if (myControls != null) {
                myControls.activateGUI(source);
                updateGUI();
                success = true;
            }
        }

        /**
         * Fill in with default stuff if GUI failed or doesn't exist
         */
        if (!success) {
            super.setupFeatureGUI(source);
        }
    }

    /**
     * Update our current GUI controls
     */
    @Override
    public void updateGUI() {
        if (myControls != null) {
            myControls.updateGUI();
        }
    }
}
