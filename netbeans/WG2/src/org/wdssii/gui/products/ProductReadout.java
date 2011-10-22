package org.wdssii.gui.products;

import org.wdssii.datatypes.DataType;

/** The root class of all product readouts.  This is a readout for one
 * point on the screen.  We do this type of thing a lot so eventually this
 * may become more generic with other stuff such as vslice output, etc.
 * 
 * This may eventually be created by a ProductReadoutFactory (to generalize it more),
 * Currently created by ProductRenderer getProductReadout, since the only method
 * I'm using right now is the color based rendering readout.
 *   
 * FIXME: Windfield has multiple data values....do we generalize multi-dimensional
 * data or have an object per DataType?
 * 
 * @author Robert Toomey
 *
 */
public class ProductReadout {

    /** The data value */
    private float myDataValue = DataType.MissingData;
    /** The units for this readout */
    private String myUnits = "?";
    /** The formatter used for single strings */
    private String myFormatter = "%3.2f ";
    /** Is readout valid?  Something actually there? */
    private boolean myValid = false;

    /** Get if this readout is valid */
    public boolean isValid() {
        return myValid;
    }

    /** Get the data value for this readout. */
    public float getValue() {
        return myDataValue;
    }

    /** Set the data value for this readout */
    public void setValue(float v) {
        myDataValue = v;
        myValid = true;
    }

    /** Get the units such as "dbZ" for this readout */
    public String getUnits() {
        return myUnits;
    }

    /** Set the units for this readout */
    public void setUnits(String u) {
        myUnits = u;
    }

    /** Get the formatter for this readout */
    public String getFormatter() {
        return myFormatter;
    }

    /** Set the formatter for this readout */
    public void setFormatter(String f) {
        myFormatter = f;
    }

    /** For now, simple method to get formatted string output for readout */
    public String getReadoutString() {

        String readout = "N/A";
        if (isValid()) {
            float v = getValue();
            String u = getUnits();

            System.out.println("VALUE IS " + v);
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
}
