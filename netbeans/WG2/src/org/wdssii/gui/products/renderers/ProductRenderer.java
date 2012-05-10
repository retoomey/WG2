package org.wdssii.gui.products.renderers;

import java.awt.Point;
import javax.media.opengl.GL;

import org.wdssii.datatypes.Table2DView.LocationType;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GridVisibleArea;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Rectangle;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;

/** ProductRenderer is a helper class of Product.  It draws the DataType in the 3D world
 * 
 * @author Robert Toomey
 *
 */
public abstract class ProductRenderer {

    /** The product we render */
    private Product myProduct = null;

    /** Do we render inside a background job? */
    private boolean myAsBackgroundJob = false;
    
    /** The worker job if we are threading */
    private backgroundRender myWorker = null;
    
    /** Volatile because renderer job creates the data in one thread, but drawn in another.
     * myCreated set to true by worker thread after the buffers are created (draw allowed)
     */
    private volatile boolean myCreated = false;
    
    public synchronized boolean isCreated(){ return myCreated; }
    
    public synchronized void setIsCreated(){ myCreated = true; }
    
    /** Job for creating in the background any rendering */
    public class backgroundRender extends WdssiiJob {

        public DrawContext dc;
        public Product aProduct;

        public backgroundRender(String jobName, DrawContext aDc, Product rec) {
            super(jobName);
            dc = aDc;
            aProduct = rec;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return createForDatatype(dc, aProduct, monitor);
        }
    }
        
    public ProductRenderer(boolean asBackgroundJob){
        myAsBackgroundJob = asBackgroundJob;
    }
    
    // Get the color map for this product (FIXME: more general product info
    // instead)
    // public ColorMap getColorMap();
    // Create anything needed to draw this product in the current dc
    public void initToProduct(DrawContext dc, Product aProduct) {
        myProduct = aProduct;
     
        // FIXME: handle background flag?
        if (myWorker == null) {
             myWorker = new backgroundRender("Job", dc, aProduct);
             myWorker.schedule();
        }
    }

    /** Draw the product in the current dc */
    public abstract void draw(DrawContext dc);

    /** Pick an object in the current dc at point */
    public void doPick(DrawContext dc, java.awt.Point pickPoint){
    
    }
    
    /** Return the product we draw */
    public Product getProduct() {
        return myProduct;
    }

    /** Get the product readout for a given screenpoint in the drawcontext */
    public ProductReadout getProductReadout(Point p, Rectangle view, DrawContext dc) {
        return new ProductReadout();
    }

    public void highlightObject(Object o){
        
    }

    public void drawReadoutCellOutline(DrawContext dc, ProductReadout pr) {
        // Subclasses should outline the data given in the ProductReadout
    }

    /** Based on a table grid visible area, draw the corresponding outline in the 3D world window */
    public void drawGridOutline(DrawContext dc, GridVisibleArea a) {
        Product aProduct = getProduct();
        if ((aProduct != null) && (a != null)) {

            // Synchronization lazy data check
            aProduct.updateDataTypeIfLoaded();
            DataType dt = aProduct.getRawDataType();
            if (dt == null) {
                return;
            }  // Data not loaded yet...
            if (!(dt instanceof Table2DView)) {
                return;
            }  // Not a table

            Globe myGlobe = dc.getGlobe();
            GL gl = dc.getGL();

            gl.glPushAttrib(GL.GL_LINE_BIT);
            gl.glLineWidth(3);
            gl.glColor4d(1d, 1d, 1d, 1d);
            Location location;
            Vec4 p;
            int counter = 0;
            location = new Location(1.0, 1.0, 1.0);

            // Product2DTable table = aProduct.get2DTable();
            Table2DView table = (Table2DView) (dt);
            int lastColumn = a.lastFullColumn;
            int lastRow = a.lastFullRow;
            int startColumn = a.startCol;
            int startRow = a.startRow;

            // Check integrity or we can crash the video driver lol
            boolean goodToDraw = ((lastRow - startRow >= 0)
                    && (lastColumn - startColumn >= 0));

            if (goodToDraw) {

                // Outline the last column that's partly visible in table.  Make this the full column to
                // only show cells that are fully drawn.
                int r = startRow;
                int c = startColumn;
                int dfaState = 0;
                int lastState = 3;
                boolean validPoint = false;
                // Using some old compiler theory to keep from having four separate loops for each side
                // and a lot of redundant code. Use a DFA to loop around the data
                gl.glLineWidth(5.0f);
                gl.glBegin(GL.GL_LINE_LOOP);

                while (dfaState <= lastState) {

                    // Get point for the bzscan outline
                    switch (dfaState) { // Going clockwise around the data
                        case 0: // top, marching left until last column to draw
                            validPoint = table.getLocation(LocationType.TOP_LEFT, r, c, location);
                            c++;
                            if (c > lastColumn) {
                                dfaState++;
                                c--;
                            }
                            break;
                        case 1: // right side, increasing row, keeping column the same
                            validPoint = table.getLocation(LocationType.TOP_RIGHT, r, c, location);
                            r++;
                            if (r > lastRow) {
                                dfaState++;
                                r--;
                            }
                            break;
                        case 2: // bottom side, decreasing column, keeping row the same
                            validPoint = table.getLocation(LocationType.BOTTOM_RIGHT, r, c, location);
                            c--;
                            if (c < startColumn) {
                                dfaState++;
                                c++;
                            }
                            break;
                        case 3: // left side, decreasing row, keeping column the same
                            validPoint = table.getLocation(LocationType.BOTTOM_LEFT, r, c, location);
                            r--;
                            if (r < startRow) {
                                dfaState++;
                                r++;
                            }
                            break;
                        default:
                            dfaState = lastState;
                            break;  // Should be unreachable
                    }
                    // Post vertex from last run of DFA (if point was valid)
                    if (validPoint) {
                        p = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(location.getLatitude()),
                                Angle.fromDegrees(location.getLongitude()),
                                location.getHeightKms() * 1000);
                        gl.glVertex3d(p.x, p.y, p.z);
                        validPoint = false;
                        counter++;
                        if (counter > 1000) {
                            System.out.println("Limiting readout boundary to " + counter + " points");
                            break;
                        }
                    }
                }

                gl.glEnd();
            }
            gl.glPopAttrib();
        }
    }

    /** Do the work of generating the OpenGL stuff */
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
        return WdssiiJobStatus.CANCEL_STATUS;
    }
    
    /**
     * 
     * @return true if this product can overlay other data for the same source
     * 
     */
    public abstract boolean canOverlayOtherData();
}
