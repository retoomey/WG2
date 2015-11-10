package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.airspaces.Geometry;
import java.awt.Point;
import java.nio.Buffer;
import java.util.*;
import javax.media.opengl.GL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V3;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.commands.PointAddCommand;
import org.wdssii.gui.commands.PointRemoveCommand;
import org.wdssii.gui.commands.PointSelectCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LLHAreaFeature;
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
 *         Merged Editor ability into this class, since we have a single
 *         'selected' object that is the currently edited one.
 */
public class LLHAreaLayer extends AbstractLayer implements WWCategoryLayer {

	private final static Logger LOG = LoggerFactory.getLogger(LLHAreaLayer.class);
	public static final String DRAW_STYLE_FILL = "Airspace.DrawStyleFill";
	public static final String DRAW_STYLE_OUTLINE = "Airspace.DrawStyleOutline";
	private static final String EXT_BLEND_FUNC_SEPARATE_STRING = "GL_EXT_blend_func_separate";
	protected static final int ELEMENT = 1;
	protected static final int VERTEX = 2;
	protected static final int NORMAL = 3;
	private boolean enableAntialiasing;
	// private boolean enableBlending;
	private boolean enableDepthOffset;
	// private boolean enableLighting;
	// private boolean useEXTBlendFuncSeparate;
	// private boolean haveEXTBlendFuncSeparate;
	private boolean drawExtents;
	private boolean drawWireframe;
	// private Material lightMaterial;
	// private Vec4 lightDirection;
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
	 * Creates a new <code>Airspace</code> with an empty collection of
	 * Airspaces.
	 */
	public LLHAreaLayer() {
		this.enableAntialiasing = false;
		// this.enableBlending = false;
		this.enableDepthOffset = false;
		this.drawExtents = false;
		this.drawWireframe = false;
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
		// pickOrdered(dc, getActiveAirspaces(), pickPoint, this);
		GLWorld newW = new GLWorldWW(dc);
		drawOrdered(newW, getActiveAirspaces(), this);

		// Draw the control points
		if (isArmed()) {
			drawControlPoints(newW, pickPoint);
		}
	}

	@Override
	protected void doRender(DrawContext dc) {
		// Draw the volumes themselves
		// renderOrdered(dc, getActiveAirspaces());
		GLWorld newW = new GLWorldWW(dc);
		drawOrdered(newW, getActiveAirspaces(), null);

		// Draw the control points
		if (this.isArmed()) {
			this.drawControlPoints(newW, null);
		}
	}

	public boolean isEnableAntialiasing() {
		return this.enableAntialiasing;
	}

	public void setEnableAntialiasing(boolean enable) {
		this.enableAntialiasing = enable;
	}

	public boolean isEnableDepthOffset() {
		return this.enableDepthOffset;
	}

	public void setEnableDepthOffset(boolean enable) {
		this.enableDepthOffset = enable;
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

	// public Material getLightMaterial() {
	// return this.lightMaterial;
	// }

	// public Vec4 getLightDirection() {
	// return this.lightDirection;
	// }

	public double getLinePickWidth() {
		return linePickWidth;
	}

	public void setLinePickWidth(double width) {
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
			final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
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

	// **************************************************************//
	// ******************** Ordered LLHArea *********************//
	// **************************************************************//
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
				// The pick method will bind pickable objects to the renderer's
				// PickSupport.
				this.draw(new GLWorldWW(dc), pickSupport);
			} finally {
				pickSupport.endPicking(dc);
			}

			pickSupport.resolvePick(dc, pickPoint, this.getLayer());
			pickSupport.clearPickList();
		}

		protected void draw(GLWorld w, PickSupport pickSupport) {
			// LLHAreaLayer renderer = this.getRenderer();
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
		// Batch render as many Airspaces as we can to save OpenGL state
		// switching.
		// OrderedRenderable top = dc.getOrderedRenderables().peek();
		final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
		OrderedRenderable top = dc.getOrderedSurfaceRenderables().peek();
		while (top != null && top instanceof OrderedLLHArea) {
			OrderedLLHArea oa = (OrderedLLHArea) top;

			// If the next OrderedAirspace's renderer is different, then we must
			// stop batching. Otherwise, we would
			// render an airspace with a renderer with potentially different
			// properties or behavior.
			if (this != oa.getRenderer()) {
				return;
			}

			this.drawAirspace(w, oa.getAirspace(), pickSupport);

			// Take the ordered airspace off the queue, then peek at the next
			// item in the queue (but do not remove it).
			// dc.getOrderedRenderables().poll();
			// top = dc.getOrderedRenderables().peek();
			dc.getOrderedSurfaceRenderables().poll();
			top = dc.getOrderedSurfaceRenderables().peek();
		}
	}

