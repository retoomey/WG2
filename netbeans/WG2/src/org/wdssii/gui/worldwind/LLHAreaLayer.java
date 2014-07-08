package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.airspaces.Geometry;
import gov.nasa.worldwind.render.airspaces.editor.AirspaceEditorUtil;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;
import java.awt.Point;
import java.nio.Buffer;
import java.util.*;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.geom.GLWorld;
import org.wdssii.geom.LLD;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.commands.PointAddCommand;
import org.wdssii.gui.commands.PointRemoveCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaFeature;
import org.wdssii.gui.volumes.*;

/**
 * A layer for all of our LLHArea collection, this handles editing a 'list' of
 * objects. A lot of this code came from Airspace work in the WorldWind library,
 * but at the time it was changing too fast and was missing features we needed
 * for vslice and isosurfaces, so it was ported and modified. -- Merged editor
 * layer and drawing layer (simpler for our purposes)
 *
 * @author Robert Toomey
 * @author dcollins
 *
 * Merged Editor ability into this class, since we have a single 'selected'
 * object that is the currently edited one.
 */
public class LLHAreaLayer extends AbstractLayer implements WWCategoryLayer {

    private final static Logger LOG = LoggerFactory.getLogger(LLHAreaLayer.class);
    public static final String DRAW_STYLE_FILL = "Airspace.DrawStyleFill";
    public static final String DRAW_STYLE_OUTLINE = "Airspace.DrawStyleOutline";
    // private final java.util.Collection<LLHArea> myLLHAreas = new java.util.concurrent.ConcurrentLinkedQueue<LLHArea>();
    private static final String EXT_BLEND_FUNC_SEPARATE_STRING = "GL_EXT_blend_func_separate";
    protected static final int ELEMENT = 1;
    protected static final int VERTEX = 2;
    protected static final int NORMAL = 3;
    private boolean enableAntialiasing;
    private boolean enableBlending;
    private boolean enableDepthOffset;
    private boolean enableLighting;
    private boolean useEXTBlendFuncSeparate;
    private boolean haveEXTBlendFuncSeparate;
    private boolean drawExtents;
    private boolean drawWireframe;
    private Material lightMaterial;
    private Vec4 lightDirection;
    private final PickSupport pickSupport = new PickSupport();
    private double linePickWidth;
    private double depthOffsetFactor;
    private double depthOffsetUnits;
    // Editor fields...
    private boolean armed = true;
    private boolean useRubberBand;
    private LLHAreaControlPointRenderer controlPointRenderer;
    // List of control points from the last call to draw().
    private ArrayList<LLHAreaControlPoint> currentControlPoints = new ArrayList<LLHAreaControlPoint>();
    // Airspace altitude constants.
    protected static final int LOWER_ALTITUDE = 0;
    protected static final int UPPER_ALTITUDE = 1;
    private static final double DEFAULT_POLYGON_HEIGHT = 10.0;

    @Override
    public String getCategory() {
        return WDSSII_CATEGORY;
    }

    public static class LLHAreaEdgeInfo {

        int locationIndex;
        int nextLocationIndex;
        int altitudeIndex;
        Vec4 point1;
        Vec4 point2;

        public LLHAreaEdgeInfo(int locationIndex, int nextLocationIndex, int altitudeIndex, Vec4 point1, Vec4 point2) {
            this.locationIndex = locationIndex;
            this.nextLocationIndex = nextLocationIndex;
            this.altitudeIndex = altitudeIndex;
            this.point1 = point1;
            this.point2 = point2;
        }
    }

    /**
     * Creates a new
     * <code>Airspace</code> with an empty collection of Airspaces.
     */
    public LLHAreaLayer() {
        this.enableAntialiasing = false;
        this.enableBlending = false;
        this.enableDepthOffset = false;
        this.enableLighting = true;
        this.useEXTBlendFuncSeparate = true;
        this.haveEXTBlendFuncSeparate = false;
        this.drawExtents = false;
        this.drawWireframe = false;
        this.lightMaterial = Material.WHITE;
        this.lightDirection = new Vec4(1.0, 0.5, 1.0);
        this.linePickWidth = 8.0;
        this.depthOffsetFactor = -2;
        this.depthOffsetUnits = -4;

        // Editor stuff
        this.armed = true;
        this.useRubberBand = true;
        this.controlPointRenderer = new LLHAreaControlPointRenderer();
    }

