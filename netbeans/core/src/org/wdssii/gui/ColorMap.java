package org.wdssii.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.xml.Tag_color;
import org.wdssii.xml.Tag_colorBin;
import org.wdssii.xml.Tag_colorMap;

/**
 * Color map converts from a float value to a Color, using a set of ColorBins.
 * Colors fall into bins, which may linearly interpolate over a low to high value,
 * or may be a set color per bin.
 * 
 * ColorBin uses a ProductTextFormatter to generate the text.
 * @todo Allow dynamic changing of ProductTextFormatter
 * 
 * @author Robert Toomey
 * 
 */
public class ColorMap {

    private static Log log = LogFactory.getLog(ColorMap.class);

    /** Output object for color map queries.  You can pre-new this outside of loops
     * for speed.  Pass your object into routines
     */
    public static class ColorMapOutput {

        // We store as 0-255 in short (since java byte is 2's complement signed)
        /** Red stored as 0-255 (unsigned byte) */
        protected short red;
        /** Green stored as 0-255 (unsigned byte) */
        protected short green;
        /** Blue stored as 0-255 (unsigned byte) */
        protected short blue;
        /** Alpha stored as 0-255 (unsigned byte) */
        protected short alpha;
        /** Filtered data value, if any */
        public float filteredValue;

        public float redF() {
            return red / 255.0f;
        }

        public int redI() {
            return red;
        }

        public float greenF() {
            return green / 255.0f;
        }

        public int greenI() {
            return green;
        }

        private short pinToUnsignedByte(int in) {
            if (in > 255) {
                in = 255;
            }
            if (in < 0) {
                in = 0;
            }
            return (short) in;  // it fits now
        }

        /** Set the color using integer values 0-255 per field */
        public void setColor(int r, int g, int b, int a) {
            red = pinToUnsignedByte(r);
            green = pinToUnsignedByte(g);
            blue = pinToUnsignedByte(b);
            alpha = pinToUnsignedByte(a);
        }

        public float blueF() {
            return blue / 255.0f;
        }

        public int blueI() {
            return blue;
        }

        public float alphaF() {
            return alpha / 255.0f;
        }

        public int alphaI() {
            return alpha;
        }
    }
    // We will keep two lists, one of sorted bounds, the other of the color bins. 
    // These should be the same size.  We make access to these locked.
    private ArrayList<Float> myUpperBounds = new ArrayList<Float>();
    private ArrayList<ColorBin> myColorBins = new ArrayList<ColorBin>();
    private String myUnits = "";
    /** The maximum bin value that is not infinity.  Used for charts to pin range/domain to color map*/
    protected double myMaxValue = -100000.0;
    /** The minimum bin value that is not -infinity. */
    protected double myMinValue = +100000.0;
    /** Is this map generated from data?  It will be recreated when loading a new product then... */
    protected boolean myGeneratedMap = false;

    /**
     * Store information for one bin of the color map. Note this class is
     * internal to the color map
     * 
     * @param colorbinXML
     *            The root node of a colorbin xml tag
     */
    protected static abstract class ColorBin {

        /** All ColorBins have an upper bound.  Values <= this fall into the bin.
         * ColorMap keeps these in binary search order to turn value into bin 
         */
        protected float myUpperBound;
        protected short red;
        protected short green;
        protected short blue;
        protected short alpha;
        /** The bin label */
        protected String myLabel;
        // FIXME: these should go into the linear subclass...
        protected float myLowerBound;
        protected short red2;
        protected short green2;
        protected short blue2;
        protected short alpha2;

        ColorBin(float lowerBound, float upperBound,
                short r,
                short g,
                short b,
                short a,
                short r2,
                short g2,
                short b2,
                short a2,
                String name) {
            red = r;
            green = g;
            blue = b;
            alpha = a;

            red2 = r2;
            green2 = g2;
            blue2 = b2;
            alpha2 = a2;

            myUpperBound = upperBound;
            myLowerBound = lowerBound;
            myLabel = name;
        }

        /** All ColorBins need to fill in a ColorMapOutput object.  This object is created by caller
         * for speed purposes (usually this is called a LOT during generation of data) */
        public abstract void fillColor(ColorMapOutput out, float value);

        /** Simple color bin that linearly interpolates over a lower to upper color */
        public static class Linear extends ColorBin {

            public Linear(float lowerBound, float upperBound,
                    short r,
                    short g,
                    short b,
                    short a,
                    short r2,
                    short g2,
                    short b2,
                    short a2,
                    String name) {
                super(lowerBound, upperBound, r, g, b, a, r2, g2, b2, a2, name);
            }