	// **************************************************************//
	// ******************** Airspace Rendering ********************//
	// **************************************************************//
	protected void drawAirspaces(GLWorld w, Iterable<? extends LLHArea> airspaces, PickSupport pickSupport) {
		for (LLHArea airspace : airspaces) {
			try {
				if (airspace != null) {
					this.drawAirspace(w, airspace, pickSupport);
				}
			} catch (Exception e) {
				// String message =
				// Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
				// Logging.logger().log(java.util.logging.Level.SEVERE, message,
				// e);
				LOG.error("Exception during render " + e.toString());
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
			// String message =
			// Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
			// Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
			LOG.error("Exception during render " + e.toString());
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
		// Draw the airspace shape using a multiple pass algorithm. The
		// motivation for this algorithm is twofold:
		//
		// 1. We want to draw the airspace on top of other intersecting shapes
		// with similar depth values to eliminate
		// z-fighting between shapes. However we do not wish to offset the
		// actual depth values, which would cause
		// a cascading increase in depth offset when many shapes are drawn.
		// 2. The airspace outline appears both in front of and behind the
		// shape. If the outline will be drawn using
		// GL line smoothing, or GL blending, then either the line must be
		// broken into two parts, or rendered in
		// two passes.
		//
		// These issues are resolved by making several passes for the interior
		// and outline, as follows:

		final GL gl = w.gl;
		final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
		// LOG.debug("MAIN PICKING MODE DC IS "+dc.isPickingMode());
		// If the outline and interior will be drawn, then draw the outline
		// color, but do not affect the depth buffer
		// (outline pixels do not need the depth test). When the interior is
		// drawn, it will draw on top of these
		// colors, and the outline will be visible behind the potentially
		// transparent interior.
		if (airspace.getAttributes().isDrawOutline() && airspace.getAttributes().isDrawInterior()) {
			gl.glColorMask(true, true, true, true);
			gl.glDepthMask(false);

			this.drawAirspaceOutline(w, airspace);
		}

		// If the interior will be drawn, then make two passes as follows. The
		// first pass draws the interior depth
		// values to the depth buffer without any polygon offset. This enables
		// the shape to contribute to the depth
		// buffer and occlude other geometries as it normally would. The second
		// pass draws the interior color values
		// with offset depth values, but does not affect the depth buffer. This
		// has the effect of givign this airspace
		// depth priority over whatever is already in the depth buffer. By
		// rendering the depth values normally, we
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

		// If the outline will be drawn, then draw the outline color, but do not
		// affect the depth buffer (outline
		// pixels do not need the depth test). This will blend outline pixels
		// with the interior pixels which are
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
			// if (this.isEnableLighting()) {
			// w.gl.glEnable(GL.GL_LIGHTING);
			// }
			airspace.getAttributes().applyInterior(dc, false); // this.isEnableLighting());
		}

		airspace.renderGeometry(w, "fill");
	}

	protected void drawAirspaceOutline(GLWorld w, LLHArea airspace) {
		final DrawContext dc = ((GLWorldWW) (w)).getDC(); // hack
		if (dc.isPickingMode()) {
			double lineWidth = airspace.getAttributes().getOutlineWidth();

			// If the airspace interior isn't drawn, make the outline wider
			// during picking.
			if (!airspace.getAttributes().isDrawInterior()) {
				if (lineWidth != 0.0) {
					lineWidth += this.getLinePickWidth();
				}
			}

			dc.getGL().glLineWidth((float) lineWidth);
		} else {
			// if (this.isEnableLighting()) {
			// dc.getGL().glDisable(GL.GL_LIGHTING);
			// }

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
			// (this.isEnableLighting() ? GL.GL_LIGHTING_BIT : 0) // For
			// lighting, material, and matrix mode
			GL.GL_COLOR_BUFFER_BIT // For color write mask. If blending is
									// enabled: for blending src and func, and
									// alpha func.
					| GL.GL_CURRENT_BIT // For current color.
					| GL.GL_DEPTH_BUFFER_BIT // For depth test, depth func,
												// depth write mask.
					| GL.GL_LINE_BIT // For line width, line smoothing.
					| GL.GL_POLYGON_BIT // For polygon mode, polygon offset.
					| GL.GL_TRANSFORM_BIT; // For matrix mode.
			gl.glPushAttrib(attribMask);

			if (this.isDrawWireframe()) {
				gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
			}

			// if (this.isEnableBlending()) {
			// this.setBlending(w);
			// }

			/*
			 * if (this.isEnableLighting()) {
			 * gl.glEnableClientState(GL.GL_NORMAL_ARRAY); this.setLighting(w);
			 * }
			 */

			if (this.isEnableAntialiasing()) {
				gl.glEnable(GL.GL_LINE_SMOOTH);
			}
		} else {
			int attribMask = GL.GL_CURRENT_BIT // For current color.
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

	// **************************************************************//
	// ******************** Geometry Rendering ********************//
	// **************************************************************//
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
			/*
			 * if (this.isEnableLighting()) { normalBuffer =
			 * geom.getBuffer(NORMAL); if (normalBuffer == null) {
			 * gl.glDisableClientState(GL.GL_NORMAL_ARRAY); } else { glType =
			 * geom.getGLType(NORMAL); stride = geom.getStride(NORMAL);
			 * gl.glNormalPointer(glType, stride, normalBuffer); } }
			 */
		}

		// On some hardware, using glDrawRangeElements allows vertex data to be
		// prefetched. We know the minimum and
		// maximum index values that are valid in elementBuffer (they are 0 and
		// vertexCount-1), so it's harmless
		// to use this approach and allow the hardware to optimize.
		minElementIndex = 0;
		maxElementIndex = geom.getCount(VERTEX) - 1;
		gl.glDrawRangeElements(mode, minElementIndex, maxElementIndex, count, type, elementBuffer);

		if (!dc.isPickingMode()) {
			// if (this.isEnableLighting()) {
			// if (normalBuffer == null) {
			// gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
			// }
			// }
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

	// **************************************************************//
	// ******************** Control Point Rendering ***************//
	// **************************************************************//

	protected void drawControlPoints(GLWorld w, Point pickPoint) {

		this.getCurrentControlPoints().clear();
		this.assembleControlPoints(w);
		if (w.isPickingMode()) {
			this.getControlPointRenderer().pick(w, this.getCurrentControlPoints(), pickPoint);
		} else {
			this.getControlPointRenderer().render(w, this.getCurrentControlPoints());
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

	protected void addControlPoints(List<LLHAreaControlPoint> controlPoints) {
		for (LLHAreaControlPoint p : controlPoints) {
			currentControlPoints.add(p);
		}
	}

	// **************************************************************//
	// ******************** Control Point Events ******************//
	// **************************************************************//
	public void moveAirspaceVertically(WorldWindow wwd, LLHArea airspace, Point mousePoint, Point previousMousePoint) {
		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null || this.getAirspace() != airspace) {
			return;
		}

		this.doMoveAirspaceVertically(wwd, airspace, mousePoint, previousMousePoint);
	}

	public LLHAreaControlPoint addControlPoint(WorldWindow wwd, LLHArea airspace, Point mousePoint, boolean shiftDown) {
		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null || this.getAirspace() != airspace) {
			return null;
		}

		if (wwd == null || mousePoint == null) {
			return null;
		}

		return this.doAddControlPoint(wwd, airspace, mousePoint, shiftDown);
	}

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

	}

	public ArrayList<LLD_X> originList(WorldWindow wwd, LLHArea area, Point mousePoint) {
		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			ArrayList<LLD_X> newList = new ArrayList<LLD_X>();
			ArrayList<LLD_X> list = null;
			list = m.get(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
			for (LLD_X l : list) {
				LLD_X copy = new LLD_X(l);
				newList.add(copy);
			}
			return newList;
		}
		return null;
	}

	public static Vec4 nearestPointOnLine(Line source, Line target) {
		// Compute the points on each ray that are closest to one another.
		// Taken from "Mathematics for 3D Game Programming..." by Eric Lengyel,
		// Section 4.1.2.

		double dot_dir = source.getDirection().dot3(target.getDirection());
		double c = 1.0 / (dot_dir * dot_dir - 1.0);
		double a1 = target.getOrigin().subtract3(source.getOrigin()).dot3(source.getDirection());
		double a2 = target.getOrigin().subtract3(source.getOrigin()).dot3(target.getDirection());
		double t1 = c * (a2 * dot_dir - a1);

		return source.getPointAt(t1);
	}

	public static double surfaceElevationAt(WorldWindow wwd, Line ray) {
		// Try to find the surface elevation at the mouse point by intersecting
		// a ray with the terrain.

		double surfaceElevation = 0.0;

		if (wwd.getSceneController().getTerrain() != null) {
			Intersection[] intersections = wwd.getSceneController().getTerrain().intersect(ray);
			if (intersections != null) {
				Vec4 point = nearestIntersectionPoint(ray, intersections);
				if (point != null) {
					Position pos = wwd.getModel().getGlobe().computePositionFromPoint(point);
					surfaceElevation = pos.getElevation();
				}
			}
		}

		return surfaceElevation;
	}

	/**
	 * Calculate mouse to position on airspace..
	 */
	public Position mouseToPosition(WorldWindow wwd, double elevation, Point mousePoint) {
		// Move the point to the ray intersection. Don't use previous/next
		// because other things can move the
		// worldwind window (such as scroll wheel)..the user expects the point
		// to be where their mouse is.
		Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

		// Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, elevation,
		// ray);
		Vec4 newPoint = intersectGlobeAt(wwd, elevation, ray);

		if (newPoint == null) {
			return null;
		}

		Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);
		return newPosition;
	}
	// **************************************************************//
	// ******************** Default Event Handling ****************//
	// **************************************************************//

	public void doMoveAirspaceLaterally(WorldWindow wwd, LLHArea airspace, ArrayList<LLD_X> originList, Position origin,
			Point mousePoint) {

		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null || this.getAirspace() != airspace) {
			return;
		}

		// Move the point to the ray intersection. Don't use previous/next
		// because other things can move the
		// worldwind window (such as scroll wheel)..the user expects the point
		// to be where their mouse is.
		// Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(),
		// mousePoint.getY());

		// Use elevation of surface, or existing elevation of point....
		Movable movable = airspace;
		Position refPos = movable.getReferencePosition();
		double elevation = refPos.getElevation();

		Position newPosition = mouseToPosition(wwd, elevation, mousePoint);
		if (newPosition == null) {
			return;
		}
		if (originList == null) {
			return;
		}

		LLHArea area = this.getAirspace();
		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			ArrayList<LLD_X> list = null;
			list = m.get(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
			if (list != null) {

				// Delta from original point to current point...
				double dlat = newPosition.latitude.degrees - origin.latitude.degrees;
				double dlon = newPosition.longitude.degrees - origin.longitude.degrees;

				if (!originList.isEmpty()) { // Need at least one existing point
												// for reference...
					int index = 0;
					for (LLD_X l : originList) {

						LLD_X l2 = new LLD_X(l.latDegrees() + dlat, l.lonDegrees() + dlon, l);
						list.set(index, l2);

						index++;

					}
					// list.add(newPosition);
					// m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
					// same list
					FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), m);
					CommandManager.getInstance().executeCommand(c, true);
				}
			}
		}
		// eturn;
	}

	protected void doMoveAirspaceVertically(WorldWindow wwd, LLHArea airspace, Point mousePoint,
			Point previousMousePoint) {
		// Find the closest points between the rays through each screen point,
		// and the ray from the control point
		// and in the direction of the globe's surface normal. Compute the
		// elevation difference between these two
		// points, and use that as the change in airspace altitude.
		//
		// If the state keepControlPointsAboveTerrain is set, we prevent the
		// control point from passing any lower than
		// the terrain elevation beneath it.

		Movable movable = airspace;
		Position referencePos = movable.getReferencePosition();
		Vec4 referencePoint = wwd.getModel().getGlobe().computePointFromPosition(referencePos);

		Vec4 surfaceNormal = wwd.getModel().getGlobe().computeSurfaceNormalAtLocation(referencePos.getLatitude(),
				referencePos.getLongitude());
		Line verticalRay = new Line(referencePoint, surfaceNormal);
		Line screenRay = wwd.getView().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());
		Line previousScreenRay = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

		// Vec4 pointOnLine = AirspaceEditorUtil.nearestPointOnLine(verticalRay,
		// screenRay);
		// Vec4 previousPointOnLine =
		// AirspaceEditorUtil.nearestPointOnLine(verticalRay,
		// previousScreenRay);
		Vec4 pointOnLine = nearestPointOnLine(verticalRay, screenRay);
		Vec4 previousPointOnLine = nearestPointOnLine(verticalRay, previousScreenRay);

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
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			@SuppressWarnings("unchecked")
			ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
			if (list != null) {
				// list.add(newLocation);
				// m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
				// same list
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
	protected void assembleControlPoints(GLWorld w) {

		Feature f = FeatureList.theFeatures.getSelected(LLHAreaFeature.LLHAreaGroup);
		if ((f == null) || (!f.getVisible())) {
			return;
		}
		LLHAreaFeature l = (LLHAreaFeature) (f);
		LLHArea area = l.getLLHArea();

		if (area == null) {
			return;
		}

		List<LLHAreaControlPoint> list = area.getControlPoints(w);
		addControlPoints(list);
	}

	// **************************************************************//
	// ******************** Control Point Events ******************//
	// **************************************************************//
	protected LLHAreaControlPoint doAddControlPoint(WorldWindow wwd, LLHArea airspace, Point mousePoint, boolean shiftDown) {

		// Ray trace eye to sphere...get a Vec4
		// FIXME: ray trace to earth surface should be built into GLWorld....
		Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
		// double surfaceElevation = AirspaceEditorUtil.surfaceElevationAt(wwd,
		// ray);
		// Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd,
		// surfaceElevation, ray);
		double surfaceElevation = surfaceElevationAt(wwd, ray);
		Vec4 newPoint = intersectGlobeAt(wwd, surfaceElevation, ray);
		if (newPoint == null) {
			return null;
		}
		V3 np = new V3(newPoint.x, newPoint.y, newPoint.z);

		// Project that Vec3 to 2D...pin altitudes?
		Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);
		double[] altitudes = new double[2];
		altitudes[LOWER_ALTITUDE] = newPosition.getElevation();
		altitudes[UPPER_ALTITUDE] = newPosition.getElevation() + DEFAULT_POLYGON_HEIGHT;
		pinAltitudes(altitudes);
		this.getAirspace().setAltitudes(altitudes[LOWER_ALTITUDE], altitudes[UPPER_ALTITUDE]);

		LLHAreaControlPoint controlPoint = null;
		LLHArea area = this.getAirspace();
		if (area instanceof LLHAreaSet) {

			LLHAreaSet set = (LLHAreaSet) (area);

			// New control point from the location
			LLD_X newLLD = new LLD_X(newPosition.latitude.degrees, newPosition.longitude.degrees);
			PointAddCommand c = new PointAddCommand(set, newLLD, -1, shiftDown);
			CommandManager.getInstance().executeCommand(c, true);

			int aIndex = c.getFinalIndex();
			controlPoint = new LLHAreaControlPoint(aIndex, np, newLLD);

		}

		return controlPoint;
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
				PointRemoveCommand c = new PointRemoveCommand(set, index, false);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}

	// Fire point select command
	public void doSelectControlPoint(LLHAreaControlPoint controlPoint) {
		if (controlPoint != null) {
			int index = controlPoint.getLocationIndex();
			LLHArea area = this.getAirspace();
			if (area instanceof LLHAreaSet) {
				LLHAreaSet set = (LLHAreaSet) (area);
				PointSelectCommand c = new PointSelectCommand(set, index);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
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

	/**
	 * Move a single point
	 */
	public void doMoveControlPoint(WorldWindow wwd, LLHAreaControlPoint controlPoint, Point mousePoint) {

		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null) {
			return;
		}

		// if (this.getAirspace() != controlPoint.getLLHArea()) {
		// return;
		// }

		// Move the point to the ray intersection. Don't use previous/next
		// because other things can move the
		// worldwind window (such as scroll wheel)..the user expects the point
		// to be where their mouse is.
		Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

		// Use elevation of surface, or existing elevation of point....
		// FIXME: rewrite this without worldwind right?
		V3 p = controlPoint.getPoint();
		Vec4 vw = new Vec4(p.x, p.y, p.z);
		Position controlPointPos = wwd.getModel().getGlobe().computePositionFromPoint(vw);
		double elevation = controlPointPos.getElevation();
		// double elevation = AirspaceEditorUtil.surfaceElevationAt(wwd, ray);

		// Vec4 newPoint = AirspaceEditorUtil.intersectGlobeAt(wwd, elevation,
		// ray);
		Vec4 newPoint = intersectGlobeAt(wwd, elevation, ray);

		if (newPoint == null) {
			return;
		}

		Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);

		int index = controlPoint.getLocationIndex();
		LLHArea area = this.getAirspace();

		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			@SuppressWarnings("unchecked")
			ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
			if (list != null) {

				// Copy the location info at least...
				LLD_X oldOne = list.get(index);
				LLD_X newOne = new LLD_X(newPosition.latitude.degrees, newPosition.longitude.degrees, oldOne);
				list.set(index, newOne);

				FeatureMemento fm = (FeatureMemento) (m); // Check it
				FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}
}
