package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.CommandManager;

import java.awt.*;
import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.WorldWindow;
import org.wdssii.gui.ColorMapRenderer;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.views.WorldWindView;

public class ColorKeyLayer extends RenderableLayer implements WWCategoryLayer {
    // private double toViewportScale = 0.2;

    private double iconScale = 1.0d;
    private int borderWidth = 2;
    private int iconWidth;
    private int iconHeight;
    private Vec4 locationCenter = null;
    private Color backColor = new Color(0f, 0f, 0f, 0.4f);
    //private PickSupport pickSupport = new PickSupport();
    private static TextRenderer aText = null;
    // private double pickAltitude = 1000e3; // Altitude for picked position
    // Draw it as ordered with an eye distance of 0 so that it shows up in front
    // of most other things.
    private OrderedIcon orderedImage = new OrderedIcon();
    private ColorMapRenderer myRenderer = new ColorMapRenderer();

    @Override
    public String getCategory() {
        return WDSSII_CATEGORY;
    }

    private class OrderedIcon implements OrderedRenderable {

        @Override
        public double getDistanceFromEye() {
            return 0;
        }

        @Override
        public void pick(DrawContext dc, Point pickPoint) {
            ColorKeyLayer.this.drawIcon(dc);
        }

        @Override
        public void render(DrawContext dc) {
            ColorKeyLayer.this.drawIcon(dc);
        }
    }

    /**
     * Displays a color key for current top product in the display
     */
    public ColorKeyLayer() {
        this.setName("ColorKey");
        this.setOpacity(0.6);
    }

    // Public properties
    /**
     * Returns the icon scale factor. See {@link #setIconScale(double)} for a
     * description of the scale factor.
     * 
     * @return the current icon scale
     */
    public double getIconScale() {
        return iconScale;
    }

    /**
     * Sets the scale factor defining the displayed size of the world map icon
     * relative to the icon's width and height in its image file. Values greater
     * than 1 magify the image, values less than one minify it. If the layer's
     * resize behavior is other than {@link #RESIZE_KEEP_FIXED_SIZE}, the icon's
     * displayed sized is further affected by the value specified by
     * {@link #setToViewportScale(double)} and the current viewport size.
     * 
     * @param iconScale
     *            the icon scale factor
     */
    public void setIconScale(double iconScale) {
        this.iconScale = iconScale;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    /**
     * Sets the color key offset from the viewport border.
     * 
     * @param borderWidth
     *            the number of pixels to offset the color key from the borders
     *            indicated by {@link #setPosition(String)}.
     */
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    public Vec4 getLocationCenter() {
        return locationCenter;
    }

    public void setLocationCenter(Vec4 locationCenter) {
        this.locationCenter = locationCenter;
    }

    public Color getBackgroundColor() {
        return this.backColor;
    }

    public void setBackgroundColor(Color color) {
        if (color == null) {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.backColor = color;
    }

    @Override
    public void doRender(DrawContext dc) {
        // Delegate drawing to the ordered renderable list
        dc.addOrderedRenderable(this.orderedImage);
    }

    @Override
    public void doPick(DrawContext dc, Point pickPoint) {
        // Delegate drawing to the ordered renderable list
        dc.addOrderedRenderable(this.orderedImage);
    }

    public void pickColorBinByPoint(Point p) {
        System.out.println("Color layer got point " + p);
    }

    private void drawIcon(DrawContext dc) {
        ProductManager man = ProductManager.getInstance();
        ColorMap aColorMap = man.getCurrentColorMap();
        myRenderer.setColorMap(aColorMap);

        // Pass in viewport to avoid getting width from context, since it
        // could be wrong for lightweight
        java.awt.Rectangle viewport = dc.getView().getViewport();
        myRenderer.paintToOpenGL(dc.getGL(), viewport.width, viewport.height, (float) this.getOpacity());

        // Temp break:
        aText = null;
        if (aText == null) {  // Only create once for speed.  We draw a LOT
            aText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);
        }
        String time = ProductManager.getInstance().getProductOrderedSet().getSimulationTimeStamp();
        if (time == null) {
            time = "No products yet";
        }

        // Ok NOW REnder the hack in 3d space
        if (!dc.isPickingMode()) {
            WorldWindView v = CommandManager.getInstance().getEarthBall();
            if (v != null) {
                //System.out.println("Drawing product outline "+counter++);
                v.DrawProductOutline(dc);
            }
        }
    }

    private double computeScale(java.awt.Rectangle viewport) {
        return 1d;
    }

    private double getScaledIconWidth() {
        return this.iconWidth * this.iconScale;
    }

    private double getScaledIconHeight() {
        return this.iconHeight * this.iconScale;
    }

    private Vec4 computeLocation(java.awt.Rectangle viewport, double scale) {
        // double width = this.getScaledIconWidth();
        double height = this.getScaledIconHeight();

        // double scaledWidth = scale * width;
        double scaledHeight = scale * height;

        double x;
        double y;

        // Color key current has only one position, the top
        x = 0d + this.borderWidth;
        y = viewport.getHeight() - scaledHeight - this.borderWidth;
        return new Vec4(x, y, 0);
    }

    /**
     * Computes the lat/lon of the pickPoint over the world map
     * 
     * @param dc
     *            the current <code>DrawContext</code>
     * @param locationSW
     *            the screen location of the bottom left corner of the map
     * @param mapSize
     *            the world map screen dimension in pixels
     * @return the picked Position
     */

    /*
     * private Position computePickPosition(DrawContext dc, Vec4 locationSW,
     * Dimension mapSize) { Position pickPosition = null; Point pickPoint =
     * dc.getPickPoint(); if (pickPoint != null) { Rectangle viewport =
     * dc.getView().getViewport(); // Check if pickpoint is inside the map if
     * (pickPoint.getX() >= locationSW.getX() && pickPoint.getX() <
     * locationSW.getX() + mapSize.width && viewport.height - pickPoint.getY()
     * >= locationSW.getY() && viewport.height - pickPoint.getY() <
     * locationSW.getY() + mapSize.height) { double lon = (pickPoint.getX() -
     * locationSW.getX()) / mapSize.width * 360 - 180; double lat =
     * (viewport.height - pickPoint.getY() - locationSW.getY()) / mapSize.height
     * * 180 - 90; pickPosition = new Position(Angle.fromDegrees(lat),
     * Angle.fromDegrees(lon), pickAltitude); } } return pickPosition; }
     */
    @Override
    public void dispose() {
        // TODO: dispose of the icon texture
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
