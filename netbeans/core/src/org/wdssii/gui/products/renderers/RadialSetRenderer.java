package org.wdssii.gui.products.renderers;

import java.awt.Point;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.media.opengl.GL;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.RadialSet.RadialSetQuery;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.util.RadialUtil;

import com.sun.opengl.util.BufferUtil;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;
import org.wdssii.gui.products.RadialSetReadout;

/** Renders a RadialSet
 * 
 * @author Robert Toomey
 *
 */
public class RadialSetRenderer extends ProductRenderer {

    private static Log log = LogFactory.getLog(RadialSetRenderer.class);
    /** We use vector (vector is synchronized for opengl thread and worker thread) */
    private Vector<Integer> myOffsets;
    /** Verts for the RadialSet */
    protected Array1Dfloat verts;
    /** Cooresponding colors */
    protected Array1Dfloat colors;
    /** Colors as readout information */
    protected Array1Dfloat readout;

    protected int updateCounter = 0;

    public RadialSetRenderer(){
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
        //long start = System.currentTimeMillis();
        int counter = 0;
        int ccounter = 0;

        try {

            // Make sure and always start monitor
            RadialSet aRadialSet = (RadialSet) aProduct.getRawDataType();
            monitor.beginTask("RadialSetRenderer:", aRadialSet.getNumRadials());

            //	long end1 = System.currentTimeMillis() - start;
            //	float seconds1 = (float) ((end1 * 1.0) / 1000.0);
            // System.out.println("RADIAL SET BUILDER SECONDS " + seconds1);
            //myProduct = aProduct;
            //	ColorMap aColorMap = aProduct.getColorMap();
            FilterList aList = aProduct.getFilterList();

            // if (aColorMap == null){
            // System.out.println("Missing color map, generating based on values");
            // }
            // / Try to create the opengl stuff for a radial set...
            int numRadials = aRadialSet.getNumRadials();

            // System.out.println("There are " + numRadials + " Radials");

            // if number_of_radials == 0 error.... FIXME

            // Get the location of the radar and the elevation of this
            // radial
            // set
            Location radarLoc = aRadialSet.getRadarLocation();
            // Precompute the sin/cos of the elevation angle of the radar.
            final float RAD = 0.017453293f;
            double sinElevAngle = aRadialSet.getElevationSin();
            double cosElevAngle = aRadialSet.getElevationCos();

            float firstGateKms = aRadialSet.getRangeToFirstGateKms();

            // "Counter" loop. Faster to calculate than reallocate memory,
            // Radial sets have missing parts no way to quick count it that
            // I can see. We also find the maximum number of gates and
            // create
            // a attenuation height cache

            int maxGateCount = 0;
            for (int i = 0; i < numRadials; i++) {

                // Get each radial from center out to end
                Radial aRadial = aRadialSet.getRadial(i);

                // If missing, just continue on
                int numGates = aRadial.getNumGates();

                if (numGates == 0) {
                    continue;
                }

                boolean needNewStrip = true;

                //float[] values = aRadial.getValues();
                Array1Dfloat values = aRadial.getValues();

                if (maxGateCount < numGates) {
                    maxGateCount = numGates;
                }

                for (int j = 0; j < numGates; j++) {
                    //double value = values[j];
                    float value = values.get(j);
                    // value = -10+( ((float)(j)/(float)(numGates))
                    // *(99+10));
                    // if (value < -90000) { // needs to be missing value
                    // FIXME
						/*
                     * if (value == DataType.MissingData){ value = 20; }
                     */
                    if (value == DataType.MissingData) {
                        needNewStrip = true;
                    } else {
                        if (needNewStrip) {
                            counter += 6; // 2 * 3
                            ccounter += 8; // 2*4
                        }
                        counter += 6;
                        ccounter += 8;
                        needNewStrip = false;
                    }
                }
            }
            // --------------End counter loop

            // System.out.println("Counted gates, total of " + counter);
            Globe myGlobe = dc.getGlobe(); // FIXME: says may be null???

            // The opengl thread can draw anytime..
            verts = new Array1DfloatAsNodes(counter, 0.0f);   // FIXME: could 'combine' both into one array I think...
            colors = new Array1DfloatAsNodes(ccounter / 4, 0.0f); // use one 'float' per color...

            // READOUT
            readout = new Array1DfloatAsNodes(ccounter / 4, 0.0f);  // use one 'float' per color...

            myOffsets = new Vector<Integer>();
            // colors.rewind(); // do I need this?

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:
            setIsCreated();
            int idx = 0;
            int idy = 0;
            int idREAD = 0;

            // The four locations of the 'box' of a gate, do the expensive
            // 'new' call
            // outside the loop
            Location gate = new Location(0, 0, 0);
            Location gate1 = new Location(0, 0, 0);
            Location gate2 = new Location(0, 0, 0);
            Location gate3 = new Location(0, 0, 0);
            //double[] c = new double[4];
            ColorMapFloatOutput out = new ColorMapFloatOutput();

            // --------------------------------------------------------
            // On first radial, create the attenuation cache...
            // FIXME: volume/etc just use straight vector calculation, so
            // why bother to attenuate anyway?
            // This is a hideous thing created because otherwise superres
            // brings us to a crawl
            double[] heights;
            double[] gcdSinCache;
            double[] gcdCosCache;
            // System.out.println("Begin height cache....");
            if (numRadials > 0) {
                heights = new double[maxGateCount + 1];
                // gcdCache = new double[maxGateCount+1];
                gcdSinCache = new double[maxGateCount + 1];
                gcdCosCache = new double[maxGateCount + 1];
                Radial aRadial = aRadialSet.getRadial(0);
                double rangeMeters = firstGateKms * 1000.0;
                double gateWidthMeters = aRadial.getGateWidthKms() * 1000.0;
                //	System.out.println("Gate width meters is "+gateWidthMeters);
                for (int i = 0; i <= maxGateCount; i++) {
                    heights[i] = RadialUtil.getAzRanElHeight(rangeMeters,
                            sinElevAngle);
                    double gcd = RadialUtil.getGCD(rangeMeters,
                            cosElevAngle, heights[i]);
                    gcdSinCache[i] = RadialUtil.getGCDSin(gcd);
                    gcdCosCache[i] = RadialUtil.getGCDCos(gcd);
                    rangeMeters += gateWidthMeters;
                }
            } else {
                heights = null; // avoid error
                gcdSinCache = null;
                gcdCosCache = null;
            }
            RadialSetQuery rq = new RadialSetQuery();

            // System.out.println("end height cache....");
            for (int i = 0; i < numRadials; i++) {

                monitor.subTask("Radial " + i + "/" + numRadials);
                monitor.worked(1);   // Do it first to ensure it's called

                //log.info("counter "+i+"/"+numRadials);
                // Get each radial from center out to end
                Radial aRadial = aRadialSet.getRadial(i);

                // If missing, just continue on
                int numGates = aRadial.getNumGates();
                if (numGates == 0) {
                    continue;
                }

                // Reset range to starting gate
                float startRAD = aRadial.getStartAzimuthDegs() * RAD;
                float endRAD = aRadial.getEndAzimuthDegs() * RAD;
                double sinStartRAD = Math.sin(startRAD);
                double cosStartRAD = Math.cos(startRAD);
                double sinEndRAD = Math.sin(endRAD);
                double cosEndRAD = Math.cos(endRAD);

                float rangeKms = firstGateKms;
                float gateWidthKms = aRadial.getGateWidthKms();
                RadialUtil.getAzRan1(gate, radarLoc, sinStartRAD,
                        cosStartRAD, rangeKms, sinElevAngle, cosElevAngle,
                        heights[0], gcdSinCache[0], gcdCosCache[0]);
                RadialUtil.getAzRan1(gate1, radarLoc, sinEndRAD, cosEndRAD,
                        rangeKms, sinElevAngle, cosElevAngle, heights[0],
                        gcdSinCache[0], gcdCosCache[0]);
                boolean needNewStrip = true;

                // We could create each 'radial' in a thread...drawing could
                // draw 'up to' current count...
                //float[] values = aRadial.getValues();
                Array1Dfloat values = aRadial.getValues();
                for (int j = 0; j < numGates; j++) {

                    rangeKms += gateWidthKms;
                    // rangeKms += gateWidthKms;
                    RadialUtil.getAzRan1(gate2, radarLoc, sinEndRAD,
                            cosEndRAD, rangeKms, sinElevAngle,
                            cosElevAngle, heights[j + 1],
                            gcdSinCache[j + 1], gcdCosCache[j + 1]);
                    RadialUtil.getAzRan1(gate3, radarLoc, sinStartRAD,
                            cosStartRAD, rangeKms, sinElevAngle,
                            cosElevAngle, heights[j + 1],
                            gcdSinCache[j + 1], gcdCosCache[j + 1]);

                    // Do the stuff creating the gate in opengl
                    //double value = values[j];
                    float value = values.get(j);

                    /*
                     * if (value == DataType.MissingData){ value = 20; }
                     */
                    //aColorMap.fillColor(out, value);

                    // FIXME: need more data.  Also a way to make sure all data is THERE...

                    rq.inDataValue = value;
                    rq.outDataValue = value;
                    aList.fillColor(out, rq, false);
                    boolean updateOffsets = false;
                    int updateIndex = 0;

                    if (value == DataType.MissingData) {
                        needNewStrip = true;
                    } else {
                        if (needNewStrip) {

                            // Don't add to offsets yet, data isn't there
                            // and the opengl thread will be
                            // using offsets for this.
                            updateIndex = idx;
                            updateOffsets = true;

                            Vec4 point = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(gate.getLatitude()),
                                    Angle.fromDegrees(gate.getLongitude()),
                                    gate.getHeightKms() * 1000);
// READOUT EXPERIMENT
//out.putUnsignedBytes(readout, idREAD++);
                            readout.set(idREAD++, value);
                            idy = out.putUnsignedBytes(colors, idy);

                            verts.set(idx++, (float) point.x);
                            verts.set(idx++, (float) point.y);
                            verts.set(idx++, (float) point.z);

                            Vec4 point1 = myGlobe.computePointFromPosition(Angle.fromDegrees(gate1.getLatitude()), Angle.fromDegrees(gate1.getLongitude()), gate1.getHeightKms() * 1000);

// READOUT EXPERIMENT
//out.putUnsignedBytes(readout, idREAD++);
                            readout.set(idREAD++, value);

                            idy = out.putUnsignedBytes(colors, idy);

                            verts.set(idx++, (float) point1.x);
                            verts.set(idx++, (float) point1.y);
                            verts.set(idx++, (float) point1.z);

                        }

                        // Push back last two vertices of quad
                        Vec4 point3 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(gate3.getLatitude()),
                                Angle.fromDegrees(gate3.getLongitude()),
                                gate2.getHeightKms() * 1000);

// READOUT EXPERIMENT
//out.putUnsignedBytes(readout, idREAD++);
                        readout.set(idREAD++, value);

                        idy = out.putUnsignedBytes(colors, idy);

                        verts.set(idx++, (float) point3.x);
                        verts.set(idx++, (float) point3.y);
                        verts.set(idx++, (float) point3.z);

                        Vec4 point2 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(gate2.getLatitude()),
                                Angle.fromDegrees(gate2.getLongitude()),
                                gate3.getHeightKms() * 1000);

// READOUT EXPERIMENT
//out.putUnsignedBytes(readout, idREAD++);
                        readout.set(idREAD++, value);

                        idy = out.putUnsignedBytes(colors, idy);

                        verts.set(idx++, (float) point2.x);
                        verts.set(idx++, (float) point2.y);
                        verts.set(idx++, (float) point2.z);
                        needNewStrip = false;
                    }

                    if (updateOffsets) {
                        // We add LAST so the other thread uses the vert/color data <= current offsets.
                        myOffsets.add(updateIndex);
                        updateOffsets = false;
                    }
                    gate = gate3;
                    gate1 = gate2;
                }

