package org.wdssii.gui.views;

import org.wdssii.geom.Location;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.worldwind.LLHAreaLayer;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.DrawContext;

/** Interface for the moment, this might become a class.
 * Made this to decouple logic from RCP ViewPart...will need more work
 * 
 * @author Robert Toomey
 */
public interface EarthBallView extends WdssiiView {

    public static final String ID = "wj.EarthBallView";

    public void takeDialogSnapshot();

    public LayerList getLayerList();

    public void setLayerEnabled(String name, boolean flag);

    public void updateOnMinTime();

    public String getProjection();

    public void setProjection(String projection);

    public void loadProduct(Product aProduct);

    public void gotoLocation(Location loc);

    public WorldWindowGLCanvas getWwd();

    public void DrawProductOutline(DrawContext dc);

    public void getColor(int x, int y);

    public LLHAreaLayer getVolumeLayer();
}
