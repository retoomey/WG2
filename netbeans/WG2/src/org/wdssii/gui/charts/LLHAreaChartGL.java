package org.wdssii.gui.charts;

import java.awt.geom.Rectangle2D;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaFeature;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * Duplicates LLHAreaChart..  humm we need to redesign some of the hierarchy here
 * 
 * @author Robert Toomey
 */
public class LLHAreaChartGL extends DataView {
    
    /**
     * The last GIS key of our LLHArea. If the physical area changes this key
     * does
     */
    private String myGISKey = "";
    /**
     * The full key representing a state of chart. If this changes, chart must
     * be regenerated. We build a lot of strings..might be able to do it better
     * with object list
     */
    private String myFullKey = "";

    /**
     * Return an LLHAreaFeature that contains a LLHAreaSlice
     */
    public static class VSliceFilter implements FeatureList.FeatureFilter {

        @Override
        public boolean matches(Feature f) {
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature a = (LLHAreaFeature) f;
                LLHArea area = a.getLLHArea();
                if (area instanceof LLHAreaSet) {
                    if (area.getLocations().size() > 1) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Return the LLHAreaSlice that we are currently drawing a plot for
     */
    public LLHAreaSet getLLHAreaToPlot() {
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
     * Get a pretty GIS label for rendering in a chart, for instance
     */
    public static String getGISLabel(double startLat, double startLon, double endLat, double endLong) {
        String newKey = String.format(
                "From (%5.2f, %5.2f) to (%5.2f, %5.2f)",
                startLat, startLon, endLat, endLong);
        return newKey;
    }

    public void setGISKey(String key) {
        myGISKey = key;
    }

    public String getGISKey() {
        return myGISKey;
    }

    public void setFullKey(String key) {
        myFullKey = key;
    }

    public String getFullKey() {
        return myFullKey;
    }

    /**
     * Utility to calculate the zoomed sub grid of the current data area given. This is used
     * by VSlice and Terrain to generate output graph. It is a 'sub grid'
     * because it is the current zoom 'sub' part of the larger full grid made by
     * the 2 end points of the slice.
     * 
     * Could put this in a plot class...
     */
    /*
    public static VolumeSliceInput calculateSubGrid(LLHArea area, Rectangle2D dataArea,
            ValueAxis rangeAxis, ValueAxis heightAxis, int rows, int cols) {
        VolumeSliceInput output = null;
        // Calculate the 'zoomed' lat/lon for our followed slice
        if (area != null) {
            VolumeSliceInput sourceGrid = area.getSegmentInfo(null, 0, rows, cols);
            if (sourceGrid != null) {
                VolumeSliceInput subGrid = new VolumeSliceInput(sourceGrid);

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
                double fullRange = area.getRangeKms(0, 1);  // 100% say 100 KMS
                double deltaLatPerKm = (subGrid.endLat - sLat) / fullRange;
                double deltaLonPerKm = (subGrid.endLon - sLon) / fullRange;

                // Now adjust the start/end lat/lon by percentage
                subGrid.startLat = sLat + (leftKms * deltaLatPerKm);
                subGrid.endLat = sLat + (rightKms * deltaLatPerKm);
                subGrid.startLon = sLon + (leftKms * deltaLonPerKm);
                subGrid.endLon = sLon + (rightKms * deltaLonPerKm);
                output = subGrid;
            }
        }
        return output;
    }*/
    
     @Override
    public Object getNewGUIForChart(Object parent) {
         return super.getNewGUIForChart(parent);
     }
}
