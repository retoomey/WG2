package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.RHIRadialSet;
import org.wdssii.datatypes.RHIRadialSet.RHIRadialSetQuery;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialATHeightGateCache;
import org.wdssii.datatypes.RadialUtil;
import org.wdssii.geom.Location;
import org.wdssii.gui.AnimateManager;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.products.*;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.GrowList;

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
            
            // Bleh, still not happy with this....
            allocateMemory(aRadialSet);
            myQuadRenderer.begin();
            Array1DOpenGL verts = myQuadRenderer.getVerts();
            Array1DOpenGL colors = myQuadRenderer.getColors();
            Array1DOpenGL readout = myQuadRenderer.getReadout();
            GrowList<Integer> myOffsets = myQuadRenderer.getOffsets();
            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:

            // colors.rewind(); // do I need this?

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:
            myQuadRenderer.setCanDraw(true);
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

                            readout.set(idREAD++, value);
                            idy = out.putUnsignedBytes(colors, idy);

                            verts.set(idx++, (float) point.x);
                            verts.set(idx++, (float) point.y);
                            verts.set(idx++, (float) point.z);

                            Vec4 point1 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(gate1.getLatitude()),
                                    Angle.fromDegrees(gate1.getLongitude()),
                                    gate1.getHeightKms() * 1000);

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

                        readout.set(idREAD++, value);

                        idy = out.putUnsignedBytes(colors, idy);

                        verts.set(idx++, (float) point3.x);
                        verts.set(idx++, (float) point3.y);
                        verts.set(idx++, (float) point3.z);

                        Vec4 point2 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(gate2.getLatitude()),
                                Angle.fromDegrees(gate2.getLongitude()),
                                gate2.getHeightKms() * 1000);

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
                    AnimateManager.updateDuringRender();  // These queue up anyway 
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

        myQuadRenderer.end();

        // System.out.println("********Ending radial set creation");
        AnimateManager.updateDuringRender();  // Humm..different thread...

        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    @Override
    public boolean canOverlayOtherData() {
        return false;
    }
}
