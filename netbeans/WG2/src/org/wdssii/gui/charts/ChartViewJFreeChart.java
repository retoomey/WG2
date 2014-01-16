package org.wdssii.gui.charts;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.event.MouseInputListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.data.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.features.FeatureList;

/**
 * A subclass of our chart that uses the JFreeChart library to render its stuff
 *
 * Might move the readout tracking mouse listeners 'higher' up in the code as
 * charts are further developed..
 *
 * @author Robert Toomey
 *
 */
public class ChartViewJFreeChart extends DataView {

    private final static Logger LOG = LoggerFactory.getLogger(ChartViewJFreeChart.class);
    /**
     * The ChartPanel from JFreeChart
     */
    public ChartPanel myChartPanel = null;
    /**
     * The chart that does all the work
     */
    public JFreeChart myJFreeChart = null;
    /**
     * Our guess on if the mouse is inside the window or not
     */
    public boolean myWithinWindow = false;

    /**
     * A NumberAxis that forces auto range (zoom out or menu picked) to be a
     * fixed range we pass in. This allows us to do dynamic resampling of data
     * and level of detail when zooming. We change JFreeCharts regular range to
     * allow resampling.
     */
    public static class FixedRangeNumberAxis extends NumberAxis {

        private Range myRange = null;
        private boolean myAllowNegative = true;

        public FixedRangeNumberAxis(String label, boolean allowNegative) {
            super(label);
            myAllowNegative = allowNegative;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Our stock axis for dynamic regenerating by zoom charts such as VSlice
         * and readout
         */
        public static FixedRangeNumberAxis getStockAxis(String label, boolean allownegative) {
            FixedRangeNumberAxis axis = new FixedRangeNumberAxis(label, allownegative);
            axis.setLowerMargin(0.0);
            axis.setUpperMargin(0.0);
            axis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
            axis.setAxisLineVisible(true);
            return axis;
        }

        public void setFixedRange(Range aRange) {
            myRange = aRange;
            setRange(myRange);
        }

        /**
         * Zoom out auto-adjust should go to the FULL vslice we're following..
         */
        @Override
        protected void autoAdjustRange() {
            if (myRange != null) {
                setRange(myRange, false, false);
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

    @Override
    public Object getNewGUIForChart(Object myChartBox) {

        myChartPanel = new ChartPanel(null) {
            private static final long serialVersionUID = 1L;
            // Don't think I need this code
            //  @Override
            //   public void update(Graphics g) {
            //       // Do not erase the background
            //       paint(g);
            //   }
        };
        myChartPanel.setMouseWheelEnabled(true);
        myChartPanel.setChart(this.myJFreeChart);
        Overlay test = new Overlay() {
            @Override
            public void paintOverlay(Graphics2D gd, ChartPanel pnl) {

                if (myWithinWindow) {
                    paintMouseOverlay(gd, pnl);
                }
            }

            @Override
            public void addChangeListener(OverlayChangeListener ol) {
            }

            @Override
            public void removeChangeListener(OverlayChangeListener ol) {
            }
        };
        myChartPanel.addOverlay(test);
        
        MouseInputListener m = new MouseInputListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                myWithinWindow = true;
                // myMouseX = e.getX();
                // myMouseY = e.getY();
                // chart will repaint due to JFreeChart zoom rectangle
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Make sure readout overlay is updated.  Note the chart
                // buffers into an image so it won't be regenerated
                myWithinWindow = true;
                // int X = e.getX();
                //  int Y = e.getY();
                handleMouseMoved(e);
                // myMouseX = e.getX();
                // myMouseY = e.getY();

                // FeaturePosition f = new FeaturePosition(lat, lon, hkms);
                //  FeatureList.theFeatures.setTrackingPosition(f);
                myChartPanel.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                myWithinWindow = true;
                myChartPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                myWithinWindow = false;
                myChartPanel.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        };
        myChartPanel.addMouseListener(m);
        myChartPanel.addMouseMotionListener(m);
        return myChartPanel;
    }

    /**
     * Draw the mouse overlay (readout) probably
     */
    public void paintMouseOverlay(Graphics2D gd, ChartPanel pnl) {
    }

    public void handleMouseMoved(MouseEvent e) {
       
    }

    @Override
    public void takeSnapshot(String name) {
        int width = 1000;
        int height = 1000;
        try {
            ChartUtilities.saveChartAsPNG(new File(name), myJFreeChart, width, height);
        } catch (IOException e) {
            LOG.error("IO exception trying to output snapshot of chart " + e.toString());
        }
    }

    @Override
    public void setTrackingPosition(FeatureList fl, FeatureList.FeaturePosition f) {
        // Nothing by default
    }
}
