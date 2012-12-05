package org.wdssii.gui.products.renderers;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Iterator;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.RHIRadialSet;
import org.wdssii.datatypes.RHIRadialSet.RHIRadialSetQuery;
import org.wdssii.datatypes.Radial;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.products.*;
import org.wdssii.storage.Array1D;
import org.wdssii.util.RadialUtil;

/**
 * Renders a RHIRadialSet
 *
 * @author Robert Toomey
 *
 */
public class RHIRadialSetRenderer extends RadialSetRenderer {

    private static Logger log = LoggerFactory.getLogger(RHIRadialSetRenderer.class);
    protected int updateCounter = 0;

    public RHIRadialSetRenderer() {
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
        //long start = System.currentTimeMillis();
        int counter = 0;
        int ccounter = 0;

        try {

            // Make sure and always start monitor
            RHIRadialSet aRadialSet = (RHIRadialSet) aProduct.getRawDataType();
            monitor.beginTask("RadialSetRenderer", aRadialSet.getNumRadials());
            Globe myGlobe = dc.getGlobe();
            FilterList aList = aProduct.getFilterList();
            final Location radarLoc = aRadialSet.getRadarLocation();
            final float firstGateKms = aRadialSet.getRangeToFirstGateKms();
            final int maxGateCount = aRadialSet.getNumGates();
            final int numRadials = aRadialSet.getNumRadials();
            allocateMemory(aRadialSet);
            verts.begin();
            colors.begin();
            readout.begin();

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:

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

            RHIRadialSetQuery rq = new RHIRadialSetQuery();

            // Azimuth is set for all radials...
            final float startRAD = aRadialSet.getFixedAngleRads();
            final float endRAD = aRadialSet.getFixedAngleRads();
            final double sinStartRAD = Math.sin(startRAD);
            final double cosStartRAD = Math.cos(startRAD);
            final double sinEndRAD = Math.sin(endRAD);
            final double cosEndRAD = Math.cos(endRAD);

            // System.out.println("end height cache....");
            for (int i = 0; i < numRadials; i++) {

                monitor.subTask("Radial " + i + "/" + numRadials);
                monitor.worked(1);   // Do it first to ensure it's called

                //log.info("counter "+i+"/"+numRadials);
                // Get each radial from center out to end
                Radial aRadial = aRadialSet.getRadial(i);
                // Need heights for THIS radial....
                final double sinElevAngle = Math.sin(aRadial.getStartRadians()); // or do we want mid?
                final double cosElevAngle = Math.cos(aRadial.getStartRadians());
                final double sinEndElevAngle = Math.sin(aRadial.getEndRadians()); // or do we want mid?
                final double cosEndElevAngle = Math.cos(aRadial.getEndRadians());
                RadialATHeightGateCache c = new RadialATHeightGateCache(aRadialSet, aRadial, maxGateCount, sinElevAngle, cosElevAngle);
                RadialATHeightGateCache d = new RadialATHeightGateCache(aRadialSet, aRadial, maxGateCount, sinEndElevAngle, cosEndElevAngle);
                // If missing, just continue on
                int numGates = aRadial.getNumGates();
                if (numGates == 0) {
                    continue;
                }

                float rangeKms = firstGateKms;
                float gateWidthKms = aRadial.getGateWidthKms();
                RadialUtil.getAzRan1(gate, radarLoc, sinStartRAD,
                        cosStartRAD, rangeKms, sinElevAngle, cosElevAngle,
                        c.heights[0], c.gcdSinCache[0], c.gcdCosCache[0]);
                RadialUtil.getAzRan1(gate1, radarLoc, sinEndRAD, cosEndRAD,
                        rangeKms, sinEndElevAngle, cosEndElevAngle, d.heights[0],
                        d.gcdSinCache[0], d.gcdCosCache[0]);
                boolean needNewStrip = true;

                // We could create each 'radial' in a thread...drawing could
                // draw 'up to' current count...
                //float[] values = aRadial.getValues();
                Array1D<Float> values = aRadial.getValues();
                for (int j = 0; j < numGates; j++) {

                    rangeKms += gateWidthKms;
                    // rangeKms += gateWidthKms;
                    RadialUtil.getAzRan1(gate2, radarLoc, sinEndRAD,
                            cosEndRAD, rangeKms, sinElevAngle,
                            cosElevAngle, c.heights[j + 1],
                            c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);
                    RadialUtil.getAzRan1(gate3, radarLoc, sinStartRAD,
                            cosStartRAD, rangeKms, sinEndElevAngle,
                            cosEndElevAngle, d.heights[j + 1],
                            d.gcdSinCache[j + 1], d.gcdCosCache[j + 1]);

                    // Do the stuff creating the gate in opengl
                    //double value = values[j];
                    float value = values.get(j);

                    /*
                     *  if (value == DataType.MissingData){ value = 20; }
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

                            Vec4 point1 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(gate1.getLatitude()),
                                    Angle.fromDegrees(gate1.getLongitude()),
                                    gate1.getHeightKms() * 1000);

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
                                gate3.getHeightKms() * 1000);

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
                                gate2.getHeightKms() * 1000);

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
                if (updateCounter > 10) {
                    CommandManager.getInstance().updateDuringRender();  // These queue up anyway 
                    //Thread.sleep(50);
                    updateCounter = 0;
                }

            }

        } catch (Exception e) {
            log.error("3D gen error" + e.toString());
            return WdssiiJobStatus.CANCEL_STATUS; // We should make this a 'cleaner' exception/catch FIXME
        }
        //long end = System.currentTimeMillis() - start;
        //	float seconds = (float) ((end * 1.0) / 1000.0);
        // System.out.println("RADIAL SET SECONDS " + seconds + " for "
        // + counter);

        verts.end();
        colors.end();
        readout.end();

        // System.out.println("********Ending radial set creation");
        CommandManager.getInstance().updateDuringRender();  // Humm..different thread...

        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /**
     * Experimental readout using drawing to get it..lol FIXME: generalize this
     * ability for all products
     */
    @Override
    public ProductReadout getProductReadout(Point p, Rectangle view, DrawContext dc) {

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
            // The GLDrawable height isn't always the height of the VISIBLE
            // opengl window.  When using a lightweight widget it's usually
            // bigger.  Heavyweight you could just use the dc.getDrawableHeight
            int fullH = (int) (view.getHeight());
            int y = fullH - p.y - 1;  // Invert Y for openGL...

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
    public static boolean flipper = false;

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        if (isCreated() && (verts != null) && (colors != null)) {
            GL gl = dc.getGL();

            boolean attribsPushed = false;
            try {
                Object lock1 = verts.getBufferLock();
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
                        FloatBuffer z = verts.getRawBuffer();
                        FloatBuffer c = readoutMode ? readout.getRawBuffer() : colors.getRawBuffer();

                        // Only render if there is data to render
                        if ((z != null) && (z.capacity() > 0)) {
                            gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());

                            // Isn't this color kinda wasteful really?  We have 4 floats per color,
                            // or 4 bytes * 4 = 16 bytes, when GL only stores 4 bytes per color lol
                            // We should use GL.GL_BYTE and convert ourselves to it, will save memory...
                            gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());

                            int test;
                            flipper = !flipper;
                            if (flipper) {
                                test = GL.GL_QUAD_STRIP;
                            } else {
                                test = GL.GL_LINE_STRIP;
                            }

                            Iterator<Integer> itr = myOffsets.iterator();
                            if (itr.hasNext()) {
                                Integer now = itr.next();
                                while (itr.hasNext()) {
                                    Integer plus1 = itr.next();
                                    if (plus1 != null) {
                                        int start_index = now;
                                        int end_index = plus1;
                                        int run_indices = end_index - start_index;
                                        int start_vertex = start_index / 3;
                                        int run_vertices = run_indices / 3;
                                        gl.glDrawArrays(GL.GL_QUAD_STRIP, start_vertex,
                                                run_vertices);
                                        now = plus1;
                                    }
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
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }

    @Override
    public boolean canOverlayOtherData() {
        return false;
    }
}
