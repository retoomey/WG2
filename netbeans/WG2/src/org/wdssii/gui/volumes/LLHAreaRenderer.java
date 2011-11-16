package org.wdssii.gui.volumes;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.airspaces.Geometry;
import gov.nasa.worldwind.util.Logging;

import java.awt.Point;
import java.nio.Buffer;

import javax.media.opengl.GL;

public class LLHAreaRenderer {

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

    public LLHAreaRenderer() {
        this.enableAntialiasing = false;
        this.enableBlending = true;
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

    public void setLightMaterial(Material material) {
        if (material == null) {
            String message = Logging.getMessage("nullValue.MaterialIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lightMaterial = material;
    }

    public Vec4 getLightDirection() {
        return this.lightDirection;
    }

    public void setLightDirection(Vec4 direction) {
        if (direction == null) {
            String message = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.lightDirection = direction;
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

    public void pickOrdered(DrawContext dc, Iterable<? extends LLHArea> airspaces, java.awt.Point pickPoint,
            Layer layer) {
        this.drawOrdered(dc, airspaces, layer);
    }

    public void renderOrdered(DrawContext dc, Iterable<? extends LLHArea> airspaces) {
        this.drawOrdered(dc, airspaces, null);
    }

    public void pickNow(DrawContext dc, Iterable<? extends LLHArea> airspaces, java.awt.Point pickPoint, Layer layer) {
        pickSupport.beginPicking(dc);
        try {
            // The pick method will bind pickable objects to the renderer's PickSupport.
            this.drawNow(dc, airspaces, pickSupport);
        } finally {
            pickSupport.endPicking(dc);
        }

        pickSupport.resolvePick(dc, pickPoint, layer);
        pickSupport.clearPickList();
    }

    public void renderNow(DrawContext dc, Iterable<? extends LLHArea> airspaces) {
        // The render method does not bind any pickable objects.
        this.drawNow(dc, airspaces, null);
    }

    protected void drawOrdered(DrawContext dc, Iterable<? extends LLHArea> airspaces, Layer layer) {
        for (LLHArea airspace : airspaces) {
            double eyeDistance = this.computeDistanceFromEye(dc, airspace);
            OrderedAirspace orderedAirspace = new OrderedAirspace(this, airspace, layer, eyeDistance);
            //dc.getOrderedRenderables().add(orderedAirspace);
            dc.getOrderedSurfaceRenderables().add(orderedAirspace);
        }
    }

    protected void drawNow(DrawContext dc, Iterable<? extends LLHArea> airspaces, PickSupport pickSupport) {
        this.beginRendering(dc);
        try {
            this.drawAirspaces(dc, airspaces, pickSupport);
        } finally {
            this.endRendering(dc);
        }
    }

    protected double computeDistanceFromEye(DrawContext dc, LLHArea airspace) {
        Extent extent = airspace.getExtent(dc);
        if (extent == null) {
            return 0;
        }

        if (dc.getView() == null || dc.getView().getEyePoint() == null) {
            return 0;
        }

        // Compute the distance from the eye point to the extent center.
        double distance = extent.getCenter().distanceTo3(dc.getView().getEyePoint());

        // If the eye point is inside the extent, then just return 0.
        if (distance < extent.getRadius()) {
            return 0;
        }
        // Otherwise, return the distance from the eye point to the nearest point on the extent.
        return distance - extent.getRadius();
    }

    //**************************************************************//
    //********************  Ordered Airspaces  *********************//
    //**************************************************************//
    protected static class OrderedAirspace implements OrderedRenderable {

        protected LLHAreaRenderer renderer;
        protected LLHArea airspace;
        protected Layer layer;
        protected double eyeDistance;

        public OrderedAirspace(LLHAreaRenderer renderer, LLHArea airspace, Layer layer, double eyeDistance) {
            this.renderer = renderer;
            this.airspace = airspace;
            this.layer = layer;
            this.eyeDistance = eyeDistance;
        }

        public LLHAreaRenderer getRenderer() {
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
            this.draw(dc, null);
        }

        @Override
        public void pick(DrawContext dc, Point pickPoint) {
            PickSupport pickSupport = this.getRenderer().getPickSupport();
            pickSupport.beginPicking(dc);
            try {
                // The pick method will bind pickable objects to the renderer's PickSupport.
                this.draw(dc, pickSupport);
            } finally {
                pickSupport.endPicking(dc);
            }

            pickSupport.resolvePick(dc, pickPoint, this.getLayer());
            pickSupport.clearPickList();
        }

        protected void draw(DrawContext dc, PickSupport pickSupport) {
            renderer.drawOrderedAirspace(dc, this, pickSupport);
        }
    }

    protected void drawOrderedAirspace(DrawContext dc, OrderedAirspace orderedAirspace, PickSupport pickSupport) {
        this.beginRendering(dc);
        try {
            this.drawAirspace(dc, orderedAirspace.getAirspace(), pickSupport);
            this.drawOrderedAirspaces(dc, pickSupport);
        } finally {
            this.endRendering(dc);
        }
    }

    protected void drawOrderedAirspaces(DrawContext dc, PickSupport pickSupport) {
        if (dc == null) {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Batch render as many Airspaces as we can to save OpenGL state switching.
        // OrderedRenderable top = dc.getOrderedRenderables().peek();
        OrderedRenderable top = dc.getOrderedSurfaceRenderables().peek();
        while (top != null && top instanceof OrderedAirspace) {
            OrderedAirspace oa = (OrderedAirspace) top;

            // If the next OrderedAirspace's renderer is different, then we must stop batching. Otherwise, we would
            // render an airspace with a renderer with potentially different properties or behavior.
            if (this != oa.getRenderer()) {
                return;
            }

            this.drawAirspace(dc, oa.getAirspace(), pickSupport);

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
    protected void drawAirspaces(DrawContext dc, Iterable<? extends LLHArea> airspaces, PickSupport pickSupport) {
        for (LLHArea airspace : airspaces) {
            try {
                if (airspace != null) {
                    this.drawAirspace(dc, airspace, pickSupport);
                }
            } catch (Exception e) {
                String message = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }
    }

    protected void drawAirspace(DrawContext dc, LLHArea airspace, PickSupport pickSupport) {
        try {
            if (pickSupport != null) {
                this.bindPickableObject(dc, airspace, pickSupport);
            }

            this.doDrawAirspace(dc, airspace);
        } catch (Exception e) {
            String message = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void doDrawAirspace(DrawContext dc, LLHArea airspace) {
        if (!airspace.isVisible()) {
            return;
        }

        if (!airspace.isAirspaceVisible(dc)) {
            return;
        }

        this.drawAirspaceShape(dc, airspace);

        if (!dc.isPickingMode()) {
            if (this.isDrawExtents()) {
                airspace.renderExtent(dc);
            }
        }
    }

    protected void drawAirspaceShape(DrawContext dc, LLHArea airspace) {
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

        GL gl = dc.getGL();

        // If the outline and interior will be drawn, then draw the outline color, but do not affect the depth buffer
        // (outline pixels do not need the depth test). When the interior is drawn, it will draw on top of these
        // colors, and the outline will be visible behind the potentially transparent interior.
        if (airspace.getAttributes().isDrawOutline() && airspace.getAttributes().isDrawInterior()) {
            gl.glColorMask(true, true, true, true);
            gl.glDepthMask(false);

            this.drawAirspaceOutline(dc, airspace);
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

                this.drawAirspaceInterior(dc, airspace);

                // Draw color.
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(false);
                gl.glPolygonOffset((float) this.getDepthOffsetFactor(), (float) this.getDepthOffsetUnits());

                this.drawAirspaceInterior(dc, airspace);
            } else {
                gl.glColorMask(true, true, true, true);
                gl.glDepthMask(true);
                this.drawAirspaceInterior(dc, airspace);
            }
        }

        // If the outline will be drawn, then draw the outline color, but do not affect the depth buffer (outline
        // pixels do not need the depth test). This will blend outline pixels with the interior pixels which are
        // behind the outline.
        if (airspace.getAttributes().isDrawOutline()) {
            gl.glColorMask(true, true, true, true);
            gl.glDepthMask(false);

            this.drawAirspaceOutline(dc, airspace);
        }
    }

    protected void drawAirspaceInterior(DrawContext dc, LLHArea airspace) {
        if (!dc.isPickingMode()) {
            if (this.isEnableLighting()) {
                dc.getGL().glEnable(GL.GL_LIGHTING);
            }

            airspace.getAttributes().applyInterior(dc, this.isEnableLighting());
        }

        airspace.renderGeometry(dc, "fill");
    }

    protected void drawAirspaceOutline(DrawContext dc, LLHArea airspace) {
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

        airspace.renderGeometry(dc, "outline");
    }

    protected void beginRendering(DrawContext dc) {
        GL gl = dc.getGL();

        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

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
                this.setBlending(dc);
            }

            if (this.isEnableLighting()) {
                gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
                this.setLighting(dc);
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

    protected void endRendering(DrawContext dc) {
        GL gl = dc.getGL();

        gl.glPopAttrib();
        gl.glPopClientAttrib();
    }

    protected void bindPickableObject(DrawContext dc, LLHArea airspace, PickSupport pickSupport) {
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
    public void setBlending(DrawContext dc) {
        GL gl = dc.getGL();

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

    public void setLighting(DrawContext dc) {
        GL gl = dc.getGL();

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
}
