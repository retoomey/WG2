package org.wdssii.gui.features;

import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.W2Config;

/**
 * A Map feature draws a map in a window
 *
 * @author rtoomey
 */
public class MapFeature extends Feature {

    private SimpleFeatureSource mySource;

    /**
     * The properties of the MapFeature
     */
    public static class MapMemento extends FeatureMemento {

        // Properties
        public static final String LINE_THICKNESS = "line_thickness";
        public static final String LINE_COLOR = "line_color";

        public MapMemento(MapMemento m) {
            super(m);
        }

        public MapMemento() {
            initProperty(LINE_THICKNESS, 2);
            initProperty(LINE_COLOR, Color.WHITE);
        }
    }
    private final static Logger LOG = LoggerFactory.getLogger(MapFeature.class);
    public static final String MapGroup = "MAPS";

    /**
     * The state we use for drawing the map.
     */
    public MapFeature(FeatureList f, String source) {
        super(f, MapGroup, new MapMemento());
        URL u = W2Config.getURL(source);
        loadURL(u, source);
    }

    /**
     * The state we use for drawing the map.
     */
    public MapFeature(FeatureList f, URL u) {
        super(f, MapGroup, new MapMemento());
        String source = "bad url";
        if (u != null) {
            source = u.toString();
        }
        loadURL(u, source);
    }

    public SimpleFeatureSource getFeatureSource() {
        return mySource;
    }

    /**
     *
     * @param u the URL we have to try to load from
     * @param source the original source string we tried to look up URL for
     */
    protected final void loadURL(URL u, String source) {
        try {
            if (u != null) {
                FileDataStore store = FileDataStoreFinder.getDataStore(u);
                SimpleFeatureSource featureSource = store.getFeatureSource();
                mySource = featureSource;
                // Set message to our shapefile url...
                String s = u.toString();
                setMessage(s);
                setKey(s);

                // Does this always work?  Get short name of map from URL
                int dot = s.lastIndexOf('.');
                int sep = s.lastIndexOf('/');  // "\"?
                String sub = s.substring(sep + 1, dot);
                setName(sub);

                // This has to go away, since we have multiview now...
                //   MapRenderer m = new MapRenderer(featureSource);
                // GOOP   addRenderer(m);
            } else {
                setMessage("?? " + source);
                setKey(source);
                // Does this always work?  Get short name of map from URL
                int dot = source.lastIndexOf('.');
                int sep = source.lastIndexOf('/');  // "\"?
                String sub = source.substring(sep + 1, dot);
                setName(sub);
            }
        } catch (Exception e) {
            LOG.error("Got exception trying to use GeoTools. " + e.toString());
        }
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
        addNewRendererItem(list, type, packageName, "MapRenderer");
        return list;
    }

    @Override
    public FeatureMemento getNewMemento() {
        MapMemento m = new MapMemento((MapMemento) getMemento());
        return m;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        return new MapGUI(this);
    }
}
