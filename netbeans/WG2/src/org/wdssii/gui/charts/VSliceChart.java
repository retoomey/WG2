package org.wdssii.gui.charts;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleInsets;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.VolumeSlice2DOutput;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.gui.volumes.LLHAreaSlice;

public class VSliceChart extends ChartViewJFreeChart {

    /** A NumberAxis that forces auto range (zoom out or menu picked) to be
     * the default height/range of the LLHAreaSlice we are following
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

        /** Zoom out auto-adjust should go to the FULL vslice we're following.. */
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
    /** The axis for range in KM of the VSlice bottom distance */
    private VSliceNumberAxis myRangeAxis;
    /** The axis for the height of the VSlice side distance */
    private VSliceNumberAxis myHeightAxis;
    static int counter = 0;
    /** The current full key for us */
    private String myCurrentKey;
    /** The last GIS key.  This is the key part that deals with the 'shape'
     * of the VSlice without product/volume
     */
    private String myGISKey = "";
    private VSliceFixedGridPlot myPlot = null;

    public VSliceChart(String arg0, Font arg1, VSliceFixedGridPlot arg2, boolean arg3) {

        myJFreeChart = new JFreeChart(arg0, arg1, arg2, arg3);
        myPlot = arg2;
    }

    /** Return the LLHAreaSlice that we are currently drawing a plot for */
    public LLHAreaSlice getVSliceToPlot() {
        // -------------------------------------------------------------------------
        // Hack snag the current slice and product...
        // Hack for now....we grab first 3d object in our LLHAreaManager
        LLHAreaSlice slice = null;
        ArrayList<VolumeTableData> test = LLHAreaManager.getInstance().getVolumes();
        if (test != null) {
            if (test.size() > 0) {
                VolumeTableData data = test.get(0);
                if (data.airspace instanceof LLHAreaSlice) {
                    slice = (LLHAreaSlice) data.airspace;
                }
            }
        }
        return slice;
    }

    /** Called during dragging of vslice to explicitly update the chart.  The chart checks for
     * changes and only draws when the vslice if different
     */
    @Override
    public void updateChart() {

        // The LLHAreaSlice is the geometry in the 3d window we are
        // matching our coordinates to.  It can be valid without
        // any product/volume information.
        LLHAreaSlice slice = getVSliceToPlot();
        if (slice == null) {
            return;
        }   // No slice to follow, return..
        myPlot.setSlice(slice);

        /** Get the GIS key of the slice */
        String gisKey = slice.getGISKey();

        // Sync the height/range axis to the GIS vslice range when updated, this resets
        // any 'zoom' in the chart...but only if GIS key has CHANGED.  This
        // way users can toggle products and keep zoom level for comparison,
        // but if they drag the vslice we reset to full slice.
        if (!myGISKey.equals(gisKey)) {
            myRangeAxis.setVSliceRange(new Range(0, slice.getRangeKms() / 1000.0));
            myHeightAxis.setVSliceRange(new Range(slice.getBottomHeightKms() / 1000.0, slice.getTopHeightKms() / 1000.0));
        }
        myGISKey = gisKey;

        /** Get the volume we are following */
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());
        if (volume == null) {
            return;
        }

        FilterList aList = null;
        String useKey = getUseProductKey();
        String titleKey = "";
        /** Get the filter list of the product we are following */
        ProductHandlerList phl = ProductManager.getInstance().getProductOrderedSet();
        if (phl != null) {
            ProductHandler tph = phl.getProductHandler(useKey);
            if (tph != null) {
                aList = tph.getFList();
            }
            Product p = tph.getProduct();
            if (p != null) {
                titleKey = p.getProductInfoString(false);
            }
        }
        if (aList == null) {
            return;
        }

        String key = gisKey;

        /** Add volume key */
        key += volume.getKey();

        /** Add filter key */
        key += aList.getFilterKey(getUseProductFilters());

        boolean keyDifferent = false;
        if (!key.equals(myCurrentKey)) {
            keyDifferent = true;
        }
        myCurrentKey = key;

        if (!keyDifferent) {
            return;
        }

        // if (keyDifferent) {
        //     System.out.println("VSLICE KEY CHANGE");
        // }

