package org.wdssii.gui.products;

import org.wdssii.datatypes.DataType;

/**
 * ProductTextFormater defines how the text is defined for a data value of a
 * product. It is used by ColorMaps, Tables, Readout to generate text from a
 * value.
 *
 * This is the default text formatter until the product is loaded.
 *
 * @author Robert Toomey
 */
public class ProductTextFormatter {

    /**
     * A default for passing into other objects
     */
    public final static ProductTextFormatter DEFAULT_FORMATTER = new ProductTextFormatter();

    /**
     * Shared default logic
     */
    public String formatRawValue(float v) {
        if (Math.abs(v) < .05) {
            return String.format("%5.5f", v);
        } else {
            return String.format("%5.2f", v);
        }
    }

    /**
     * Format text for the mouse readout
     */
    public String formatForReadout(float v, String u) {
        String readout;
        if (v == DataType.MissingData) {
            readout = "Missing";
        } else if (v == DataType.RangeFolded) {
            readout = "RF";
        } else if (v == DataType.DataUnavailable) {
            readout = "Unavailable";
        } else {
            readout = formatRawValue(v);
            readout += " ";
            readout += u;
        }
        return readout;
    }

    /**
     * Format text for a color map bin
     */
    public String formatForColorMap(float v) {
        String text;
        if (DataType.isRealDataValue(v)) {
            text = formatRawValue(v);
        } else {
            text = "";
        }
        return text;
    }

    public String formatForTable(float v) {
        String readout;
        if (v == DataType.MissingData) {
            readout = ".";
        } else if (v == DataType.RangeFolded) {
            readout = "RF";
        } else if (v == DataType.DataUnavailable) {
            readout = "U";
        } else {
            readout = formatRawValue(v);
        }
        return readout;
    }

    /**
     * Format text for a color map linear range bin
     */
    public String formatForColorMapRange(float l, float u) {
        return (formatForColorMap(l) + "-" + formatForColorMap(u));
    }
}
