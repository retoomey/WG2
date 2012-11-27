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
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.PPIRadialSet.PPIRadialSetQuery;
import org.wdssii.datatypes.Radial;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.products.*;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.util.RadialUtil;

/**
 * Renders a RadialSet
 *
 * @author Robert Toomey
 *
 */
public class PPIRadialSetRenderer extends RadialSetRenderer {

    private static Logger log = LoggerFactory.getLogger(PPIRadialSetRenderer.class);
    protected int updateCounter = 0;

    public PPIRadialSetRenderer() {
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
        //long start = System.currentTimeMillis();

        try {
            // Make sure and always start monitor
            PPIRadialSet aRadialSet = (PPIRadialSet) aProduct.getRawDataType();
            monitor.beginTask("RadialSetRenderer", aRadialSet.getNumRadials());
            Globe myGlobe = dc.getGlobe(); // FIXME: says may be null???
            FilterList aList = aProduct.getFilterList();
            final Location radarLoc = aRadialSet.getRadarLocation();
            final double sinElevAngle = aRadialSet.getFixedAngleSin();
            final double cosElevAngle = aRadialSet.getFixedAngleCos();
            final float firstGateKms = aRadialSet.getRangeToFirstGateKms();
            final int maxGateCount = aRadialSet.getNumGates();
            final int numRadials = aRadialSet.getNumRadials();

            allocateMemory(aRadialSet);
            verts.begin();
            colors.begin();
            readout.begin();

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:
            setIsCreated();

            int idx = 0;
            int idy = 0;
            int idREAD = 0;

            // The four locations of the quad of the data cell
            Location loc = new Location(0, 0, 0);
            Location loc2 = new Location(0, 0, 0);
            Location loc3 = new Location(0, 0, 0);
            Location loc4 = new Location(0, 0, 0);
            ColorMapFloatOutput out = new ColorMapFloatOutput();

            // --------------------------------------------------------
            // On first radial, create the attenuation cache...
            // FIXME: volume/etc just use straight vector calculation, so
            // why bother to attenuate anyway?
            // This is a hideous thing created because otherwise superres
            // brings us to a crawl
            Radial firstRadial = (numRadials > 0) ? aRadialSet.getRadial(0) : null;
            RadialATHeightGateCache c = new RadialATHeightGateCache(
                    aRadialSet, firstRadial, maxGateCount, sinElevAngle, cosElevAngle);

            PPIRadialSetQuery rq = new PPIRadialSetQuery();
            Vec4 point, point1, point2 = null, point3 = null;
            boolean startQuadStrip;
            int updateIndex = 0;
            
            for (int i = 0; i < numRadials; i++) {
                monitor.subTask("Radial " + i + "/" + numRadials);
                monitor.worked(1);   // Do it first to ensure it's called

                // If missing, just continue on
                Radial r = aRadialSet.getRadial(i);
                int numGates = r.getNumGates();
                if (numGates == 0) {
                    continue;
                }

                // Radial set only calculation
                float startAzimuthRAD = r.getStartRadians();
                float endAzimuthRAD = r.getEndRadians();
                double sinStartAzRAD = Math.sin(startAzimuthRAD);
                double cosStartAzRAD = Math.cos(startAzimuthRAD);
                double sinEndAzRAD = Math.sin(endAzimuthRAD);
                double cosEndAzRAD = Math.cos(endAzimuthRAD);
                float gateWidthKms = r.getGateWidthKms();

                // Reset range to starting gate
                float rangeKms = firstGateKms;
                int lastJWithData = -2;           
                Array1Dfloat values = r.getValues();
                for (int j = 0; j < numGates; j++) {
                    float value = values.get(j);
                    if (value == DataType.MissingData) {
                        // This new way we don't have to calculate anything
                        // with missing data.  Much better for long bursts of
                        // missing...
                    } else {

                        // Calculate the two points closest 'bottom' to the radar center
                        // if last written then we have this cached from the 
                        // old two 'top' points...
                        if (lastJWithData == (j - 1)) {
                            // The previous 'top' is our bottom,
                            // we don't need a new strip in this case...
                            loc = loc3;
                            loc2 = loc4;
                            point = point3;
                            point1 = point2;
                            startQuadStrip = false;
                        } else {
                            // Calculate the closet points to radar center, the bottom
                            // of the quadstrip.
                            RadialUtil.getAzRan1(loc, radarLoc, sinStartAzRAD,
                                    cosStartAzRAD, rangeKms, sinElevAngle, cosElevAngle,
                                    c.heights[j], c.gcdSinCache[j], c.gcdCosCache[j]);
                            point = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc.getLatitude()),
                                    Angle.fromDegrees(loc.getLongitude()),
                                    loc.getHeightKms() * 1000);
                            RadialUtil.getAzRan1(loc2, radarLoc, sinEndAzRAD, cosEndAzRAD,
                                    rangeKms, sinElevAngle, cosElevAngle, c.heights[j],
                                    c.gcdSinCache[j], c.gcdCosCache[j]);
                            point1 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc2.getLatitude()),
                                    Angle.fromDegrees(loc2.getLongitude()),
                                    loc2.getHeightKms() * 1000);
                            startQuadStrip = true;
                        }
                        // Calculate the furthest two points 'top' of the quad
                        // from the radar center.                     
                        float endRangeKms = rangeKms + gateWidthKms;
                        RadialUtil.getAzRan1(loc3, radarLoc, sinEndAzRAD,
                                cosEndAzRAD, endRangeKms, sinElevAngle,
                                cosElevAngle, c.heights[j + 1],
                                c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);
                        RadialUtil.getAzRan1(loc4, radarLoc, sinStartAzRAD,
                                cosStartAzRAD, endRangeKms, sinElevAngle,
                                cosElevAngle, c.heights[j + 1],
                                c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);
                        lastJWithData = j;

                        // Filler data value...
                        rq.inDataValue = value;
                        rq.outDataValue = value;
                        aList.fillColor(out, rq, false);

                        if (startQuadStrip) {
                            // Then we have to write the new bottom values...
                            updateIndex = idx;

                            readout.set(idREAD++, value);
                            idy = out.putUnsignedBytes(colors, idy);
                            verts.set(idx++, (float) point.x);
                            verts.set(idx++, (float) point.y);
                            verts.set(idx++, (float) point.z);

                            readout.set(idREAD++, value);
                            idy = out.putUnsignedBytes(colors, idy);
                            verts.set(idx++, (float) point1.x);
                            verts.set(idx++, (float) point1.y);
                            verts.set(idx++, (float) point1.z);
                        }

                        // Always write the 'top' of the strip
                        // Push back last two vertices of quad
                        point3 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc4.getLatitude()),
                                Angle.fromDegrees(loc4.getLongitude()),
                                loc4.getHeightKms() * 1000);
                        point2 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc3.getLatitude()),
                                Angle.fromDegrees(loc3.getLongitude()),
                                loc3.getHeightKms() * 1000);

                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);
                        verts.set(idx++, (float) point3.x);
                        verts.set(idx++, (float) point3.y);
                        verts.set(idx++, (float) point3.z);
                        
                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);
                        verts.set(idx++, (float) point2.x);
                        verts.set(idx++, (float) point2.y);
                        verts.set(idx++, (float) point2.z);

                        // Update the offsets last...
                        if (startQuadStrip) {
                            myOffsets.add(updateIndex);
                        }
                    }
                    rangeKms += gateWidthKms;

                    // Update during render call...
                    updateCounter++;
                    if (updateCounter > 200) {
                        CommandManager.getInstance().updateDuringRender();  // These queue up anyway 
                        //Thread.sleep(50);
                        updateCounter = 0;
                    }

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
        CommandManager.getInstance().updateDuringRender();

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
