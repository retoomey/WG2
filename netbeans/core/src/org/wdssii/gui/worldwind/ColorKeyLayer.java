package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;
import javax.media.opengl.GL;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.CommandManager;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import com.sun.opengl.util.j2d.TextRenderer;
import org.wdssii.gui.views.EarthBallView;

public class ColorKeyLayer extends RenderableLayer implements WWCategoryLayer {
    // private double toViewportScale = 0.2;

    private double iconScale = 1.0d;
    private int borderWidth = 2;
    private int iconWidth;
    private int iconHeight;
    private Vec4 locationCenter = null;
    private Color color = Color.white;
    private Color backColor = new Color(0f, 0f, 0f, 0.4f);
    //private PickSupport pickSupport = new PickSupport();
    private static TextRenderer aText = null;
    // private double pickAltitude = 1000e3; // Altitude for picked position
    // Draw it as ordered with an eye distance of 0 so that it shows up in front
    // of most other things.
    private OrderedIcon orderedImage = new OrderedIcon();

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
        // ProductLayer-->ProductCollection-->CurrentProductInfo-->ColorMap?
        // How to get the productlayer from this layer...
        // Currently we have one view..so use the world manager
        CommandManager man = CommandManager.getInstance();
        ColorMap aColorMap = man.getCurrentColorMap();

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        // Colorkey covers entire width of current viewport
        java.awt.Rectangle viewport = dc.getView().getViewport();
        this.iconWidth = viewport.width - (2 * this.borderWidth);
        this.iconHeight = 20; // 20 pixels for icon scale of '1'

        // percentage the size based on the scale number..not really needed
        /// for colorkey I think...
        double width = this.getScaledIconWidth();
        double height = this.getScaledIconHeight();

