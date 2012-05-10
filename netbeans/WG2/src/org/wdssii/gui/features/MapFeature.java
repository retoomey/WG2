package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.net.URL;
import javax.swing.JComponent;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.W2Config;
import org.wdssii.gui.MapGUI;
import org.wdssii.gui.MapRenderer;

/**
 * A Map feature draws a map in a window
 *
 * @author rtoomey
 */
public class MapFeature extends Feature {

    /**
     * The properties of the MapFeature
     */
    public static class MapMemento extends FeatureMemento {

        private int linethickness = 2;
        private boolean useLineThickness = false;
        private Color lineColor = Color.WHITE;
        private boolean useLineColor = false;

        public MapMemento(MapMemento m) {
            super(m);
            linethickness = m.linethickness;
            lineColor = m.lineColor;
        }

        /**
         * Sync to another memento by only copying what is wanted to be changed.
         *
         * @param m
         */
        public void syncToMemento(MapMemento m) {
            super.syncToMemento(m);
            if (m.useLineThickness) {
                linethickness = m.linethickness;
            }
            if (m.useLineColor) {
                lineColor = m.lineColor;
            }
        }

        public MapMemento(boolean v, boolean o, int line) {
            super(v, o);
            linethickness = line;
            lineColor = Color.WHITE;
        }

        public int getLineThickness() {
            return linethickness;
        }

        public void setLineThickness(int l) {
            linethickness = l;
            useLineThickness = true;
        }

        public Color getLineColor() {
            return lineColor;
        }

        public void setLineColor(Color c) {
            lineColor = c;
            useLineColor = true;
        }
    }
    private static Logger log = LoggerFactory.getLogger(MapFeature.class);
    public static final String MapGroup = "MAPS";
    /**
     * The renderer we use for drawing the map.
     */
    private MapRenderer myRenderer;
    /**
     * The GUI for this Feature
     */
    private MapGUI myControls;

    /**
     * The state we use for drawing the map.
     */
    public MapFeature(FeatureList f, String source) {
        super(f, MapGroup, new MapMemento(true, false, 2));
        URL u = W2Config.getURL(source);
        loadURL(u, source);
    }

    /**
     * The state we use for drawing the map.
     */
    public MapFeature(FeatureList f, URL u) {
        super(f, MapGroup, new MapMemento(true, false, 2));
        String source = "bad url";
        if (u != null) {
            source = u.toString();
        }
        loadURL(u, source);
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

                // Set message to our shapefile url...
                String s = u.toString();
                setMessage(s);
                setKey(s);

                // Does this always work?  Get short name of map from URL
                int dot = s.lastIndexOf(".");
                int sep = s.lastIndexOf("/");  // "\"?
                String sub = s.substring(sep + 1, dot);
                setName(sub);

                myRenderer = new MapRenderer(featureSource);
            } else {
                setMessage("?? " + source);
                setKey(source);
                // Does this always work?  Get short name of map from URL
                int dot = source.lastIndexOf(".");
                int sep = source.lastIndexOf("/");  // "\"?
                String sub = source.substring(sep + 1, dot);
                setName(sub);
            }
        } catch (Exception e) {
            log.error("Got exception trying to use GeoTools. " + e.toString());
        }
    }

    @Override
    public FeatureMemento getNewMemento() {
        MapMemento m = new MapMemento((MapMemento) getMemento());
        return m;
    }

    @Override
    public void setMemento(FeatureMemento f) {
        /**
         * Handle map mementos
         */
        if (f instanceof MapMemento) {
            MapMemento mm = (MapMemento) (f);
            ((MapMemento) getMemento()).syncToMemento(mm);
        } else {
            super.setMemento(f);
        }
    }

    /**
     * Render a feature
     */
    @Override
    public void render(DrawContext dc) {

        if (myRenderer != null) {
           // myRenderer.setupIfNeeded(dc);
            myRenderer.draw(dc, getMemento());
        }
    }

    @Override
    public void setupFeatureGUI(JComponent source) {

        // FIXME: general FeatureFactory..move code up into Feature
        boolean success = false;
        // if (myFactory != null) {

        if (myControls == null) {
            //myControls = myFactory.createGUI(myLLHArea, source);
            myControls = new MapGUI(this);
        }

        // Set the layout and add our controls
        if (myControls != null) {
            myControls.activateGUI(source);
            updateGUI();
            success = true;
        }
        //  }

        /**
         * Fill in with default stuff if GUI failed or doesn't exist
         */
        if (!success) {
            super.setupFeatureGUI(source);
        }
    }

    @Override
    public void updateGUI() {
        if (myControls != null) {
            myControls.updateGUI();
        }
    }
}