        myPlot.setVolumeAndSlice(slice, volume, aList);
        myJFreeChart.setTitle(titleKey);
        myJFreeChart.fireChartChanged();
        //myJFreeChart.setTitle("Vertical Slice (F" + counter++ + ")");
    }

    /** Terrain XYZDataset is a JFreeChart dataset where we sample
     * the terrain 'height' along the range.
     */
    public static class TerrainXYZDataset implements XYZDataset {

        /** Sample size of the terrain by range */
        public int sampleSize = 201;
        private double[] myHeights = new double[sampleSize];
        /** The full range of the terrain line */
        private double rangeKM;
        /** The 'delta' range per sample point of range line */
        private double rangeDelta;
        /** The starting range location (left side) of range line */
        private double rangeLower;

        // We dynamically resample the terrain data depending on
        // zoom level....
        public void syncToRange(ValueAxis x,
                double startLat,
                double startLon,
                double endLat,
                double endLon) {
            Range r = x.getRange();
            rangeLower = r.getLowerBound();
            rangeKM = r.getUpperBound() - r.getLowerBound();
            rangeDelta = rangeKM / (sampleSize - 1);
            WorldWindView eb = CommandManager.getInstance().getEarthBall();
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
            return sampleSize;
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

    /** Fixed grid draws a background of vslice at current zoom level.
     * We render the vslice grid directly in the render function of the plot.
     * Why not use a JFreeChart block renderer?  Well we want a set grid resolution,
     * but 'infinite' sampling resolution as we zoom in.  Freechart has a 'fixed'
     * data grid.  However we do call super here so that we can add
     * regular freechart stuff over our vslice.  Think of the vslice as
     * a very special background to the plot.
     */
    public static class VSliceFixedGridPlot extends XYPlot {

        private ProductVolume myVolume;
        private LLHAreaSlice mySlice;
        private FilterList myList;
        private TerrainXYZDataset myTerrain;

        private VSliceFixedGridPlot(TerrainXYZDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
            super(dataset, domainAxis, rangeAxis, renderer);
            myTerrain = dataset;
        }

        @Override
        public void drawBackground(Graphics2D g2, Rectangle2D dataArea) {

            // Calculate the 'zoomed' lat/lon for our followed slice
            if (mySlice != null) {
                VolumeSliceInput sourceGrid = mySlice.getGrid();
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
                double fullRange = mySlice.getRangeKms();  // 100% say 100 KMS
                double deltaLatPerKm = (subGrid.endLat - sLat) / fullRange;
                double deltaLonPerKm = (subGrid.endLon - sLon) / fullRange;

                // Now adjust the start/end lat/lon by percentage
                subGrid.startLat = sLat + (leftKms * deltaLatPerKm);
                subGrid.endLat = sLat + (rightKms * deltaLatPerKm);
                subGrid.startLon = sLon + (leftKms * deltaLonPerKm);
                subGrid.endLon = sLon + (rightKms * deltaLonPerKm);

                myTerrain.syncToRange(rangeAxis, subGrid.startLat,
                        subGrid.startLon,
                        subGrid.endLat,
                        subGrid.endLon);

                if (myVolume != null) {
                    //VolumeSliceInput sourceGrid = mySlice.getGrid();

                    // FIXME: modify grid only when axis change...
                    VolumeSlice2DOutput dest = new VolumeSlice2DOutput();
                    myVolume.generate2DGrid(subGrid, dest, myList, false);
                    int[] data = dest.getColor2dFloatArray(0);

                    // Render the dynamic 'grid' of data. Note that unlike
                    // JFreeChart we dynamically resample our dataset based
                    // upon the 'zoom' level
                    int numOfCols = dest.getCols();
                    int numOfRows = dest.getRows();
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
        }

        private void setSlice(LLHAreaSlice slice) {
            mySlice = slice;
        }

        private void setVolumeAndSlice(LLHAreaSlice slice, ProductVolume volume, FilterList list) {
            myVolume = volume;
            mySlice = slice;
            myList = list;
        }
    }

    /** Static method to create a vslice chart, called by reflection */
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
        XYAreaRenderer ar = new XYAreaRenderer();
        ar.setSeriesPaint(0, Color.DARK_GRAY);
        // Doesn't work...great.
        //ar.setSeriesOutlinePaint(0, Color.GREEN);
        // ar.setSeriesOutlineStroke(0, new BasicStroke(2));     

        // a 'fake' dataset since we bypass the normal renderer...
        VSliceFixedGridPlot plot = new VSliceFixedGridPlot(ds, rangeAxis, heightAxis, ar);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        VSliceChart chart = new VSliceChart("Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        /** Make it white (ignore theme color) */
        chart.myJFreeChart.setBackgroundPaint(Color.white);
        chart.myJFreeChart.removeLegend();

        chart.myRangeAxis = rangeAxis;
        chart.myHeightAxis = heightAxis;

        // Just a test, we're probably going to have to make our own
        // scale subclass that uses our color map
		/*	NumberAxis colorScale = new NumberAxis("Color");
        colorScale.setAutoRange(true);
        PaintScaleLegend p = new PaintScaleLegend(scale, colorScale);
        p.setSubdivisionCount(20);
        p.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        p.setAxisOffset(5D);
        //	p.setMargin(new RectangleInsets(5D, 5D, 5D, 5D));
        p.setFrame(new BlockBorder(Color.red));
        //	p.setPadding(new RectangleInsets(10D, 10D, 10D, 10D));
        chart.addSubtitle(p);
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
        /* final Composite box = new Composite((Composite) parent, SWT.NONE);
        box.setLayout(new RowLayout());
        
        // SRV 
        Text srv = new Text(box, SWT.LEFT);
        srv.setText("HeightKMs:");
        srv.setEditable(false);
        final Spinner speedSpin = new Spinner(box, 0);
        speedSpin.setMaximum(200);
        speedSpin.setMinimum(-200);
        speedSpin.setSelection(12);
        
        // Add listeners for GUI elements
        speedSpin.addSelectionListener(new SelectionListener() {
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
        }
        
        @Override
        public void widgetSelected(SelectionEvent arg0) {
        //myCutOff = speedSpin.getSelection();
        //myDirtySRM = true;
        //CommandManager.getInstance().executeCommand(new ProductChangeCommand.ProductFilterCommand(), true);
        }
        });
        
        return box;
         * 
         */
        return null;
    }
}