            @Override
            public void fillColor(ColorMapOutput out, float value) {

                float wt = (value - myLowerBound) / (myUpperBound - myLowerBound);
                if (wt < 0.0f) {
                    wt = 0.0f;
                } else if (wt > 1.0f) {
                    wt = 1.0f;
                }

                // FIXME: make it nicer now that we're 0-255
                out.red = (short) (255.0 * (red2 / 255.0f + wt * (red / 255.0f - red2 / 255.0f)));
                out.green = (short) (255.0 * (green2 / 255.0f + wt * (green / 255.0f - green2 / 255.0f)));
                out.blue = (short) (255.0 * (blue2 / 255.0f + wt * (blue / 255.0f - blue2 / 255.0f)));
                out.alpha = (short) (255.0 * (alpha2 / 255.0f + wt * (alpha / 255.0f - alpha2 / 255.0f)));
            }
        }

        /** Simple color bin that returns a single color for our box */
        public static class Single extends ColorBin {

            public Single(float bound,
                    short r,
                    short g,
                    short b,
                    short a, String name) {
                super(bound, bound, r, g, b, a, r, g, b, a, name);
            }

            @Override
            public void fillColor(ColorMapOutput out, float value) {
                out.red = red;
                out.green = green;
                out.blue = blue;
                out.alpha = alpha;
            }
        }

        public static ColorBin ColorBinFactory(Tag_colorBin colorbinXML, float previousUpperBound,
                ProductTextFormatter f) {

            ColorBin newBin = null;

            // -----------------------------------------------------------------------
            // Bind upper and lower bound values...
            // <colorBin upperBound=
            float ub, lb;
            /*  Using the new built in float parsing in Tag library....
            String upperBoundTag = colorbinXML.upperBound;
            //  String upperBoundTag = colorbinXML.getAttribute("upperBound");
            if (upperBoundTag.equalsIgnoreCase("infinity")) {
                ub = Float.POSITIVE_INFINITY;
            } else if (upperBoundTag.equalsIgnoreCase("-infinity")) {
                ub = Float.NEGATIVE_INFINITY;
            } else {
                ub = Float.parseFloat(upperBoundTag);
            }*/
            ub = colorbinXML.upperBound;
            lb = previousUpperBound;

            // -----------------------------------------------------------------------
            // Get the colors for the upper and lower bound
            // <colorBin...<color r= g= b= a=
            //  NodeList colorsXML = colorbinXML.getElementsByTagName("color");
            ArrayList<Tag_color> colors = colorbinXML.colors;

            short[] c = {255, 255, 255, 255};
            short[] lbc = {0, 0, 0, 0};
            short[] ubc = {255, 255, 255, 255};

            // int size = colorsXML.getLength();
            int size = colors.size();
            for (int i = 0; i < size; ++i) {
                Tag_color aColorXML = colors.get(i);
                int r = aColorXML.r;
                int g = aColorXML.g;
                int b = aColorXML.b;
                int a = aColorXML.a;
                c[0] = (short) r;
                c[1] = (short) g;
                c[2] = (short) b;
                c[3] = (short) a; // FIXME: check for lose of precision..shouldn't happen

                switch (i) {
                    case 0:  // On first color, make lower color and upper the same
                        //lbc = c;  // copy
                        lbc[0] = c[0];
                        lbc[1] = c[1];
                        lbc[2] = c[2];
                        lbc[3] = c[3];
                        ubc[0] = c[0];
                        ubc[1] = c[1];
                        ubc[2] = c[2];
                        ubc[3] = c[3];
                        break;
                    case 1:  // On second color, make upper color the new one
                        //ubc = c;
                        ubc[0] = c[0];
                        ubc[1] = c[1];
                        ubc[2] = c[2];
                        ubc[3] = c[3];
                        break;
                    default:
                        break; // Add for more colors in list if wanted...
                }
            }


            // This is how we determine if start of bin color is the same as the
            // ending color.
            boolean sameColor = ((ubc[0] == lbc[0]) && (ubc[1] == lbc[1]) && (ubc[2] == lbc[2]) && (ubc[3] == lbc[3]));

            // <colorBin name=
            String name;
            // if (colorbinXML.hasAttribute("name")) { // Because getAttribute can
            if (colorbinXML.name != null) {
                //  name = colorbinXML.getAttribute("name");
                name = colorbinXML.name;
            } else {
                // If no name is given for bin, use the bound to make one
                try {
                    if (sameColor) {
                        name = f.format(ub);
                    } else {
                        name = f.format(lb, ub);
                    }
                } catch (Exception e) { // FIXME: check format errors?
                    log.error("Exception is" + e.toString());
                    name = "?";
                }
            }

            // Determine if color bin is linear or not
            boolean linear = false;
            if ((sameColor) || (Double.isInfinite(ub))
                    || (Double.isInfinite(lb))
                    || (Double.isNaN(ub))
                    || (Double.isNaN(lb))) {
                //myLinear = false;
            } else {
                linear = true;
            }
            if (linear) {
                newBin = new Linear(lb, ub, ubc[0], ubc[1], ubc[2], ubc[3], lbc[0], lbc[1], lbc[2], lbc[3], name);
            } else {
                newBin = new Single(ub, ubc[0], ubc[1], ubc[2], ubc[3], name);
            }
            return newBin;
        }

