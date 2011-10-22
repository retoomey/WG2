package org.wdssii.gui.charts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.commands.ProductFollowCommand;

/** A Chart that goes inside the ChartView.
 * 
 * Charts are created by reflection from the static method of the form:
 * public T createTChart();
 * 
 * 
 * The 'base' ability of all our charts
 * ChartView handles two boxes for a chart, one is the 'chart' area, the other is the
 * 'gui control' area which can be toggled on/off.
 * 
 * ChartView has 'stock' controls in the menu for consistency:
 * 
 * Charts have a 'use virtual volume' flag for viewing volumes
 * Charts have a 'current product' flag for following a particular product
 * 
 * A chart can ignore these flags if they are meaningless for it.
 * Eventually we might need to subclass/interface chart types more, but
 * for now this will work as a foundation.
 * 
 * @author Robert Toomey
 * 
 */
public class ChartViewChart {

    private static Log log = LogFactory.getLog(ChartViewChart.class);

    /** Update chart when needed (check should be done by chart) */
    public void updateChart() {
    }
    ;
	
	/** The current volume we follow, 99.9% of charts will do this */
	private boolean myUseVirtualVolume;
    /** The current use product filters toggle */
    private boolean myUseProductFilters;
    /** The current product we follow, 99.9% of charts will do this */
    private String myCurrentProduct = ProductFollowCommand.top;

    /** Set if this chart should use virtual volumes */
    public final void setUseVirtualVolume(boolean flag) {
        myUseVirtualVolume = flag;
    }

    /** Get if this chart uses virtual volumes */
    public final boolean getUseVirtualVolume() {
        return myUseVirtualVolume;
    }

    /** Set the key of the product to follow */
    public void setUseProductKey(String p) {
        myCurrentProduct = p;
    }

    /** Get the key of the product to follow */
    public String getUseProductKey() {
        return myCurrentProduct;
    }

    /** Generate the Chart itself.  Basically the stuff that will draw the chart in the composite */
    public Object getNewGUIForChart(Object parent) {
        // FIXME: maybe some text? "Hey this isn't working?"
        //Composite nothing = new Composite((Composite)parent, SWT.NONE);
        //return nothing;
        return null;
    }

    /** Generate the GUI controls for this chart, allowing more changes */
    public Object getNewGUIBox(Object parent) {
        // FIXME: maybe some text? "Hey this isn't working?"
        //Composite nothing = new Composite((Composite)parent, SWT.NONE);
        //return nothing;
        return null;
    }

    /** Generate a snapshot of this chart, if possible */
    public void takeSnapshot(String name) {
        log.info("Snapshot not implemented for this chart. :(");
    }

    /** Set if this chart uses product filters */
    public void setUseProductFilters(boolean flag) {
        myUseProductFilters = flag;
    }

    /** Get if this chart uses the product filters */
    public final boolean getUseProductFilters() {
        return myUseProductFilters;
    }
}
