package org.wdssii.gui.features;

import java.util.ArrayList;
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
    public final String myInset, myCompass, myScale, myViewControls;

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

        addNewRendererItem(list, type, "org.wdssii.gui.renderers", "ColorMapRenderer"); 
        addNewRendererItem(list, type, packageName, "LegendInset");    
        addNewRendererItem(list, type, packageName, "LegendCompass");
        addNewRendererItem(list, type, packageName, "LegendScale");    
        addNewRendererItem(list, type, packageName, "LegendViewControls");
       
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
}
