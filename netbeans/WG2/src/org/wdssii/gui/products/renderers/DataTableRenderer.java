package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.geom.Location;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.ColorMap;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.renderers.icons.BaseIconAnnotation;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolRenderer;
import org.wdssii.util.GLUtil;
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Renders a DataTable in a worldwind window
 *
 * Lots of mess here, will need cleanup, redesign, etc....just trying to get it
 * to work at moment...
 *
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private ArrayList<BaseIconAnnotation> myIcons = new ArrayList<BaseIconAnnotation>();
    private static Logger log = LoggerFactory.getLogger(DataTableRenderer.class);
    //public static BasicAnnotationRenderer myRenderer = new BasicAnnotationRenderer();
    private ColorMap myTextColorMap = null;
    private ColorMap myPolygonColorMap = null;
    // Not sure this should be here....
    private PickSupport pickSupport = new PickSupport();
    // FIXME: really need non-static...
    public static BaseIconAnnotation hovered = null;
    /**
     * Store world points for a DataTable
     */
    private ArrayList<Vec4> myWorldPoints = null;
    private ArrayList<SymbolRenderer> myRenderers = null;
    private SymbolRenderer myRenderer = null;
    private static boolean refreshRenderers = true;
    
    private static Symbol hackme = new StarSymbol();
    public DataTableRenderer() {
        super(true);                 
    }

    public static void setHackMe(Symbol s){
        refreshRenderers = true;
        hackme = s;
    }
    
    public static Symbol getHackMe(){
        return hackme;
    }
    
    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {

        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        ProductDataInfo info = ProductManager.getInstance().getProductDataInfo(aProduct.getDataType());

        // Factory
        //  IconSetConfig tag = info.getIconSetConfig();

        // HACK.  Make one if it's missing..
        // if (tag == null){
        //     tag = new IconSetConfig();
        //    // tag.polygonTextConfig = new Tag_PolygonTextConfig();
        // }
        // Check for mesonet icons...
        //  MesonetIconFactory.create(monitor, aDataTable, tag.mesonetConfig, myIcons);
        // Check for polygon icons...
        //  PolygonIconFactory.create(monitor, aDataTable, tag.polygonTextConfig, myIcons);
        //  IconFactory.create(monitor, aDataTable, myIcons);


        AnimateManager.updateDuringRender();
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /**
     * Experimental readout using drawing to get it..lol FIXME: generalize this
     * ability for all products
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
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean pickMode, java.awt.Point pickPoint) {
        if (pickMode) {
            return;
        }
        Product p = getProduct();
        if (p == null) {
            return;
        }
        DataTable aDataTable = (DataTable) p.getRawDataType();

        // Sync issues?  Can table be added to while we are doing this?
        // think currently it's fully loaded.  Might be nice to be able to display
        // partial icons as large files load
        //PolygonSymbol s = new PolygonSymbol();
        //StarSymbol s = new StarSymbol();
        if (myRenderer == null) {
            ImageSymbol s = new ImageSymbol();

            SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(s);
            if (renderer == null) {
                return;
            }
            renderer.setSymbol(s);
            myRenderer = renderer;
        }

        View v = dc.getView();
        Globe g = dc.getGlobe();
        ArrayList<Location> locations = aDataTable.getLocations();

        // hack to try category off of mping...
        Column symbolColumn = aDataTable.getColumnByName("Type_id");
        TreeMap<String, SymbolRenderer> myCategoryRenderer = new TreeMap<String, SymbolRenderer>();
        // We'd set up from xml, creating a symbol renderer per class
        // "hail" --> StarSymbolRenderer...

        // Project each row in table to a world point
        // Warning: If you change project (round to flat, etc.. need to 
        // recalculate)

        if ((myWorldPoints == null) || refreshRenderers){
            myWorldPoints = new ArrayList<Vec4>();
            // Only needed if renderer can change per point (such as category)
            myRenderers = new ArrayList<SymbolRenderer>();
            int row = 0;
            for (Location l : locations) {
                Vec4 worldPoint = g.computePointFromPosition(
                        Angle.fromDegrees(l.getLatitude()),
                        Angle.fromDegrees(l.getLongitude()),
                        l.getHeightKms() * 1000.0);
                myWorldPoints.add(worldPoint);
                // Lookup renderer for point...
                SymbolRenderer rr = null;

                // Hacking in mping categories for proof of concept, will
                // be moving to xml and editor over time...
                if (symbolColumn != null) {
                    String text = symbolColumn.getValue(row);
                    if (text.equalsIgnoreCase("ds")) {   // Snow
                        rr = SymbolFactory.getSymbolRenderer(hackme);
                        rr.setSymbol(hackme);
                    } else if (text.equalsIgnoreCase("ws")) {  // wet snow
                        StarSymbol newOne = new StarSymbol();
                        newOne.color = Color.BLUE;
                        newOne.ocolor = Color.BLACK;
                        newOne.osize = 3;
                        rr = SymbolFactory.getSymbolRenderer(newOne);
                        rr.setSymbol(newOne);
                        newOne.toAsterisk();
                        newOne.pointsize = 20;
                    } else if (text.equalsIgnoreCase("dz")) { // Drizzle
                        PolygonSymbol newOne = new PolygonSymbol();
                        newOne.color = Color.GREEN;
                        newOne.ocolor = Color.BLACK;
                        rr = SymbolFactory.getSymbolRenderer(newOne);
                        rr.setSymbol(newOne);
                        newOne.toCircle();
                        newOne.pointsize = 20;
                    }

                }
                if (rr == null) {
                    rr = myRenderer; // Fall back renderer
                }
                //myRenderers.add(rr);
                myRenderers.add(rr);
                // log.debug("ADDED RENDERER "+row+" "+rr);
                // SymbolRenderer getback = myRenderers.get(row);
                // log.debug("Compare "+rr+" --"+getback);
                row++;
            }
            // sync?
            refreshRenderers = false;
        }

        // FIXME: add a pass check for LOD maybe....skipping for now
        GL gl = dc.getGL();
        GLUtil.pushOrtho2D(dc);
        gl.glDisable(GL.GL_DEPTH_TEST);
        int row = 0;
        for (Vec4 at3D : myWorldPoints) {
            // Project 3D world coordinates to 2D view (whenever eye changes)
            // Could maybe cache these...check for view changing somehow...
            // FIXME: Possible speed up here
            Vec4 at2D = v.project(at3D);

            // log.debug("translate "+at2D.x, ", "+at2D.y);
            gl.glTranslated(at2D.x, at2D.y, 0);
            SymbolRenderer item = myRenderers.get(row);
            item.render(gl);
            gl.glTranslated(-at2D.x, -at2D.y, 0);
            row++;

        }
        GLUtil.popOrtho2D(dc);
        /*  if (dc == null) {
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
         * */
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false, null);
    }

    /**
     * Pick an object in the current dc at point
     */
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