        /**
         * parse the text of a color number field into an integer value
         * 
         * @param textOfNumber
         *            the raw text of number, such as 0xFF or 45
         */
        protected static int parseColorValue(String textOfNumber) {
            int value = 255;
            try {
                if (textOfNumber.toLowerCase().startsWith("0x")) {
                    textOfNumber = textOfNumber.substring(2);
                    value = Integer.parseInt(textOfNumber, 16);
                } else {
                    value = Integer.parseInt(textOfNumber);
                }
            } catch (NumberFormatException e) {
                // Recover...we'll just return 1.0;
            }
            return value;
        }

        /**
         * @return the upper bound of this color bin. The bin covers a range of
         *         lower bound to upperbound.
         */
        public float getUpperBound() {
            return myUpperBound;
        }

        /**
         * @return the lower bound of this color bin. The bin covers a range of
         *         lower bound to upperbound.
         */
        public float getLowerBound() {
            return myLowerBound;
        }

        /**
         * @return the label for this color bin. Each bin has a label which can
         *         be shown graphically in the display color key in the window.
         */
        public String getBinLabel() {
            return myLabel;
        }
    }

    /**
     * @return the number of sorted bins that we are holding onto
     */
    public int getNumberOfBins() {
        return myColorBins.size();
    }

    /**
     * @return the upper bound of given bin number. The bin covers a range of
     *         lower bound to upperbound.
     */
    public float getUpperBound(int i) {
        return myColorBins.get(i).myUpperBound;
    }

    /**
     * @return the lower bound of this color bin. The bin covers a range of
     *         lower bound to upperbound.
     */
    public float getLowerBound(int i) {
        return myColorBins.get(i).myLowerBound;
    }

    public void getLowerBoundColor(ColorMapOutput out, int i) {
        ColorBin b = myColorBins.get(i);
        out.red = b.red2;
        out.green = b.green2;
        out.blue = b.blue2;
        out.alpha = b.alpha2;
    }

    public void getUpperBoundColor(ColorMapOutput out, int i) {
        ColorBin b = myColorBins.get(i);
        out.red = b.red;
        out.green = b.green;
        out.blue = b.blue;
        out.alpha = b.alpha;
    }

    /**
     * @return the label for given color bin. Each bin has a label which can be
     *         shown graphically in the display color key in the window.
     */
    public String getBinLabel(int i) {
        return myColorBins.get(i).myLabel;
    }

    /**
     * Fill in a colormap object from XML information
     * 
     * @param Tag_colorMap
     *            The root node for generating this color map
     */
    public boolean initFromTag(Tag_colorMap aTag, ProductTextFormatter formatter) {
        if (aTag.unit != null) {
            myUnits = aTag.unit.name;
        }
        // Change any names such as 'white' into RGB values...
        ProductManager.getInstance().updateNamesToColors(aTag);

        float previousUpperBound = Float.NEGATIVE_INFINITY;
        for (Tag_colorBin bin : aTag.colorBins) {
            //ColorBin aColor = new ColorBin(aColorBinXML, previousUpperBound);
            ColorBin aColor = ColorBin.ColorBinFactory(bin, previousUpperBound, ProductTextFormatter.DEFAULT_FORMATTER);
            myColorBins.add(aColor);
            previousUpperBound = aColor.getUpperBound();
            addUpperBound(previousUpperBound);
        }

        // Sort the upperbound, and since the index of it will find our color
        // bin,
        // sort the color bins by the same. Usually the xml is in order, but
        // this
        // will ensure we always work
        Collections.sort(myUpperBounds);
        Collections.sort(myColorBins, new Comparator<ColorBin>() {

            @Override
            public int compare(ColorBin o1, ColorBin o2) {
                double u1 = o1.getUpperBound();
                double u2 = o2.getUpperBound();
                if (u1 < u2) {
                    return -1;
                }
                if (u1 > u2) {
                    return 1;
                }
                return 0;
            }
        });
        return true;
    }

