package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Point;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Vector;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.Contour;
import org.wdssii.datatypes.Contours;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.Location;
import org.wdssii.geom.V3;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.Product;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DfloatAsNodes;

/**
 *  Contours renderer draws a Contours datatype
 *  (alpha) Lots to do on this....
 * 
 * @author Robert Toomey
 */
public class ContoursRenderer extends ProductRenderer {

    /** We use vector (vector is synchronized for opengl thread and worker thread) */
    // FIXME: Replace with GrowList
    private Vector<Integer> myOffsets;
    
    /** points for the contour line strips */
    protected Array1D<Float> verts;
    /** Corresponding colors */
    protected Array1D<Float> colors;
    /** Colors as readout information */
    protected Array1D<Float> readout;
    
    public ContoursRenderer(){
        super(true);
    }
    
     /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(GLWorld w, FeatureMemento m) {
        super.draw(w, m);
        drawData(w, false);
    }
    
    @Override
    public WdssiiJobStatus createForDatatype(GLWorld w, Product aProduct, WdssiiJobMonitor monitor) {
        
        
        try {

            // Make sure and always start monitor
            Contours aContourSet = (Contours) aProduct.getRawDataType();
            monitor.beginTask("ContoursRenderer:", aContourSet.getNumberOfContours());
            //int size = aContourSet.getNumberOfContours();
            //Globe myGlobe = dc.getGlobe();
            
            // COUNT pass.  We should stick this in the Contour object,
            // it can add stuff up as it loads.
            
            // By here the contours are loaded already, so no sync issues
            ArrayList<Contour> set = aContourSet.getContours();
            int numOfLocations = 0;
            for(Contour c: set){   
                ArrayList<Location> l = c.getLocations();
                if (l != null){
                    numOfLocations += l.size();  // 3 floats per location
                }
            }
            //int vertCount = numOfLocations;
            verts = new Array1DfloatAsNodes(numOfLocations*3, 0.0f);
            colors = new Array1DfloatAsNodes(numOfLocations, 0.0f);
            readout = new Array1DfloatAsNodes(numOfLocations, 0.0f);
            myOffsets = new Vector<Integer>();
            // colors = new Array1DfloatAsNodes(ccounter / 4, 0.0f); // use one 'float' per color...
            int idx = 0;
            int idy = 0;
            int idREAD = 0;
            ProductManager pm = ProductManager.getInstance();
            for(Contour c: set){   
                myOffsets.add(idx);  // For this contour....
                ArrayList<Location> l = c.getLocations();
                if (l != null){
                   // Get the location and store a vertex....
                   // and put 'white' for color for now...
                    for(Location loc:l){
                    //Vec4 point = myGlobe.computePointFromPosition(
                    //                Angle.fromDegrees(loc.getLatitude()),
                    //                Angle.fromDegrees(loc.getLongitude()),
                    //                loc.getHeightKms() * 1000);
                    V3 point = w.projectLLH(loc.latDegrees(), loc.lonDegrees(), loc.getHeightKms() * 1000.0);
                    readout.set(idREAD++, 6.0f);
                  //  idy = out.putUnsignedBytes(colors, idy);

                    verts.set(idx++, (float) point.x);
                    verts.set(idx++, (float) point.y);
                    verts.set(idx++, (float) point.z);
                    
                    String colorName = c.getAttribute("Color"); 
                    Color aColor =  pm.getNamedColor(colorName); 
                    idy = ColorMapFloatOutput.putUnsignedBytes(colors, idy, (short)(aColor.getRed()), (short)(aColor.getGreen()), (short)(aColor.getBlue()), (short)(aColor.getAlpha()));
                  // idy = ColorMapFloatOutput.putUnsignedBytes(colors, idy, (short)(255), (short)(0), (short)(255), (short)(255));
                  //  colors.set(idy++, (float) 1.0);
                    //colors.set(idy++, (float)1.0);
                    //colors.set(idy++, (float)1.0);
                   // colors.set(idy++, (float)1.0);
                    }
                }
                AnimateManager.updateDuringRender();
            }
            // Add last offset if we had at least one...
            if (myOffsets.size() > 0){
                myOffsets.add(idx);
            }
            setIsCreated();
        }catch(Exception e){
            
        }
        return WdssiiJobStatus.OK_STATUS;
    }
  
        /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(GLWorld w, boolean readoutMode) {
        
        if (isCreated() && (verts != null)) {
            final GL glold = w.gl;
        	final GL2 gl = glold.getGL().getGL2();

            boolean attribsPushed = false;
            try {
                Object lock1 = verts.getBufferLock();
                //Object lock2 = colors.getBufferLock();
                Object lock2 = readoutMode ? readout.getBufferLock() : colors.getBufferLock();
                synchronized (lock1) {
                    synchronized (lock2) {

                        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL2.GL_LIGHTING_BIT
                                | GL.GL_COLOR_BUFFER_BIT
                                | GL2.GL_ENABLE_BIT
                                | GL2.GL_TEXTURE_BIT | GL2.GL_TRANSFORM_BIT
                                | GL2.GL_VIEWPORT_BIT | GL2.GL_CURRENT_BIT);

                        gl.glDisable(GL2.GL_LIGHTING);
                        gl.glDisable(GL.GL_TEXTURE_2D);

                        if (readoutMode) {
                            gl.glDisable(GL.GL_DEPTH_TEST);

                        } else {
                          //  gl.glEnable(GL.GL_DEPTH_TEST);
                            // Contours should always draw over right?
                            // This doesn't work..need to make it an overlay...
                            
                            gl.glDisable(GL.GL_DEPTH_TEST);
                        }
                        gl.glLineWidth(2.0f);
                        gl.glShadeModel(GL2.GL_FLAT);
                        gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT
                                | GL2.GL_CLIENT_PIXEL_STORE_BIT);
                        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
                        attribsPushed = true;
//gl.glEnable(GL.GL_BLEND);
//gl.glBlendFunc(GL.GL_SRC_ALPHA)
                        FloatBuffer z = verts.getRawBuffer();
                        //FloatBuffer c = colors.getRawBuffer();
                        FloatBuffer c = readoutMode ? readout.getRawBuffer() : colors.getRawBuffer();

                        // Only render if there is data to render
                        if (z.capacity() > 0) {
                            gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());
                            gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());
                            int size = myOffsets.size();
                            if (size > 1) {
                                
                                // Draw each 'offset' which should be one 
                                // per warning strip...
                              //  gl.glColor4i(size, size, size, size);
                                for (int i = 0; i < size - 1; i++) {
                                    int start_index = myOffsets.get(i);
                                    int end_index = myOffsets.get(i + 1);
                                    int run_indices = end_index - start_index;
                                    int start_vertex = start_index / 3;
                                    int run_vertices = run_indices / 3;
                                    gl.glDrawArrays(GL.GL_LINE_STRIP, start_vertex,
                                            run_vertices);
                                }
                            }

                        }
                    }
                }
            } finally {
                if (attribsPushed) {
                    gl.glPopClientAttrib();
                    gl.glPopAttrib();
                }
            }
        }
    }

    @Override
    public boolean canOverlayOtherData() {
       return true;
    }

    @Override
    public void preRender(GLWorld w, FeatureMemento m) {
    }
    @Override
    public void pick(GLWorld w, Point p, FeatureMemento m) {
    }
}
