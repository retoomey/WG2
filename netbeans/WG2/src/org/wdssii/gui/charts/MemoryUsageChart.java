package org.wdssii.gui.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * Show current JVM memory and NIO memory in a JFreeChart...
 * 
 * @author Robert Toomey
 */
public class MemoryUsageChart extends ChartViewJFreeChart {

    /**
     * 
     */
    private static final long serialVersionUID = -916766604020466149L;
    //private JFreeChart myJFreeChart = null;
    /** Time series for total memory used. */
    private TimeSeries total;
    /** Time series for free memory. */
    private TimeSeries free;

    public MemoryUsageChart(String string, Font defaultTitleFont,
            XYPlot xyplot, boolean b) {
        myJFreeChart = new JFreeChart(string, defaultTitleFont, xyplot, b);
    }

    /**
     * Creates a new application.
     */
    public static MemoryUsageChart create() {
        // create two series that automatically discard data more than 30 seconds old...
        //  this.total = new TimeSeries("Total", Millisecond.class);


        //   this.free.setHistoryCount(30000);

        // The axis for the chart
        DateAxis domain = new DateAxis("Time");
        domain.setAutoRange(true);
        domain.setLowerMargin(0.0);
        domain.setUpperMargin(0.0);
        domain.setTickLabelsVisible(true);
        NumberAxis range = new NumberAxis("Memory");
        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // The renderer for the chart
        XYItemRenderer renderer = new DefaultXYItemRenderer();
        renderer.setSeriesPaint(0, Color.red);
        renderer.setSeriesPaint(1, Color.green);
        renderer.setBaseStroke(
                new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

        // The dataset for the chart

        // The plotter for the chart, which uses a dataset and renderer
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        XYPlot xyplot = new XYPlot(dataset, domain, range, renderer);
        xyplot.setBackgroundPaint(Color.black);

        MemoryUsageChart chart = new MemoryUsageChart("MemoryUsage", JFreeChart.DEFAULT_TITLE_FONT,
                xyplot, true);
        chart.total = new TimeSeries("Total");
        chart.total.setMaximumItemAge(60 * 1000); // In milliseconds

        dataset.addSeries(chart.total);
        dataset.addSeries(chart.total);

        //  this.total.setHistoryCount(30000);
        //  this.free = new TimeSeries("Free", Millisecond.class);
        chart.free = new TimeSeries("Free");
        chart.free.setMaximumItemAge(60 * 1000); // In milliseconds
        chart.myJFreeChart.setBorderVisible(true);
        chart.new DataGenerator().start();
        return chart;
    }

    /**
     * Adds an observation to the 'total memory' time series.
     *
     * @param y  the total memory used.
     */
    private void addTotalObservation(double y) {
        total.add(new Millisecond(), y);
    }

    /**
     * Adds an observation to the 'free memory' time series.
     *
     * @param y  the free memory.
     */
    private void addFreeObservation(double y) {
        free.add(new Millisecond(), y);
    }

    /**
     * The time for adding data to the chart
     * This is a swing timer since it's displaying in a swing widget window...
     */
    public class DataGenerator extends Timer implements ActionListener {

        /**
         * 
         */
        private static final long serialVersionUID = -1934200267900472696L;

        /**
         * Constructor.
         */
        public DataGenerator() {
            super(1000, null);
            addActionListener(this);
        }

        /**
         * Adds a new free/total memory reading to the dataset.
         *
         * @param event  the action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            System.out.println("Memory table time pulse");
            long f = Runtime.getRuntime().freeMemory();
            long t = Runtime.getRuntime().totalMemory();
            addTotalObservation(t);
            addFreeObservation(f);
        }

        /** Make sure no dangling objects */
        public void cleanup() {
            removeActionListener(this);
            stop();
        }
    }

    @Override
    public void updateChart(boolean force) {
        // TODO Auto-generated method stub
    }
}
