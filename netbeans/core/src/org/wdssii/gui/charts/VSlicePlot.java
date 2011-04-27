package org.wdssii.gui.charts;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.volumes.LLHAreaSlice;

public class VSlicePlot extends XYPlot {

//Plot implements ValueAxisPlot 
    private ValueAxis myXAxis;
    private ValueAxis myYAxis;

    public VSlicePlot(XYDataset dataset,
            ValueAxis domainAxis,
            ValueAxis rangeAxis,
            XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
        myXAxis = domainAxis;
        myYAxis = rangeAxis;


        // Create a height domain axis
        //NumberAxis yAxis = new NumberAxis("Height");
        /// yAxis.setAutoRangeIncludesZero(false);
        // yAxis.setPlot(this);
        /// yAxis.addChangeListener(this);
        // domainAxisLocations...hummmm

        // We create an XYDataset with the four corners of the current VSlice,
        // this allows JFreeChart to scale axis for us..
        // Hummm..how to multiaxis?

    }

    @Override
    public ValueAxis getRangeAxis() {
        return myXAxis;
    }

    public ValueAxis getHeightAxis() {
        return myYAxis;
    }
    /**
     * 
     */
    private static final long serialVersionUID = 6151318589542420575L;

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

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        super.draw(g2, area, anchor, parentState, info);
    }

    @Override
    public String getPlotType() {
        return "TestPlotType";
    }

    /*
    @Override
    public void drawBackground(Graphics2D g2, Rectangle2D area){
    
    //g2.setColor(new Color(255, 0, 0));
    //int x = (int) area.getX();
    //int y = (int) area.getY();
    //int x2 = (int) (x+area.getWidth());
    //int y2 = (int) (y+area.getHeight());
    //g2.drawLine(x, y, x2, y2);
    drawVSlice(g2, area);
    //fillBackground(g2, area, PlotOrientation.VERTICAL);
    //drawQuadrants(g2, area);
    //drawBackgroundImage(g2, area);
    //System.out.println("Called draw background call "+x+","+y+","+x2+","+y2);
    
    }
    
    public void drawVSlice(Graphics2D g2, Rectangle2D area)
    {
    int numOfCols = 200;
    int numOfRows = 200;
    boolean haveData = false;
    float[] colors = null;
    double sliceRangeKMS = 0.0;
    double sliceHeightKMS = 0.0;
    
    // Grab data from our current 3d vslice, if any....
    LLHAreaSlice slice = getVSliceToPlot();
    if (slice != null){
    numOfRows = slice.getNumRows();
    numOfCols = slice.getNumCols();
    //colors = slice.getColors();
    sliceRangeKMS = slice.getRangeKms();
    sliceHeightKMS = slice.getHeightKms();
    ///if (colors != null){
    //	haveData = true;
    //}
    }
    if (!haveData){
    System.out.println("No data from a vslice to draw, returning...");
    return;
    }
    
    // Update axis only on a change in range...this will cause a 'draw'
    // Ugggghh.  Ok...we should set the range OUTSIDE this draw during dragging....but how?
    // FIXME: set this outside without a refresh
    Range r = myXAxis.getRange();
    if (r.getUpperBound() != sliceRangeKMS){
    //		myXAxis.setRange(new Range(0, sliceRangeKMS));
    //		return; // causes a redraw...
    }
    Range h = myYAxis.getRange();
    if (h.getUpperBound() != sliceHeightKMS){
    //		myYAxis.setRange(new Range(0, sliceHeightKMS));
    //		return ; // causes a redraw
    }
    
    // -------------------------------------------------------------------
    // Just a test to see if graphics2d can keep up.  We might need to 
    // have a opengl window here...
    double startX = area.getX();
    double startY = area.getY();
    double xSize = area.getWidth()/(1.0*numOfCols);
    double ySize = area.getHeight()/(1.0*numOfRows);
    //int width = (int)(xSize);
    //int height = (int)(ySize);
    
    //if (width < 1){ width = 1; }
    //if (height < 1){ height = 1; }
    int colorCounter = 0;
    
    for(int row = 0; row < numOfRows; row++){
    startX = area.getX();
    int startYInt = (int)(startY);
    int hy = ((int)(startY+ySize))-startYInt;
    for (int col = 0; col < numOfCols; col++){
    g2.setColor(new Color(colors[colorCounter++], colors[colorCounter++], colors[colorCounter++]));
    int startXInt = (int)(startX);
    int w = ((int)(startX+xSize))-startXInt; // The next rounded down x location....
    g2.fillRect(startXInt, startYInt, w+1, hy+1);
    startX += xSize;
    }
    
    startY += ySize;
    }
    }
     */
}
