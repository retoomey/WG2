package org.wdssii.gui.products;

/** ProductTextFormater defines how the text is defined for a data value
 * of a product.  It is used by ColorMaps, Tables, Readout to generate
 * text from a value.
 * 
 * @author Robert Toomey
 */
public class ProductTextFormatter {

    /** A default for passing into other objects */
    public static ProductTextFormatter DEFAULT_FORMATTER = new ProductTextFormatter();
    
    /** Return true iff data value is considered a real value and not
     * something like 'missing' or 'na', etc.
     * @param value
     * @return true if it is actual data value
     */
    public boolean isRealDataValue(float value) {
        return (value > -99900);
    }

    /** Get the text for a single box value */
    public String format(float v) {
        String text = null;
        if (isRealDataValue(v)) {
            if (v < .05) {
                text = String.format("%5.5f", v);
            } else {
                text = String.format("%5.2f", v);
            }

        } else {
            text = ".";
        }
        return text;
    }
    
    /** Used by ColorMap to format a range for a linear color bin */
    public String format(float l, float u){
        return (format(l)+"-"+format(u));
    }
}
