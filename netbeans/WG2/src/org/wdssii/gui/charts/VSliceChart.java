package org.wdssii.gui.charts;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.GeometryBuilder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import javax.media.opengl.GL;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.commands.VolumeValueCommand;
import org.wdssii.gui.commands.VolumeValueCommand.VolumeValueFollowerView;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeatureFilter;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.products.*;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.gui.volumes.VSliceRenderer;

public class VSliceChart extends ChartViewJFreeChart implements VolumeValueFollowerView {

    private static Logger log = LoggerFactory.getLogger(VSliceChart.class);
    private ProductVolume myVolume = null;
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
    public static final int myNumRows = 50;  //50
    /**
     * The number of cols or change in Lat/Lon
     */
    public static final int myNumCols = 100; //100
    /**
     * Holder for the slice GIS 'state'
     */
    private VolumeSliceInput myCurrentGrid =
            new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
            0, 0, 0, 50);
    private VolumeSliceInput my2DCurrentGrid =
            new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
            0, 0, 0, 50);
    private LatLon myLeftLocation;
    private LatLon myRightLocation;
    /**
     * How many times to 'split' the raw fill or outline of slice. This isn't
     * the same as the vslice resolution.
     */
    private int subdivisions = 4;  // power of 2 breakdown of side..
    private VSliceRenderer myRenderer = new VSliceRenderer();
    private ProductVolume myVolumeProduct = null;
    private VolumeSlice3DOutput myGeometry = new VolumeSlice3DOutput();
    private String myCacheKey = "";
    private GeometryBuilder geometryBuilder = new GeometryBuilder();

    // end VSlice 3d
    // Volume value follower
    @Override
    public void setCurrentVolumeValue(String changeTo) {
        myCurrentVolumeValue = changeTo;
        if (myVolume != null) {
            regenerateSlice(true); // Force update
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
     * A NumberAxis that forces auto range (zoom out or menu picked) to be the
     * default height/range of the LLHAreaSlice we are following
     */
    public static class VSliceNumberAxis extends NumberAxis {

        private Range myVSliceRange = null;
        private boolean myAllowNegative = true;

        public VSliceNumberAxis(String label, boolean allowNegative) {
            super(label);
            myAllowNegative = allowNegative;
        }

        public void setVSliceRange(Range aRange) {
            myVSliceRange = aRange;
            setRange(myVSliceRange);
        }

        /**
         * Zoom out auto-adjust should go to the FULL vslice we're following..
         */
        @Override
        protected void autoAdjustRange() {
            if (myVSliceRange != null) {
                setRange(myVSliceRange, false, false);
            }
        }

        @Override
        public Range getRange() {
            Range sRange = super.getRange();
            double min;
            if (myAllowNegative) {
                min = sRange.getLowerBound();
            } else {
                min = Math.max(0, sRange.getLowerBound());
            }
            // ensure lowerBound < upperBound to prevent exception
            return new Range(
                    min, Math.max(1e-8, sRange.getUpperBound()));
        }

        @Override
        public double getLowerBound() {
            Range r = getRange();
            return r.getLowerBound();
        }
    }
    // static boolean toggle = false;
    /**
     * The axis for range in KM of the VSlice bottom distance
     */
    private VSliceNumberAxis myRangeAxis;
    /**
     * The axis for the height of the VSlice side distance
     */
    private VSliceNumberAxis myHeightAxis;
    static int counter = 0;
    /**
     * The current full key for us
     */
    private String myCurrentKey;
    /**
     * The last GIS key. This is the key part that deals with the 'shape' of the
     * VSlice without product/volume
     */
    private String myGISKey = "";
    private VSliceFixedGridPlot myPlot = null;

    public VSliceChart(String arg0, Font arg1, VSliceFixedGridPlot arg2, boolean arg3) {
        myJFreeChart = new JFreeChart(arg0, arg1, arg2, arg3);
        myPlot = arg2;
        myPlot.myText = new TextTitle("", new Font("Dialog", Font.PLAIN, 11));
        myPlot.myText.setPosition(RectangleEdge.BOTTOM);
        myPlot.myText.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        myJFreeChart.addSubtitle(myPlot.myText);
        created = this;  // temp hack for LLHArea rendering
    }

    /**
     * Return an LLHAreaFeature that contains a LLHAreaSlice
     */
    public static class VSliceFilter implements FeatureFilter {

        @Override
        public boolean matches(Feature f) {
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature a = (LLHAreaFeature) f;
                LLHArea area = a.getLLHArea();
                if (area instanceof LLHAreaSet) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return the LLHAreaSlice that we are currently drawing a plot for
     */
    public LLHAreaSet getVSliceToPlot() {
        // -------------------------------------------------------------------------
        // Hack snag the current slice and product...
        // Hack for now....we grab first 3d object in our FeatureList that is vslice
        // This has the effect of following the top selected vslice...

        LLHAreaSet slice = null;
        LLHAreaFeature f = FeatureList.theFeatures.getTopMatch(new VSliceFilter());
        if (f != null) {
            LLHArea area = f.getLLHArea();
            if (area instanceof LLHAreaSet) {
                slice = (LLHAreaSet) (area);
            }
        }
        return slice;
    }

    /**
     * Called during dragging of vslice to explicitly update the chart. The
     * chart checks for changes and only draws when the vslice if different
     */
    @Override
    public void updateChart() {
        regenerateSlice(false);
    }

    /**
     * Return info for a segment
     */
    public static VolumeSliceInput getSegmentInfo(LLHArea slice, VolumeSliceInput buffer, int segment) {
        java.util.List<LatLon> l = slice.getLocations();
        if (l.size() > segment + 1) {
            double[] altitudes = slice.getAltitudes();
            ArrayList<LatLon> list = getVSliceLocations(slice.getLocations());
            if (buffer != null) {
                buffer.set(myNumRows, myNumCols, list.get(0).latitude.degrees, list.get(0).longitude.degrees,
                        list.get(1).latitude.degrees, list.get(1).longitude.degrees,
                        altitudes[0], altitudes[1]);
                return buffer;
            } else {
                return new VolumeSliceInput(myNumRows, myNumCols, list.get(0).latitude.degrees, list.get(0).longitude.degrees,
                        list.get(1).latitude.degrees, list.get(1).longitude.degrees,
                        altitudes[0], altitudes[1]);
            }
        } else {
            return null;
        }
    }

    /**
     * Given a lat lon list, return vslice order
     */
    protected static ArrayList<LatLon> getVSliceLocations(java.util.List<LatLon> input) {

        ArrayList<LatLon> out = null;
        if (input.size() > 1) {
            out = new ArrayList<LatLon>();
            LatLon l1 = input.get(0);
            LatLon l2 = input.get(1);
            LatLon leftBottom, rightBottom;
            if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
                leftBottom = l1;
                rightBottom = l2;
            } else {
                leftBottom = l2;
                rightBottom = l1;
            }
            out.add(leftBottom);
            out.add(rightBottom);
        }
        return out;
    }

    /**
     * Get a pretty GIS label for rendering in a chart, for instance
     */
    /* public String getGISLabel() {
     LatLon left = getLeftLocation();
     LatLon right = getRightLocation();
     return getGISLabel(left.getLatitude().degrees,
     left.getLongitude().degrees,
     right.getLatitude().degrees,
     right.getLongitude().degrees);
     }*/
    public static String getGISLabel(double startLat, double startLon, double endLat, double endLong) {
        String newKey = String.format(
                "(%5.2f, %5.2f)                        (%5.2f, %5.2f)",
                startLat, startLon, endLat, endLong);
        return newKey;
    }

    public void regenerateSlice(boolean force) {
        // The LLHAreaSlice is the geometry in the 3d window we are
        // matching our coordinates to.  It can be valid without
        // any product/volume information.
        myPlot.setCurrentVolumeValueName(myCurrentVolumeValue);
        LLHAreaSet slice = getVSliceToPlot();
        if (slice == null) {
            // If there isn't a 3D slice LLHArea object geometry to follow,
            // clear us...
            myVolume = null;
            myPlot.setVolumeAndSlice(null, null, null);
            myJFreeChart.setTitle("No slice in 3d window");
            myJFreeChart.fireChartChanged();
            return;
        }   // No slice to follow, return..
        myPlot.setSlice(slice);

        /**
         * Get the GIS key of the slice
         */
        String gisKey = slice.getGISKey();

        // Sync the height/range axis to the GIS vslice range when updated, this resets
        // any 'zoom' in the chart...but only if GIS key has CHANGED.  This
        // way users can toggle products and keep zoom level for comparison,
        // but if they drag the vslice we reset to full slice.
        if (!myGISKey.equals(gisKey)) {
            VolumeSliceInput info = getSegmentInfo(slice, myCurrentGrid, 0);
            if (info != null) {
                myRangeAxis.setVSliceRange(new Range(0, slice.getRangeKms(0, 1) / 1000.0));
                myHeightAxis.setVSliceRange(new Range(slice.getBottomHeightKms() / 1000.0, slice.getTopHeightKms() / 1000.0));
                myPlot.myText.setText(getGISLabel(myCurrentGrid.startLat, myCurrentGrid.startLon,
                        myCurrentGrid.endLat, myCurrentGrid.endLon));

            }//else what?
        }
        myGISKey = gisKey;

        /**
         * Get the volume we are following
         */
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());
        // if (volume == null) {
        //     return;
        // }

        FilterList aList = null;
        String useKey = getUseProductKey();
        String titleKey = "";
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
            myPlot.setVolumeAndSlice(slice, null, null);
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
        if (!key.equals(myCurrentKey)) {
            keyDifferent = true;
        }
        myCurrentKey = key;

        if (!force && !keyDifferent) {
            return;
        }

        // if (keyDifferent) {
        //     System.out.println("VSLICE KEY CHANGE");
        // }
        myVolume = volume;
        // Try to keep the setting PER chart...note if volume
        // type changed it will go to last setting for that type
        if (myVolume != null) {
            //	myVolume.setCurrentVolumeValue(myCurrentVolumeValue);
        }
        myPlot.setVolumeAndSlice(slice, volume, aList);
        myJFreeChart.setTitle(titleKey);
        myJFreeChart.fireChartChanged();
    }

    /**
     * Terrain XYZDataset is a JFreeChart dataset where we sample the terrain
     * 'height' along the range.
     */
    public static class TerrainXYZDataset implements XYZDataset {

        /**
         * Sample size of the terrain by range
         */
        public int sampleSize = 201;
        private boolean showHeights;
        private double[] myHeights = new double[sampleSize];
        /**
         * The full range of the terrain line
         */
        private double rangeKM;
        /**
         * The 'delta' range per sample point of range line
         */
        private double rangeDelta;
        /**
         * The starting range location (left side) of range line
         */
        private double rangeLower;

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
            Range r = x.getRange();
            rangeLower = r.getLowerBound();
            rangeKM = r.getUpperBound() - r.getLowerBound();
            rangeDelta = rangeKM / (sampleSize - 1);
            WorldWindView eb = FeatureList.theFeatures.getWWView();
            Globe globe = eb.getWwd().getModel().getGlobe();
            ElevationModel m = globe.getElevationModel();
            double deltaLat = (endLat - startLat) / (sampleSize - 1);
            double deltaLon = (endLon - startLon) / (sampleSize - 1);
            double lat = startLat;
            double lon = startLon;
            for (int i = 0; i < sampleSize; i++) {
                myHeights[i] = (m.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon))) / 1000.0d;
                lat += deltaLat;
                lon += deltaLon;
            }
            showHeights = true;
        }

        public void clearRange() {
            showHeights = false;
        }

        @Override
        public Number getZ(int series, int item) {

            // This number is meaningless since the 'value' is
            // the same as the height, we might find a use for this later
            return 0;
        }

        @Override
        public double getZValue(int series, int item) {
            return new Double(getZValue(series, item));
        }

        @Override
        public DomainOrder getDomainOrder() {
            return DomainOrder.ASCENDING;
        }

        @Override
        public int getItemCount(int i) {
            if (showHeights) {
                return sampleSize;
            } else {
                return 0;
            }
        }

        @Override
        public Number getX(int series, int item) {
            return new Double(getXValue(series, item));
        }

        @Override
        public double getXValue(int series, int item) {
            double range = rangeLower + (rangeDelta * item);
            return range;
        }

        @Override
        public Number getY(int series, int item) {
            return new Double(getYValue(series, item));
        }

        @Override
        public double getYValue(int series, int item) {
            return myHeights[item];
        }

        @Override
        public int getSeriesCount() {
            return 1; // just 1 terrain height per range
        }

        @Override
        public Comparable getSeriesKey(int i) {
            return "Terrain";
        }

        @Override
        public int indexOf(Comparable cmprbl) {
            return 0;
        }

        @Override
        public void addChangeListener(DatasetChangeListener dl) {
            // ignore
        }

        @Override
        public void removeChangeListener(DatasetChangeListener dl) {
            // ignore
        }

        @Override
        public DatasetGroup getGroup() {
            return null;
        }

        @Override
        public void setGroup(DatasetGroup dg) {
            // ignore
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
        private LLHArea mySlice;
        private FilterList myList;
        private TerrainXYZDataset myTerrain;
        /**
         * The buffer for holding onto our 2D slice output data
         */
        private VolumeSlice2DOutput my2DSlice = new VolumeSlice2DOutput();

        private VSliceFixedGridPlot(TerrainXYZDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
            super(dataset, domainAxis, rangeAxis, renderer);
            myTerrain = dataset;
        }

        public void setCurrentVolumeValueName(String name) {
            myCurrentVolumeValueName = name;
        }

        @Override
        public void draw(Graphics2D g, Rectangle2D area, Point2D p, PlotState state, PlotRenderingInfo info) {
            super.draw(g, area, p, state, info);
        }

        @Override
        public void drawBackground(Graphics2D g2, Rectangle2D dataArea) {
            // Calculate the 'zoomed' lat/lon for our followed slice
            my2DSlice.setValid(false);
            if (mySlice != null) {
                VolumeSliceInput sourceGrid = getSegmentInfo(mySlice, null, 0);
                if (sourceGrid != null) {
                    VolumeSliceInput subGrid = new VolumeSliceInput(sourceGrid);

                    // Hummm..which index do we draw on?
                    ValueAxis rangeAxis = getDomainAxisForDataset(0);
                    ValueAxis heightAxis = getRangeAxisForDataset(0);
                    double bottomKms = heightAxis.getLowerBound() * 1000.0;
                    double topKms = heightAxis.getUpperBound() * 1000.0;
                    double leftKms = rangeAxis.getLowerBound() * 1000.0;
                    double rightKms = rangeAxis.getUpperBound() * 1000.0;

                    // modify the fullGrid to the 'zoomed' subview.  We can tell
                    // this by looking at the axis range and height.

                    // Zoom height is easy:
                    subGrid.bottomHeight = bottomKms;
                    subGrid.topHeight = topKms;

                    // Figure out number of rows/cols for a 'pixel' size
                    // Maximum resolution is 1 pixel per row/col..
                    // FIXME: maybe configurable if too 'slow' on a system
                    final double pixelR = 1.0f;
                    final double pixelC = 1.0f;
                    subGrid.rows = (int) (dataArea.getHeight() / pixelR);
                    subGrid.cols = (int) (dataArea.getWidth() / pixelC);

                    // Maximum rows/col for speed?
                    // Doing 16:9 ratio.  This should at least be ignore when
                    // saving as an image in order to get the best detail.
                    if (subGrid.rows > 150) {
                        subGrid.rows = 150;
                    }
                    if (subGrid.cols > 200) {
                        subGrid.cols = 200;
                    }

                    // System.out.println("ROWS/COLS "+subGrid.rows+", "+subGrid.cols+
                    //       " {"+dataArea.getHeight()+","+dataArea.getWidth());
                    // Range is harder....since this changes startlat/lon,
                    // and end lat/lon by a percentage of the range values...
                    // so range is assumed at '0--> R'
                    // We assume 'delta' change in lat/lon from start to end is linear based on
                    // the fullRange...
                    final double sLat = subGrid.startLat;
                    final double sLon = subGrid.startLon;
                    double fullRange = mySlice.getRangeKms(0, 1);  // 100% say 100 KMS
                    double deltaLatPerKm = (subGrid.endLat - sLat) / fullRange;
                    double deltaLonPerKm = (subGrid.endLon - sLon) / fullRange;

                    // Now adjust the start/end lat/lon by percentage
                    subGrid.startLat = sLat + (leftKms * deltaLatPerKm);
                    subGrid.endLat = sLat + (rightKms * deltaLatPerKm);
                    subGrid.startLon = sLon + (leftKms * deltaLonPerKm);
                    subGrid.endLon = sLon + (rightKms * deltaLonPerKm);
                    String newKey = getGISLabel(subGrid.startLat, subGrid.startLon,
                            subGrid.endLat, subGrid.endLon);
                    myText.setText(newKey);
                    myTerrain.syncToRange(rangeAxis, subGrid.startLat,
                            subGrid.startLon,
                            subGrid.endLat,
                            subGrid.endLon);

                    if (myVolume != null) {

                        VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
                        if (v != null) {
                            myCurrentVolumeValueName = v.getName();
                        }
                        myVolume.generate2DGrid(subGrid, my2DSlice, myList, false, v);
                        int[] data = my2DSlice.getColor2dFloatArray(0);

                        // Render the dynamic 'grid' of data. Note that unlike
                        // JFreeChart we dynamically resample our dataset based
                        // upon the 'zoom' level
                        int numOfCols = my2DSlice.getCols();
                        int numOfRows = my2DSlice.getRows();
                        double stepX = dataArea.getWidth() / numOfCols;
                        double stepY = dataArea.getHeight() / numOfRows;
                        double atX = dataArea.getX();
                        double atY = dataArea.getY();
                        int stepColor = 0;

                        // We want antialiasing turned OFF for the rectangle pass
                        // since it slows us down too much and doesn't change appearance
                        RenderingHints rhints = g2.getRenderingHints();
                        boolean antiOn = rhints.containsValue(RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);

                        for (int r = 0; r < numOfRows; r++) {
                            atX = dataArea.getX();
                            for (int c = 0; c < numOfCols; c++) {
                                try {
                                    g2.setColor(new Color(data[stepColor], data[stepColor + 1], data[stepColor + 2]));
                                    // +2 to cover the round off due to doubles
                                    g2.fillRect((int) atX, (int) atY, (int) stepX + 2, (int) stepY + 2);
                                } catch (Exception e) {
                                    // Because if it's off it hangs all drawing... FIXME:
                                    // System.out.println("EXCEPTION WAS AT " + r + ", " + c + " with size " + data.length);
                                }
                                stepColor += 3;
                                atX += stepX;
                            }
                            atY += stepY;
                        }

                        // Restore anti for text/etc done in overlay
                        if (antiOn) {
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                        }
                    }
                }
            } else {
                myTerrain.clearRange(); // No geometry to follow
            }
        }

        private void setSlice(LLHArea slice) {
            mySlice = slice;
        }

        private void setVolumeAndSlice(LLHArea slice, ProductVolume volume, FilterList list) {
            myVolume = volume;
            mySlice = slice;
            myList = list;
        }
    }

    /**
     * Static method to create a vslice chart, called by reflection
     */
    public static VSliceChart createVSliceChart() {

        // The range in KM for the VSlice
        VSliceNumberAxis rangeAxis = new VSliceNumberAxis("Range KM", true);
        rangeAxis.setLowerMargin(0.0);
        rangeAxis.setUpperMargin(0.0);
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setAxisLineVisible(true);

        // The height in KM for the VSlice
        VSliceNumberAxis heightAxis = new VSliceNumberAxis("Height KM", true);
        heightAxis.setLowerMargin(0.0);
        heightAxis.setUpperMargin(0.0);
        heightAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        heightAxis.setAxisLineVisible(true);

        // The terrain overlay for the chart...
        TerrainXYZDataset ds = new TerrainXYZDataset();
        XYLineAndShapeRenderer arLine = new XYLineAndShapeRenderer();
        arLine.setSeriesOutlinePaint(0, Color.GREEN);
        arLine.setSeriesFillPaint(0, Color.GREEN);
        arLine.setSeriesPaint(0, Color.BLUE);

        // The terrain renderer
        XYAreaRenderer ar = new XYAreaRenderer(XYAreaRenderer.AREA);
        ar.setOutline(true);
        ar.setSeriesPaint(0, new Color(34, 139, 034));  // ForestGreen
        ar.setSeriesOutlinePaint(0, Color.RED);         // Red outline
        ar.setSeriesOutlineStroke(0, new BasicStroke(2));

        // a 'fake' dataset since we bypass the normal renderer...
        VSliceFixedGridPlot plot = new VSliceFixedGridPlot(ds, rangeAxis, heightAxis, ar);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        VSliceChart chart = new VSliceChart("Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        /**
         * Make it white (ignore theme color)
         */
        chart.myJFreeChart.setBackgroundPaint(Color.white);
        chart.myJFreeChart.removeLegend();

        chart.myRangeAxis = rangeAxis;
        chart.myHeightAxis = heightAxis;

        // Just a test, we're probably going to have to make our own
        // scale subclass that uses our color map
		/*
         * NumberAxis colorScale = new NumberAxis("Color");
         * colorScale.setAutoRange(true); PaintScaleLegend p = new
         * PaintScaleLegend(scale, colorScale);
         * p.setSubdivisionCount(20);
         * p.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
         * p.setAxisOffset(5D); //	p.setMargin(new RectangleInsets(5D,
         * 5D, 5D, 5D)); p.setFrame(new BlockBorder(Color.red)); //
         * p.setPadding(new RectangleInsets(10D, 10D, 10D, 10D));
         * chart.addSubtitle(p);
         */
        //NumberAxis xAxis3 = new NumberAxis("Number2");
        //	xAxis3.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        //xAxis3.setLowerMargin(0.0);
        //	xAxis3.setUpperMargin(0.0);
        chart.myJFreeChart.setBorderVisible(true);
        //plot.setDomainAxes(new ValueAxis[]{ xAxis, xAxis2, xAxis3 });
        return chart;
    }

    @Override
    public Object getNewGUIBox(Object parent) {
        /*
         * final Composite box = new Composite((Composite) parent,
         * SWT.NONE); box.setLayout(new RowLayout());
         *
         * // SRV Text srv = new Text(box, SWT.LEFT);
         * srv.setText("HeightKMs:"); srv.setEditable(false); final
         * Spinner speedSpin = new Spinner(box, 0);
         * speedSpin.setMaximum(200); speedSpin.setMinimum(-200);
         * speedSpin.setSelection(12);
         *
         * // Add listeners for GUI elements
         * speedSpin.addSelectionListener(new SelectionListener() {
         *
         * @Override public void widgetDefaultSelected(SelectionEvent
         * arg0) { }
         *
         * @Override public void widgetSelected(SelectionEvent arg0) {
         * //myCutOff = speedSpin.getSelection(); //myDirtySRM = true;
         * //CommandManager.getInstance().executeCommand(new
         * ProductChangeCommand.ProductFilterCommand(), true); } });
         *
         * return box;
         *
         */
        return null;
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
            if (myPlot != null) {
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
                    String out = String.format("%f at (%d, %d)", value, myRow, myCol);
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
        addTo.add(VolumeValueCommand.getDropButton(this));
    }

    // --------------------------------------------------------------------
    // Start 3D stuff...
    //
    /**
     * Cache key represented by the vslice we're looking at. When this changes,
     * we know we need to render a new vslice. Takes into account the size/shape
     * of vslice, the volume, and the filters
     *
     * @return
     *
     * This is the current key used to tell if the 3D slice in the earth view
     * needs to be remade or not.....
     */
    private String getNewCacheKey(java.util.List<LatLon> locations) {

        // FIXME: this is wrong....should be the filters for the product we are following....this
        // is the top product.....
        FilterList f = ProductManager.getInstance().getFilterList(get3DProductToFollow());

        // Add the key of the current filter list...
        String newKey = getKey(locations, get3DProductToFollow(), getUseVirtualVolume(), f, true);
        return newKey;
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
    public String getKey(java.util.List<LatLon> locations, String follow, boolean virtual, FilterList list, boolean useFilters) {
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
    public String getGISKey(java.util.List<LatLon> locations) {
        String newKey = "";

        // Add location and altitude...
        //java.util.List<LatLon> locations = getLocationList();
        for (int i = 0; i < locations.size(); i++) {
            LatLon l = locations.get(i);
            newKey = newKey + l.getLatitude() + ":";
            newKey = newKey + l.getLongitude() + ":";
        }
        newKey = newKey + myCurrentGrid.bottomHeight;
        newKey = newKey + myCurrentGrid.topHeight;
        return newKey;
    }

    private String get3DProductToFollow() {

        // This will follow same product as chart which makes sense.
        // FIXME: it 'will'
        return ProductManager.TOP_PRODUCT;
    }

    // It actually makes sense for the 3D stuff to be within the chart as well...
    // This allows different charts to draw in 3D differently...
    // Render in 3D
    @Override
    // All the 3D render stuff of the Chrt
    // Render in 3D
    public void drawChartInLLHArea(DrawContext dc, java.util.List<LatLon> locations, double[] altitudes, java.util.List<Boolean> edgeFlags) {
        if (locations.size() > 1) {
            ArrayList<LatLon> ordered = updateCurrentGrid(locations);
            myCurrentGrid.bottomHeight = altitudes[0];
            myCurrentGrid.topHeight = altitudes[1];
            VolumeSlice3DOutput geom = this.getVSliceGeometry(dc, ordered, edgeFlags);

            myRenderer.drawVSlice(dc, geom);
        }
    }

    protected void orderLocations(java.util.List<LatLon> input) {
        // VSlice only.  Two locations, the points on the bottom. 
        // Make sure the east one is right of the west one...
        LatLon l1 = input.get(0);
        LatLon l2 = input.get(1);
        LatLon leftBottom, rightBottom;
        if (l1.getLongitude().getDegrees() < l2.getLongitude().getDegrees()) {
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
    public ArrayList<LatLon> updateCurrentGrid(java.util.List<LatLon> locations) {

        // Generate the 3D VSlice in the window, and the 2D slice for charting...
        orderLocations(locations);
        double startLat = myLeftLocation.getLatitude().getDegrees();
        double startLon = myLeftLocation.getLongitude().getDegrees();
        double endLat = myRightLocation.getLatitude().getDegrees();
        double endLon = myRightLocation.getLongitude().getDegrees();

        myCurrentGrid.startLat = startLat;
        myCurrentGrid.startLon = startLon;
        myCurrentGrid.endLat = endLat;
        myCurrentGrid.endLon = endLon;
        ArrayList<LatLon> orderedList = new ArrayList<LatLon>();
        orderedList.add(myLeftLocation);
        orderedList.add(myRightLocation);
        return orderedList;
    }

    /**
     * This gets the special fill-in geometry for the vslice, a multi-set of
     * triangles with data value colors.
     *
     * @author Robert Toomey
     *
     * @return
     */
    private VolumeSlice3DOutput getVSliceGeometry(DrawContext dc, java.util.List<LatLon> locations, java.util.List<Boolean> edgeFlags) {
        String newKey = getNewCacheKey(locations);
        // Add exaggeration to cache key so changing exaggeration will redraw it
        newKey += dc.getVerticalExaggeration();
        if (newKey.compareTo(myCacheKey) == 0) {
            return myGeometry;
        }

        // System.out.println("_------------>>> REGENERATE VSLICE!!!");
        myCacheKey = newKey;
        myGeometry.setHaveVSliceData(false);
        this.makeVSlice(dc, locations, edgeFlags, myGeometry);

        // Fire changed event?  Is this enough?
        CommandManager.getInstance().executeCommand(new FeatureCommand(), true);
        return myGeometry;
    }

    protected GeometryBuilder getGeometryBuilder() {
        return this.geometryBuilder;
    }

    protected int computeCartesianPolygon(DrawContext dc, java.util.List<? extends LatLon> locations, java.util.List<Boolean> edgeFlags,
            Vec4[] points, Boolean[] edgeFlagArray, Matrix[] transform) {
        Globe globe = dc.getGlobe();

        // Allocate space to hold the list of locations and location vertices.
        int locationCount = locations.size();

        // Compute the cartesian points for each location.
        for (int i = 0; i < locationCount; i++) {
            LatLon ll = locations.get(i);
            points[i] = globe.computePointFromPosition(ll.getLatitude(), ll.getLongitude(), 0.0);

            if (edgeFlagArray != null) {
                edgeFlagArray[i] = (edgeFlags != null) ? edgeFlags.get(i) : true;
            }
        }

        // Compute the average of the cartesian points.
        Vec4 centerPoint = Vec4.computeAveragePoint(Arrays.asList(points));

        // Test whether the polygon is closed. If it is not closed, repeat the first vertex.
        if (!points[0].equals(points[locationCount - 1])) {
            points[locationCount] = points[0];
            if (edgeFlagArray != null) {
                edgeFlagArray[locationCount] = edgeFlagArray[0];
            }

            locationCount++;
        }

        // Compute a transform that will map the cartesian points to a local coordinate system centered at the average
        // of the points and oriented with the globe surface.
        Position centerPos = globe.computePositionFromPoint(centerPoint);
        Matrix tx = globe.computeModelCoordinateOriginTransform(centerPos);
        Matrix txInv = tx.getInverse();
        // Map the cartesian points to a local coordinate space.
        for (int i = 0; i < locationCount; i++) {
            points[i] = points[i].transformBy4(txInv);
        }

        transform[0] = tx;

        return locationCount;
    }

    private void makeSectionOutlineIndices(int subdivisions, int vertexPos, int indexPos, int[] indices,
            boolean beginEdgeFlag, boolean endEdgeFlag) {
        GeometryBuilder gb = this.getGeometryBuilder();
        int count = gb.getSubdivisionPointsVertexCount(subdivisions);

        int index = indexPos;
        int pos, nextPos;

        if (beginEdgeFlag) {
            pos = vertexPos;
            indices[index++] = pos;
            indices[index++] = pos + 1;
        }

        for (int i = 0; i < count - 1; i++) {
            pos = vertexPos + 2 * i;
            nextPos = vertexPos + 2 * (i + 1);
            indices[index++] = pos;
            indices[index++] = nextPos;
            indices[index++] = pos + 1;
            indices[index++] = nextPos + 1;
        }

        if (endEdgeFlag) {
            pos = vertexPos + 2 * (count - 1);
            indices[index++] = pos;
            indices[index] = pos + 1;
        }
    }

    private int getEdgeFillIndexCount(int count, int subdivisions) {
        return (count - 1) * this.getSectionFillIndexCount(subdivisions);
    }

    private int getEdgeOutlineIndexCount(int count, int subdivisions, Boolean[] edgeFlags) {
        int sum = 0;
        for (int i = 0; i < count - 1; i++) {
            sum += this.getSectionOutlineIndexCount(subdivisions, edgeFlags[i], edgeFlags[i + 1]);
        }

        return sum;
    }

    private int getEdgeVertexCount(int count, int subdivisions) {
        return (count - 1) * this.getSectionVertexCount(subdivisions);
    }

    private int getSectionVertexCount(int subdivisions) {
        GeometryBuilder gb = this.getGeometryBuilder();
        return 2 * gb.getSubdivisionPointsVertexCount(subdivisions);
    }

    private int getSectionFillIndexCount(int subdivisions) {
        GeometryBuilder gb = this.getGeometryBuilder();
        return 6 * (gb.getSubdivisionPointsVertexCount(subdivisions) - 1);
    }

    private int getSectionOutlineIndexCount(int subdivisions, boolean beginEdgeFlag, boolean endEdgeFlag) {
        GeometryBuilder gb = this.getGeometryBuilder();
        int count = 4 * (gb.getSubdivisionPointsVertexCount(subdivisions) - 1);
        if (beginEdgeFlag) {
            count += 2;
        }
        if (endEdgeFlag) {
            count += 2;
        }

        return count;
    }

    private void makeVSlice(DrawContext dc, java.util.List<LatLon> locations, java.util.List<Boolean> edgeFlags,
            VolumeSlice3DOutput dest) {
        if (locations.isEmpty()) {
            return;
        }

        // For dynamic sizing outlines...I might need this code for 'smart' legend over vslice, so
        // I'm leaving it here for the moment --Robert Toomey
        GeometryBuilder gb = this.getGeometryBuilder();

        Vec4[] polyPoints = new Vec4[locations.size() + 1];
        Boolean[] polyEdgeFlags = new Boolean[locations.size() + 1];
        Matrix[] polyTransform = new Matrix[1];
        int polyCount = this.computeCartesianPolygon(dc, locations, edgeFlags, polyPoints, polyEdgeFlags,
                polyTransform);

        // Copy from polyVertices into polyPoints?  why???
        float[] polyVertices = new float[3 * polyCount];
        for (int i = 0; i < polyCount; i++) {
            int index = 3 * i;
            polyVertices[index] = (float) polyPoints[i].x;
            polyVertices[index + 1] = (float) polyPoints[i].y;
            polyVertices[index + 2] = (float) polyPoints[i].z;
        }

        int fillIndexCount = 0;
        int outlineIndexCount = 0;
        int vertexCount = 0;

        // GeometryBuilder.IndexedTriangleArray ita = null;
        fillIndexCount += this.getEdgeFillIndexCount(polyCount, subdivisions);
        outlineIndexCount += this.getEdgeOutlineIndexCount(polyCount, subdivisions, polyEdgeFlags);
        vertexCount += this.getEdgeVertexCount(polyCount, subdivisions);

        int[] fillIndices = new int[fillIndexCount];
        int[] outlineIndices = new int[outlineIndexCount];
        float[] vertices = new float[3 * vertexCount];

        int fillIndexPos = 0;
        int outlineIndexPos = 0;
        int vertexPos = 0;

        // make edge
        gb.setOrientation(GeometryBuilder.OUTSIDE);

        int sectionFillIndexCount = this.getSectionFillIndexCount(subdivisions);
        int sectionVertexCount = this.getSectionVertexCount(subdivisions);

        for (int i = 0; i < polyCount - 1; i++) {
            boolean beginEdgeFlag = polyEdgeFlags[i];
            boolean endEdgeFlag = polyEdgeFlags[i + 1];

            // Make section fill indices....
            int count = gb.getSubdivisionPointsVertexCount(subdivisions);

            int index = fillIndexPos;
            int pos, nextPos;
            for (int fill = 0; fill < count - 1; fill++) {
                pos = vertexPos + 2 * fill;
                nextPos = vertexPos + 2 * (fill + 1);
                fillIndices[index++] = pos + 1;
                fillIndices[index++] = pos;
                fillIndices[index++] = nextPos + 1;
                fillIndices[index++] = nextPos + 1;
                fillIndices[index++] = pos;
                fillIndices[index++] = nextPos;
            }
            // End Make section fill indices     

            // Make the fill vertices
            int numPoints = gb.getSubdivisionPointsVertexCount(subdivisions);

            Globe globe = dc.getGlobe();
            int index1 = 3 * i;
            int index2 = 3 * (i + 1);

            float[] locationVerts = new float[3 * numPoints];
            gb.makeSubdivisionPoints(
                    polyVertices[index1], polyVertices[index1 + 1], polyVertices[index1 + 2],
                    polyVertices[index2], polyVertices[index2 + 1], polyVertices[index2 + 2],
                    subdivisions, locationVerts);
            double altitudes[] = new double[2];
            altitudes[0] = myCurrentGrid.bottomHeight;
            altitudes[1] = myCurrentGrid.topHeight;
            double vert = dc.getVerticalExaggeration();
            for (int p = 0; p < numPoints; p++) {
                int pindex = 3 * p;
                Vec4 vec = new Vec4(locationVerts[pindex], locationVerts[pindex + 1], locationVerts[pindex + 2]);
                vec = vec.transformBy4(polyTransform[0]);
                Position pos2 = globe.computePositionFromPoint(vec);

                for (int j = 0; j < 2; j++) {
                    // vec = this.computePointFromPosition(dc, pos2.getLatitude(), pos2.getLongitude(), altitudes[j],
                    //        terrainConformant[j]);
                    vec = globe.computePointFromPosition(pos2.getLatitude(), pos2.getLongitude(), altitudes[j] * vert);

                    pindex = 2 * p + j;
                    pindex = 3 * (vertexPos + pindex);
                    vertices[pindex] = (float) (vec.x);
                    vertices[pindex + 1] = (float) (vec.y);
                    vertices[pindex + 2] = (float) (vec.z);

                }

            }
            // end make section vertices

            // Outline the polys..this is lines from one to the other...
            this.makeSectionOutlineIndices(subdivisions, vertexPos, outlineIndexPos, outlineIndices,
                    beginEdgeFlag, endEdgeFlag);


            // Due we need normals for a vslice?  Probably not..we don't really want data colors changing since
            // it's a key.  Now with isosurfaces we might...
            //    gb.makeIndexedTriangleArrayNormals(fillIndexPos, sectionFillIndexCount, fillIndices,
            //        vertexPos, sectionVertexCount, vertices, normals);

            fillIndexPos += sectionFillIndexCount;
            outlineIndexPos += this.getSectionOutlineIndexCount(subdivisions, beginEdgeFlag, endEdgeFlag);
            vertexPos += sectionVertexCount;
        }

        // end make edge

        dest.getFillIndexGeometry().setElementData(GL.GL_TRIANGLES, fillIndexCount, fillIndices);
        dest.getOutlineIndexGeometry().setElementData(GL.GL_LINES, outlineIndexCount, outlineIndices);
        dest.getVertexGeometry().setVertexData(vertexCount, vertices);
        //  dest.getVertexGeometry().setNormalData(vertexCount, normals);

        // Volume VSlice color generation....
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(get3DProductToFollow(), getUseVirtualVolume());
        myVolumeProduct = volume;

        // Get the filter list and the record object
        FilterList aList = null;
        ProductFeature pf = ProductManager.getInstance().getTopProductFeature();
        if (pf != null) {
            aList = pf.getFList();
        }

        if (aList == null) {
            return;
        }
        aList.prepForVolume(volume);

        // Generate the 3D VSlice in the window, and the 2D slice for charting...
        myCurrentGrid.set(myNumRows, myNumCols, myCurrentGrid.startLat, myCurrentGrid.startLon,
                myCurrentGrid.endLat, myCurrentGrid.endLon,
                myCurrentGrid.bottomHeight, myCurrentGrid.topHeight);
        // Let the volume generate the 3D slice output
        // FIXME: Need to be able to change volumevalue
        myVolumeProduct.generateSlice3D(myCurrentGrid, dest, dc.getGlobe(), aList, true, dc.getVerticalExaggeration(), null);
    }
}