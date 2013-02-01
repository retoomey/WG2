package org.wdssii.gui.products;

/**
 *
 * @author Robert Toomey
 */
public class LatLonGridReadout extends ProductReadout {

    public LatLonGridReadout() {
        setFormatter("%3.2f ");
    }

    @Override
    public void setValue(float v) {
        super.setValue(v);
        // if (v < 1.0) setFormatter(..blah blah blah)
    }
}
