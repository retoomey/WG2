package org.wdssii.gui.charts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.VolumeSlice2DOutput;
import org.wdssii.gui.volumes.LLHAreaSlice;

public class VSliceChart extends ChartViewJFreeChart {

    static boolean toggle = false;
    private VSliceDataset myDataset = new VSliceDataset();
    private NumberAxis myRangeAxis = null;
    private NumberAxis myHeightAxis = null;

    public static class VSlicePaintScale implements PaintScale {

        Color myColor;

        @Override
        public double getLowerBound() {
            // TODO Auto-generated method stub
            return 0;
        }

        public void hack(int r, int g, int b) {
            myColor = new Color(r, g, b);
        }

        @Override
        public Paint getPaint(double arg0) {
            return myColor;
        }

        @Override
        public double getUpperBound() {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    public static class VSliceChartRenderer extends XYBlockRenderer {

        VSlicePaintScale myPrivate = null;
        public int[] myColors = null;
        private static final long serialVersionUID = -1814981271936657507L;
        
        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                int series, int item, CrosshairState crosshairState, int pass) {
            if (myPrivate == null) {
                myPrivate = new VSlicePaintScale();
                myPrivate.hack(0, 0, 0);
                setPaintScale(myPrivate);
            }
            if (myColors == null) {
                //p = this.getPaintScale().getPaint(z);
            } else {
                int cstart = item * 3;
                if (myColors.length > cstart+2){
                     myPrivate.hack(myColors[cstart], myColors[cstart + 1], myColors[cstart + 2]);
                }
            }
            //setPaintScale(myPrivate); inf loop

            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
            /*
            double x = dataset.getXValue(series, item);
            double y = dataset.getYValue(series, item);
            double z = 0.0;
            if (dataset instanceof XYZDataset) {
            z = ((XYZDataset) dataset).getZValue(series, item);
            }
            Paint p;
            if (myColors == null){
            p = this.getPaintScale().getPaint(z);
            }else{
            int cstart = item*3;
            
            p = new Color(myColors[cstart], myColors[cstart+1], myColors[cstart+2]);
            }
            System.out.println("Draw item "+item*3);
            double bwidth = getBlockWidth();
            double bheight = getBlockHeight();
            double xOffset = -bwidth/2.0;  // Anchor center
            double yOffset = -bheight/2.0;
            
            double xx0 = domainAxis.valueToJava2D(x + xOffset, dataArea,
            plot.getDomainAxisEdge());
            double yy0 = rangeAxis.valueToJava2D(y + yOffset, dataArea,
            plot.getRangeAxisEdge());
            double xx1 = domainAxis.valueToJava2D(x + bwidth
            + xOffset, dataArea, plot.getDomainAxisEdge());
            double yy1 = rangeAxis.valueToJava2D(y + bheight
            + yOffset, dataArea, plot.getRangeAxisEdge());
            Rectangle2D block;
            PlotOrientation orientation = plot.getOrientation();
            if (orientation.equals(PlotOrientation.HORIZONTAL)) {
            block = new Rectangle2D.Double(Math.min(yy0, yy1),
            Math.min(xx0, xx1), Math.abs(yy1 - yy0),
            Math.abs(xx0 - xx1));
            }
            else {
            block = new Rectangle2D.Double(Math.min(xx0, xx1),
            Math.min(yy0, yy1), Math.abs(xx1 - xx0),
            Math.abs(yy1 - yy0));
            }
            g2.setPaint(p);
            g2.fill(block);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(block);
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
            addEntity(entities, block, dataset, series, item, 0.0, 0.0);
            }	*/
        }

        public void setColors(int[] colors) {
            myColors = colors;
        }
    }

    /** We implement the XYZDataset so that our vslice data can go into any table
     * type that is made by the JFreeChart library (That uses an XYZDataset)
     * @author Robert Toomey
     *
     */
    public static class VSliceDataset implements XYZDataset {

        LLHAreaSlice myVSlice;
        int numOfCols = LLHAreaSlice.myNumCols;
        int numOfRows = LLHAreaSlice.myNumRows;

        @Override
        public int getSeriesCount() {
            return 1;
        }

