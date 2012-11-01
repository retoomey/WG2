package org.wdssii.gui.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * A chart that displays the data value of a product in its Y axis, and a
 * distance in its X. Uses a Slice Uses LLHAreaSlice...
 *
 * Some of this chart should probably be pulled out as a superclass. Any chart
 * doing a range/height of a set of products in the display could use the code
 * here
 *
 * @author Robert Toomey
 *
 */
public class DataRangeValueChart extends LLHAreaChart {

    private static final long serialVersionUID = 7770368607537155914L;
    /**
     * The number of rows or altitudes of the VSlice
     */
    public static final int myNumRows = 50;  //50
    /**
     * The number of cols or change in Lat/Lon
     */
    public static final int myNumCols = 10; //100
    private FixedRangeNumberAxis myDistanceAxis = null;
    private FixedRangeNumberAxis myReadoutAxis = null;
    private DataRangePlot myPlot = null;

    /**
     * Internal range axis info. Used to generate the corresponding JFreeChart
     * range axis
     */
    private static class rangeAxisInfo {

        public rangeAxisInfo(String u, double l, double up) {
            units = u;
            lowBound = l;
            upperBound = up;
        }
        String units;
        double lowBound;
        double upperBound;
        ArrayList<XYSeries> series = new ArrayList<XYSeries>();

        public void addSeries(XYSeries s) {
            series.add(s);
        }
    }
    //
    private ArrayList<rangeAxisInfo> myRangeAxis = new ArrayList<rangeAxisInfo>();

    public DataRangeValueChart(String title, Font defaultTitleFont,
            DataRangePlot plot, boolean legend) {
        myJFreeChart = new JFreeChart(title, defaultTitleFont, plot, legend);
        myPlot = plot;
        myPlot.myText = new TextTitle("", new Font("Dialog", Font.PLAIN, 11));
        myPlot.myText.setPosition(RectangleEdge.BOTTOM);
        myPlot.myText.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        myJFreeChart.addSubtitle(myPlot.myText);
    }

    public static class DataRangePlot extends XYPlot {

        private TextTitle myText;
        private LLHArea myLLHArea;
        private FilterList myList;
        private Product myProduct;
        private ReadoutXYZDataset myReadout;
        private productXYRenderer myRenderer;
        /**
         * The current zoomed grid
         */
        private VolumeSliceInput mySubGrid;

        private DataRangePlot(ReadoutXYZDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, productXYRenderer renderer) {
            super(dataset, domainAxis, rangeAxis, renderer);
            myReadout = dataset;
            myRenderer = renderer;
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
            // Update the readout dataset
            // This is actually drawn in the 'draw' method
            if (subGrid != null) {
                ValueAxis rangeAxis = getDomainAxisForDataset(0);
                myReadout.syncToRange(myProduct, rangeAxis, subGrid.startLat,
                        subGrid.startLon,
                        subGrid.endLat,
                        subGrid.endLon);

            } else {
                myReadout.clearRange();
            }

            // -----------------------------------------------------
            // Update the text showing start/end points
            if (subGrid != null) {
                String newKey = getGISLabel(subGrid.startLat, subGrid.startLon,
                        subGrid.endLat, subGrid.endLon);
                myText.setText(newKey);
            } else {
                myText.setText("Need at least 2 points in 3D world for a readout");
            }

            myRenderer.updateColorKey(myProduct);

            // -----------------------------------------------------
            // Update and render the 2D VSlice colored grid
            // could 'separate' update from render I guess..
            // We'd have to hold onto the subGrid to draw later...
            //updateAndDrawVSliceGrid(subGrid, g2, dataArea);
        }

         @Override
         public void draw(Graphics2D g, Rectangle2D area, Point2D p, PlotState state, PlotRenderingInfo info) {
                     if (myProduct != null) {
                ColorMap cm = myProduct.getColorMap();
                ColorMapOutput output = new ColorMapOutput();
                cm.fillColor(output, DataType.MissingData);
                setBackgroundPaint(new Color(output.redI(), output.greenI(), output.blueI()));
            } else {
                setBackgroundPaint(Color.WHITE);
            }
             super.draw(g, area, p, state, info);
         }
        @Override
        public void drawBackground(Graphics2D g2, Rectangle2D dataArea) {
            predraw(g2, dataArea);
            super.drawBackground(g2, dataArea);
        }

        private void setLLHArea(LLHArea area) {
            myLLHArea = area;
        }

        private void setPlotData(LLHArea llhArea, Product p, FilterList list) {
            myLLHArea = llhArea;
            myList = list;
            myProduct = p;
        }
    }

