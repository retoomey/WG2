package org.wdssii.gui.charts;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapPane;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.MapFeature;

/**
 *
 * @author Robert Toomey
 *
 * A GeoTool map pane
 *
 */
public class GeoToolDataView extends DataView {

    private final static Logger LOG = LoggerFactory.getLogger(GeoToolDataView.class);
    // public SimpleFeatureSource featureSource;
    private myJMapPane myMapPane = null;

    /**
     * Static method to create, called by reflection
     */
    public static GeoToolDataView create() {

        return new GeoToolDataView();

    }

    public GeoToolDataView() {
        // display a data store file chooser dialog for shapefiles
       /* File file = JFileDataStoreChooser.showOpenFile("shp", null);
         if (file == null) {
         return;
         }

         try {
         FileDataStore store = FileDataStoreFinder.getDataStore(file);
         featureSource = store.getFeatureSource();


         } catch (Exception e) {
         }*/
    }

    @Override
    public Object getNewGUIForChart(Object parent) {

        //JPanel worldHolder = new JPanel();
        //worldHolder.setLayout(new WorldLayoutManager());
        // worldHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));


        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Map browser using geotools");

        // Style style = SLD.createSimpleStyle(featureSource.getSchema());
        // Layer layer = new FeatureLayer(featureSource, style);
        // map.addLayer(layer);

        // Now display the map
        myJMapPane mapPane = new myJMapPane(map);
        File file = JFileDataStoreChooser.showOpenFile("tif", null);
        if (file != null) {
            AbstractGridFormat format = GridFormatFinder.findFormat(file);
            myMapPane.reader = format.getReader(file);
            // Initially display the raster in greyscale using the
            // data from the first image band
            //Style rasterStyle = createGreyscaleStyle(1);
        }
        //   mapPane.setBackground(Color.BLUE);
        //   mapPane.setRenderer(new StreamingRenderer());
        //   mapPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //testPane.showMap(map);

        //JMapFrame.showMap(map);
//JMapFrame.showMap(map);
        myMapPane = mapPane;
        return mapPane;
        //return worldHolder;
    }

    public static class myJMapPane extends JMapPane {

        public GridCoverage2DReader reader;
        private StyleFactory sf = CommonFactoryFinder.getStyleFactory();
        private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        public myJMapPane(MapContent m) {
            super(m);
        }

        protected synchronized void paintComponent(Graphics g) {
            super.paintComponent(g);

        }

        /**
         * * sync to keep GUI calling too fast...
         */
        public synchronized void update(boolean force) {
            LOG.debug("UPDATE CALLED ON GEOTOOLS");
            setMapContent(null);

            // Completely new map context and rebuild... bleh...
            // FIXME: do we need to rebuild every time?
            MapContent map = new MapContent();
            map.setTitle("Map browser using geotools");
            setBackground(Color.BLACK);
            setRenderer(new StreamingRenderer());

            // Go through feature list, rebuild anything different
            List<Feature> list = FeatureList.theFeatures.getFeatures();
            for (Feature f : list) {
                FeatureMemento memento = f.getMemento();
                boolean visible = f.getVisible();
                if (visible && (f instanceof MapFeature)) {
                    MapFeature mf = (MapFeature) (f);
                    SimpleFeatureSource s = mf.getFeatureSource();
                    if (s != null) {
                        // For the moment just trying to get it to work
                        // Bleh we only handle line maps at moment....so much work to do 
                        MapFeature.MapMemento mm = (MapFeature.MapMemento) (mf.getMemento());
                        Color line = mm.getPropertyValue(MapFeature.MapMemento.LINE_COLOR);
                        Integer t = mm.getPropertyValue(MapFeature.MapMemento.LINE_THICKNESS);
                        // Style style = SLD.createSimpleStyle(s.getSchema(), line);
                        Style style = SLD.createLineStyle(line, t);
                        Layer layer = new FeatureLayer(s, style);
                        map.addLayer(layer);
                    }
                }
                // Layer layer = new FeatureLayer(featureSource, style);
                // map.addLayer(layer);
            }
            // Initially display the raster in greyscale using the
            // data from the first image band
            Style rasterStyle = createGreyscaleStyle(1);
            Layer rasterLayer = new GridReaderLayer(reader, rasterStyle);
            map.addLayer(rasterLayer);
            setMapContent(map);
            setBackground(Color.WHITE);
            setBackground(Color.BLACK);
        }

        /**
         * Create a Style to display a selected band of the GeoTIFF image as a
         * greyscale layer
         *
         * @return a new Style instance to render the image in greyscale
         */
       /* private Style createGreyscaleStyle() {
            GridCoverage2D cov = null;
            try {
                cov = reader.read(null);
            } catch (IOException giveUp) {
                throw new RuntimeException(giveUp);
            }
            int numBands = cov.getNumSampleDimensions();
            Integer[] bandNumbers = new Integer[numBands];
            for (int i = 0; i < numBands; i++) {
                bandNumbers[i] = i + 1;
            }
            Object selection = JOptionPane.showInputDialog(
                    null, // frame
                    "Band to use for greyscale display",
                    "Select an image band",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    bandNumbers,
                    1);
            if (selection != null) {
                int band = ((Number) selection).intValue();
                return createGreyscaleStyle(band);
            }
            return null;
        }*/

        /**
         * Create a Style to display the specified band of the GeoTIFF image as
         * a greyscale layer.
         * <p>
         * This method is a helper for createGreyScale() and is also called
         * directly by the displayLayers() method when the application first
         * starts.
         *
         * @param band the image band to use for the greyscale display
         *
         * @return a new Style instance to render the image in greyscale
         */
        private Style createGreyscaleStyle(int band) {
            ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
            SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);

            RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
            ChannelSelection sel = sf.channelSelection(sct);
            sym.setChannelSelection(sel);

            return SLD.wrapSymbolizers(sym);
        }
    }

    /**
     * Update chart when needed (check should be done by chart)
     */
    public void updateChart(boolean force) {
        if (myMapPane != null) {
            myMapPane.update(force);
        }

    }
}
