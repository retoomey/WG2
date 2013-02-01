package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;
import java.awt.Rectangle;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;
import org.wdssii.gui.products.RadialSetReadout;
import org.wdssii.storage.Array1D;
import org.wdssii.util.RadialUtil;

/**
 *
 * @author rtoomey
 */
public abstract class RadialSetRenderer extends ProductRenderer {

    protected QuadStripRenderer myQuadRenderer = new QuadStripRenderer();

    /**
     * RadialHeightGateCache.
     */
    public static class RadialATHeightGateCache {

        /**
         * Cache of height value for each gate
         */
        public double[] heights;
        /**
         * Cache of gcd of sin for each gate
         */
        public double[] gcdSinCache;
        /**
         * Cache of gcd of cos for each gate
         */
        public double[] gcdCosCache;
        /**
         * Size of us
         */
        public int size;

        /**
         * Create a height gate cache
         */
        public RadialATHeightGateCache(RadialSet set, Radial radial, int maxGateCount,
                double sinElevAngle, double cosElevAngle) {
            if (radial != null) {
                generateHeightForEachGate(set, radial, maxGateCount, sinElevAngle, cosElevAngle);
            }
        }

        /**
         * Generate the height for each gate
         */
        private void generateHeightForEachGate(RadialSet set, Radial aRadial, int maxGateCount, double sinElevAngle, double cosElevAngle) {
            // System.out.println("Begin height cache....");
            heights = new double[maxGateCount + 1];
            // gcdCache = new double[maxGateCount+1];
            gcdSinCache = new double[maxGateCount + 1];
            gcdCosCache = new double[maxGateCount + 1];
            size = maxGateCount + 1;
            double rangeMeters = set.getRangeToFirstGateKms() * 1000.0;
            double gateWidthMeters = aRadial.getGateWidthKms() * 1000.0;
            //	System.out.println("Gate width meters is "+gateWidthMeters);
            for (int i = 0; i <= maxGateCount; i++) {
                heights[i] = RadialUtil.getAzRanElHeight(rangeMeters,
                        sinElevAngle);
                double gcd = RadialUtil.getGCD(rangeMeters,
                        cosElevAngle, heights[i]);
                gcdSinCache[i] = RadialUtil.getGCDSin(gcd);
                gcdCosCache[i] = RadialUtil.getGCDCos(gcd);
                rangeMeters += gateWidthMeters;
            }
        }
    }

    public RadialSetRenderer(boolean asBackgroundJob) {
        super(asBackgroundJob);
    }

    public void allocateMemory(RadialSet radialSet) {
        // "Counter" loop. Faster to calculate than reallocate memory,
        // Radial sets have missing parts no way to quick count it that
        // I can see. We also find the maximum number of gates and
        // create a attenuation height cache
        int counter = 0;
        int ccounter = 0;
        int numRadials = radialSet.getNumRadials();
        for (int i = 0; i < numRadials; i++) {

            // If missing, just continue on
            Radial r = radialSet.getRadial(i);
            int numGates = r.getNumGates();
            if (numGates == 0) {
                continue;
            }
            int lastJWithData = -2;
            Array1D<Float> values = r.getValues();
            boolean startQuadStrip;
            for (int j = 0; j < numGates; j++) {
                float value = values.get(j);
                if (value == DataType.MissingData) {
                } else {
                    if (lastJWithData == (j - 1)) {
                        startQuadStrip = false;
                    } else {
                        startQuadStrip = true;
                    }
                    if (startQuadStrip) {
                        counter += 6; // 2 * 3
                        ccounter += 8; // 2*4
                    }
                    counter += 6;
                    ccounter += 8;
                }
            }
        }
        // --------------End counter loop

        myQuadRenderer.allocate(counter, ccounter);

    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        if (isCreated()) {
            myQuadRenderer.drawData(dc, readoutMode);
        }
    }
    
    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }
    
    /**
     * Get the readout for this product
     */
    @Override
    public ProductReadout getProductReadout(Point p, Rectangle view, DrawContext dc) {

        RadialSetReadout out = new RadialSetReadout();
        if (p != null) {

            // FIXME: outside of radial should be unavailable...
            float value = myQuadRenderer.getReadout(p, view, dc, DataType.MissingData);
            out.setValue(value);

            Product prod = getProduct();
            String units = "";
            if (prod != null) {
                units = prod.getCurrentUnits();
            }
            out.setUnits(units);

        } else {
            //out.setValue(readoutValue);
            //out = "No readout for renderer";
        }
        return out;
    }
}
