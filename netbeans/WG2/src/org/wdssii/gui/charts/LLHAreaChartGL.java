package org.wdssii.gui.charts;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLJPanel;
import javax.swing.JSlider;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.Application;
import org.wdssii.gui.commands.FeatureCreateCommand;
import org.wdssii.gui.commands.VolumeValueCommand.VolumeValueFollowerView;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaFeature;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * Duplicates LLHAreaChart.. humm we need to redesign some of the hierarchy here
 *
 * @author Robert Toomey
 */
public class LLHAreaChartGL extends DataView implements VolumeValueFollowerView {

    public ProductVolume myVolume = null;
    /**
     * Keep volume value setting per chart
     */
    public String myCurrentVolumeValue = "";
    private Component myComponent = null;
    /**
     * The last GIS key of our LLHArea. If the physical area changes this key
     * does
     */
    private String myGISKey = "";
    /**
     * The full key representing a state of chart. If this changes, chart must
     * be regenerated. We build a lot of strings..might be able to do it better
     * with object list
     */
    private String myFullKey = "";

    /**
     * Return an LLHAreaFeature that contains a LLHAreaSlice
     */
    public static class VSliceFilter implements FeatureList.FeatureFilter {

        @Override
        public boolean matches(Feature f) {
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature a = (LLHAreaFeature) f;
                LLHArea area = a.getLLHArea();
                if (area instanceof LLHAreaSet) {
                    if (area.getLocations().size() > 1) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void setCurrentVolumeValue(String changeTo) {
        myCurrentVolumeValue = changeTo;
        if (myVolume != null) {
            updateChart(true); // Force update
        }
    }

    @Override
    public String getCurrentVolumeValue() {
        if (myVolume != null) {
            VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValue);
            if (v != null) {
                myCurrentVolumeValue = v.getName();
            }
            return myCurrentVolumeValue;
        }
        return "";
    }

    @Override
    public java.util.List<String> getValueNameList() {

        // We get this from the current volume...
        java.util.List<String> s;
        if (myVolume == null) {
            s = new ArrayList<String>();
            s.add("No volume data");
        } else {
            s = myVolume.getValueNameList();
        }

        return s;
    }

    /**
     * Return the LLHAreaSlice that we are currently drawing a plot for
     */
    public LLHAreaSet getLLHAreaToPlot() {
        // -------------------------------------------------------------------------
        // Hack snag the current slice and product...
        // Hack for now....we grab first 3d object in our FeatureList that is vslice
        // This has the effect of following the top selected vslice...

        LLHAreaSet slice = null;
        LLHAreaFeature f = FeatureList.theFeatures.getTopMatch(new VSliceFilter());
         if (f == null) {
            // Do ahead and try to make one...
            FeatureCreateCommand doit = new FeatureCreateCommand("Set", Integer.valueOf(2));
            CommandManager.getInstance().executeCommand(doit, true);
            f = FeatureList.theFeatures.getTopMatch(new VSliceFilter());
        }
        if (f != null) {
            LLHArea area = f.getLLHArea();
            if (area instanceof LLHAreaSet) {
                slice = (LLHAreaSet) (area);
            }
        }
        return slice;
    }

    /**
     * Get a pretty GIS label for rendering in a chart, for instance
     */
    public static String getGISLabel(double startLat, double startLon, double endLat, double endLong) {
        String newKey = String.format(
                "From (%5.2f, %5.2f) to (%5.2f, %5.2f)",
                startLat, startLon, endLat, endLong);
        return newKey;
    }

    public void setGISKey(String key) {
        myGISKey = key;
    }

    public String getGISKey() {
        return myGISKey;
    }

    public void setFullKey(String key) {
        myFullKey = key;
    }

    public String getFullKey() {
        return myFullKey;
    }

    public void repaintChart() {
        if (myComponent != null) {
            myComponent.repaint();
        }
    }

    public GLEventListener getGLEventListener() {
        return null;
    }

    public void setUpControls(Container parent) {
        JSlider j = new JSlider(0, 10000, 10);
        j.setPaintLabels(true);
        j.setOrientation(JSlider.VERTICAL);
        ((Container) parent).add(j, java.awt.BorderLayout.EAST);
    }

    /**
     * Generate the Chart itself. Basically the stuff that will draw the chart
     * in the composite
     */
    @Override
    public Object getNewGUIForChart(Object parent) {

        // Hack...
        setUpControls((Container) parent);

        boolean heavy = Application.USE_HEAVYWEIGHT_GL;

        // Humm...let's try to use true and deal with the effects for now,
        // need to check frame rate vs panel
        heavy = false;

        GLEventListener l = getGLEventListener();
        if (heavy) {
            GLCanvas glCanvas = new GLCanvas();
            // GLCapabilities glcaps = new GLCapabilities();
            // GLCanvas glcanvas =  jogl 2?
            //        GLDrawableFactory.getFactory().createGLCanvas(glcaps);
            if (l != null) {
                glCanvas.addGLEventListener(l);
            }
            myComponent = glCanvas;
            return glCanvas;

        } else {
            GLJPanel glPanel = new GLJPanel();
            if (l != null) {
                glPanel.addGLEventListener(l);
            }
            myComponent = glPanel;
            return glPanel;

        }
    }
    
}
