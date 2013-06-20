package org.wdssii.gui.products.readouts;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.geom.GLWorld;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.gui.products.renderers.ProductRenderer;

/**
 * The root helper class of all product readouts. This classes job is hide the
 * details of getting a readout. Currently I get readout from RadialSet and
 * LatLonGrids by color sampling, but other products might do this differently.
 *
 * Formatting of the text output is given to the ProductTextFormatter
 *
 * @author Robert Toomey
 *
 */
public class ProductReadout {

    private final static Logger LOG = LoggerFactory.getLogger(ProductReadout.class);
    /**
     * The data value
     */
    private float myDataValue = DataType.MissingData;
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

    /**
     * Get the readout for this product at given point in view..
     */
    public void doReadoutAtPoint(Product prod, Point p, Rectangle view, GLWorld w) {

        // Default for now uses color trick...this works for RadialSets,
        // LatLonGrids
        float value = DataType.MissingData;
        String units = "";

        if (prod != null) {
            ProductRenderer pr = prod.getRenderer();
            if (pr != null) {
                value = pr.getReadoutValue(p, view, w);
            }
            // Get the formatter for this product...
            ProductTextFormatter f = prod.getProductFormatter();
            if (f != null) {
                myFormatter = f;
            }
            // Setup units of readout
            units = prod.getCurrentUnits();
        }

        setValue(value);
        setUnits(units);
    }

    /**
     * For now, simple method to get formatted string output for readout
     */
    public String getReadoutString() {
        String readout = "N/A";
        if (isValid()) {
            float v = getValue();
            String u = getUnits();
            if (myFormatter != null) {
                readout = myFormatter.formatForReadout(v, u);
            } else {
                // Crappy fallback
                readout = String.format("%5.2f %s", v, u);
            }
        }
        return readout;
    }

    /**
     * Get if this readout is valid
     */
    public boolean isValid() {
        return myValid;
    }

    /**
     * Get the data value for this readout.
     */
    public float getValue() {
        return myDataValue;
    }

    /**
     * Set the data value for this readout
     */
    public void setValue(float v) {
        myDataValue = v;
        myValid = true;
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
