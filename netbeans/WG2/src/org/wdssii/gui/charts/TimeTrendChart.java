package org.wdssii.gui.charts;

import gov.nasa.worldwind.geom.LatLon;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.volumes.LLHAreaFeature;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.Navigation;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.volumes.LLHArea;

public class TimeTrendChart extends ChartViewJFreeChart {

    static boolean toggle = false;
    private VSliceDataset myDataset = new VSliceDataset();
    private NumberAxis myRangeAxis = null;
    private NumberAxis myHeightAxis = null;
    //private JFreeChart myJFreeChart = null;

    public static class VSliceChartRenderer extends XYBlockRenderer {

        //private ColorMap myColorMap = null;
        public float[] myColors = null;
        private static final long serialVersionUID = -1814981271936657507L;

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
        //public void setColorMap(ColorMap map){
        //	myColorMap = map;
        //}

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                int series, int item, CrosshairState crosshairState, int pass) {
            //				System.out.println("drawItem called "+dataArea.getX());

            //	if (toggle){
            // super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset,
            //		 series, item, crosshairState, pass);
            //	}else{
            double x = dataset.getXValue(series, item);
            double y = dataset.getYValue(series, item);
            double z = 0.0;
            if (dataset instanceof XYZDataset) {
                z = ((XYZDataset) dataset).getZValue(series, item);
            }
            //    Paint p =this.getPaintScale().getPaint(z);
            Paint p;
            if (myColors == null) {
                p = this.getPaintScale().getPaint(z);
            } else {
                int cstart = item * 3;

                p = new Color(myColors[cstart], myColors[cstart + 1], myColors[cstart + 2]);
            }

            double bwidth = getBlockWidth();
            double bheight = getBlockHeight();
            double xOffset = -bwidth / 2.0;  // Anchor center
            double yOffset = -bheight / 2.0;

            // RectangleAnchor anchor = getBlockAnchor();
            //System.out.println("Here it is "+anchor);

            //  Paint p = this.paintScale.getPaint(z);
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
            } else {
                block = new Rectangle2D.Double(Math.min(xx0, xx1),
                        Math.min(yy0, yy1), Math.abs(xx1 - xx0),
                        Math.abs(yy1 - yy0));
            }
            g2.setPaint(p);
            g2.fill(block);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(block);
            // System.out.println("item "+item);
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addEntity(entities, block, dataset, series, item, 0.0, 0.0);
            }
            //	}
            //toggle = !toggle;
        }

        public void setColors(float[] colors) {
            myColors = colors;

        }
    }

    /**
     * We implement the XYZDataset so that our vslice data can go into any table
     * type that is made by the JFreeChart library (That uses an XYZDataset)
     *
     * @author Robert Toomey
     *
     */
    public static class VSliceDataset implements XYZDataset {

        private int numOfCols = 0;
        private int numOfRows = 0;
        private float[] myColors = null;

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
            //if (myVSlice != null){
            //	float[] f = myVSlice.getTableValues();
            //	return f[item];
            //}/else{
            return 0.0;
            //}
        }

        /**
         * THe VSliceChartRenderer uses this to get the color. This is for
         * speed, since usually the VSlice in the 3d window will be same as the
         * chart (for now)
         */
        public Color getColor(int series, int item) {
            //if (myVSlice != null){
            //	float[] f = myVSlice.getColors();
            float r, g, b;
            if (myColors != null) {
                int index = item * 3;
                r = myColors[index];
                g = myColors[index + 1];
                b = myColors[index + 2];
            } else {
                r = g = b = 0;
            }
            return new Color(r, g, b);
        }

        public void setColor(float[] c) {
            myColors = c;
        }

        public void setNumCols(int c) {
            numOfCols = c;
        }

        public void setNumRows(int r) {
            numOfRows = r;
        }

        @Override
        public DatasetGroup getGroup() {
            return null;
        }

        @Override
        public Comparable getSeriesKey(int series) {
            return "Vertical Slice";
        }

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
        //public void setVSlice(LLHAreaSlice slice) {
        //    myVSlice = slice;
        //}
    }
    static int counter = 0;
    // We use this so that we only update when the vslice CHANGES.
    // VSlices keep a counter of each time they recreate the vslice 'grid' of colors.
    private int myIterationCount = -1;
    private VSliceChartRenderer myRenderer = null;
    /**
     * The XAxis for the vslice showing range
     */
    //private NumberAxis myXAxis = null;
    /**
     * The YAxis of the vslice showing height
     */
    //private NumberAxis myYAxis = null;
    /**
     *
     */
    private static final long serialVersionUID = 385221424727576754L;

    public TimeTrendChart(String arg0, Font arg1, Plot arg2, VSliceDataset data, VSliceChartRenderer r, boolean arg3) {

        myJFreeChart = new JFreeChart(arg0, arg1, arg2, arg3);
        myDataset = data;
        myRenderer = r;
        //myPlot = (VSlicePlot) arg2;
        //this.addSubtitle(new TextTitle("Testing"));
    }

    /**
     * Return the LLHAreaSlice that we are currently drawing a plot for
     */
    /* public LLHAreaSlice getVSliceToPlot() {
     // -------------------------------------------------------------------------
     // Hack snag the current slice and product...
     // Hack for now....we grab first 3d object in our FeatureList
     LLHAreaSlice slice = null;
     LLHAreaFeature f = (LLHAreaFeature) FeatureList.theFeatures.getFirstFeature(LLHAreaFeature.class);
        
     if (f != null){
     LLHArea area = f.getLLHArea(); 
     if (area instanceof LLHAreaSlice){
     slice = (LLHAreaSlice) (area);
     }
     }
     return slice;
     }
     */
    /**
     * Called during dragging of vslice to explicitly update the chart. The
     * chart checks for changes and only draws when the vslice if different
     */
    @Override
    public void updateChart(boolean force) {
        //myDataset.setVSlice(getVSliceToPlot());

        // On update, pull top product (for now)
        // FIXME: This should be a util

       // Product p = ProductManager.getInstance().getTopProduct();

        // If we found a product, we can do the slice range.....
        // We snag 10 products in time...
        // We'll probably need a thread for this to lazy update the table,
        // some products are HUGE...

        // LLHAreaSlice slice = getVSliceToPlot();
       /* if (slice != null) {
         int numberHeight = 50; // fairly cheap
         int numberOfTimes = 10;// the pig volumes
         LatLon l = slice.getLeftLocation();
         //LatLon r = slice.getRightLocation();

         // FIXME: duplicate code with LLHAreaSlice marching
         // Maybe we create an iterator class....
         double startLat = l.getLatitude().getDegrees();
         double startLon = l.getLongitude().getDegrees();
         //double endLat = r.getLatitude().getDegrees();
         //double endLon = r.getLongitude().getDegrees();
         //double deltaLat = (endLat-startLat)/numberOfTimes;
         //double deltaLon = (endLon-startLon)/numberOfTimes;

         double bottom = slice.getBottomHeightKms();
         double top = slice.getTopHeightKms();
         double deltaHeight = (top - bottom) / numberHeight;

         //int iteration = slice.getIterationCount();
         //if (iteration == myIterationCount){
         // Nothing has changed in the vslice, so do nothing...
         // FIXME: this won't work for multiple vslices...
         //	return;
         //}
         //	myIterationCount = iteration;
         //	myRenderer.setColors(slice.getColors());
         //myRangeAxis.setRange(new Range(0, slice.getRangeKms()/1000.0));
         myHeightAxis.setRange(new Range(slice.getBottomHeightKms() / 1000.0, slice.getTopHeightKms() / 1000.0));

         //float[][] data = new float[numberOfTimes][numberHeight]; // fixme need this
         int colorCounter = 0;
         float[] colors = new float[3 * numberOfTimes * numberHeight];
         ColorMapOutput output = new ColorMapOutput();

         Product c = p;
         ProductVolume v = null;
         // Do we trend backwards from the selected?  Forward?? How to do it?
         // Let's go backwards from the selected value...
         double curHeight = bottom;
         Location buffer = new Location(0,0,0);
         for (int i = 0; i < numberOfTimes; i++) {

         // Check for product and then volume existance...
         boolean haveData = false;
         if (c != null) {
         v = c.getProductVolume(false);
         if (v != null) {
         haveData = true;
         }
         }
         // Draw order is row order, however with volumes we go left to right to save memory,
         // so we calculate the correct color location

         if (haveData) {//&& (i %2 !=0)){		
         System.out.println("Have data " + v.toString());
         // Sample in the volume the entire height range...
         curHeight = bottom;
         for (int j = 0; j < numberHeight; j++) {

         buffer.init(startLat, startLon, curHeight/1000.0f);
         //v.getValueAt(startLat, startLon, curHeight, output, null, null, false);
         v.getValueAt(buffer, output, null, null, false, null);
         colorCounter = (3 * numberOfTimes * (numberHeight - j - 1)) + (3 * i);
         colors[colorCounter++] = output.redF();
         colors[colorCounter++] = output.greenF();
         colors[colorCounter++] = output.blueF();
         curHeight += deltaHeight;
         }
         //Date time = c.getTime(); // time for axis...
         c = c.getProduct(Navigation.PreviousTime);
         } else {
         // Fill with missing value???
         for (int j = 0; j < numberHeight; j++) {
         colorCounter = (3 * numberOfTimes * (numberHeight - j - 1)) + (3 * i);
         colors[colorCounter++] = 1;
         colors[colorCounter++] = 0;
         colors[colorCounter++] = 0;
         curHeight += deltaHeight;
         }

         }
         }
         myRenderer.setColors(colors);  //????
         myDataset.setNumRows(numberHeight);
         myDataset.setNumCols(numberOfTimes);
         System.out.println("rows cols are " + numberHeight + ", " + numberOfTimes);
         XYPlot plot = (XYPlot) (myJFreeChart.getPlot());
         plot.getDomainAxis().setRange(new Range(0, numberOfTimes - 1));
         plot.getRangeAxis().setRange(new Range(0, numberHeight - 1));
         }

         // Gotta read each product over time...hummmm
         // FIXME: Need some sort of threaded interface for reading in data values...
         myJFreeChart.setTitle("Showing (Refresh" + counter++ + ")");
         */
    }

    /**
     * Static method to create a vslice chart
     */
    public static TimeTrendChart create() {

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
        PaintScale scale = new GrayPaintScale(0.0, 100.0);
        renderer.setPaintScale(scale);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        //	plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.green);
        TimeTrendChart chart = new TimeTrendChart("Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, dataset, renderer, true);
        chart.myJFreeChart.removeLegend();
        //	chart.setBackgroundPaint(Color.white);

        // The range in KM for the VSlice
	/*	NumberAxis xAxis2 = new NumberAxis("Range KM");
         xAxis2.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
         xAxis2.setLowerMargin(0.0);
         xAxis2.setUpperMargin(0.0);
         plot.setDomainAxis(1, xAxis2);
         plot.setDomainAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
         chart.myRangeAxis = xAxis2;
         */
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

        //plot.setDomainAxes(new ValueAxis[]{ xAxis, xAxis2, xAxis3 });
        return chart;
    }
}