package org.wdssii.gui.worldwind;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.wdssii.core.CommandManager;
import org.wdssii.core.StopWatch;
import org.wdssii.geom.LLD_X;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.DynamicXYZDataset;
import org.wdssii.gui.charts.LLHAreaChart;
import org.wdssii.gui.commands.VolumeSetTypeCommand;
import org.wdssii.gui.commands.VolumeSetTypeCommand.VolumeTypeFollowerView;
import org.wdssii.gui.commands.VolumeValueCommand;
import org.wdssii.gui.commands.VolumeValueCommand.VolumeValueFollowerView;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeaturePosition;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.VolumeSlice2DOutput;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.ElevationModel;

/**
 * Chart that draws a dynamic grid sampled from a product volume in a
 * JFreeChart.  Deprecating since JFreeChart is (not by it's fault) slower than
 * rendering native openGL.
 */
public class VSliceChart extends LLHAreaChart implements VolumeValueFollowerView, VolumeTypeFollowerView {

    private final static Logger LOG = LoggerFactory.getLogger(VSliceChart.class);
    private ProductVolume myVolume = null;
    private JToggleButton jVirtualToggleButton;
    //hack for first attempt..will need a list of charts
    public static VSliceChart created = null;
    /**
     * Keep volume value setting per chart
     */
    public String myCurrentVolumeValue = "";
    // -- Start VSlice 3d
    /**
     * The number of rows or altitudes of the VSlice
     */
    public static final int myNumRows = 100;  //50
    /**
     * The number of cols or change in Lat/Lon
     */
    public static final int myNumCols = 200; //100
    /**
     * Holder for the slice GIS 'state'
     */
    private VolumeSliceInput myCurrentGrid =
            new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
            0, 0, 0, 50);
    private LLD_X myLeftLocation;
    private LLD_X myRightLocation;
 
    private int myMouseX = -1;
    private int myMouseY = -1;

    // end VSlice 3d
    // Volume value follower
    @Override
    public void setCurrentVolumeValue(String changeTo) {
        myCurrentVolumeValue = changeTo;
        if (myVolume != null) {
            updateChart(true); // Force update
        }
    }

    @Override
    public String getCurrentVolumeValue() {

        if (myVolume != null) {
            VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValue);
            if (v != null) {
                myCurrentVolumeValue = v.getName();
            }
            return myCurrentVolumeValue;
        }
        return "";
    }

    @Override
    public java.util.List<String> getValueNameList() {

        // We get this from the current volume...
        java.util.List<String> s;
        if (myVolume == null) {
            s = new ArrayList<String>();
            s.add("No volume data");
        } else {
            s = myVolume.getValueNameList();
        }

        return s;
    }
    /**
     * The axis for distance in KM
     */
    private FixedRangeNumberAxis myDistanceAxis;
    /**
     * The axis for the height of the VSlice side distance
     */
    private FixedRangeNumberAxis myHeightAxis;
    static int counter = 0;
    /**
     * The last GIS key. This is the key part that deals with the 'shape' of the
     * VSlice without product/volume
     */
    private VSliceFixedGridPlot myPlot = null;

    public VSliceChart(String arg0, Font arg1, VSliceFixedGridPlot plot, boolean arg3) {
        myJFreeChart = new JFreeChart(arg0, arg1, plot, arg3);
        myPlot = plot;
        myPlot.myText = new TextTitle("", new Font("Dialog", Font.PLAIN, 11));
        myPlot.myText.setPosition(RectangleEdge.BOTTOM);
        myPlot.myText.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        myJFreeChart.addSubtitle(myPlot.myText);
        created = this;  // temp hack for LLHArea rendering
    }

    /**
     * Called during dragging of vslice to explicitly update the chart. The
     * chart checks for changes and only draws when the vslice if different
     */
    @Override
    public void updateChart(boolean force) {
        // The LLHArea is the geometry in the 3d window we are
        // matching our coordinates to.  It can be valid without
        // any product/volume information.
        myPlot.setCurrentVolumeValueName(myCurrentVolumeValue);
        LLHAreaSet llhArea = getLLHAreaToPlot();
        if (llhArea == null) {
            // If there isn't a 3D slice LLHArea object geometry to follow,
            // clear us...
            myPlot.setPlotData(null, null, null);
            myJFreeChart.setTitle("No slice in 3d window");
            myJFreeChart.fireChartChanged();
            return;
        }   // No slice to follow, return..
        myPlot.setLLHArea(llhArea);

        /**
         * Get the GIS key
         */
        String gisKey = llhArea.getGISKey();

        // Sync the height/range axis to the GIS vslice range when updated, this resets
        // any 'zoom' in the chart...but only if GIS key has CHANGED.  This
        // way users can toggle products and keep zoom level for comparison,
        // but if they drag the vslice we reset to full slice.
        if (!getGISKey().equals(gisKey)) {
            // Get the full non-subgrid area and make the distance line axis correct,
            // even if we are missing everything else
            VolumeSliceInput info = llhArea.getSegmentInfo(myCurrentGrid, 0, myNumRows, myNumCols);
            if (info != null) {
                myDistanceAxis.setFixedRange(new Range(0, llhArea.getRangeKms(0, 1) / 1000.0));
                myHeightAxis.setFixedRange(new Range(llhArea.getBottomHeightKms() / 1000.0, llhArea.getTopHeightKms() / 1000.0));
            }
        }
        setGISKey(gisKey);

        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());

        FilterList aList = null;
        String useKey = getUseProductKey();
        String titleKey;
        /**
         * Get the filter list of the product we are following
         */
        ProductFeature tph = ProductManager.getInstance().getProductFeature(useKey);
        Product p = null;
        if (tph != null) {
            aList = tph.getFList();
            p = tph.getProduct();
        }
        if (p != null) {
            titleKey = p.getProductInfoString(false);
        } else {
            titleKey = "No product";
        }

        if ((volume == null) || (aList == null)) {
            // If there isn't a valid data source, clear us out...
            // clear us...
            myVolume = null;
            myPlot.setPlotData(llhArea, null, null);
            myJFreeChart.setTitle("No volume data");
            myJFreeChart.fireChartChanged();
            return;
        }

        /**
         * Physical key of the Lat/Lon/Height location
         */
        String key = gisKey;

        /**
         * Add volume key
         */
        key += volume.getKey();

        /**
         * Add filter key
         */
        key += aList.getFilterKey(getUseProductFilters());

        boolean keyDifferent = false;
        if (!key.equals(getFullKey())) {
            keyDifferent = true;
        }
        setFullKey(key);

        if (!force && !keyDifferent) {
            return;
        }

        // if (keyDifferent) {
        //     System.out.println("VSLICE KEY CHANGE");
        // }
        myVolume = volume;
        myPlot.setPlotData(llhArea, volume, aList);
        myJFreeChart.setTitle(titleKey);
        myJFreeChart.fireChartChanged();
    }

    /**
     * Terrain XYZDataset is a JFreeChart dataset where we sample the terrain
     * 'height' along the range.
     */
    public static class TerrainXYZDataset extends DynamicXYZDataset {

        public TerrainXYZDataset() {
            super("Terrain", 201);
        }
        /*
         * We dynamically resample the terrain data depending on zoom
         * level. This is called with the current lat/lon of the chart
         * so that the terrain can be resampled by zoom
         */

        public void syncToRange(ValueAxis x,
                double startLat,
                double startLon,
                double endLat,
                double endLon) {

            // Clear range
            clearRange();

            // Sample and fill in with new values

            // Uh oh..we might not have a WWView right?  No terrain then...
            //  WorldWindView eb = FeatureList.theFeatures.getWWView();


            // Globe globe = eb.getWwd().getModel().getGlobe();
            // ElevationModel m = globe.getElevationModel();
            ElevationModel m = WorldWindDataView.getElevationModel();

            int size = getSampleSize();
            double deltaLat = (endLat - startLat) / (size - 1);
            double deltaLon = (endLon - startLon) / (size - 1);
            double lat = startLat;
            double lon = startLon;
            for (int i = 0; i < size; i++) {
                setSample(i, m.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon)) / 1000.0d);
                lat += deltaLat;
                lon += deltaLon;
            }

            // Set to new range
            setRange(x.getRange());
        }
    }

    /**
     * Fixed grid draws a background of vslice at current zoom level. We render
     * the vslice grid directly in the render function of the plot. Why not use
     * a JFreeChart block renderer? Well we want a set grid resolution, but
     * 'infinite' sampling resolution as we zoom in. Freechart has a 'fixed'
     * data grid. However we do call super here so that we can add regular
     * freechart stuff over our vslice. Think of the vslice as a very special
     * background to the plot.
     */
    public static class VSliceFixedGridPlot extends XYPlot {

        private TextTitle myText;
        private ProductVolume myVolume;
        private String myCurrentVolumeValueName;
        private LLHArea myLLHArea;
        private FilterList myList;
        private TerrainXYZDataset myTerrain;
        /**
         * The buffer for holding onto our 2D slice output data
         */
        private VolumeSlice2DOutput my2DSlice = new VolumeSlice2DOutput();
        private VolumeSliceInput myInput;

        private VSliceFixedGridPlot(TerrainXYZDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
            super(dataset, domainAxis, rangeAxis, renderer);
            myTerrain = dataset;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        public void setCurrentVolumeValueName(String name) {
            myCurrentVolumeValueName = name;
        }

        /**
         * Our pre draw function, should be called before rendering chart.
         * Currently called during drawBackground
         */
        public void predraw(Graphics2D g2, Rectangle2D dataArea) {

            // Calculate current zoomed subgrid here...
            VolumeSliceInput subGrid = calculateSubGrid(myLLHArea, dataArea,
                    getDomainAxisForDataset(0), getRangeAxisForDataset(0), myNumRows, myNumCols);

            // -----------------------------------------------------
            // Update the terrain dataset
            // This is actually drawn in the 'draw' method
            if (subGrid != null) {
                ValueAxis rangeAxis = getDomainAxisForDataset(0);
                myTerrain.syncToRange(rangeAxis, subGrid.startLat,
                        subGrid.startLon,
                        subGrid.endLat,
                        subGrid.endLon);
            } else {
                myTerrain.clearRange();
            }

            // -----------------------------------------------------
            // Update the text showing start/end points
            if (subGrid != null) {
                String newKey = getGISLabel(subGrid.startLat, subGrid.startLon,
                        subGrid.endLat, subGrid.endLon);
                myText.setText(newKey);
            } else {
                myText.setText("Need at least 2 points in 3D world for a VSlice");
            }

            // -----------------------------------------------------
            // Update and render the 2D VSlice colored grid
            // could 'separate' update from render I guess..
            // We'd have to hold onto the subGrid to draw later...
            updateAndDrawVSliceGrid(subGrid, g2, dataArea);
        }

        // @Override
        // public void draw(Graphics2D g, Rectangle2D area, Point2D p, PlotState state, PlotRenderingInfo info) {
        // drawVSliceGrid(g, 
        //        info.getDataArea());
        //     super.draw(g, area, p, state, info);
        // }
        @Override
        public void drawBackground(Graphics2D g2, Rectangle2D dataArea) {
            predraw(g2, dataArea);
        }

        /**
         * Update the colored grid to give subGrid and render
         */
        public void updateAndDrawVSliceGrid(VolumeSliceInput subGrid, Graphics2D g2, Rectangle2D dataArea) {

            StopWatch watch = new StopWatch();
            watch.start();
            my2DSlice.setValid(false);  // Force new of data...bad...
            myInput = null;
            if (subGrid != null) {
                // -----------------------------------------------------
                // Draw the vslice grid
                if (myVolume != null) {

                    VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
                    if (v != null) {
                        myCurrentVolumeValueName = v.getName();
                    }
                    myInput = subGrid;
                    myVolume.generate2DGrid(subGrid, my2DSlice, myList, false, v);
                    int[] data = my2DSlice.getColor2dFloatArray(0);

                    // Render the dynamic 'grid' of data. Note that unlike
                    // JFreeChart we dynamically resample our dataset based
                    // upon the 'zoom' level
                    int numOfCols = my2DSlice.getCols();
                    int numOfRows = my2DSlice.getRows();
                    double stepX = dataArea.getWidth() / numOfCols;
                    double stepY = dataArea.getHeight() / numOfRows;
                    double atX;
                    double atY = dataArea.getY();
                    int stepColor = 0;

                    // We want antialiasing turned OFF for the rectangle pass
                    // since it slows us down too much and doesn't change appearance
                    RenderingHints rhints = g2.getRenderingHints();
                    boolean antiOn = rhints.containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);

                    try {
                        for (int r = 0; r < numOfRows; r++) {
                            atX = dataArea.getX();
                            for (int c = 0; c < numOfCols; c++) {
                                // g2.setColor(new Color(data[stepColor], data[stepColor + 1], data[stepColor + 2]));

                                int pixel = data[stepColor];
                                // Don't want bytes because bytes are signed, and there isn't space
                                // so that a 1000 0000 is seen as -127 instead of 128
                                int red = (pixel >>> 16) & 0xFF;
                                int green = (pixel >>> 8) & 0xFF;
                                int blue = (pixel & 0xFF);           // -128 WRONG, because byte is signed in the 8th bit..need more room
                                g2.setColor(new Color(
                                        red,
                                        green,
                                        blue));

                                // g2.setColor(new Color(255,0,0));

                                // +2 to cover the round off due to doubles
                                g2.fillRect((int) atX, (int) atY, (int) stepX + 2, (int) stepY + 2);

                                //stepColor += 3;
                                stepColor += 1;
                                atX += stepX;
                            }
                            atY += stepY;
                        }
                    } catch (Exception e) {
                        // An exception during drawing hangs the GUI thread...
                        LOG.debug("Exception during vslice renderering " + e.toString());
                    }

                    // Restore anti for text/etc done in overlay
                    if (antiOn) {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                    }
                }
            }
            watch.stop();
            LOG.debug("FREECHART VSLICE RENDER TIME IS " + watch);
        }

        private void setLLHArea(LLHArea area) {
            myLLHArea = area;
        }

        private void setPlotData(LLHArea llhArea, ProductVolume volume, FilterList list) {
            myVolume = volume;
            myLLHArea = llhArea;
            myList = list;
        }
    }

    /**
     * Static method to create a vslice chart, called by reflection
     */
    public static VSliceChart create() {

        // Our special axis
        FixedRangeNumberAxis distanceAxis = FixedRangeNumberAxis.getStockAxis("Distance KM", true);
        FixedRangeNumberAxis heightAxis = FixedRangeNumberAxis.getStockAxis("Height KM", true);

        // The terrain dataset generator
        TerrainXYZDataset ds = new TerrainXYZDataset();
        // XYLineAndShapeRenderer arLine = new XYLineAndShapeRenderer();
        // arLine.setSeriesOutlinePaint(0, Color.GREEN);
        // arLine.setSeriesFillPaint(0, Color.GREEN);
        // arLine.setSeriesPaint(0, Color.BLUE);

        // The terrain renderer
        XYAreaRenderer ar = new XYAreaRenderer(XYAreaRenderer.AREA);
        ar.setOutline(true);
        ar.setSeriesPaint(0, new Color(34, 139, 034));  // ForestGreen
        ar.setSeriesOutlinePaint(0, Color.RED);         // Red outline
        ar.setSeriesOutlineStroke(0, new BasicStroke(2));

        // a 'fake' dataset since we bypass the normal renderer...
        VSliceFixedGridPlot plot = new VSliceFixedGridPlot(ds, distanceAxis, heightAxis, ar);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        VSliceChart chart = new VSliceChart("Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.myJFreeChart.setBackgroundPaint(Color.white);
        chart.myJFreeChart.removeLegend();
        chart.myDistanceAxis = distanceAxis;
        chart.myHeightAxis = heightAxis;
        chart.myJFreeChart.setBorderVisible(true);
        return chart;
    }

    public boolean haveData() {
        return ((myChartPanel != null) && (myPlot != null)
                && myPlot.my2DSlice.isValid());
    }

    /**
     * When mouse is moved, update the feature position... this effects all view
     * synchronization
     */
    @Override
    public void handleMouseMoved(MouseEvent e) {
        // Making public for moment...
        if ((myChartPanel != null) && (myPlot != null)) {
            boolean haveData = myPlot.my2DSlice.isValid();
            if (haveData) {
                Rectangle2D b = myChartPanel.getScreenDataArea();
                final int y = (int) b.getY();
                final int x = (int) b.getX();
                final int h = (int) b.getHeight();
                final int w = (int) b.getWidth();

                int mx = e.getX();
                int my = e.getY();
                if ((w > 1) // need for divide by w-1
                        && (h > 1)
                        && (mx >= x)
                        && (my >= y)
                        && (mx < x + w)
                        && (my < y + h)) {
                    // Calculate the Lat/Lon value from X/Y mouse position...
                    //myPlot.myInput.cols; // resolution
                    // Columns span multiple pixels....find column we're in
                    double percentX = (mx - x) / (1.0 * (w - 1));
                    double percentY = (my - y) / (1.0 * (h - 1));
                    int col = (int) (percentX * myPlot.myInput.cols);
                    int row = (int) (percentY * myPlot.myInput.rows);

                    float lat = (float) myPlot.myInput.getLatDegrees(col);
                    float lon = (float) myPlot.myInput.getLonDegrees(col);
                    float ht = (float) myPlot.myInput.getHeightKMS(row);
                    FeaturePosition f = new FeaturePosition(lat, lon, ht);
                    FeatureList.getFeatureList().setTrackingPosition(f);
                }
            }
        }
    }

    /**
     * Draw the mouse overlay (readout) probably
     */
    @Override
    public void paintMouseOverlay(Graphics2D gd, ChartPanel pnl) {
        Rectangle2D b = pnl.getScreenDataArea();
        final int y = (int) b.getY();
        final int x = (int) b.getX();
        final int h = (int) b.getHeight();
        final int w = (int) b.getWidth();
        final double wf = b.getWidth();
        final double wh = b.getHeight();
        final double xf = b.getX();
        final double yf = b.getY();

        FeaturePosition f = FeatureList.getFeatureList().getTrackingPosition();

        if (f == null) {
            return;
        }

        // Take position and convert to the 'grid' of our view....
        myMouseY = 10;
        myMouseX = 10;
        if (myPlot != null) {

            // float lat = (float) myPlot.myInput.getLatDegrees(0);
            //        float lon = (float) myPlot.myInput.getLonDegrees(0);
            //        float ht = (float) myPlot.myInput.getHeightKMS(0);
            if (myPlot.myInput == null) {
                return;
            }
            double p = myPlot.myInput.getPercentOfLat(f.latDegrees);
            myMouseX = (int) (x + (p * w));

            double p2 = myPlot.myInput.getPercentOfHeight(f.elevKM);
            myMouseY = (int) (y + h - (p2 * h));

            // Check to see mouse is inside the actual vslice chart area
            if ((myMouseX > x)
                    && (myMouseY > y)
                    && (myMouseX < x + w)
                    && (myMouseY < y + h)) {

                // Draw a crosshair...
                // Fixme: make this pretty with stroke
                gd.setColor(Color.BLUE);
                gd.setStroke(new BasicStroke(
                        1f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        1f,
                        new float[]{2f},
                        0f));
                // vertical
                gd.drawLine(myMouseX, y + 5, myMouseX, y + h - 5);
                // horizontal
                gd.drawLine(x + 5, myMouseY, x + w - 5, myMouseY);
                gd.setStroke(new BasicStroke(1));

                // Draw readout text.

                boolean haveData = myPlot.my2DSlice.isValid();
                if (haveData) {

                    // FIXME: Readout will have to be more advanced,
                    // getting the data format/unit etc from the actual
                    // product....first pass just want something working
                    float[] data = myPlot.my2DSlice.getValue2dFloatArray(0);

                    // Render the dynamic 'grid' of data. Note that unlike
                    // JFreeChart we dynamically resample our dataset based
                    // upon the 'zoom' level
                    int numOfCols = myPlot.my2DSlice.getCols();
                    int numOfRows = myPlot.my2DSlice.getRows();

                    // Actually need double accuracy, kinda annoying but it's
                    // because the 'grid' is broken up with double precision
                    // in rendering
                    double pixelPerCol = wf / numOfCols;
                    double pixelPerRow = wh / numOfRows;

                    int myRow = (int) ((myMouseY - yf) / pixelPerRow);
                    int myCol = (int) ((myMouseX - xf) / pixelPerCol);

                    // Draw an 'outline' around the cell of the slice
                    int boxx = (int) (xf + (myCol * pixelPerCol));
                    int boxy = (int) (yf + (myRow * pixelPerRow));
                    gd.drawRect(boxx, boxy, (int) pixelPerCol, (int) pixelPerRow);

                    gd.setColor(Color.WHITE);
                    float value = 0;
                    int index = (myRow * numOfCols) + myCol;
                    if (index < data.length) {
                        value = data[index];
                    }
                    // FIXME: needs a lot of work..should depend on product right?
                    //String out = String.format("%f at (%d, %d)", value, myRow, myCol);
                    float lat = (float) myPlot.myInput.getLatDegrees(myCol);
                    float lon = (float) myPlot.myInput.getLonDegrees(myCol);
                    float hkms = (float) myPlot.myInput.getHeightKMS(myRow);
                    // FeaturePosition f = new FeaturePosition(lat, lon, hkms);
                    //  FeatureList.theFeatures.setTrackingPosition(f);

                    String out = String.format("%f (%f, %f, %f)", value, lat, lon, hkms);
                    gd.drawString(out, myMouseX, myMouseY);

                }
            }
        }

    }

    /**
     * Get extra menu items for the chart
     */
    @Override
    public void addCustomTitleBarComponents(java.util.List<Object> addTo) {
        // Virtual Volume toggle
        jVirtualToggleButton = new JToggleButton();
        Icon i = SwingIconFactory.getIconByName("brick_add.png");
        jVirtualToggleButton.setIcon(i);
        jVirtualToggleButton.setToolTipText("Toggle virtual/nonvirtual volume");
        jVirtualToggleButton.setFocusable(false);
        jVirtualToggleButton.setBorderPainted(false);
        jVirtualToggleButton.setOpaque(false);
        jVirtualToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jVirtualToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jVirtualToggleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jVirtualToggleButtonActionPerformed(evt);
            }
        });
        addTo.add(jVirtualToggleButton);

        addTo.add(VolumeValueCommand.getDropButton(this));
    }

    private void jVirtualToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        VolumeSetTypeCommand vToggle = new VolumeSetTypeCommand(this, selected);

        vToggle.setToggleState(selected);
        CommandManager.getInstance().executeCommand(vToggle, true);
    }
  
    /**
     * Get a volume key for this slice, either virtual or nonvirtual volume
     */
    public String getVolumeKey(String follow, boolean useVirtual) {
        // Add the key of the volume....
        String newKey = "";
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, useVirtual);
        newKey += volume.getKey();	// java 6 StringBuilder is internally used...
        return newKey;
    }

    /**
     * Get a unique key representing all states. Used by charts and 3d slice to
     * tell unique vslice. Note the parameters are passed in because different
     * things are in different states...
     *
     * @param virtual
     * @param useFilters
     * @return
     */
    public String getKey(java.util.List<LLD_X> locations, String follow, boolean virtual, FilterList list, boolean useFilters) {
        // Start with GIS location key
        String newKey = getGISKey(locations);

        // Add the key of the volume....
        newKey += getVolumeKey(follow, virtual);
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, virtual);
        newKey += volume.getKey();	// java 6 StringBuilder is internally used...

        // Add filter settings if wanted
        if (list != null) {
            newKey += list.getFilterKey(useFilters);
        }
        return newKey;
    }

    /**
     * Get a key that represents the GIS location of this slice
     */
    public String getGISKey(java.util.List<LLD_X> locations) {
        // Add location and altitude...
        //java.util.List<LatLon> locations = getLocationList();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < locations.size(); i++) {
            LLD_X l = locations.get(i);
            buf.append(l.latDegrees());
            buf.append(':');
            buf.append(l.lonDegrees());
            buf.append(':');

        }
        buf.append(myCurrentGrid.bottomHeight);
        buf.append(myCurrentGrid.topHeight);
        String newKey = buf.toString();
        return newKey;
    }

    protected void orderLocations(java.util.List<LLD_X> input) {
        // VSlice only.  Two locations, the points on the bottom. 
        // Make sure the east one is right of the west one...
        LLD_X l1 = input.get(0);
        LLD_X l2 = input.get(1);
        LLD_X leftBottom, rightBottom;
        if (l1.lonDegrees() < l2.lonDegrees()) {
            leftBottom = l1;
            rightBottom = l2;
        } else {
            leftBottom = l2;
            rightBottom = l1;
        }
        myLeftLocation = leftBottom;
        myRightLocation = rightBottom;
    }

    /**
     * Update the current grid that is the GIS location of the slice
     */
    public ArrayList<LLD_X> updateCurrentGrid(java.util.List<LLD_X> locations) {

        // Generate the 3D VSlice in the window, and the 2D slice for charting...
        orderLocations(locations);
        double startLat = myLeftLocation.latDegrees();
        double startLon = myLeftLocation.lonDegrees();
        double endLat = myRightLocation.latDegrees();
        double endLon = myRightLocation.lonDegrees();

        myCurrentGrid.startLat = startLat;
        myCurrentGrid.startLon = startLon;
        myCurrentGrid.endLat = endLat;
        myCurrentGrid.endLon = endLon;
        ArrayList<LLD_X> orderedList = new ArrayList<LLD_X>();
        orderedList.add(myLeftLocation);
        orderedList.add(myRightLocation);
        return orderedList;
    }

    @Override
    public void setTrackingPosition(FeatureList fl, FeaturePosition f) {
        // Bleh repaint overlay is all we need...
        // FIXME: just redo overlay?
        repaint();

    }

    @Override
    public void repaint() {
        if (myChartPanel != null) {
            myChartPanel.repaint();
        }
    }
}