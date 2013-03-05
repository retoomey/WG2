package org.wdssii.gui.products;

import java.awt.Color;
import org.wdssii.datatypes.DataType;

/**
 * The root class of all product readouts. This is a readout for one point on
 * the screen. We do this type of thing a lot so eventually this may become more
 * generic with other stuff such as vslice output, etc.
 *
 * This may eventually be created by a ProductReadoutFactory (to generalize it
 * more), Currently created by ProductRenderer getProductReadout, since the only
 * method I'm using right now is the color based rendering readout.
 *
 * FIXME: Windfield has multiple data values....do we generalize
 * multi-dimensional data or have an object per DataType?
 *
 * @author Robert Toomey
 *
 */
public class ProductReadout {

    /**
     * The data value
     */
    private float myDataValue = DataType.MissingData;
    /**
     * The units for this readout
     */
    private String myUnits = "?";
    /**
     * The formatter used for single strings
     */
    private String myFormatter = "%3.2f ";
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

    /**
     * Get the formatter for this readout
     */
    public String getFormatter() {
        return myFormatter;
    }

    /**
     * Set the formatter for this readout
     */
    public void setFormatter(String f) {
        myFormatter = f;
    }

    /**
     * For now, simple method to get formatted string output for readout
     */
    public String getReadoutString() {

        String readout = "N/A";
        if (isValid()) {
            float v = getValue();
            String u = getUnits();

            if (v == DataType.MissingData) {
                readout = "Missing";
            } else if (v == DataType.RangeFolded) {
                readout = "RF";
            } else if (v == DataType.DataUnavailable) {
                readout = "Unavailable";
            } else {
                readout = String.format(myFormatter, v);
                readout += u;
            }
        }
        return readout;
    }

    // Convert from a data value to a string. 
    // This is used by the 2D Table chart only for ALL products..bleh
    // FIXME: non-static...called by Table, etc from ProductReadout directly..
    public static String valueToString(float value) {
        String text = null;
        if (DataType.isRealDataValue(value)) {
            if (Math.abs(value) < .05) {
                text = String.format("%5.5f", value);
            } else {
                text = String.format("%5.2f", value);
            }

        } else {
            if (value == DataType.MissingData) {
                text = ".";
            } else if (value == DataType.RangeFolded) {
                text = "RF";
            } else if (value == DataType.DataUnavailable) {
                text = "U";
            } else {
            }
        }
        return text;
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