        @Override
        public int getItemCount(int series) {
            return numOfCols * numOfRows;
        }

        @Override
        public Number getX(int series, int item) {
            return new Double(getXValue(series, item));
        }

        @Override
        public double getXValue(int series, int item) {
            return item % numOfCols;  // X is the column number
        }

        @Override
        public Number getY(int series, int item) {
            return new Double(getYValue(series, item));
        }

        @Override
        public double getYValue(int series, int item) {
            return numOfRows - (Math.floor(item / numOfCols)) - 1;
        }

        @Override
        public Number getZ(int series, int item) {
            return new Double(getZValue(series, item));
        }

        @Override
        public double getZValue(int series, int item) {
            return 0.0;
        }

        @Override
        public DatasetGroup getGroup() {
            return null;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Comparable getSeriesKey(int series) {
            return "Vertical Slice";
        }

        @SuppressWarnings("rawtypes")
        @Override
        public int indexOf(Comparable seriesKey) {
            return 0;
        }

        @Override
        public DomainOrder getDomainOrder() {
            return DomainOrder.ASCENDING;
        }

        @Override
        public void addChangeListener(DatasetChangeListener listener) {
            // ignore
        }

        @Override
        public void removeChangeListener(DatasetChangeListener listener) {
            // ignore
        }

        @Override
        public void setGroup(DatasetGroup group) {
            // ignore
        }

        public void setVSlice(LLHAreaSlice slice) {
            myVSlice = slice;
        }
    }
    static int counter = 0;
    // We use this so that we only update when the vslice CHANGES.
    // VSlices keep a counter of each time they recreate the vslice 'grid' of colors.
    private int myIterationCount = -1;
    private VSliceChartRenderer myRenderer = null;
    private Object myCurrentKey;

    public VSliceChart(String arg0, Font arg1, Plot arg2, VSliceDataset data, VSliceChartRenderer r, boolean arg3) {

        myJFreeChart = new JFreeChart(arg0, arg1, arg2, arg3);
        myDataset = data;
        myRenderer = r;
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

    /** Called during dragging of vslice to explicitly update the chart.  The chart checks for
     * changes and only draws when the vslice if different
     */
    @Override
    public void updateChart() {

        FilterList aList = null;
        LLHAreaSlice slice = getVSliceToPlot();
        myDataset.setVSlice(slice);

        // Slice existence check....
        if (slice == null) {
            return;
        }   // No slice to follow, return..


        /*int iteration = slice.getIterationCount();
        boolean iterationDifferent = false;
        if (iteration != myIterationCount){
        iterationDifferent = true;
        //System.out.println("VSLICE ITERATION CHANGE "+iteration+" was "+myIterationCount);
        }
        myIterationCount = iteration;
         */

        /** Get the volume we are following */
        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());
        if (volume == null){ return; }
        //	String key = volume.getKey();
        //	boolean keyDifferent = false;
        //	if (!key.equals(myCurrentKey)){
        //		keyDifferent = true;
        //	}
        //	myCurrentKey = key;

        /** Get the filter list of the product we are following */
        ProductHandlerList phl = ProductManager.getInstance().getProductOrderedSet();
        if (phl != null) {
            ProductHandler tph = phl.getProductHandler(getUseProductKey());
            if (tph != null) {
                aList = tph.getFList();
            }
        }
        if (aList == null){ return; }
        
        /** Get the GIS key of the slice */
        String key = slice.getGISKey();

        /** Add volume key */
        key += volume.getKey();

        /** Add filter key */
        key += aList.getFilterKey(getUseProductFilters());

        //String key = slice.getKey(getUseVirtualVolume(), aList, getUseProductFilters());

        boolean keyDifferent = false;
        if (!key.equals(myCurrentKey)) {
            keyDifferent = true;
        }
        myCurrentKey = key;
        //if (!(iterationDifferent || keyDifferent)){
        //	return;
        //}
        if (!keyDifferent) {
            return;
        }

        if (keyDifferent) {
            //System.out.println("VSLICE KEY CHANGE");
        }
        VolumeSlice2DOutput dest = new VolumeSlice2DOutput();

        volume.generate2DGrid(slice.getGrid(), dest, aList, getUseProductFilters());

        myRenderer.setColors(dest.getColor2dFloatArray(0));
        myRangeAxis.setRange(new Range(0, slice.getRangeKms() / 1000.0));
        myHeightAxis.setRange(new Range(slice.getBottomHeightKms() / 1000.0, slice.getTopHeightKms() / 1000.0));
        //myHeightKms = slice.getHeightKms();

        myJFreeChart.setTitle("Vertical Slice (F" + counter++ + ")");
    }

