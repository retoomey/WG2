package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.PolarGridGUI;
import org.wdssii.gui.PolarGridRenderer;

/**
 * A PolarGrid feature renders a 3d set of range circles in a 3d window around
 * a center point.  It can also use elevation.
 *
 * @author Robert Toomey
 */
public class PolarGridFeature extends Feature {

    /**
     * The properties of the PolarGridFeature
     */
    public static class PolarGridMemento extends FeatureMemento {

        private int linethickness = 2;
        private boolean useLineThickness = false;
        private Color lineColor = Color.WHITE;
        private boolean useLineColor = false;

        public PolarGridMemento(PolarGridMemento m) {
            super(m);
            linethickness = m.linethickness;
            lineColor = m.lineColor;
        }

        /**
         * Sync to another memento by only copying what is wanted to be changed.
         *
         * @param m
         */
        public void syncToMemento(PolarGridMemento m) {
            super.syncToMemento(m);
            if (m.useLineThickness) {
                linethickness = m.linethickness;
            }
            if (m.useLineColor) {
                lineColor = m.lineColor;
            }
        }

        public PolarGridMemento(boolean v, boolean o, int line) {
            super(v, o);
            linethickness = line;
            lineColor = Color.WHITE;
        }

        public int getLineThickness() {
            return linethickness;
        }

        public void setLineThickness(int l) {
            linethickness = l;
            useLineThickness = true;
        }

        public Color getLineColor() {
            return lineColor;
        }

        public void setLineColor(Color c) {
            lineColor = c;
            useLineColor = true;
        }
    }
    private static Logger log = LoggerFactory.getLogger(PolarGridFeature.class);
    public static final String PolarGridGroup = "POLARGRIDS";
    /**
     * The renderer we use for drawing the polar grid.
     */
    private PolarGridRenderer myRenderer;
    /**
     * The GUI for this Feature
     */
    private PolarGridGUI myControls;

    private static int counter = 1;
    /**
     * The state we use for drawing the polar grid.
     */
    public PolarGridFeature() {
        super(PolarGridGroup, new PolarGridMemento(true, false, 2));
        myRenderer = new PolarGridRenderer();
        String name = "Circle"+counter++;
        setName(name);
        setKey(name);
        setMessage("");
    }

    @Override
    public FeatureMemento getNewMemento() {
        PolarGridMemento m = new PolarGridMemento((PolarGridMemento) getMemento());
        return m;
    }

    @Override
    public void setMemento(FeatureMemento f) {
        /**
         * Handle polar grid  mementos
         */
        if (f instanceof PolarGridMemento) {
            PolarGridMemento mm = (PolarGridMemento) (f);
            ((PolarGridMemento) getMemento()).syncToMemento(mm);
        } else {
            super.setMemento(f);
        }
    }

    /**
     * Render a feature
     */
    @Override
    public void render(DrawContext dc) {

        if (myRenderer != null) {
            myRenderer.setupIfNeeded(dc);
            myRenderer.draw(dc, ((PolarGridMemento) getMemento()));
        }
    }

    @Override
    public void setupFeatureGUI(JComponent source) {

        // FIXME: general FeatureFactory..move code up into Feature
        boolean success = false;
        // if (myFactory != null) {

        if (myControls == null) {
            //myControls = myFactory.createGUI(myLLHArea, source);
            myControls = new PolarGridGUI(this);
        }

        // Set the layout and add our controls
        if (myControls != null) {
            myControls.activateGUI(source);
            updateGUI();
            success = true;
        }
        //  }

        /**
         * Fill in with default stuff if GUI failed or doesn't exist
         */
        if (!success) {
            super.setupFeatureGUI(source);
        }
    }

    @Override
    public void updateGUI() {
        if (myControls != null) {
            myControls.updateGUI();
        }
    }
}
