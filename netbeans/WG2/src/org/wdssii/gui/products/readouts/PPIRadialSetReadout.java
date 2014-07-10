package org.wdssii.gui.products.readouts;

import java.awt.Point;
import java.awt.Rectangle;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.Table2DView.CellQuery;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.renderers.PPIRadialSetRenderer;

/**
 *
 * @author Robert Toomey
 */
public class PPIRadialSetReadout extends RadialSetReadout {

    /**
     * My output, 2 integers
     */
    public int[] myOutput;
    public float myValue;

    /**
     * Get the readout for this product at given point in view..
     */
    @Override
    public void doReadoutAtPoint(Product prod, Point p, Rectangle view, GLWorld w) {

        String units = "";

        if (prod != null) {
            PPIRadialSetRenderer pr = (PPIRadialSetRenderer) prod.getRenderer();
            if (pr != null) {
                int[] out = pr.getReadout(p, view, w);
                myOutput = out;
                myValue = DataType.MissingData;
                if (out[0] >= 0) {  // -1 is the no value
                    DataType d = prod.getRawDataType();
                    if (d != null) {
                        if (d instanceof PPIRadialSet) {
                            PPIRadialSet r = (PPIRadialSet) (d);
                            CellQuery q = new CellQuery();                    
                            r.getCellValue(myOutput[0], myOutput[1], q);
                            myValue = q.value;
                        }
                    }
                }
            }

            // Setup units of readout
            units = prod.getCurrentUnits();
        }

        setUnits(units);
        setValid();
    }

    @Override
    public String getReadoutString() {
        String stuff = "";
        if (isValid()) {
            if (myOutput != null) {
                stuff = String.format("%5.2f %s", myValue, getUnits());
            }
        }
        return stuff;
    }
}
