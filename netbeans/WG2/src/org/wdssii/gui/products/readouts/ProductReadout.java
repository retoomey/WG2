package org.wdssii.gui.products.readouts;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductTextFormatter;

/**
 * The root helper class of all product readouts. This classes job is hide the
 * details of getting a readout. Currently I get readout from RadialSet and
 * LatLonGrids by color sampling, but other products might do this differently.
 *
 * Formatting of the text output is given to the ProductTextFormatter
 *
 * FIXME: Readout could be 'higher' than just 'products', generalize this
 *
 * @author Robert Toomey
 *
 */
public class ProductReadout {

    private final static Logger LOG = LoggerFactory.getLogger(ProductReadout.class);
    /**
     * The units for this readout
     */
    private String myUnits = "?";
    /**
     * The formatter used for readout
     */
    private ProductTextFormatter myFormatter = null;
    /**
     * Is readout valid? Something actually there?
     */
    private boolean myValid = false;
    /**
     * Background color for value
     */
    private Color myBackground = Color.BLACK;
    /**
     * Foreground color for data value
     */
    private Color myForeground = Color.WHITE;

    // Storage of floats  
    public static float byteBufferToFloat(ByteBuffer data) {

        byte d0 = data.get(0);
        byte d1 = data.get(1);
        byte d2 = data.get(2);
        byte d3 = data.get(3);
        return bytesToFloat(d0, d1, d2, d3);
    }

    /**
     * Convert four storage bytes into a float...
     */
    public static float bytesToFloat(byte d0, byte d1, byte d2, byte d3) {

        // byte type is SIGNED, we really just want the hex digits
        int b0 = (0x000000FF & d0);
        int b1 = (0x000000FF & d1);
        int b2 = (0x000000FF & d2);
        int b3 = (0x000000FF & d3);
        int v1 = (b3 << 24);
        int v2 = (b2 << 16);
        int v3 = (b1 << 8);
        int v4 = (b0);
        int finalBits = v1 + v2 + v3 + v4;
        float aFloat = Float.intBitsToFloat(finalBits);

        return aFloat;
    }

    // Storage of shorts
    /**
     * Convert two int into a float value. Note precision is lost, the range is
     * pinned to unsigned 0 to 65535
     */
    public static float intsToFloat(int d0, int d1) {

        // Pin to positive 0-65535, it's all the bits we have
        if (d0 < 0) {
            d0 = 0;
        }
        if (d0 > 65535) {
            d0 = 65535;
        }
        if (d1 < 0) {
            d0 = 0;
        }
        if (d1 > 65535) {
            d1 = 65535;
        }

       // d0 = 258;
       // d1 = 8;
        int b0 = ((d1) << 16);
        int b1 = (d0);

        float aFloat = Float.intBitsToFloat(b1 + b0);
        return aFloat;
    }

    // Dumps int as pure formatted 32 binary bits (for testing)
    public static String format(int i) {
        return Long.toBinaryString((1L << 32) | (i & (-1L >>> 32))).substring(1)+"\n";
    }
    
       // Dumps int as pure formatted 32 binary bits (for testing)
    public static String format(byte i) {
        return Long.toBinaryString((1L << 8) | (i & (-1L >>> 8))).substring(1)+"\n";
    }

    /**
     * We get the four bytes of the float
     */
    public static int[] bytesToInts(byte d0, byte d1, byte d2, byte d3) {

        int[] fs = new int[2];
        fs[0] = ((0x000000FF & d1) << 8)+(0x000000FF & d0);
        fs[1] = ((0x000000FF & d3) << 8)+(0x000000FF & d2);   
        return fs;
    }

    /**
     * Get the readout for this product at given point in view..
     */
    public void doReadoutAtPoint(Product prod, Point p, Rectangle view, GLWorld w) {

        // Default for now uses color trick...this works for RadialSets,
        // LatLonGrids

        String units = "";
        setUnits(units);
        myValid = true;
    }

    /**
     * For now, simple method to get formatted string output for readout
     */
    public String getReadoutString() {
        return "BaseEmpty";
    }

    public void setValid() {
        myValid = true;
    }

    /**
     * Get if this readout is valid
     */
    public boolean isValid() {
        return myValid;
    }

    /**
     * Get the units such as "dbZ" for this readout
     */
    public String getUnits() {
        return myUnits;
    }

    /**
     * Set the units for this readout
     */
    public void setUnits(String u) {
        myUnits = u;
    }

    public Color getBackground() {
        return myBackground;
    }

    public void setBackground(Color b) {
        myBackground = b;
    }

    public Color getForeground() {
        return myForeground;
    }

    public void setForeground(Color b) {
        myForeground = b;
    }
}