    /** Static method to create a vslice chart, called by reflection */
    public static VSliceChart createVSliceChart() {

        System.out.println("CREATE VSLICE CHART CALLED");
        VSliceDataset dataset = new VSliceDataset();

        // These get autosized to the 'units' of the slice...
        NumberAxis xAxis = new NumberAxis("X");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);

        NumberAxis yAxis = new NumberAxis("Y");
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);

        // These axis don't mean much for us.  They show the 'block' number, which is useful for debugging
        // only.  The library needs it though
        //xAxis.setVisible(false);
        //yAxis.setVisible(false);

        VSliceChartRenderer renderer = new VSliceChartRenderer();

        // We really need our own paint scale thing...
        //	PaintScale scale = new GrayPaintScale(0.0, 100.0);
        //	renderer.setPaintScale(scale);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        //	plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.green);
        VSliceChart chart = new VSliceChart("Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, dataset, renderer, true);
        chart.myJFreeChart.removeLegend();
        //	chart.setBackgroundPaint(Color.white);

        // The range in KM for the VSlice
        NumberAxis xAxis2 = new NumberAxis("Range KM");
        xAxis2.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis2.setLowerMargin(0.0);
        xAxis2.setUpperMargin(0.0);
        plot.setDomainAxis(1, xAxis2);
        plot.setDomainAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
        chart.myRangeAxis = xAxis2;

        // The height in KM for the VSlice
        NumberAxis yAxis2 = new NumberAxis("Height KM");
        yAxis2.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis2.setLowerMargin(0.0);
        yAxis2.setUpperMargin(0.0);
        plot.setRangeAxis(1, yAxis2);
        plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
        chart.myHeightAxis = yAxis2;

        // Just a test, we're probably going to have to make our own
        // scale subclass that uses our color map
		/*	NumberAxis colorScale = new NumberAxis("Color");
        colorScale.setAutoRange(true);
        PaintScaleLegend p = new PaintScaleLegend(scale, colorScale);
        p.setSubdivisionCount(20);
        p.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        p.setAxisOffset(5D);
        //	p.setMargin(new RectangleInsets(5D, 5D, 5D, 5D));
        p.setFrame(new BlockBorder(Color.red));
        //	p.setPadding(new RectangleInsets(10D, 10D, 10D, 10D));
        chart.addSubtitle(p);
         */
        //NumberAxis xAxis3 = new NumberAxis("Number2");
        //	xAxis3.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        //xAxis3.setLowerMargin(0.0);
        //	xAxis3.setUpperMargin(0.0);
        chart.myJFreeChart.setBorderVisible(true);
        //plot.setDomainAxes(new ValueAxis[]{ xAxis, xAxis2, xAxis3 });
        return chart;
    }

    @Override
    public Object getNewGUIBox(Object parent) {
        /* final Composite box = new Composite((Composite) parent, SWT.NONE);
        box.setLayout(new RowLayout());
        
        // SRV 
        Text srv = new Text(box, SWT.LEFT);
        srv.setText("HeightKMs:");
        srv.setEditable(false);
        final Spinner speedSpin = new Spinner(box, 0);
        speedSpin.setMaximum(200);
        speedSpin.setMinimum(-200);
        speedSpin.setSelection(12);
        
        // Add listeners for GUI elements
        speedSpin.addSelectionListener(new SelectionListener() {
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
        }
        
        @Override
        public void widgetSelected(SelectionEvent arg0) {
        //myCutOff = speedSpin.getSelection();
        //myDirtySRM = true;
        //CommandManager.getInstance().executeCommand(new ProductChangeCommand.ProductFilterCommand(), true);
        }
        });
        
        return box;
         * 
         */
        return null;
    }
}