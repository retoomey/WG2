package org.wdssii.gui.features;

import gov.nasa.worldwind.WorldWindow;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    static {
        myFactoryList.put("Set", new LLHAreaSetFactory());
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
	public FeatureMemento getNewMemento() {
            if (myLLHArea != null){
                return myLLHArea.getMemento();
            }else{
                return super.getNewMemento();
            }
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

        WorldWindView v = list().getWWView();
        if (v != null) {
            world = v.getWwd();
        }

        return world;
    }

    public LLHArea getLLHArea() {
        return myLLHArea;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        if (myFactory != null) {
            return myFactory.createGUI(this, myLLHArea);
        } else {
            return super.createNewControls();
        }
    }
}
