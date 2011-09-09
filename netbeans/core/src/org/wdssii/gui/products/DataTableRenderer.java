package org.wdssii.gui.products;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import java.awt.Point;

import gov.nasa.worldwind.render.DrawContext;

import gov.nasa.worldwind.render.FrameFactory;
import gov.nasa.worldwind.render.GlobeAnnotation;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openide.util.Exceptions;
import org.wdssii.core.W2Config;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.storage.Array1Dfloat;

import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.xml.Tag_iconSetConfig;

/** Renders a DataTable in a worldwind window
 * 
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private ArrayList<IconAnnotation> myIcons = new ArrayList<IconAnnotation>();
    
    private static Log log = LogFactory.getLog(DataTableRenderer.class);

    public DataTableRenderer(){
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
  
        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        
        // Ok for the moment get the icon configuration file here.
        // We might actually read this into the DataType before this point.
        // Probably should NOT do the xml here..
        File test = W2Config.getFile("/icons/MergerInputRadarsTable");
        Tag_iconSetConfig tag = new Tag_iconSetConfig();
        tag.processAsRoot(test);
        
        // Create an icon per row in table..using the icon configuration
        ArrayList<Location> loc = aDataTable.getLocations();
        int i = 1;
        for(Location l:loc){
            addIcon(l);
            monitor.subTask("Icon "+i++);
        }
        /*
        int counter = 10;
        for(int i=0; i< counter;i++){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
               // Exceptions.printStackTrace(ex);
            }
            monitor.subTask("Task "+i);
        }
        
        */
        CommandManager.getInstance().updateDuringRender();  // Humm..different thread...
        monitor.done();
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /** Experimental readout using drawing to get it..lol 
     * FIXME: generalize this ability for all products
     */
    @Override
    public ProductReadout getProductReadout(Point p, DrawContext dc) {
        RadialSetReadout out = new RadialSetReadout();
        return out;
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        for(IconAnnotation a:myIcons){
            a.render(dc);
        }
    }
    
    public void addIcon(Location loc){
        
        // try to add something....
        AnnotationAttributes eqAttributes;

        // Init default attributes for all eq
        eqAttributes = new AnnotationAttributes();
        eqAttributes.setLeader(AVKey.SHAPE_NONE);
        eqAttributes.setDrawOffset(new Point(0, -16));
        eqAttributes.setSize(new Dimension(32, 32));
        eqAttributes.setBorderWidth(5);
        eqAttributes.setCornerRadius(0);
        eqAttributes.setBackgroundColor(new Color(0, 0, 0, 0));
        // ea.getAttributes().setImageSource(eqIcons[Math.min(days, eqIcons.length - 1)]);
        //    ea.getAttributes().setTextColor(eqColors[Math.min(days, eqColors.length - 1)]);
        //    ea.getAttributes().setScale(earthquake.magnitude / 10);
       // eqAttributes.setScale(5);
        eqAttributes.setTextColor(new Color(255, 0, 0, 0));
        Position p = new Position(new LatLon(
                Angle.fromDegrees(loc.getLatitude()),
               Angle.fromDegrees(loc.getLongitude())), loc.getHeightKms());
        IconAnnotation ea = new IconAnnotation(p, eqAttributes);
       myIcons.add(ea);
      //  myProducts.addRenderable(ea);
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }
    
    
    /**
     * First pass use ww annotation object.  Since we have so many icons
     * we'll probably need to make it flyweight
     */
    private class IconAnnotation extends GlobeAnnotation {
        // public Earthquake earthquake;

        public Position position;

        public IconAnnotation(Position p, AnnotationAttributes defaults) {
            super("", p, defaults);
            this.position = p;
        }

        protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale) {
            double finalScale = scale * this.computeScale(dc);

            GL gl = dc.getGL();
            gl.glTranslated(x, y, 0);
            gl.glScaled(finalScale, finalScale, 1);
        }
        // Override annotation drawing for a simple circle
        private DoubleBuffer shapeBuffer;

        protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
            // Draw colored circle around screen point - use annotation's text color
            if (dc.isPickingMode()) {
                this.bindPickableObject(dc, pickPosition);
            }

          //  this.applyColor(dc, this.getAttributes().getTextColor(), 0.6 * opacity, true);
            this.applyColor(dc, new Color(255, 0, 0, 255), 1.0, true);
            
            // Draw 32x32 shape from its bottom left corner
            int size = 32;
            if (this.shapeBuffer == null) {
                this.shapeBuffer = FrameFactory.createShapeBuffer(AVKey.SHAPE_ELLIPSE, size, size, 0, null);
            }
            dc.getGL().glTranslated(-size / 2, -size / 2, 0);
            FrameFactory.drawBuffer(dc, GL.GL_TRIANGLE_FAN, this.shapeBuffer);
        }
    }
}