    /** Fill in a color map from linear information
     * 
     * @param numOfBins Number of unique bins
     * @param minValue Minimum value of color key (-infinity to this for first bin)
     * @param maxValue Maximum value of color key (this to +infinity for final bin)
     * @return true on success
     */
    public boolean initFromLinear(int numOfBins, float minValue, float maxValue, String units,
            ProductTextFormatter f) {

        float low = Float.NEGATIVE_INFINITY;
        float high = minValue;
        float step = (maxValue - minValue) / numOfBins;

        // FIXME: color ranging will need some work, for now black upto max (white)
        float redStep = 1.0f / numOfBins;
        float greenStep = 1.0f / numOfBins;
        float blueStep = 1.0f / numOfBins;
        float curRed = 0;
        float curGreen = 0;
        float curBlue = 0;

        float lowRed = 0;
        float lowGreen = 0;
        float lowBlue = 0;
        float lowAlpha = 1.0f;

        float upRed = 0;
        float upGreen = 0;
        float upBlue = 0;
        float upAlpha = 1.0f;

        // First bin is -infinity to minValue
        // Last bin has maxValue to +infinity
        for (int i = 0; i < numOfBins; ++i) {
            lowRed = curRed;
            lowGreen = curGreen;
            lowBlue = curBlue;
            curRed += redStep;
            curGreen += greenStep;
            curBlue += blueStep;
            upRed = curRed;
            upGreen = curGreen;
            upBlue = curBlue;
            ColorBin aColor = new ColorBin.Linear(low, high,
                    (short) (upRed * 255.0f), (short) (upGreen * 255.0f), (short) (upBlue * 255.0f), (short) (upAlpha * 255.0f),
                    (short) (lowRed * 255.0f), (short) (lowGreen * 255.0f), (short) (lowBlue * 255.0f), (short) (lowAlpha * 255.0f),
                    f.format((float) (low + step / 2.0)));
            myColorBins.add(aColor);
            addUpperBound(low);
            low = aColor.getUpperBound();
            if (i == numOfBins - 1) {
                high = Float.POSITIVE_INFINITY;
            } else {
                high += step;
            }
        }
        myUnits = units;
        myGeneratedMap = true;
        return true;
    }

    private void addUpperBound(float upper) {
        myUpperBounds.add(upper);
        if (!Float.isInfinite(upper)) {
            // Humm doesn't catch everything, some color maps are using
            // just 'low' values for range... FIXME: fix all color maps
            // Velocity for example uses -99899.5 which comes back as a real value
            //if (DataType.isRealDataValue((float)(upper))){
            if (upper > -99800.0) { // a guess
                if (upper > myMaxValue) {
                    myMaxValue = upper;
                }
                if (upper < myMinValue) {
                    myMinValue = upper;
                }
            }
        }
    }

    public double getMaxValue() {
        return myMaxValue;
    }

    public double getMinValue() {
        return myMinValue;
    }

    /** 
     * 
     * @return true if map is math generated from data range.
     */
    public boolean isGenerated() {
        return this.myGeneratedMap;
    }

    /** Fill in the color/final filtered value for a given value 
     * 
     */
    public void fillColor(ColorMapOutput out, float v) {

        int index = Collections.binarySearch(myUpperBounds, v);
        // Will be either: x == upperbound match which is the bin we want
        // or (-(insertion point) -1) which we convert to closest bin
        // 0, 1, 2, 3, 4 ---> 3 ==> 3, 3.2 => -5 ==> +1 == -4 -(x)=> 4
        int upperBound = (index < 0) ? -(index + 1) : index;
        //FIXME: will give size as index if above LAST value I think....
        try {
            myColorBins.get(upperBound).fillColor(out, v);
            out.filteredValue = v;
        } catch (Exception e) {
            out.red = 255;
            out.green = out.blue = out.alpha = 255;
            out.filteredValue = -99000;
        }
    }

    /**
     * @return the units for the color map, such as 'dBZ'
     */
    public String getUnits() {
        return myUnits;
    }

    public static Color getW3cContrast(Color back, Color fore) {
        // W3c contrast algorithm:
        int bright1 = ((back.getRed() * 299) + (back.getGreen() * 587) + (back.getBlue() * 114)) / 1000;
        int bright2 = ((fore.getRed() * 299) + (fore.getGreen() * 587) + (fore.getBlue() * 114)) / 1000;
        int diff = bright1 - bright2;
        if (diff < 0) {
            if (diff > -125) {
                fore = Color.black;
            }
        } else {
            if (diff < 125) {
                fore = Color.black;
            }
        }
        return fore;
    }
}
