package org.wdssii.gui.products;

public class RadialSetReadout extends ProductReadout {

    public RadialSetReadout() {
        setFormatter("%3.2f");
    }

    @Override
    public void setValue(float v) {
        super.setValue(v);
        // if (v < 1.0) setFormatter(..blah blah blah)
    }
}
