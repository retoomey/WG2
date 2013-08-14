package org.wdssii.gui.products.renderers;

import org.wdssii.gui.renderers.QuadStripRenderer;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.geom.GLWorld;
import org.wdssii.storage.Array1D;

/**
 *
 * @author rtoomey
 */
public abstract class RadialSetRenderer extends ProductRenderer {

    protected QuadStripRenderer myQuadRenderer = new QuadStripRenderer();

    public RadialSetRenderer(boolean asBackgroundJob) {
        super(asBackgroundJob);
    }

    public void allocateMemory(RadialSet radialSet) {
        // "Counter" loop. Faster to calculate than reallocate memory,
        // Radial sets have missing parts no way to quick count it that
        // I can see. We also find the maximum number of gates and
        // create a attenuation height cache
        int counter = 0;
        int ccounter = 0;
        int numRadials = radialSet.getNumRadials();
        for (int i = 0; i < numRadials; i++) {

            // If missing, just continue on
            Radial r = radialSet.getRadial(i);
            int numGates = r.getNumGates();
            if (numGates == 0) {
                continue;
            }
            int lastJWithData = -2;
            Array1D<Float> values = r.getValues();
            boolean startQuadStrip;
            for (int j = 0; j < numGates; j++) {
                float value = values.get(j);
                if (value == DataType.MissingData) {
                } else {
                    if (lastJWithData == (j - 1)) {
                        startQuadStrip = false;
                    } else {
                        startQuadStrip = true;
                    }
                    if (startQuadStrip) {
                        counter += 6; // 2 * 3
                        ccounter += 8; // 2*4
                    }
                    counter += 6;
                    ccounter += 8;
                }
            }
        }
        // --------------End counter loop

        myQuadRenderer.allocate(counter, ccounter);

    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(GLWorld w, boolean readoutMode) {
        if (isCreated()) {
            myQuadRenderer.drawData(w.gl, readoutMode);
        }
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(GLWorld w) {
        drawData(w, false);
    }
}
