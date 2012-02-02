package org.wdssii.gui.charts;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.event.MouseInputListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.panel.Overlay;

/** A subclass of our chart that uses the JFreeChart library
 * to render its stuff
 * 
 * Might move the readout tracking mouse listeners 'higher' up in
 * the code as charts are further developed..
 * 
 * @author Robert Toomey
 *
 */
public class ChartViewJFreeChart extends ChartViewChart {

    private static Logger log = LoggerFactory.getLogger(ChartViewJFreeChart.class);
    /** The ChartPanel from JFreeChart */
    private ChartPanel myChartPanel = null;
    /** The chart that does all the work */
    public JFreeChart myJFreeChart = null;
    /** Our guess on if the mouse is inside the window or not */
    public boolean myWithinWindow = false;
    /** Latest known local mouse X for readout */
    public int myMouseX = 0;
    /** Latest known local mouse Y for readout */
    public int myMouseY = 0;

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
                myMouseX = e.getX();
                myMouseY = e.getY();
                // chart will repaint due to JFreeChart zoom rectangle
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Make sure readout overlay is updated.  Note the chart
                // buffers into an image so it won't be regenerated
                myWithinWindow = true;
                myMouseX = e.getX();
                myMouseY = e.getY();
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

    /** Draw the mouse overlay (readout) probably */
    public void paintMouseOverlay(Graphics2D gd, ChartPanel pnl) {
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