    /**
     * A line renderer that 'breaks' when it encounters a non regular data point
     * such as Missing
     */
    public static class productXYRenderer extends XYLineAndShapeRenderer {

        static int skipper = 0;
        private ColorMap myColorMap;

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

            // do nothing if item is not visible
            if (!getItemVisible(series, item)) {
                return;
            }

            // Replace line pass with our check for non-normal data....
            if (isLinePass(pass)) {
                if (getItemLineVisible(series, item)) {
                    if (item > 0) {

                        // If this item or previous is missing, don't draw to previous
                        // (we don't want any crazy lines due to special values)
                        double v = dataset.getYValue(series, item);
                        if (DataType.isRealDataValue((float) (v))) {
                            v = dataset.getYValue(series, item - 1);
                            if (DataType.isRealDataValue((float) (v))) {


                                // Set the color to color key...this should
                                // actually gradiant from this to last...OR
                                // split in half for two non-linear boxes...
                                if (myColorMap != null) {
                                    ColorMap.ColorMapOutput data = new ColorMap.ColorMapOutput();
                                    myColorMap.fillColor(data, (float) v);
                                    g2.setColor(new Color(data.redI(), data.greenI(), data.blueI()));
                                }
                                drawPrimaryLine(state, g2, plot, dataset, pass, series,
                                        item, domainAxis, rangeAxis, dataArea);
                            }
                        }
                    }
                } else {
                    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
                }

            }
        }

        @Override
        protected void drawFirstPassShape(Graphics2D g2, int pass, int series,
                int item, Shape shape) {
            g2.setStroke(getItemStroke(series, item));
            // g2.setPaint(getItemPaint(series, item));     
            g2.draw(shape);
        }

        public productXYRenderer(boolean line, boolean shape) {
            super(line, shape);
        }

