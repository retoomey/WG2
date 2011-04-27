package org.wdssii.gui.charts;

import gov.nasa.worldwind.geom.LatLon;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.ListIterator;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.wdssii.datatypes.DataType;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.volumes.LLHAreaSlice;

/** A chart that displays the data value of a product in its Y axis, and a
 * distance in its X.  Uses a Slice
 * Uses LLHAreaSlice...
 * 
 * Some of this chart should probably be pulled out as a superclass.  Any chart
 * doing a range/height of a set of products in the display could use the code here
 * 
 * @author Robert Toomey
 *
 */
public class DataRangeValueChart extends ChartViewJFreeChart {

    private static final long serialVersionUID = 7770368607537155914L;
    private static final int myNumSamples = 1000;
    private String myChartKey;
    private NumberAxis myRangeAxis = null;

    /** Internal range axis info.  Used to generate the corresponding JFreeChart range axis */
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
    //private ArrayList<rangeAxisInfo> myRangeAxis = new ArrayList<rangeAxisInfo>();

    public DataRangeValueChart(String title, Font defaultTitleFont,
            XYPlot plot, boolean legend) {
        myJFreeChart = new JFreeChart(title, defaultTitleFont, plot, legend);
    }

    public static DataRangeValueChart create(String title,
            String xAxisLabel,
            String yAxisLabel,
            XYDataset dataset,
            PlotOrientation orientation,
            boolean legend,
            boolean tooltips,
            boolean urls) {
        // Default range axis
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setAutoRangeIncludesZero(false);

        // Default domain axis
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setAutoRange(false);

        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setOrientation(orientation);
        if (tooltips) {
            renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        }
        if (urls) {
            renderer.setURLGenerator(new StandardXYURLGenerator());
        }
        DataRangeValueChart chart = new DataRangeValueChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
        chart.myRangeAxis = yAxis;

        return chart;

    }

    /** Static method to create a DataRangeValueChart chart, called by reflection */
    public static DataRangeValueChart createDataRangeValueChart() {
        //	XYDataset xydataset = createDataset1();
        //JFreeChart jfreechart = ChartFactory.createXYLineChart("Annotation Demo 2", "Date", "Price Per Unit", xydataset, PlotOrientation.VERTICAL, false, true, false);
        DataRangeValueChart jfreechart = create("Readout Range", "Range KMs", "Value", null, PlotOrientation.VERTICAL, false, true, false);

        XYPlot xyplot = (XYPlot) jfreechart.myJFreeChart.getPlot();

        NumberAxis numberaxis = (NumberAxis) xyplot.getRangeAxis();
        numberaxis.setAutoRangeIncludesZero(false);
        numberaxis.setAutoRange(false); // Range will be set in updateChart

        //	NumberAxis numberaxis1 = new NumberAxis("Secondary");
        //	numberaxis1.setAutoRangeIncludesZero(false);
        //	xyplot.setRangeAxis(1, numberaxis1);

        //xyplot.setDataset(1, createDataset2());
        //	xyplot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyplot.getRenderer();
        xylineandshaperenderer.setBaseToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());

        //xylineandshaperenderer.setBaseShapesVisible(true);
        //xylineandshaperenderer.setBaseShapesFilled(true);
        xylineandshaperenderer.setSeriesPaint(0, Color.blue);

        //	XYPointerAnnotation xypointerannotation = new XYPointerAnnotation("Annotation 1 (2.0, 167.3)", 2D, 167.30000000000001D, -0.78539816339744828D);
        //	xypointerannotation.setTextAnchor(TextAnchor.BOTTOM_LEFT);
        //	xypointerannotation.setPaint(Color.red);
        //	xypointerannotation.setArrowPaint(Color.red);
        //	xylineandshaperenderer.addAnnotation(xypointerannotation);

        //XYLineAndShapeRenderer xylineandshaperenderer1 = new XYLineAndShapeRenderer(true, true);
        //xylineandshaperenderer.setBaseToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());

