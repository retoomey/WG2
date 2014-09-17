package org.wdssii.gui.charts;

import java.util.List;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.LLD;
import org.wdssii.gui.commands.ProductFollowCommand;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeaturePosition;

/**
 * DataView displays output from a FeatureList
 * 
 * @author Robert Toomey
 *
 */
public class DataView {

    private final static Logger LOG = LoggerFactory.getLogger(DataView.class);

    /**
     * Update chart when needed (check should be done by chart)
     */
    public void updateChart(boolean force) {
    }
    /**
     * The current volume we follow, 99.9% of charts will do this
     */
    private boolean myUseVirtualVolume;
    /**
     * The current use product filters toggle
     */
    private boolean myUseProductFilters;
    /**
     * The current product we follow, 99.9% of charts will do this
     */
    private String myCurrentProduct = ProductFollowCommand.top;

    /**
     * Set if this chart should use virtual volumes
     */
    public final void setUseVirtualVolume(boolean flag) {
        myUseVirtualVolume = flag;
        updateChart(false);
    }

    /**
     * Get if this chart uses virtual volumes
     */
    public final boolean getUseVirtualVolume() {
        return myUseVirtualVolume;
    }

    /**
     * Set the key of the product to follow
     * Should be called only within GUI thread
     */
    public void setUseProductKey(String p) {
        myCurrentProduct = p;
        updateChart(false);
    }

    /**
     * Get the key of the product to follow
     */
    public String getUseProductKey() {
        return myCurrentProduct;
    }

    /**
     * Generate the Chart itself. Basically the stuff that will draw the chart
     * in the composite
     */
    public Object getNewGUIForChart(Object parent) {
        // FIXME: maybe some text? "Hey this isn't working?"
        //Composite nothing = new Composite((Composite)parent, SWT.NONE);
        //return nothing;
        return null;
    }

    /**
     * Get extra menu items for the chart
     */
    public void addCustomTitleBarComponents(List<Object> addTo) {
    }

    /**
     * Generate a snapshot of this chart, if possible
     */
    public void takeSnapshot(String name) {
        LOG.info("Snapshot not implemented for this chart. :(");
    }

    /**
     * Set if this chart uses product filters
     */
    public void setUseProductFilters(boolean flag) {
        myUseProductFilters = flag;
    }

    /**
     * Get if this chart uses the product filters
     */
    public final boolean getUseProductFilters() {
        return myUseProductFilters;
    }

    // Given a GLWorld object, render part of our chart stuff in it...
    public void drawChartGLWorld(GLWorld w){
        
    }
    
    public void updateOnMinTime(){}
    public void repaint(){}

    public void addViewComponent(String name, Object component) {
    }

    public void setTrackingPosition(FeatureList fl, FeaturePosition f) {
        // Nothing by default
    }
}
