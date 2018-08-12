package org.wdssii.gui.features;

import java.util.ArrayList;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

public class EarthBallFeature extends Feature {

    /**
     * The properties of the MapFeature
     */
    public static class EarthBallMemento extends FeatureMemento {

        // Properties
       public static final String BALL_DENSITY = "ball_density";
       // public static final String LINE_COLOR = "line_color";

        public EarthBallMemento(EarthBallMemento m) {
            super(m);
        }

        public EarthBallMemento() {
            initProperty(BALL_DENSITY, 200);
            //initProperty(LINE_COLOR, Color.WHITE);
        }
    }
    @SuppressWarnings("unused")
	private final static Logger LOG = LoggerFactory.getLogger(EarthBallFeature.class);
    public static final String MapGroup = "BASEMAP";
    
	public EarthBallFeature(FeatureList f) {
		super(f, MapGroup, new EarthBallMemento());
		setName("EarthBall");
		setMessage("Render simple earth ball");
		setKey("EarthBall");
	}
	
    @Override
    public void addNewRendererItem(ArrayList<FeatureRenderer> list, String id, String packageName, String className) {
        FeatureRenderer r = createRenderer(id, packageName, className);
        if (r != null) {
            r.initToFeature(this);
            list.add(r);
        }
    }

    @Override
    public ArrayList<FeatureRenderer> getNewRendererList(String type, String packageName) {
        ArrayList<FeatureRenderer> list = new ArrayList<FeatureRenderer>();
        addNewRendererItem(list, type, packageName, "EarthBallRenderer");
        return list;
    }

    @Override
    public FeatureMemento getNewMemento() {
        EarthBallMemento m = new EarthBallMemento((EarthBallMemento) getMemento());
        return m;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new EarthBallGUI(this);
    }
}