        // Created text renderer once for bin labels
        // Resize in netbeans causing TextRenderer to get messed up
        // somehow.  This fixes it for now at least until I can investigate
        // further..might be gl state.
        aText = null;
        if (aText == null) {  // Only create once for speed.  We draw a LOT
            aText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);
        }

        // Bounds calculations
        final int fontYOffset = 5;
        final Rectangle2D maxText = aText.getBounds("gW"); // a guess of
        // size
        // (FIXME:
        // better
        // guess?)
        final int textHeight = (int) (maxText.getHeight());
        final int bheight = textHeight + fontYOffset + fontYOffset;
        int top = viewport.y + bheight - 1;
        int bottom = top - bheight;

        GL gl = dc.getGL();
        try {

            // System.out.println("Drawing color key layer");
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
                    | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
                    | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
                    | GL.GL_CURRENT_BIT);
            attribsPushed = true;

            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Load a parallel projection with xy dimensions (viewportWidth,
            // viewportHeight)
            // into the GL projection matrix.
            gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;

            gl.glLoadIdentity();
            double maxwh = width > height ? width : height;
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh,
                    0.6 * maxwh);
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            modelviewPushed = true;

            gl.glLoadIdentity();
            // Translate and scale
            double scale = this.computeScale(viewport);
            Vec4 locationSW = this.computeLocation(viewport, scale);

            // Scale to 0..1 space
            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            gl.glScaled(scale, scale, 1);
            gl.glScaled(width, height, 1d);

            if (!dc.isPickingMode()) {

                if (aColorMap != null) {
                    ColorMapOutput hi = new ColorMapOutput();
                    ColorMapOutput lo = new ColorMapOutput();
                    // Width of unit text
                    int wtxt = 0;
                    String unitName = aColorMap.getUnits();
                    if ((unitName != null) && (unitName.length() > 0)) {
                        Rectangle2D boundsUnits = aText.getBounds(unitName);
                        wtxt = (int) (boundsUnits.getWidth() + 2.0d);
                    } else {
                        wtxt = 0;
                    }

                    // Calculate height
                    int barwidth = Math.max(viewport.width - wtxt, 1);
                    int aSize = aColorMap.getNumberOfBins();
                    int cellWidth = barwidth / aSize;
                    barwidth = cellWidth * aSize;

                    float[] colorRGB = this.color.getRGBColorComponents(null);

                    gl.glDisable(GL.GL_TEXTURE_2D); // no textures

                    // Draw background square color
                    gl.glColor4ub((byte) this.backColor.getRed(),
                            (byte) this.backColor.getGreen(), (byte) this.backColor.getBlue(),
                            (byte) (this.backColor.getAlpha() * this.getOpacity()));
                    gl.glBegin(GL.GL_POLYGON);
                    gl.glVertex3d(0, 0, 0);
                    gl.glVertex3d(1, 0, 0);
                    gl.glVertex3d(1, 1, 0);
                    gl.glVertex3d(0, 1, 0);
                    gl.glVertex3d(0, 0, 0);
                    gl.glEnd();

                    gl.glLoadIdentity();
                    // Interesting, remove this and is goes above the status
                    // line..
                    gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
                    // Scale to width x height space
                    gl.glScaled(scale, scale, 1);
                    gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
                    double currentX = 0.0;

                    gl.glBegin(GL.GL_QUADS);

                    for (int i = 0; i < aSize; i++) {

                        aColorMap.getUpperBoundColor(hi, i);  // FIXME: let bin draw itself for possible future classes?
                        aColorMap.getLowerBoundColor(lo, i);
                        gl.glColor4f(lo.redF(), lo.greenF(), lo.blueF(),
                                (float) this.getOpacity());
                        gl.glVertex2d(currentX, top);
                        gl.glVertex2d(currentX, bottom);
                        gl.glColor4f(hi.redF(), hi.greenF(), hi.blueF(),
                                (float) this.getOpacity());
                        gl.glVertex2d(currentX + cellWidth, bottom);
                        gl.glVertex2d(currentX + cellWidth, top);
                        currentX += cellWidth;
                    }
                    //
                    //gl.glColor4f(1.0f,1.0f,1.0f,1.0f);
                    // gl.glBegin(GL.GL_LINES); for(int i = 0; i<aSize;i++){
                    // gl.glVertex3d(currentX, 0, 0); // Draw a vertical line
                    // (at percentage space) gl.glVertex3d(currentX, 1, 0);
                    // currentX += xoffset; }
                    //

                    gl.glEnd();


                    // Draw the text labels for bins
                    aText.begin3DRendering();
                    boolean drawText = (barwidth >= 100);
                    if (drawText) {
                        currentX = viewport.x;
                        int extraXGap = 7; // Force at least these pixels
                        // between labels
                        int drawnToX = viewport.x;
                        for (int i = 0; i < aSize; i++) {
                            String label = aColorMap.getBinLabel(i);
                            // System.out.println("Label is "+label);
                            // if (aColorMap.myColorBins.get(i) == null){
                            // System.out.println("---NULL");
                            // }
                            Rectangle2D boundsLabel = aText.getBounds(label);
                            wtxt = (int) (boundsLabel.getWidth());
                            // Sparse draw, skipping when text overlaps
                            if (currentX >= drawnToX) {

                                // Don't draw if text sticks outside box
                                if (currentX + wtxt < (viewport.x + barwidth)) {

                                    // Ok, render and remember how far it drew
                                    aText.draw(label, (int) (currentX + 2),
                                            bottom + fontYOffset);
                                    drawnToX = (int) (currentX + wtxt + extraXGap);
                                }
                            }

                            currentX += cellWidth;
                            // Color lower =
                            // aColorMap.myColorBins.get(i).getLowerBoundColor();
                        }
                    }
                    //aText.end3DRendering();

                    // Draw 1px border around and inside the map
                    //gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity());
                    gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], 1.0);
                    gl.glBegin(GL.GL_LINE_STRIP);
                    gl.glVertex3d(viewport.x, top, 0.0);
                    gl.glVertex3d(currentX, top, 0.0);
                    gl.glVertex3d(currentX, bottom, 0.0);
                    gl.glVertex3d(viewport.x, bottom, 0.0);
                    gl.glEnd();

                    // Draw the units
                    if (unitName.length() > 0) {
                        int start = (viewport.x + viewport.width - wtxt);
                        //aText.begin3DRendering();
                        aText.draw(unitName, start, bottom + fontYOffset);

                    }
                    aText.end3DRendering();


                }


            } else {
                /* Pick mode stuff
                // Hack. CellWidth should be a function, instead we stick it in
                // a variable for moment FIXME
                // Point at = dc.getPickPoint();
                // int cell = (at.x)/cellWidth;
                // myCellWidth = cell;
                
                // World wind has a global pick object list. Add ourselves so we
                // can capture the click
                // This is a unique colored rectangle of our 'click' space
                this.pickSupport.clearPickList();
                this.pickSupport.beginPicking(dc);
                // Draw unique color across the map
                Color color = dc.getUniquePickColor();
                int colorCode = color.getRGB();
                // Add our object(s) to the pickable list
                this.pickSupport
                .addPickableObject(colorCode, this, null, false);
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(),
                (byte) color.getBlue());
                gl.glBegin(GL.GL_POLYGON);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(1, 0, 0);
                gl.glVertex3d(1, 1, 0);
                gl.glVertex3d(0, 1, 0);
                gl.glVertex3d(0, 0, 0);
                gl.glEnd();
                // Done picking
                this.pickSupport.endPicking(dc);
                this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
                 */
            }

            //Draw time simulation window
            gl.glLoadIdentity();
            // Interesting, remove this and is goes above the status
            // line..
            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            // Scale to width x height space
            gl.glScaled(scale, scale, 1);
            //gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
            String time = CommandManager.getInstance().getProductOrderedSet().getSimulationTimeStamp();
            if (time == null) {
                time = "No products yet";
            }
            // Hack for moment..simulation time for debugging
            aText.begin3DRendering();
            //aText.beginRendering(100,100);
            aText.draw("Time:" + time, 0, bottom - textHeight
                    - fontYOffset - fontYOffset - 2);
            //System.out.println("Drawing "+time);
            //aText.endRendering();
            aText.end3DRendering();


        } finally {
            if (projectionPushed) {
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed) {
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attribsPushed) {
                gl.glPopAttrib();
            }
        }


        // Ok NOW REnder the hack in 3d space
        if (!dc.isPickingMode()) {
            EarthBallView v = CommandManager.getInstance().getEarthBall();
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