        private void updateColorKey(Product p) {
            if (p != null) {
                myColorMap = p.getColorMap(); // slow?
            } else {
                myColorMap = null;
            }
        }
    }

    /**
     * Static method to create a DataRangeValueChart chart, called by reflection
     */
    public static DataRangeValueChart createDataRangeValueChart() {

        FixedRangeNumberAxis distanceAxis = FixedRangeNumberAxis.getStockAxis("Distance KM", true);
        FixedRangeNumberAxis readoutAxis = FixedRangeNumberAxis.getStockAxis("Readout", true);

        // The readout dataset generator
        ReadoutXYZDataset ds = new ReadoutXYZDataset();
        productXYRenderer ar = new productXYRenderer(true, false);
        //ar.setSeriesOutlineStroke(0, new BasicStroke(10));
        ar.setSeriesStroke(0, new BasicStroke(2));
        //ar.setSeriesOutlinePaint(0, Color.WHITE);

        DataRangePlot plot = new DataRangePlot(ds, distanceAxis, readoutAxis, ar);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        // Create the renderer for plot
        // XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        // XYPlot plot = new XYPlot(null, distanceAxis, readoutAxis, renderer);
        //plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        DataRangeValueChart chart = new DataRangeValueChart("Readout", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.myJFreeChart.setBackgroundPaint(Color.white);
        chart.myJFreeChart.removeLegend();
        chart.myDistanceAxis = distanceAxis;
        chart.myReadoutAxis = readoutAxis;
        chart.myJFreeChart.setBorderVisible(true);

        return chart;

    }

    @Override
    public void updateChart(boolean force) {

        // If we found a product, we can do the slice range.....
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
            VolumeSliceInput info = llhArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
            if (info != null) {
                myDistanceAxis.setFixedRange(new Range(0, llhArea.getRangeKms(0, 1) / 1000.0));
            }
        }
        setGISKey(gisKey);

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

            // Update domain range to color map min/max
            ColorMap aColorMap = p.getColorMap();
            // Set the Y axis range to the color map (typically larger)
            double aColorMax = aColorMap.getMaxValue();
            double aColorMin = aColorMap.getMinValue();
            aColorMin -= 1.0; // Some padding
            aColorMax += 1.0;
            myReadoutAxis.setFixedRange(new Range(aColorMin, aColorMax));
            myReadoutAxis.setLabel(aColorMap.getUnits());

        } else {
            titleKey = "No product";
        }

        if ((aList == null)) {
            // If there isn't a valid data source, clear us out...
            // clear us...
            // myVolume = null;
            myPlot.setPlotData(llhArea, p, null);
            myJFreeChart.setTitle("No product filter");
            myJFreeChart.fireChartChanged();
            return;
        }
        myPlot.setPlotData(llhArea, p, aList);
        myJFreeChart.setTitle(titleKey);
        myJFreeChart.fireChartChanged();
    }

    /**
     * Readout XYZDataset is a JFreeChart dataset where we sample the product
     * along the range.
     */
    public static class ReadoutXYZDataset extends DynamicXYZDataset {

        public ReadoutXYZDataset() {
            super("Readout", 201);
        }
        /*
         * We dynamically resample the terrain data depending on zoom
         * level. This is called with the current lat/lon of the chart
         * so that the terrain can be resampled by zoom
         */

        public void syncToRange(Product p, ValueAxis x,
                double startLat,
                double startLon,
                double endLat,
                double endLon) {

            // Clear range
            clearRange();
            boolean success = false;
            if (p != null) {
                DataType dt = p.getRawDataType();
                if (dt != null) {

                    DataTypeQuery query = dt.getNewQueryObject();
                    int size = getSampleSize();
                    double deltaLat = (endLat - startLat) / (size - 1);
                    double deltaLon = (endLon - startLon) / (size - 1);
                    double lat = startLat;
                    double lon = startLon;
                    for (int i = 0; i < size; i++) {

                        Location loc = new Location(lat, lon, 0);
                        query.inLocation = loc;
                        query.inUseHeight = false;
                        dt.queryData(query);

                        // Note we store even missing values such as -99000,
                        // so we have to use a special line renderer
                        setSample(i, query.outDataValue);
                        lat += deltaLat;
                        lon += deltaLon;
                    }
                    success = true;
                }
            }

            // Set to new range
            setRange(x.getRange());
            if (!success) {
                this.setShowSamples(false);
            }

        }
    }
    /**
     * Old code playing with multiple merged readout lines...might bring this
     * back later...
     *
     * Add a new domain axis info to a given list. Merge if possible (depending
     * on settings)
     *
     *
     * public void updateAxis(ArrayList<rangeAxisInfo> list, rangeAxisInfo
     * newOne, XYSeries series) {
     *
     * // We 'combine' any two axis with same units, but expand the drawing
     * range to include // the larger one. Maybe this should be a setting since
     * it could make some datasets look too tiny. // Say we have spectrum width
     * m/s and velocity m/s...only one axis will draw with the color map/data
     * range // of the union of the two color map ranges + max and min data
     * values. boolean found = false; for (rangeAxisInfo r : list) { if
     * (r.units.compareToIgnoreCase(newOne.units) == 0) { if (r.lowBound >
     * newOne.lowBound) { r.lowBound = newOne.lowBound; } if (r.upperBound <
     * newOne.upperBound) { r.upperBound = newOne.upperBound; } found = true;
     * r.addSeries(series); break; // because we're done... } } if (!found) {
     * list.add(newOne); newOne.addSeries(series); } }
     *
     * public void updatePlotToAxis(ArrayList<rangeAxisInfo> list) { int
     * axisCount = 0; int seriesCount = 0; XYPlot plot = (XYPlot)
     * (myJFreeChart.getPlot());
     *
     * for (rangeAxisInfo r : list) { NumberAxis numberaxis = (NumberAxis)
     * plot.getRangeAxis(axisCount); if (numberaxis == null) { numberaxis = new
     * NumberAxis(r.units); numberaxis.setAutoRangeIncludesZero(false);
     * numberaxis.setAutoRange(false); plot.setRangeAxis(axisCount, numberaxis);
     * } numberaxis.setRange(r.lowBound, r.upperBound);
     * numberaxis.setLabel(r.units);
     *
     * // Ok add the series that use this axis now that it's ready for
     * (XYSeries x : r.series) { // They all share a single axis
     * XYSeriesCollection xyseriescollection = new XYSeriesCollection();
     * xyseriescollection.addSeries(x); plot.setDataset(seriesCount,
     * xyseriescollection); // Each series is a unique data set with label
     * plot.mapDatasetToRangeAxis(seriesCount++, new Integer(axisCount)); }
     * axisCount++; } // Remove any lingering axis from a previous draw....
     * ValueAxis v = plot.getRangeAxis(axisCount); while (v != null) {
     * plot.setRangeAxis(axisCount, null); v = plot.getRangeAxis(axisCount++); }
     * // Remove any lingering dataset from a previous update... XYDataset d =
     * plot.getDataset(seriesCount); while (d != null) {
     * plot.setDataset(seriesCount, null); d = plot.getDataset(seriesCount++); }
     * // Now that the axis are good, add the series using same logic...
     * //for(rangeAxisInfo r:list){ // //} }
     */
}
