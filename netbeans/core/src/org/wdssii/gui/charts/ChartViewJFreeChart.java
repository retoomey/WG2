package org.wdssii.gui.charts;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/** A subclass of our chart that uses the JFreeChart library
 * to render its stuff
 * 
 * @author Robert Toomey
 *
 */
public class ChartViewJFreeChart extends ChartViewChart {

    private static Log log = LogFactory.getLog(ChartViewJFreeChart.class);
    /** The ChartPanel from JFreeChart */
    private ChartPanel myChartPanel = null;
    /** The chart that does all the work */
    public JFreeChart myJFreeChart = null;

    @Override
    public Object getNewGUIForChart(Object myChartBox) {

        /*	Composite holder = new Composite((Composite)myChartBox, SWT.EMBEDDED | SWT.NO_BACKGROUND);
        Frame frame = SWT_AWT.new_Frame(holder);
        
        myChartPanel = new ChartPanel(null){
        private static final long serialVersionUID = 1L;
        
        @Override
        public void update(Graphics g){
        // Do not erase the background
        paint(g);
        }
        
        };
        frame.add(myChartPanel);
        myChartPanel.setMouseWheelEnabled(true);
        myChartPanel.setChart(this.myJFreeChart);
        
        return holder;
         * 
         */
        return null;
    }

    @Override
    public void takeSnapshot(String name) {
        int width = 1000;
        int height = 1000;
        try {
            ChartUtilities.saveChartAsPNG(new File(name), myJFreeChart, width, height);
        } catch (IOException e) {
            log.error("IO exception trying to output snapshot of chart " + e.toString());
        }
    }
}