                updateCounter++;
                if (updateCounter > 1) {
                    CommandManager.getInstance().updateDuringRender();  // These queue up anyway 
                    //Thread.sleep(50);
                    updateCounter = 0;
                }

            }

            monitor.done();
        } catch (Exception e) {
            log.error("3D gen error" + e.toString());
            if (monitor != null) {
                monitor.done();
            }
            return WdssiiJobStatus.CANCEL_STATUS; // We should make this a 'cleaner' exception/catch FIXME
        }
        //long end = System.currentTimeMillis() - start;
        //	float seconds = (float) ((end * 1.0) / 1000.0);
        // System.out.println("RADIAL SET SECONDS " + seconds + " for "
        // + counter);

        // System.out.println("********Ending radial set creation");
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

        // Ok we're gonna do a 'readout' of the color here....
        // Eventually either in a seperate gl context, or scissor
        // the area to the current product...?

        // This is the BIGGEST opengl hack...but it should work.
        // What we do is draw the readout values as colors within a scissor box around
        // the cursor.  
        RadialSetReadout out = new RadialSetReadout();
        if (p != null) {
            GL gl = dc.getGL();
            ByteBuffer data = BufferUtil.newByteBuffer(4);
            int y = dc.getDrawableHeight() - p.y - 1;
            int boxWidth = 1;
            int xbox = p.x - (boxWidth / 2);
            if (xbox < 0) {
                xbox = 0;
            }
            int ybox = y - (boxWidth / 2);
            if (ybox < 0) {
                ybox = 0;
            }
            gl.glScissor(xbox, ybox, boxWidth, boxWidth);
            gl.glEnable(GL.GL_SCISSOR_TEST);
            gl.glClearColor(0, 0, 0, 0);  // FIXME pop?
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            drawData(dc, true);
            gl.glDisable(GL.GL_SCISSOR_TEST);
            gl.glReadPixels(p.x, y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data);

            byte d0 = data.get(0);
            byte d1 = data.get(1);
            byte d2 = data.get(2);
            byte d3 = data.get(3);

            Product prod = getProduct();
            String units = "";
            if (prod != null) {
                units = prod.getCurrentUnits();
            }
            out.setUnits(units);

            if ((d0 == 0) && (d1 == 0) && (d2 == 0) && (d3 == 0)) {
                //out = "N/A";
                //out.setValue(-1);
            } else {
                // byte type is SIGNED, we really just want the hex digits
                int b0 = (0x000000FF & data.get(0));
                int b1 = (0x000000FF & data.get(1));
                int b2 = (0x000000FF & data.get(2));
                int b3 = (0x000000FF & data.get(3));
                int v1 = (b3 << 24);
                int v2 = (b2 << 16);
                int v3 = (b1 << 8);
                int v4 = (b0);
                int finalBits = v1 + v2 + v3 + v4;
                float readoutValue = Float.intBitsToFloat(finalBits);
                out.setValue(readoutValue);
            }
        } else {
            //out.setValue(readoutValue);
            //out = "No readout for renderer";
        }
        return out;
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        if (isCreated() && (verts != null) && (colors != null)) {
            GL gl = dc.getGL();

            boolean attribsPushed = false;
            try {
                Object lock1 = verts.getBufferLock();
                //Object lock2 = colors.getBufferLock();
                Object lock2 = readoutMode ? readout.getBufferLock() : colors.getBufferLock();
                synchronized (lock1) {
                    synchronized (lock2) {

                        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                                | GL.GL_COLOR_BUFFER_BIT
                                | GL.GL_ENABLE_BIT
                                | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                                | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT);

                        gl.glDisable(GL.GL_LIGHTING);
                        gl.glDisable(GL.GL_TEXTURE_2D);

                        if (readoutMode) {
                            gl.glDisable(GL.GL_DEPTH_TEST);

                        } else {
                            gl.glEnable(GL.GL_DEPTH_TEST);
                        }
                        gl.glShadeModel(GL.GL_FLAT);
                        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                                | GL.GL_CLIENT_PIXEL_STORE_BIT);
                        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
                        attribsPushed = true;
//gl.glEnable(GL.GL_BLEND);
//gl.glBlendFunc(GL.GL_SRC_ALPHA)
                        FloatBuffer z = verts.getRawBuffer();
                        //FloatBuffer c = colors.getRawBuffer();
                        FloatBuffer c = readoutMode ? readout.getRawBuffer() : colors.getRawBuffer();

                        // Only render if there is data to render
                        if ((z!= null)&& (z.capacity() > 0)) {
                            gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());

                            // Isn't this color kinda wasteful really?  We have 4 floats per color,
                            // or 4 bytes * 4 = 16 bytes, when GL only stores 4 bytes per color lol
                            // We should use GL.GL_BYTE and convert ourselves to it, will save memory...

                            gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());
                            //gl.glColorPointer(4, GL.GL_FLOAT, 0, c.rewind());

                            int size = myOffsets.size();
                            if (size > 1) {
                                for (int i = 0; i < size - 1; i++) {
                                    int start_index = myOffsets.get(i);
                                    int end_index = myOffsets.get(i + 1);
                                    int run_indices = end_index - start_index;
                                    int start_vertex = start_index / 3;
                                    int run_vertices = run_indices / 3;
                                    gl.glDrawArrays(GL.GL_QUAD_STRIP, start_vertex,
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

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }
}