        //XYPointerAnnotation xypointerannotation1 = new XYPointerAnnotation("Annotation 2 (15.0, 613.2)", 15D, 613.20000000000005D, 1.5707963267948966D);
        //.setTextAnchor(TextAnchor.TOP_CENTER);
        //xylineandshaperenderer1.addAnnotation(xypointerannotation1);
        //xyplot.setRenderer(1, xylineandshaperenderer1);

        LegendTitle legendtitle = new LegendTitle(xylineandshaperenderer);
        //LegendTitle legendtitle1 = new LegendTitle(xylineandshaperenderer1);
        BlockContainer blockcontainer = new BlockContainer(new BorderArrangement());
        blockcontainer.add(legendtitle, RectangleEdge.LEFT);
        //blockcontainer.add(legendtitle1, RectangleEdge.RIGHT);
        blockcontainer.add(new EmptyBlock(2000D, 0.0D));
        CompositeTitle compositetitle = new CompositeTitle(blockcontainer);
        compositetitle.setPosition(RectangleEdge.BOTTOM);
        jfreechart.myJFreeChart.addSubtitle(compositetitle);
        return jfreechart;

    }

    @Override
    public void updateChart() {

        StringBuilder key = new StringBuilder();

        // Get the products
        Product p = null;
        ProductHandlerList current = CommandManager.getInstance().getProductOrderedSet();

        // If we found a product, we can do the slice range.....
        LLHAreaSlice slice = getVSliceToPlot();
        if (slice != null) {
            LatLon l = slice.getLeftLocation();
            LatLon r = slice.getRightLocation();
            key.append(l);
            key.append(r);
            String newChartKey = key.toString();
            if (myChartKey != null) {
                if (newChartKey.compareTo(myChartKey) == 0) {
                    System.out.println("Chart not updated..vslice the same...");
                    return;
                }
            }
            myChartKey = key.toString();

            // FIXME: duplicate code with LLHAreaSlice marching
            // Maybe we create an iterator class....
            double startLat = l.getLatitude().getDegrees();
            double startLon = l.getLongitude().getDegrees();
            double startKms = 0.0;
            double endLat = r.getLatitude().getDegrees();
            double endLon = r.getLongitude().getDegrees();
            double deltaLat = (endLat - startLat) / myNumSamples;
            double deltaLon = (endLon - startLon) / myNumSamples;

            double kmsPerMove = slice.getRangeKms() / myNumSamples;

            myRangeAxis.setRange(new Range(0, slice.getRangeKms() / 1000.0));
            //	System.out.println("Updated range to "+slice.getRangeKms()/1000.0);
            //XYPlot plot = (XYPlot)(getPlot());

            //XYSeriesCollection xyseriescollection = new XYSeriesCollection();

            // Reverse iterator for draw list.  We plot in reverse order of the drawing...
            ListIterator<ProductHandler> iter = current.getDrawIterator();
            while (iter.hasNext()) {
                iter.next();  // Move to end of list.
            }

            // We should probably have a 'create series' object that takes a list of products?
            // While it may be slower, it will be easier to manage and subclass in the future
            int count = 0;

            ArrayList<rangeAxisInfo> newAxisList = new ArrayList<rangeAxisInfo>();

            while (iter.hasPrevious()) {
                ProductHandler h = iter.previous();
                if (h.getIsVisible()) {
                    p = h.getProduct();
                    if (p != null) {

                        // FIXME: Probably could make a renderer that just uses our 'product' as a function, then
                        // zooming would work perfectly.
                        XYSeries xyseries = new XYSeries(p.getProductInfoString());
                        ColorMap cm = p.getColorMap();

                        double max = -100000.0;
                        double min = 100000.0;
                        double curLat = startLat;
                        double curLon = startLon;
                        double curKms = startKms;

                        for (int i = 1; i < myNumSamples; i++) {

                            Location loc = new Location(curLat, curLon, 0);
                            double value = p.getValueAtLocation(loc, false);

                            // FIXME: how to handle missing data?
                            if (DataType.isRealDataValue((float) (value))) {
                                xyseries.add(curKms / 1000.0, value);
                                if (value > max) {
                                    max = value;
                                }
                                if (value < min) {
                                    min = value;
                                }
                            } else {
                                // FIXME: Can we 'break' the line like Lak would like?
                                // We'll need our own xyseries class or renderer I think...
                                // For now we just skip the point, which connects n-1 to n+1
                            }
                            curLat += deltaLat;
                            curLon += deltaLon;
                            curKms += kmsPerMove;
                        }

                        // Set the Y axis range to the color map (typically larger)
                        double colorMax = cm.getMaxValue();
                        double colorMin = cm.getMinValue();
                        if (colorMax < max) {
                            colorMax = max;
                        }
                        if (colorMin > min) {
                            colorMin = min;
                        }
                        colorMin -= 1.0; // Padding?
                        colorMax += 1.0;
                        String units = cm.getUnits();

                        // Create a new axis list (tiny helper class before we modify the JFreeChart)
                        rangeAxisInfo range = new rangeAxisInfo(units, colorMin, colorMax);
                        updateAxis(newAxisList, range, xyseries);

                        //System.out.println("Got range "+colorMin+" to "+colorMax +" data max is "+max);


                        //	goooop...need to understand setDataset in order to get it to use the correct Axis
                        //XYSeriesCollection xyseriescollection = new XYSeriesCollection();
                        //	xyseriescollection.addSeries(xyseries); // I think this is per range axis...
                        //plot.setDataset(count, xyseriescollection);
                        count++;

                    }
                }

            }
            //	plot.setDataset(0, xyseriescollection);
            updatePlotToAxis(newAxisList);


        }
    }

    /** Add a new domain axis info to a given list.  Merge if possible (depending on settings) */
    public void updateAxis(ArrayList<rangeAxisInfo> list, rangeAxisInfo newOne, XYSeries series) {

        // We 'combine' any two axis with same units, but expand the drawing range to include
        // the larger one.  Maybe this should be a setting since it could make some datasets look too tiny.
        // Say we have spectrum width m/s and velocity m/s...only one axis will draw with the color map/data range
        // of the union of the two color map ranges + max and min data values.
        boolean found = false;
        for (rangeAxisInfo r : list) {
            if (r.units.compareToIgnoreCase(newOne.units) == 0) {
                if (r.lowBound > newOne.lowBound) {
                    r.lowBound = newOne.lowBound;
                }
                if (r.upperBound < newOne.upperBound) {
                    r.upperBound = newOne.upperBound;
                }
                found = true;
                r.addSeries(series);
                break; // because we're done...
            }
        }
        if (!found) {
            list.add(newOne);
            newOne.addSeries(series);
        }
    }

    public void updatePlotToAxis(ArrayList<rangeAxisInfo> list) {
        int axisCount = 0;
        int seriesCount = 0;
        XYPlot plot = (XYPlot) (myJFreeChart.getPlot());

        for (rangeAxisInfo r : list) {
            NumberAxis numberaxis = (NumberAxis) plot.getRangeAxis(axisCount);
            if (numberaxis == null) {
                numberaxis = new NumberAxis(r.units);
                numberaxis.setAutoRangeIncludesZero(false);
                numberaxis.setAutoRange(false);
                plot.setRangeAxis(axisCount, numberaxis);
            }
            numberaxis.setRange(r.lowBound, r.upperBound);
            numberaxis.setLabel(r.units);

            // Ok add the series that use this axis now that it's ready
            for (XYSeries x : r.series) {  // They all share a single axis
                XYSeriesCollection xyseriescollection = new XYSeriesCollection();
                xyseriescollection.addSeries(x);
                plot.setDataset(seriesCount, xyseriescollection);  // Each series is a unique data set with label
                plot.mapDatasetToRangeAxis(seriesCount++, new Integer(axisCount));
            }
            axisCount++;
        }
        // Remove any lingering axis from a previous draw....
        ValueAxis v = plot.getRangeAxis(axisCount);
        while (v != null) {
            plot.setRangeAxis(axisCount, null);
            v = plot.getRangeAxis(axisCount++);
        }
        // Remove any lingering dataset from a previous update...
        XYDataset d = plot.getDataset(seriesCount);
        while (d != null) {
            plot.setDataset(seriesCount, null);
            d = plot.getDataset(seriesCount++);
        }
        // Now that the axis are good, add the series using same logic...
        //for(rangeAxisInfo r:list){
        //	
        //}
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
}
