package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.OGLStackHandler;
import java.awt.Color;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.renderers.icons.BaseIconAnnotation;
import org.wdssii.gui.products.renderers.icons.MesonetIcon.MesonetIconFactory;
import org.wdssii.gui.products.renderers.icons.PolygonIcon.PolygonIconFactory;
import org.wdssii.xml.iconSetConfig.IconSetConfig;

/** Renders a DataTable in a worldwind window
 * 
 * Lots of mess here, will need cleanup, redesign, etc....just trying to get
 * it to work at moment...
 * 
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private ArrayList<BaseIconAnnotation> myIcons = new ArrayList<BaseIconAnnotation>();
    private static Logger log = LoggerFactory.getLogger(DataTableRenderer.class);
    public static BasicAnnotationRenderer myRenderer = new BasicAnnotationRenderer();
    private ColorMap myTextColorMap = null;
    private ColorMap myPolygonColorMap = null;
    // Not sure this should be here....
    private PickSupport pickSupport = new PickSupport();
    // FIXME: really need non-static...
    public static BaseIconAnnotation hovered = null;

    public DataTableRenderer() {
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {

        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        ProductDataInfo info = ProductManager.getInstance().getProductDataInfo(aProduct.getDataType());

        // Factory
        IconSetConfig tag = info.getIconSetConfig();
        
        // HACK.  Make one if it's missing..
        if (tag == null){
            tag = new IconSetConfig();
           // tag.polygonTextConfig = new Tag_PolygonTextConfig();
        }
        // Check for mesonet icons...
        MesonetIconFactory.create(monitor, aDataTable, tag.mesonetConfig, myIcons);
        // Check for polygon icons...
        PolygonIconFactory.create(monitor, aDataTable, tag.polygonTextConfig, myIcons);

        CommandManager.getInstance().updateDuringRender();  // Humm..different thread...
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /** Experimental readout using drawing to get it..lol 
     * FIXME: generalize this ability for all products
     */
    /*
    @Override
    public ProductReadout getProductReadout(Point p, Rectangle view, DrawContext dc) {
        final Object o = hovered;
        ProductReadout out = new ProductReadout() {

            @Override
            public String getReadoutString() {
                if ((o != null) && (o instanceof BaseIconAnnotation)) {
                    BaseIconAnnotation a = (BaseIconAnnotation) (o);
                    return a.getReadoutString();
                }
                return "N/A";
            }
        };
        return out;
    }
*/
    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean pickMode, java.awt.Point pickPoint) {
        if (dc == null) {
            return;
        }

        if (dc.getVisibleSector() == null) {
            return;
        }
        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null) {
            return;
        }

        if (myIcons == null) {
            return;
        }

        GL gl = dc.getGL();

        // For our icons we have two render passes.  One is for any 3D component
        // of the icon..the 2nd is the 2D component which overlays any 3D...
        if (pickMode) {
            this.pickSupport.clearPickList();
            this.pickSupport.beginPicking(dc);

            // Save current color...
            // float[] inColor = new float[4];	
            // gl.glGetFloatv(GL.GL_CURRENT_COLOR, inColor, 0);

        }

        // 3D Pass
        int size = myIcons.size();
        for (int i = 0; i < size; i++) {
            BaseIconAnnotation aIcon = myIcons.get(i);
            // aIcon.renderNow(dc);
            aIcon.do3DDraw(dc);
            //aIcon.draw(dc, i, i, i, Position.ZERO)
        }
        // 2D Pass 
        int attributeMask = GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, blend func
                | GL.GL_CURRENT_BIT // for current color
                | GL.GL_DEPTH_BUFFER_BIT // for depth test, depth mask, depth func
                | GL.GL_ENABLE_BIT // for enable/disable changes
                | GL.GL_HINT_BIT // for line smoothing hint
                | GL.GL_LINE_BIT // for line width, line stipple
                | GL.GL_TEXTURE_BIT // for texture env
                | GL.GL_TRANSFORM_BIT // for matrix mode
                | GL.GL_VIEWPORT_BIT; // for viewport, depth range

        // Wow never knew of this..this object is awesome
        OGLStackHandler stackHandler = new OGLStackHandler();
        stackHandler.pushAttrib(gl, attributeMask);

        // Load a parallel projection with dimensions (viewportWidth, viewportHeight)
        stackHandler.pushProjectionIdentity(gl);

        gl.glOrtho(0d, dc.getView().getViewport().width, 0d, dc.getView().getViewport().height, -1d, 1d);
        // Push identity matrices on the texture and modelview matrix stacks. Leave the matrix mode as modelview.

        stackHandler.pushTextureIdentity(gl);
        stackHandler.pushModelviewIdentity(gl);

        // Enable the alpha test.
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);

        for (int i = 0; i < size; i++) {
            Annotation aIcon = myIcons.get(i);
            Color color = Color.WHITE;
            if (pickMode) {
                color = dc.getUniquePickColor();
                dc.getGL().glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
            }
            aIcon.render(dc);
            if (pickMode) {
                this.pickSupport.addPickableObject(color.getRGB(), aIcon,
                        ((Locatable) aIcon).getPosition(), false);
            }
        }
        stackHandler.pop(gl);
        if (pickMode) {
            // Adds picked object to the DrawContext..
            this.pickSupport.resolvePick(dc, pickPoint, dc.getCurrentLayer());
            this.pickSupport.endPicking(dc);
        }
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false, null);
    }

    /** Pick an object in the current dc at point */
    @Override
    public void doPick(DrawContext dc, java.awt.Point pickPoint) {
        drawData(dc, true, pickPoint);
    }

    @Override
    public void highlightObject(Object o) {
        hovered = null;
        if ((o != null) && (o instanceof BaseIconAnnotation)) {
            // Humm..need to make sure this is OUR icon
            BaseIconAnnotation b = (BaseIconAnnotation) (o);
            hovered = b;
	    FeatureList.theFeatures.repaintViews();
        }
    }

    @Override
    public boolean canOverlayOtherData() {
        return true;
    }
}
