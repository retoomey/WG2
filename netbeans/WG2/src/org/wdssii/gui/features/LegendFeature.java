package org.wdssii.gui.features;

import java.util.ArrayList;

import org.wdssii.gui.GLWorld;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.gui.renderers.CompassRenderer;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Legend shows the color key as well as product type, etc.
 *
 * @author Robert Toomey 
 */
public class LegendFeature extends Feature {

    private final static Logger LOG = LoggerFactory.getLogger(LegendFeature.class);
    public static final String LegendGroup = "2D Overlay";
    public final String myInset;
	public final String myCompass;
	public final String myScale;
	public final String myViewControls;
	
	CompassRenderer myCompassRenderer = null;
			
    /**
     * The properties of the LegendFeature
     */
    public static class LegendMemento extends FeatureMemento {

        // Properties
        public static final String SHOWMOUSE = "mousereadout";
        public static final String SHOWLABELS = "colorkeylabels";
        public static final String SHOWCOMPASS = "compass";
        public static final String SHOWSCALE = "scale";
        public static final String SHOWWORLDINSET = "inset";
        public static final String SHOWVIEWCONTROLS = "controls";
        public static final String UNITS = "units";
        public static final String INCOMPASS = "compassin";

        public LegendMemento(LegendMemento m) {
            super(m);
        }

        public LegendMemento() {
            // Override initial feature delete to false
            initProperty(CAN_DELETE, false);
            initProperty(SHOWMOUSE, true);
            initProperty(SHOWLABELS, true);
            initProperty(SHOWCOMPASS, true);
            initProperty(SHOWSCALE, true);
            initProperty(SHOWWORLDINSET, false);
            initProperty(SHOWVIEWCONTROLS, true);
            initProperty(UNITS, "");
            initProperty(INCOMPASS, false);
        }
    };

    @Override
    public FeatureMemento getNewMemento() {
        LegendMemento m = new LegendMemento((LegendMemento) getMemento());
        return m;
    }

    /**
     * The state we use for drawing the map.
     */
    public LegendFeature(FeatureList f, String c, String s, String i, String vc) {
        super(f, LegendGroup, new LegendMemento());
        setName("Legend Overlay");
        setKey("Legend");
        setMessage("Legend controls and settings");
        myInset = i;
        myCompass = c;
        myScale = s;
        myViewControls = vc;
    }

    @Override
    public void addNewRendererItem(ArrayList<FeatureRenderer> list, String id, String packageName, String className) {
        FeatureRenderer r = createRenderer(id, packageName, className);
        if (r != null){
            r.initToFeature(this);
            list.add(r);
        }
    }
    
    @Override
    public ArrayList<FeatureRenderer> getNewRendererList(String type, String packageName) {

        ArrayList<FeatureRenderer> list = new ArrayList<FeatureRenderer>();

        addNewRendererItem(list, "", "org.wdssii.gui.renderers", "ColorMapRenderer"); 
  //      addNewRendererItem(list, type, packageName, "LegendInset");    
   //     addNewRendererItem(list, type, packageName, "LegendCompass");
   //     addNewRendererItem(list, type, packageName, "LegendScale");    
   //     addNewRendererItem(list, type, packageName, "LegendViewControls");
       // addNewRendererItem(list, "", "org.wdssii.gui.renderers", "CompassRenderer");
     
        // Kinda violates the whole create by name thing...  Need a generic way of getting
        // pick ids from renderers if we want to avoid this...
        myCompassRenderer = new CompassRenderer();
        myCompassRenderer.initToFeature(this);
        list.add(myCompassRenderer);
        
        // On SHOWMOUSE ...list.add(new simpleReadout());
        return list;
    }

    /*
     * Steal the stock worldwind layers from the world wind view and put
     * them in our legend feature.  We need to control these to make sure
     * they don't overlap and allow properties to be set/changed
     */
    public static LegendFeature createLegend(FeatureList list,
            String compass, String scalebar, String inset, String controls) {

        LegendFeature f = new LegendFeature(list, compass, scalebar, inset, controls);
        return f;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new LegendGUI(this);
    }
    
    // Proof of concept messing around with compass interaction
    
    @Override
    public boolean handleMouseMoved(int pickID, GLWorld w, FeatureMouseEvent e) {
		/*int id = myCompassRenderer.getCompassPickID();
		//LOG.error("ID BACK HANDLE PICK IS "+id);

		//boolean value = (leftDown && (pickID == id) && (pickID > -1));
		boolean value = ((pickID == id) && (pickID > -1));

		// FIXME: only return true iff the value toggles...
		getMemento().setProperty(LegendMemento.INCOMPASS, value);
		return true;
		*/
    	return false;
	}
    
    @Override
	public boolean handleMousePressed(int pickID, GLWorld w, FeatureMouseEvent e) {
    	int id = myCompassRenderer.getCompassPickID();
		boolean inCompass = ((pickID == id) && (pickID > -1));

		// Return true if compass toggled, since we'll need a redraw
		// of us.
		FeatureMemento m = getMemento();
		boolean oldState = m.get(LegendMemento.INCOMPASS, false);	
		if (oldState != inCompass) {
			m.setProperty(LegendMemento.INCOMPASS, inCompass);
			return true;
		}
		return false;
	}
    
    @Override
	public boolean handleMouseReleased(int pickID, GLWorld w, FeatureMouseEvent e) {
		boolean inCompass = false;
		
		// Return true if compass toggled, since we'll need a redraw
		// of us.
		FeatureMemento m = getMemento();
		boolean oldState = m.get(LegendMemento.INCOMPASS, false);	
		if (oldState != inCompass) {
			m.setProperty(LegendMemento.INCOMPASS, inCompass);
			return true;
		}
		return false;
	}
    
    @Override
	public boolean handleMouseDragged(int pickID, GLWorld w, FeatureMouseEvent e) {
		return false;
	}
	
	
}