    /**
     * Returns the Iterable of currently active Airspaces. If the caller has
     * specified a custom Iterable via {@link #setAirspaces}, this will returns
     * a reference to that Iterable. If the caller passed
     * <code>setAirspaces</code> a null parameter, or if
     * <code>setAirspaces</code> has not been called, this returns a view of
     * this layer's internal collection of Airspaces.
     *
     * @return Iterable of currently active Airspaces.
     */
    private Iterable<LLHArea> getActiveAirspaces() {

        // Fixme: We'll make a FeatureLayer that takes a group...
        Collection<LLHArea> theStuff = new ArrayList<LLHArea>();

        List<Feature> list = FeatureList.theFeatures.getActiveFeatureGroup("3D");
        Iterator<Feature> i = list.iterator();
        while (i.hasNext()) {
            Feature f = i.next();
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature l = (LLHAreaFeature) (f);
                LLHArea a = l.getLLHArea();
                if (a != null) {
                    theStuff.add(a);
                }
            }
        }
        return theStuff;
    }

    @Override
    /**
     * The 'pick' mode, draw just outlines for opengl picking
     */
    protected void doPick(DrawContext dc, java.awt.Point pickPoint) {
        // Draw the volumes themselves
        //pickOrdered(dc, getActiveAirspaces(), pickPoint, this);
        drawOrdered(new GLWorldWW(dc), getActiveAirspaces(), this);

        // Draw the control points
        if (isArmed()) {
            drawControlPoints(dc, pickPoint);
        }
    }

    @Override
    protected void doRender(DrawContext dc) {
        // Draw the volumes themselves
        //renderOrdered(dc, getActiveAirspaces());
        drawOrdered(new GLWorldWW(dc), getActiveAirspaces(), null);

        // Draw the control points
        if (this.isArmed()) {
            this.drawControlPoints(dc, null);
        }
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.WdssiiVolumeLayer.Name");
    }

    public boolean isEnableAntialiasing() {
        return this.enableAntialiasing;
    }

    public void setEnableAntialiasing(boolean enable) {
        this.enableAntialiasing = enable;
    }

    public boolean isEnableBlending() {
        return this.enableBlending;
    }

    public void setEnableBlending(boolean enable) {
        this.enableBlending = enable;
    }

    public boolean isEnableDepthOffset() {
        return this.enableDepthOffset;
    }

    public void setEnableDepthOffset(boolean enable) {
        this.enableDepthOffset = enable;
    }

    public boolean isEnableLighting() {
        return this.enableLighting;
    }

    public void setEnableLighting(boolean enable) {
        this.enableLighting = enable;
    }

    public boolean isUseEXTBlendFuncSeparate() {
        return this.useEXTBlendFuncSeparate;
    }

    public void setUseEXTBlendFuncSeparate(boolean useEXTBlendFuncSeparate) {
        this.useEXTBlendFuncSeparate = useEXTBlendFuncSeparate;
    }

    protected boolean isHaveEXTBlendFuncSeparate() {
        return this.haveEXTBlendFuncSeparate;
    }

    protected void setHaveEXTBlendFuncSeparate(boolean haveEXTBlendFuncSeparate) {
        this.haveEXTBlendFuncSeparate = haveEXTBlendFuncSeparate;
    }

    public boolean isDrawExtents() {
        return this.drawExtents;
    }

    public void setDrawExtents(boolean draw) {
        this.drawExtents = draw;
    }

    public boolean isDrawWireframe() {
        return this.drawWireframe;
    }

    public void setDrawWireframe(boolean draw) {
        this.drawWireframe = draw;
    }

    public Material getLightMaterial() {
        return this.lightMaterial;
    }

    public Vec4 getLightDirection() {
        return this.lightDirection;
    }

    public double getLinePickWidth() {
        return linePickWidth;
    }

    public void setLinePickWidth(double width) {
        if (width < 0.0) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.linePickWidth = width;
    }

    public double getDepthOffsetFactor() {
        return this.depthOffsetFactor;
    }

    public void setDepthOffsetFactor(double factor) {
        this.depthOffsetFactor = factor;
    }

    public double getDepthOffsetUnits() {
        return depthOffsetUnits;
    }

    public void setDepthOffsetUnits(double units) {
        this.depthOffsetUnits = units;
    }

    public PickSupport getPickSupport() {
        return this.pickSupport;
    }

    protected void drawOrdered(GLWorld w, Iterable<? extends LLHArea> llhAreas, Layer layer) {
        for (LLHArea a : llhAreas) {
            double eyeDistance = this.computeDistanceFromEye(w, a);
            OrderedLLHArea orderedAirspace = new OrderedLLHArea(this, a, layer, eyeDistance);
            final DrawContext dc = ((GLWorldWW)(w)).getDC(); // hack
            dc.getOrderedSurfaceRenderables().add(orderedAirspace);
            // dc.getOrderedRenderables().add(orderedAirspace);
        }
    }

    protected double computeDistanceFromEye(GLWorld w, LLHArea airspace) {
        Extent extent = airspace.getExtent(w);
        if (extent == null) {
            return 0;
        }
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
        if (dc.getView() == null || dc.getView().getEyePoint() == null) {
            return 0;
        }

        // Compute the distance from the eye point to the extent center.
        double distance = extent.getCenter().distanceTo3(dc.getView().getEyePoint());

        // If the eye point is inside the extent, then just return 0.
        double value = 0;
        if (distance >= extent.getRadius()) {
            value = distance - extent.getRadius();
        }
        return value;
    }

    //**************************************************************//
    //********************  Ordered LLHArea  *********************//
    //**************************************************************//
    protected static class OrderedLLHArea implements OrderedRenderable {

        protected LLHAreaLayer renderer;
        protected LLHArea airspace;
        protected Layer layer;
        protected double eyeDistance;

        public OrderedLLHArea(LLHAreaLayer renderer, LLHArea airspace, Layer layer, double eyeDistance) {
            this.renderer = renderer;
            this.airspace = airspace;
            this.layer = layer;
            this.eyeDistance = eyeDistance;
        }

        public LLHAreaLayer getRenderer() {
            return this.renderer;
        }

        public LLHArea getAirspace() {
            return this.airspace;
        }

        public Layer getLayer() {
            return this.layer;
        }

        @Override
        public double getDistanceFromEye() {
            return this.eyeDistance;
        }

        @Override
        public void render(DrawContext dc) {
            // The render method does not bind any pickable objects.
            this.draw(new GLWorldWW(dc), null);
        }

        @Override
        public void pick(DrawContext dc, Point pickPoint) {
            PickSupport pickSupport = this.getRenderer().getPickSupport();
            pickSupport.beginPicking(dc);
            try {
                // The pick method will bind pickable objects to the renderer's PickSupport.
                this.draw(new GLWorldWW(dc), pickSupport);
            } finally {
                pickSupport.endPicking(dc);
            }

            pickSupport.resolvePick(dc, pickPoint, this.getLayer());
            pickSupport.clearPickList();
        }

        protected void draw(GLWorld w, PickSupport pickSupport) {
            //LLHAreaLayer renderer = this.getRenderer();
            renderer.drawOrderedAirspace(w, this, pickSupport);
        }
    }

    protected void drawOrderedAirspace(GLWorld w, OrderedLLHArea orderedAirspace, PickSupport pickSupport) {

        this.beginRendering(w);
        try {
            this.drawAirspace(w, orderedAirspace.getAirspace(), pickSupport);
            this.drawOrderedAirspaces(w, pickSupport);
        } finally {
            this.endRendering(w);
        }
    }

    protected void drawOrderedAirspaces(GLWorld w, PickSupport pickSupport) {
        // Batch render as many Airspaces as we can to save OpenGL state switching.
        // OrderedRenderable top = dc.getOrderedRenderables().peek();
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
        OrderedRenderable top = dc.getOrderedSurfaceRenderables().peek();
        while (top != null && top instanceof OrderedLLHArea) {
            OrderedLLHArea oa = (OrderedLLHArea) top;

            // If the next OrderedAirspace's renderer is different, then we must stop batching. Otherwise, we would
            // render an airspace with a renderer with potentially different properties or behavior.
            if (this != oa.getRenderer()) {
                return;
            }

            this.drawAirspace(w, oa.getAirspace(), pickSupport);

            // Take the ordered airspace off the queue, then peek at the next item in the queue (but do not remove it).
            // dc.getOrderedRenderables().poll();
            // top = dc.getOrderedRenderables().peek();
            dc.getOrderedSurfaceRenderables().poll();
            top = dc.getOrderedSurfaceRenderables().peek();
        }
    }

    //**************************************************************//
    //********************  Airspace Rendering  ********************//
    //**************************************************************//
    protected void drawAirspaces(GLWorld w, Iterable<? extends LLHArea> airspaces, PickSupport pickSupport) {
        for (LLHArea airspace : airspaces) {
            try {
                if (airspace != null) {
                    this.drawAirspace(w, airspace, pickSupport);
                }
            } catch (Exception e) {
                String message = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }
    }

    protected void drawAirspace(GLWorld w, LLHArea airspace, PickSupport pickSupport) {
        try {
            if (pickSupport != null) {
                this.bindPickableObject(w, airspace, pickSupport);
            }

            this.doDrawAirspace(w, airspace);
        } catch (Exception e) {
            String message = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void doDrawAirspace(GLWorld w, LLHArea airspace) {
        if (!airspace.isVisible()) {
            return;
        }

        this.drawAirspaceShape(w, airspace);
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
        if (!dc.isPickingMode()) {
            if (this.isDrawExtents()) {
                airspace.renderExtent(w);
            }
        }
    }

    protected void drawAirspaceShape(GLWorld w, LLHArea airspace) {
        // Draw the airspace shape using a multiple pass algorithm. The motivation for this algorithm is twofold:
        //
        // 1. We want to draw the airspace on top of other intersecting shapes with similar depth values to eliminate
        //    z-fighting between shapes. However we do not wish to offset the actual depth values, which would cause
        //    a cascading increase in depth offset when many shapes are drawn.
        // 2. The airspace outline appears both in front of and behind the shape. If the outline will be drawn using
        //    GL line smoothing, or GL blending, then either the line must be broken into two parts, or rendered in
        //    two passes.
        //
        // These issues are resolved by making several passes for the interior and outline, as follows:

        final GL gl = w.gl;
 final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
        LOG.debug("MAIN PICKING MODE DC IS "+dc.isPickingMode());
        // If the outline and interior will be drawn, then draw the outline color, but do not affect the depth buffer
        // (outline pixels do not need the depth test). When the interior is drawn, it will draw on top of these
        // colors, and the outline will be visible behind the potentially transparent interior.
        if (airspace.getAttributes().isDrawOutline() && airspace.getAttributes().isDrawInterior()) {
            gl.glColorMask(true, true, true, true);
            gl.glDepthMask(false);

            this.drawAirspaceOutline(w, airspace);
        }

        // If the interior will be drawn, then make two passes as follows. The first pass draws the interior depth
        // values to the depth buffer without any polygon offset. This enables the shape to contribute to the depth
        // buffer and occlude other geometries as it normally would. The second pass draws the interior color values
        // with offset depth values, but does not affect the depth buffer. This has the effect of givign this airspace
        // depth priority over whatever is already in the depth buffer. By rendering the depth values normally, we
        // avoid the problem of having to use ever increasing depth offsets.
        if (airspace.getAttributes().isDrawInterior()) {
            if (this.isEnableDepthOffset()) {
                // Draw depth.
                gl.glColorMask(false, false, false, false);
                gl.glDepthMask(true);
                gl.glPolygonOffset(0, 0);

                this.drawAirspaceInterior(w, airspace);

                // Draw color.
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(false);
                gl.glPolygonOffset((float) this.getDepthOffsetFactor(), (float) this.getDepthOffsetUnits());

                this.drawAirspaceInterior(w, airspace);
            } else {
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(true);
                this.drawAirspaceInterior(w, airspace);
            }
        }

        // If the outline will be drawn, then draw the outline color, but do not affect the depth buffer (outline
        // pixels do not need the depth test). This will blend outline pixels with the interior pixels which are
        // behind the outline.
        if (airspace.getAttributes().isDrawOutline()) {
            gl.glColorMask(true, true, true, true);
            gl.glDepthMask(false);

            this.drawAirspaceOutline(w, airspace);
        }
    }

    protected void drawAirspaceInterior(GLWorld w, LLHArea airspace) {
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack

        if (!dc.isPickingMode()) {
            if (this.isEnableLighting()) {
                w.gl.glEnable(GL.GL_LIGHTING);
            }
            airspace.getAttributes().applyInterior(dc, this.isEnableLighting());
        }

        airspace.renderGeometry(w, "fill");
    }

    protected void drawAirspaceOutline(GLWorld w, LLHArea airspace) {
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
        if (dc.isPickingMode()) {
            double lineWidth = airspace.getAttributes().getOutlineWidth();

            // If the airspace interior isn't drawn, make the outline wider during picking.
            if (!airspace.getAttributes().isDrawInterior()) {
                if (lineWidth != 0.0) {
                    lineWidth += this.getLinePickWidth();
                }
            }

            dc.getGL().glLineWidth((float) lineWidth);
        } else {
            if (this.isEnableLighting()) {
                dc.getGL().glDisable(GL.GL_LIGHTING);
            }

            airspace.getAttributes().applyOutline(dc, false);
        }

        airspace.renderGeometry(w, "outline");
    }

    protected void beginRendering(GLWorld w) {

        final GL gl = w.gl;

        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack

        if (!dc.isPickingMode()) {
            int attribMask =
                    (this.isEnableLighting() ? GL.GL_LIGHTING_BIT : 0) // For lighting, material, and matrix mode
                    | GL.GL_COLOR_BUFFER_BIT // For color write mask. If blending is enabled: for blending src and func, and alpha func.
                    | GL.GL_CURRENT_BIT // For current color.
                    | GL.GL_DEPTH_BUFFER_BIT // For depth test, depth func, depth write mask.
                    | GL.GL_LINE_BIT // For line width, line smoothing.
                    | GL.GL_POLYGON_BIT // For polygon mode, polygon offset.
                    | GL.GL_TRANSFORM_BIT; // For matrix mode.
            gl.glPushAttrib(attribMask);

            if (this.isDrawWireframe()) {
                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
            }

            if (this.isEnableBlending()) {
                this.setBlending(w);
            }

            if (this.isEnableLighting()) {
                gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
                this.setLighting(w);
            }

            if (this.isEnableAntialiasing()) {
                gl.glEnable(GL.GL_LINE_SMOOTH);
            }
        } else {
            int attribMask =
                    GL.GL_CURRENT_BIT // For current color.
                    | GL.GL_DEPTH_BUFFER_BIT // For depth test and depth func.
                    | GL.GL_LINE_BIT; // For line width.
            gl.glPushAttrib(attribMask);
        }

        if (this.isEnableDepthOffset()) {
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        }

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
    }

    protected void endRendering(GLWorld w) {
        final GL gl = w.gl;

        gl.glPopAttrib();
        gl.glPopClientAttrib();
    }

    protected void bindPickableObject(GLWorld w, LLHArea airspace, PickSupport pickSupport) {
        final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack

        java.awt.Color pickColor = dc.getUniquePickColor();
        int colorCode = pickColor.getRGB();
        dc.getGL().glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());

        if (airspace instanceof Locatable) {
            pickSupport.addPickableObject(colorCode, airspace, ((Locatable) airspace).getPosition(), false);
        } else {
            pickSupport.addPickableObject(pickColor.getRGB(), airspace);
        }
    }

    //**************************************************************//
    //********************  Geometry Rendering  ********************//
    //**************************************************************//
    public void drawGeometry(DrawContext dc, int mode, int count, int type, Buffer elementBuffer, Geometry geom) {

        GL gl = dc.getGL();

        int minElementIndex, maxElementIndex;
        int size, glType, stride;
        Buffer vertexBuffer, normalBuffer;

        size = geom.getSize(VERTEX);
        glType = geom.getGLType(VERTEX);
        stride = geom.getStride(VERTEX);
        vertexBuffer = geom.getBuffer(VERTEX);
        gl.glVertexPointer(size, glType, stride, vertexBuffer);

        normalBuffer = null;
        if (!dc.isPickingMode()) {
            if (this.isEnableLighting()) {
                normalBuffer = geom.getBuffer(NORMAL);
                if (normalBuffer == null) {
                    gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
                } else {
                    glType = geom.getGLType(NORMAL);
                    stride = geom.getStride(NORMAL);
                    gl.glNormalPointer(glType, stride, normalBuffer);
                }
            }
        }

        // On some hardware, using glDrawRangeElements allows vertex data to be prefetched. We know the minimum and
        // maximum index values that are valid in elementBuffer (they are 0 and vertexCount-1), so it's harmless
        // to use this approach and allow the hardware to optimize.
        minElementIndex = 0;
        maxElementIndex = geom.getCount(VERTEX) - 1;
        gl.glDrawRangeElements(mode, minElementIndex, maxElementIndex, count, type, elementBuffer);

        if (!dc.isPickingMode()) {
            if (this.isEnableLighting()) {
                if (normalBuffer == null) {
                    gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
                }
            }
            this.logGeometryStatistics(dc, geom);
        }
    }

    public void drawGeometry(DrawContext dc, Geometry geom) {

        int mode, count, type;
        Buffer elementBuffer;

        mode = geom.getMode(ELEMENT);
        count = geom.getCount(ELEMENT);
        type = geom.getGLType(ELEMENT);
        elementBuffer = geom.getBuffer(ELEMENT);

        this.drawGeometry(dc, mode, count, type, elementBuffer, geom);
    }

    public void drawGeometry(DrawContext dc, Geometry elementGeom, Geometry vertexGeom) {
        int mode, count, type;
        Buffer elementBuffer;

        mode = elementGeom.getMode(ELEMENT);
        count = elementGeom.getCount(ELEMENT);
        type = elementGeom.getGLType(ELEMENT);
        elementBuffer = elementGeom.getBuffer(ELEMENT);

        this.drawGeometry(dc, mode, count, type, elementBuffer, vertexGeom);
    }

    //**************************************************************//
    //********************  Rendering Support  *********************//
    //**************************************************************//
    public void setBlending(GLWorld w) {

        final GL gl = w.gl;

        if (this.isUseEXTBlendFuncSeparate()) {
            this.setHaveEXTBlendFuncSeparate(gl.isExtensionAvailable(EXT_BLEND_FUNC_SEPARATE_STRING));
        }

        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

        gl.glEnable(GL.GL_BLEND);
        // The separate blend function correctly handles regular (non-premultiplied) colors. We want
        //     Cd = Cs*As + Cf*(1-As)
        //     Ad = As    + Af*(1-As)
        // So we use GL_EXT_blend_func_separate to specify different blending factors for source color and source
        // alpha.
        if (this.isUseEXTBlendFuncSeparate() && this.isHaveEXTBlendFuncSeparate()) {
            gl.glBlendFuncSeparate(
                    GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, // rgb   blending factors
                    GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);                  // alpha blending factors
        } // Fallback to a single blending factor for source color and source alpha. The destination alpha will be
        // incorrect.
        else {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); // rgba  blending factors
        }
    }

    public void setLighting(GLWorld w) {

        final GL gl = w.gl;

        gl.glEnable(GL.GL_LIGHTING);
        setLightModel(gl);
        setShadeModel(gl);

        gl.glEnable(GL.GL_LIGHT0);
        setLightMaterial(gl, GL.GL_LIGHT0, this.lightMaterial);
        setLightDirection(gl, GL.GL_LIGHT0, this.lightDirection);
    }

    protected static void setLightModel(GL gl) {
        float[] modelAmbient = new float[4];
        modelAmbient[0] = 1.0f;
        modelAmbient[1] = 1.0f;
        modelAmbient[2] = 1.0f;
        modelAmbient[3] = 0.0f;

        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, modelAmbient, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_FALSE);
    }

    protected static void setShadeModel(GL gl) {
        gl.glShadeModel(GL.GL_SMOOTH);
    }

    protected static void setLightMaterial(GL gl, int light, Material material) {
        // The alpha value at a vertex is taken only from the diffuse material's alpha channel, without any
        // lighting computations applied. Therefore we specify alpha=0 for all lighting ambient, specular and
        // emission values. This will have no effect on material alpha.

        float[] ambient = new float[4];
        float[] diffuse = new float[4];
        float[] specular = new float[4];
        material.getDiffuse().getRGBColorComponents(diffuse);
        material.getSpecular().getRGBColorComponents(specular);
        ambient[3] = diffuse[3] = specular[3] = 0.0f;

        gl.glLightfv(light, GL.GL_AMBIENT, ambient, 0);
        gl.glLightfv(light, GL.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(light, GL.GL_SPECULAR, specular, 0);
    }

    protected static void setLightDirection(GL gl, int light, Vec4 direction) {

        // Setup the light as a directional light coming from the viewpoint. This requires two state changes
        // (a) Set the light position as direction x, y, z, and set the w-component to 0, which tells OpenGL this is
        //     a directional light.
        // (b) Invoke the light position call with the identity matrix on the modelview stack. Since the position
        //     is transfomed by the 

        Vec4 vec = direction.normalize3();
        float[] params = new float[4];
        params[0] = (float) vec.x;
        params[1] = (float) vec.y;
        params[2] = (float) vec.z;
        params[3] = 0.0f;

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glLightfv(light, GL.GL_POSITION, params, 0);

        gl.glPopMatrix();
    }

    protected void logGeometryStatistics(DrawContext dc, Geometry geom) {

        int geomCount = 0;
        int vertexCount = 0;

        Iterator<PerformanceStatistic> iter = dc.getPerFrameStatistics().iterator();
        while (iter.hasNext()) {
            PerformanceStatistic stat = iter.next();
            if (PerformanceStatistic.AIRSPACE_GEOMETRY_COUNT.equals(stat.getKey())) {
                geomCount += (Integer) stat.getValue();
                iter.remove();
            }
            if (PerformanceStatistic.AIRSPACE_VERTEX_COUNT.equals(stat.getKey())) {
                vertexCount += (Integer) stat.getValue();
                iter.remove();
            }
        }

        geomCount += 1;
        vertexCount += geom.getCount(VERTEX);
        dc.setPerFrameStatistic(PerformanceStatistic.AIRSPACE_GEOMETRY_COUNT, "Airspace Geometry Count", geomCount);
        dc.setPerFrameStatistic(PerformanceStatistic.AIRSPACE_VERTEX_COUNT, "Airspace Vertex Count", vertexCount);
    }

    // Editor methods......
    public boolean isArmed() {
        return this.armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    public boolean isUseRubberBand() {
        return this.useRubberBand;
    }

    public void setUseRubberBand(boolean state) {
        this.useRubberBand = state;
    }

    public LLHAreaControlPointRenderer getControlPointRenderer() {
        return this.controlPointRenderer;
    }

    public void setControlPointRenderer(LLHAreaControlPointRenderer renderer) {
        this.controlPointRenderer = renderer;
    }
    /*
     public LLHAreaEditListener[] getEditListeners() {
     return this.eventListeners.getListeners(LLHAreaEditListener.class);
     }

     public void addEditListener(LLHAreaEditListener listener) {
     this.eventListeners.add(LLHAreaEditListener.class, listener);
     }

     public void removeEditListener(LLHAreaEditListener listener) {
     this.eventListeners.remove(LLHAreaEditListener.class, listener);
     }
     */
    //**************************************************************//
    //********************  Control Point Rendering  ***************//
    //**************************************************************//

    protected void drawControlPoints(DrawContext dc, Point pickPoint) {

        this.getCurrentControlPoints().clear();
        this.assembleControlPoints(dc);
        if (dc.isPickingMode()) {
            this.getControlPointRenderer().pick(dc, this.getCurrentControlPoints(), pickPoint, this);
        } else {
            this.getControlPointRenderer().render(dc, this.getCurrentControlPoints());
        }
    }

    protected java.util.List<LLHAreaControlPoint> getCurrentControlPoints() {
        return currentControlPoints;
    }

    protected void setCurrentControlPoints(java.util.List<? extends LLHAreaControlPoint> controlPointList) {
        currentControlPoints.clear();
        currentControlPoints.addAll(controlPointList);
    }

    protected void addControlPoint(DrawContext dc, LLHAreaControlPoint controlPoint) {
        currentControlPoints.add(controlPoint);
    }

    protected void addControlPoints(DrawContext dc, List<LLHAreaControlPoint> controlPoints) {
        for (LLHAreaControlPoint p : controlPoints) {
            currentControlPoints.add(p);
        }
    }

    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//
    public void moveAirspaceVertically(WorldWindow wwd, LLHArea airspace,
            Point mousePoint, Point previousMousePoint) {
        // Include this test to ensure any derived implementation performs it.
        if (this.getAirspace() == null || this.getAirspace() != airspace) {
            return;
        }

        this.doMoveAirspaceVertically(wwd, airspace, mousePoint, previousMousePoint);
    }

    public LLHAreaControlPoint addControlPoint(WorldWindow wwd, LLHArea airspace,
            Point mousePoint) {
        // Include this test to ensure any derived implementation performs it.
        if (this.getAirspace() == null || this.getAirspace() != airspace) {
            return null;
        }

        if (wwd == null || mousePoint == null) {
            return null;
        }

        return this.doAddControlPoint(wwd, airspace, mousePoint);
    }

    /*
     protected void fireAirspaceResized(LLHAreaEditEvent e) {
     // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
     // order they were added.
     LLHAreaEditListener[] listeners = this.eventListeners.getListeners(LLHAreaEditListener.class);
     for (int i = listeners.length - 1; i >= 0; i--) {
     listeners[i].airspaceResized(e);
     }
     }
     */
    public Position airspaceOrigin(WorldWindow wwd, LLHArea airspace, Point mousePoint) {
        // Position newPosition = mouseToPosition(wwd, elevation, mousePoint);
        // return newPosition;    
        Movable movable = airspace;
        Position refPos = movable.getReferencePosition();
        double elevation = refPos.getElevation();
        Position clickedPosition = mouseToPosition(wwd, elevation, mousePoint);
        if (clickedPosition == null) {
            return null;
        }

        return clickedPosition;
        /*
         Position newPosition = null;
         LLHArea area = this.getAirspace();
         if (area instanceof LLHAreaSet) {
         LLHAreaSet set = (LLHAreaSet) (area);
         FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui control...hummm
         // currently copying all points into 'points'
         @SuppressWarnings("unchecked")
         ArrayList<LatLon> list = ((ArrayList<LatLon>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
         if (list != null) {

         if (list.size() > 0) {  // Need at least one existing point for reference...
         LatLon z = list.get(0);
         LatLon copyZ = new LatLon(z);
         copyZ.subtract(clickedPosition);
         newPosition = new Position(copyZ, elevation);
         }
         }
         }
         return newPosition;
         * */
    }

    public ArrayList<LLD> originList(WorldWindow wwd, LLHArea area, Point mousePoint) {
        if (area instanceof LLHAreaSet) {
            LLHAreaSet set = (LLHAreaSet) (area);
            FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui control...hummm
            // currently copying all points into 'points'
            ArrayList<LLD> newList = new ArrayList<LLD>();
            @SuppressWarnings("unchecked")
            ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
            for (LLD l : list) {
                LLD copy = new LLD(l);
                newList.add(copy);
            }
            return newList;
        }
        return null;
    }

    /**
     * Calculate mouse to position on airspace..
     */
    public Position mouseToPosition(WorldWindow wwd, double elevation, Point mousePoint) {
        // Move the point to the ray intersection.  Don't use previous/next because other things can move the
        // worldwind window (such as scroll wheel)..the user expects the point to be where their mouse is.
        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, elevation, ray);
        if (newPoint == null) {
            return null;
        }

        Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);
        return newPosition;
    }
    //**************************************************************//
    //********************  Default Event Handling  ****************//
    //**************************************************************//

    public void doMoveAirspaceLaterally(WorldWindow wwd, LLHArea airspace,
            ArrayList<LLD> originList, Position origin, Point mousePoint) {

        // Include this test to ensure any derived implementation performs it.
        if (this.getAirspace() == null || this.getAirspace() != airspace) {
            return;
        }

        // Move the point to the ray intersection.  Don't use previous/next because other things can move the
        // worldwind window (such as scroll wheel)..the user expects the point to be where their mouse is.
        //  Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        // Use elevation of surface, or existing elevation of point....
        Movable movable = airspace;
        Position refPos = movable.getReferencePosition();
        double elevation = refPos.getElevation();
        //double elevation = AirspaceEditorUtil.surfaceElevationAt(wwd, ray);

        //  Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, elevation, ray);
        //   if (newPoint == null) {
        //       return;
        //  }

        //Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);

        Position newPosition = mouseToPosition(wwd, elevation, mousePoint);
        if (newPosition == null) {
            return;
        }
        if (originList == null) {
            return;
        }

        //Position delta = newPosition.subtract(origin);

        LLHArea area = this.getAirspace();
        if (area instanceof LLHAreaSet) {
            LLHAreaSet set = (LLHAreaSet) (area);
            FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui control...hummm
            // currently copying all points into 'points'
            @SuppressWarnings("unchecked")
            ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
            if (list != null) {

                // Delta from original point to current point...
                double dlat = newPosition.latitude.degrees - origin.latitude.degrees;
                double dlon = newPosition.longitude.degrees - origin.longitude.degrees;

                if (!originList.isEmpty()) {  // Need at least one existing point for reference...
                    int index = 0;
                    for (LLD l : originList) {
                       // LatLon l2 = LatLon.fromDegrees(l.latitude.degrees + dlat, l.longitude.degrees + dlon);
                        LLD l2 = new LLD(l.latDegrees() + dlat, l.lonDegrees() + dlon);
                        list.set(index, l2);
              // FIXMELLD: Not sure we use/need the elevation stored...
              //          Position temp = new Position(l2, elevation);
              //          list.set(index, temp);
                        index++;

                    }
                    //list.add(newPosition);
                    // m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, list); same list
                    FeatureMemento fm = (FeatureMemento) (m); // Check it
                    FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
                    CommandManager.getInstance().executeCommand(c, true);
                }
            }
        }
        //eturn;
    }

    protected void doMoveAirspaceVertically(WorldWindow wwd, LLHArea airspace,
            Point mousePoint, Point previousMousePoint) {
        // Find the closest points between the rays through each screen point, and the ray from the control point
        // and in the direction of the globe's surface normal. Compute the elevation difference between these two
        // points, and use that as the change in airspace altitude.
        //
        // If the state keepControlPointsAboveTerrain is set, we prevent the control point from passing any lower than
        // the terrain elevation beneath it.

        Movable movable = airspace;
        Position referencePos = movable.getReferencePosition();
        Vec4 referencePoint = wwd.getModel().getGlobe().computePointFromPosition(referencePos);

        Vec4 surfaceNormal = wwd.getModel().getGlobe().computeSurfaceNormalAtLocation(referencePos.getLatitude(), referencePos.getLongitude());
        Line verticalRay = new Line(referencePoint, surfaceNormal);
        Line screenRay = wwd.getView().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());
        Line previousScreenRay = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        Vec4 pointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, screenRay);
        Vec4 previousPointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, previousScreenRay);

        Position pos = wwd.getModel().getGlobe().computePositionFromPoint(pointOnLine);
        Position previousPos = wwd.getModel().getGlobe().computePositionFromPoint(previousPointOnLine);
        double elevationChange = previousPos.getElevation() - pos.getElevation();
        elevationChange /= wwd.getSceneController().getDrawContext().getVerticalExaggeration();

        double[] altitudes = this.getAirspace().getAltitudes();

        // Always keep control points above terran
        // Note that this trims entire vertical movement
        if (altitudes[LOWER_ALTITUDE] + elevationChange < 0.0) {
            elevationChange = 0.0 - altitudes[LOWER_ALTITUDE];
        }
        altitudes[LOWER_ALTITUDE] += elevationChange;
        altitudes[UPPER_ALTITUDE] += elevationChange;
        pinAltitudes(altitudes);
        this.getAirspace().setAltitudes(altitudes[LOWER_ALTITUDE], altitudes[UPPER_ALTITUDE]);

        LLHArea area = this.getAirspace();
        if (area instanceof LLHAreaSet) {
            LLHAreaSet set = (LLHAreaSet) (area);
            FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui control...hummm
            // currently copying all points into 'points'
            @SuppressWarnings("unchecked")
            ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
            if (list != null) {
                //list.add(newLocation);
                // m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, list); same list
                FeatureMemento fm = (FeatureMemento) (m); // Check it
                FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
        // this.fireAirspaceMoved(new LLHAreaEditEvent(wwd, airspace, this));
    }

    public void pinAltitudes(double[] altitudes) {
        if (altitudes[LOWER_ALTITUDE] < 0.0) {
            altitudes[LOWER_ALTITUDE] = 0.0;
        }
        if (altitudes[UPPER_ALTITUDE] < 0.001) {
            altitudes[UPPER_ALTITUDE] = 0.001;
        }
    }

    public static Vec4 nearestPointOnLine(Line source, Line target) {
        // Compute the points on each ray that are closest to one another.
        // Taken from "Mathematics for 3D Game Programming..." by Eric Lengyel, Section 4.1.2.

        double dot_dir = source.getDirection().dot3(target.getDirection());
        double c = 1.0 / (dot_dir * dot_dir - 1.0);
        double a1 = target.getOrigin().subtract3(source.getOrigin()).dot3(source.getDirection());
        double a2 = target.getOrigin().subtract3(source.getOrigin()).dot3(target.getDirection());
        double t1 = c * (a2 * dot_dir - a1);

        return source.getPointAt(t1);
    }

    public static double computeLowestHeightAboveSurface(WorldWindow wwd,
            Iterable<? extends LLHAreaControlPoint> controlPoints, int altitudeIndex) {
        double minHeight = Double.MAX_VALUE;

        for (LLHAreaControlPoint controlPoint : controlPoints) {
            if (altitudeIndex == controlPoint.getAltitudeIndex()) {
                double height = computeHeightAboveSurface(wwd, controlPoint.getPoint());
                if (height < minHeight) {
                    minHeight = height;
                }
            }
        }

        return minHeight;
    }

    public static double computeHeightAboveSurface(WorldWindow wwd, Vec4 point) {
        Position pos = wwd.getModel().getGlobe().computePositionFromPoint(point);
        Vec4 surfacePoint = computeSurfacePoint(wwd, pos.getLatitude(), pos.getLongitude());
        Vec4 surfaceNormal = wwd.getModel().getGlobe().computeSurfaceNormalAtPoint(point);
        return point.subtract3(surfacePoint).dot3(surfaceNormal);
    }

    public static Vec4 computeSurfacePoint(WorldWindow wwd, Angle latitude, Angle longitude) {
        Vec4 point = wwd.getSceneController().getTerrain().getSurfacePoint(latitude, longitude);
        if (point != null) {
            return point;
        }

        return wwd.getModel().getGlobe().computePointFromPosition(latitude, longitude, 0.0);
    }

    public LLHArea getAirspace() {
        LLHArea ap = null;

        Feature f = FeatureList.theFeatures.getSelected(LLHAreaFeature.LLHAreaGroup);
        if (f != null) {
            LLHAreaFeature l = (LLHAreaFeature) (f);
            ap = l.getLLHArea();
        }
        return ap;
    }

    /**
     * Get the current controls points. Grabs the points from the current
     * selected LLHArea
     */
    protected void assembleControlPoints(DrawContext dc) {
        Feature f = FeatureList.theFeatures.getSelected(LLHAreaFeature.LLHAreaGroup);
        if ((f == null) || (!f.getVisible())) {
            return;
        }
        LLHAreaFeature l = (LLHAreaFeature) (f);
        LLHArea area = l.getLLHArea();

        if (area == null) {
            return;
        }

        List<LLHAreaControlPoint> list = area.getControlPoints(dc);
        addControlPoints(dc, list);
    }

    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//
    protected LLHAreaControlPoint doAddControlPoint(WorldWindow wwd, LLHArea airspace,
            Point mousePoint) {
        if (this.getAirspace().getLocations().isEmpty()) {
            return this.doAddFirstLocation(wwd, mousePoint);
        }
        return this.doAddNextLocation(wwd, mousePoint);
    }

    protected LLHAreaControlPoint doAddFirstLocation(WorldWindow wwd, Point mousePoint) {

        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        double surfaceElevation = AirspaceEditorUtil.surfaceElevationAt(wwd, ray);

        Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, surfaceElevation, ray);
        if (newPoint == null) {
            return null;
        }

        Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);
        double[] altitudes = new double[2];
        altitudes[LOWER_ALTITUDE] = newPosition.getElevation();
        altitudes[UPPER_ALTITUDE] = newPosition.getElevation() + DEFAULT_POLYGON_HEIGHT;
        pinAltitudes(altitudes);

        this.getAirspace().setAltitudes(altitudes[LOWER_ALTITUDE], altitudes[UPPER_ALTITUDE]);

        ArrayList<LLD> locationList = new ArrayList<LLD>();
        LatLon l = new LatLon(newPosition);
        //FIXMELLH: position ignored
        locationList.add(new LLD(l.latitude.degrees, l.longitude.degrees));
        //locationList.add(new LatLon(newPosition));

        this.getAirspace().setLocations(locationList);

        LLHAreaControlPoint controlPoint =
                new LLHAreaControlPoint(this.getAirspace(), 0, LOWER_ALTITUDE, newPoint);

        return controlPoint;
    }

    protected LLHAreaControlPoint doAddNextLocation(WorldWindow wwd, Point mousePoint) {
        // Try to find the edge that is closest to a ray passing through the screen point. We're trying to determine
        // the user's intent as to which edge a new two control points should be added to. We create a list of all
        // potentiall control point edges, then find the best match. We compute the new location by intersecting the
        // geoid with the screen ray, then create a new control point by inserting that point into the location list
        // based on the points orientaton relative to the edge.

        List<LLHAreaEdgeInfo> edgeInfoList = computeEdgeInfoFor(
                this.getAirspace().getLocations().size(), this.getCurrentControlPoints());

        if (edgeInfoList.isEmpty()) {
            return null;
        }

        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        LLHAreaEdgeInfo bestMatch = selectBestEdgeMatch(
                wwd, ray, this.getAirspace(), edgeInfoList);

        if (bestMatch == null) {
            return null;
        }

        LLHAreaControlPoint controlPoint = createControlPointFor(
                wwd, ray, this, this.getAirspace(), bestMatch);

        Vec4 newPoint = controlPoint.getPoint();
        LatLon newLocation = new LatLon(wwd.getModel().getGlobe().computePositionFromPoint(newPoint));
        LLD l = new LLD(newLocation.latitude.degrees, newLocation.longitude.degrees);
        ArrayList<LLD> locationList = new ArrayList<LLD>(this.getAirspace().getLocations());
        locationList.add(controlPoint.getLocationIndex(), l);

        LLHArea area = this.getAirspace();
        if (area instanceof LLHAreaSet) {
            LLHAreaSet set = (LLHAreaSet) (area);
            LLD newLLD = new LLD(newLocation.latitude.degrees, newLocation.longitude.degrees);
            PointAddCommand c = new PointAddCommand(set, newLLD, controlPoint.getLocationIndex());
            CommandManager.getInstance().executeCommand(c, true);
        }

        return controlPoint;
    }

    public static LLHAreaEdgeInfo selectBestEdgeMatch(WorldWindow wwd, Line ray,
            LLHArea airspace, List<? extends LLHAreaEdgeInfo> edgeInfoList) {
        // Try to find the edge that is closest to the given ray. This is used by the routine doAddNextLocation(),
        // which is trying to determine the user's intent as to which edge a new two control points should be added to.
        // Therefore consider the potential locations of a new control point on the ray: one for each of the lower
        // and upper airspace altitudes. We choose the edge that is closest to one of these points. We will ignore
        // an edge if its nearest point is behind the ray origin.

        Vec4[] pointOnLine = new Vec4[2];
        pointOnLine[LOWER_ALTITUDE] = intersectAirspaceAltitudeAt(wwd, airspace, LOWER_ALTITUDE, ray);
        pointOnLine[UPPER_ALTITUDE] = intersectAirspaceAltitudeAt(wwd, airspace, UPPER_ALTITUDE, ray);

        LLHAreaEdgeInfo bestEdge = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LLHAreaEdgeInfo edge : edgeInfoList) {
            for (int index = 0; index < 2; index++) {
                Vec4 pointOnEdge = nearestPointOnSegment(edge.point1, edge.point2, pointOnLine[index]);
                if (!isPointBehindLineOrigin(ray, pointOnEdge)) {
                    double d = pointOnEdge.distanceTo3(pointOnLine[index]);
                    if (d < nearestDistance) {
                        bestEdge = edge;
                        nearestDistance = d;
                    }
                }
            }
        }

        return bestEdge;
    }

    public static Vec4 nearestPointOnSegment(Vec4 p1, Vec4 p2, Vec4 point) {
        Vec4 segment = p2.subtract3(p1);
        Vec4 dir = segment.normalize3();

        double dot = point.subtract3(p1).dot3(dir);
        if (dot < 0.0) {
            return p1;
        } else if (dot > segment.getLength3()) {
            return p2;
        } else {
            return Vec4.fromLine3(p1, dot, dir);
        }
    }

    public static Vec4 intersectAirspaceAltitudeAt(WorldWindow wwd, LLHArea airspace, int altitudeIndex, Line ray) {
        double elevation = airspace.getAltitudes()[altitudeIndex];

        return intersectGlobeAt(wwd, elevation, ray);
    }

    public static Vec4 intersectGlobeAt(WorldWindow wwd, double elevation, Line ray) {
        Intersection[] intersections = wwd.getModel().getGlobe().intersect(ray, elevation);
        if (intersections == null || intersections.length == 0) {
            return null;
        }

        return nearestIntersectionPoint(ray, intersections);
    }

    public static Vec4 nearestIntersectionPoint(Line line, Intersection[] intersections) {
        Vec4 intersectionPoint = null;

        // Find the nearest intersection that's in front of the ray origin.
        double nearestDistance = Double.MAX_VALUE;
        for (Intersection intersection : intersections) {
            // Ignore any intersections behind the line origin.
            if (!isPointBehindLineOrigin(line, intersection.getIntersectionPoint())) {
                double d = intersection.getIntersectionPoint().distanceTo3(line.getOrigin());
                if (d < nearestDistance) {
                    intersectionPoint = intersection.getIntersectionPoint();
                    nearestDistance = d;
                }
            }
        }

        return intersectionPoint;
    }

    public static boolean isPointBehindLineOrigin(Line line, Vec4 point) {
        double dot = point.subtract3(line.getOrigin()).dot3(line.getDirection());
        return dot < 0.0;
    }

    public static LLHAreaControlPoint createControlPointFor(WorldWindow wwd, Line ray,
            LLHAreaLayer editor,
            //  LLHAreaEditor editor, 

            LLHArea airspace, LLHAreaEdgeInfo edge) {
        // If the nearest point occurs before the line segment, then insert the new point before the segment. If the
        // nearest point occurs after the line segment, then insert the new point after the segment. If the nearest
        // point occurs inside the line segment, then insert the new point in the segment.

        Vec4 newPoint = intersectAirspaceAltitudeAt(wwd, airspace, edge.altitudeIndex, ray);
        Vec4 pointOnEdge = nearestPointOnSegment(edge.point1, edge.point2, newPoint);

        int locationIndex;
        int altitudeIndex = edge.altitudeIndex;

        if (pointOnEdge == edge.point1) {
            locationIndex = edge.locationIndex;
        } else if (pointOnEdge == edge.point2) {
            locationIndex = edge.nextLocationIndex + 1;
        } else // (o == Orientation.INSIDE)
        {
            locationIndex = edge.nextLocationIndex;
        }

        return new LLHAreaControlPoint(airspace, locationIndex, altitudeIndex, newPoint);
    }

    public static List<LLHAreaEdgeInfo> computeEdgeInfoFor(int numLocations,
            Iterable<? extends LLHAreaControlPoint> controlPoints) {
        // Compute edge data structures for the segment between each successive pair of control points, including the
        // edge between the last and first control points. Do this for the upper and lower altitudes of the airspace.
        // We cannot assume anything about the ordering of the control points handed to us, but we must be able to
        // access them by location index and altitude index. To achieve this we place them in a map that will be
        // indexable by location and altitude.

        ArrayList<LLHAreaEdgeInfo> edgeInfoList = new ArrayList<LLHAreaEdgeInfo>();

        HashMap<Object, LLHAreaControlPoint> map = new HashMap<Object, LLHAreaControlPoint>();
        for (LLHAreaControlPoint p : controlPoints) {
            map.put(p.getKey(), p);
        }

        for (int altitudeIndex = 0; altitudeIndex < 2; altitudeIndex++) {
            for (int locationIndex = 0; locationIndex < numLocations; locationIndex++) {
                int nextLocationIndex = (locationIndex < numLocations - 1) ? (locationIndex + 1) : 0;
                Object key = LLHAreaControlPoint.keyFor(locationIndex, altitudeIndex);
                Object nextKey = LLHAreaControlPoint.keyFor(nextLocationIndex, altitudeIndex);

                LLHAreaControlPoint controlPoint = map.get(key);
                LLHAreaControlPoint nextControlPoint = map.get(nextKey);

                if (controlPoint != null && nextControlPoint != null) {
                    edgeInfoList.add(new LLHAreaEdgeInfo(locationIndex, nextLocationIndex, altitudeIndex,
                            controlPoint.getPoint(), nextControlPoint.getPoint()));
                }
            }
        }

        return edgeInfoList;
    }

    /**
     * Delete a single point
     */
    public void doRemoveControlPoint(LLHAreaControlPoint controlPoint) {
        if (controlPoint != null) {
            int index = controlPoint.getLocationIndex();
            LLHArea area = this.getAirspace();
            if (area instanceof LLHAreaSet) {
                LLHAreaSet set = (LLHAreaSet) (area);
                PointRemoveCommand c = new PointRemoveCommand(set, index);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
    }

    /**
     * Move a single point
     */
    public void doMoveControlPoint(WorldWindow wwd, LLHAreaControlPoint controlPoint,
            Point mousePoint) {

        // Include this test to ensure any derived implementation performs it.
        if (this.getAirspace() == null) {
            return;
        }

        if (this.getAirspace() != controlPoint.getAirspace()) {
            return;
        }

        // Move the point to the ray intersection.  Don't use previous/next because other things can move the
        // worldwind window (such as scroll wheel)..the user expects the point to be where their mouse is.
        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        // Use elevation of surface, or existing elevation of point....
        Position controlPointPos = wwd.getModel().getGlobe().computePositionFromPoint(controlPoint.getPoint());
        double elevation = controlPointPos.getElevation();
        //double elevation = AirspaceEditorUtil.surfaceElevationAt(wwd, ray);

        Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, elevation, ray);
        if (newPoint == null) {
            return;
        }

        Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);

        int index = controlPoint.getLocationIndex();
        LLHArea area = this.getAirspace();
        if (area instanceof LLHAreaSet) {
            LLHAreaSet set = (LLHAreaSet) (area);
            FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui control...hummm
            // currently copying all points into 'points'
            @SuppressWarnings("unchecked")
            ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
            if (list != null) {
             // FIXMELLH:   list.set(index, newPosition);
                LLD newOne = new LLD(newPosition.latitude.degrees, newPosition.longitude.degrees);
                list.set(index, newOne);
                
                FeatureMemento fm = (FeatureMemento) (m); // Check it
                FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
                CommandManager.getInstance().executeCommand(c, true);
            }
        } else {
            List<LLD> newLocationList = new ArrayList<LLD>(this.getAirspace().getLocations());
             // FIXMELLH:   list.set(index, newPosition);
                LLD newOne = new LLD(newPosition.latitude.degrees, newPosition.longitude.degrees);
            newLocationList.set(index, newOne);
            this.getAirspace().setLocations(newLocationList);
        }
    }

    public void doResizeAtControlPoint(WorldWindow wwd, LLHAreaControlPoint controlPoint,
            Point mousePoint, Point previousMousePoint) {
        // Include this test to ensure any derived implementation performs it.
        if (this.getAirspace() == null) {
            return;
        }
        if (this.getAirspace() != controlPoint.getAirspace()) {
            return;
        }
        // Find the closest points between the rays through each screen point, and the ray from the control point
        // and in the direction of the globe's surface normal. Compute the elevation difference between these two
        // points, and use that as the change in airspace altitude.
        //
        // When the airspace is collapsed, override the
        // selected control point altitude. This will typically be the case when the airspace is new. If the user drags
        // up, then adjust the upper altiutde. If the user drags down, then adjust the lower altitude.
        //
        // If the state keepControlPointsAboveTerrain is set, we prevent the control point from passing any lower than
        // the terrain elevation beneath it.

        Vec4 surfaceNormal = wwd.getModel().getGlobe().computeSurfaceNormalAtPoint(controlPoint.getPoint());
        Line verticalRay = new Line(controlPoint.getPoint(), surfaceNormal);
        Line screenRay = wwd.getView().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());
        Line previousScreenRay = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        Vec4 pointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, screenRay);
        Vec4 previousPointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay, previousScreenRay);

        Position pos = wwd.getModel().getGlobe().computePositionFromPoint(pointOnLine);
        Position previousPos = wwd.getModel().getGlobe().computePositionFromPoint(previousPointOnLine);
        double elevationChange = previousPos.getElevation() - pos.getElevation();
        elevationChange /= wwd.getSceneController().getDrawContext().getVerticalExaggeration();

        int index = controlPoint.getAltitudeIndex();

        double[] altitudes = controlPoint.getAirspace().getAltitudes();

        double d = computeMinimumDistanceBetweenAltitudes(this.getAirspace().getLocations().size(),
                this.getCurrentControlPoints());
        if (index == LOWER_ALTITUDE) {
            if (elevationChange > d) {
                elevationChange = d;
            }
        } else if (index == UPPER_ALTITUDE) {
            if (elevationChange < -d) {
                elevationChange = -d;
            }
        }

        altitudes[index] += elevationChange;
        pinAltitudes(altitudes);

        controlPoint.getAirspace().setAltitudes(altitudes[LOWER_ALTITUDE], altitudes[UPPER_ALTITUDE]);

        // LLHAreaEditEvent editEvent = new LLHAreaEditEvent(wwd, controlPoint.getAirspace(), this, controlPoint);
        // this.fireControlPointChanged(editEvent);
        //this.fireAirspaceResized(editEvent);
    }

    public static double computeMinimumDistanceBetweenAltitudes(int numLocations,
            Iterable<? extends LLHAreaControlPoint> controlPoints) {
        // We cannot assume anything about the ordering of the control points handed to us, but we must be able to
        // access them by location index and altitude index. To achieve this we place them in a map that will be
        // indexable by location and altitude.

        double minDistance = Double.MAX_VALUE;

        HashMap<Object, LLHAreaControlPoint> map = new HashMap<Object, LLHAreaControlPoint>();
        for (LLHAreaControlPoint p : controlPoints) {
            map.put(p.getKey(), p);
        }

        for (int locationIndex = 0; locationIndex < numLocations; locationIndex++) {
            Object lowerKey = LLHAreaControlPoint.keyFor(locationIndex, LOWER_ALTITUDE);
            Object upperKey = LLHAreaControlPoint.keyFor(locationIndex, UPPER_ALTITUDE);

            LLHAreaControlPoint lowerControlPoint = map.get(lowerKey);
            LLHAreaControlPoint upperControlPoint = map.get(upperKey);

            if (lowerControlPoint != null && upperControlPoint != null) {
                double distance = lowerControlPoint.getPoint().distanceTo3(upperControlPoint.getPoint());
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return (minDistance == Double.MAX_VALUE) ? 0.0 : minDistance;
    }
}
