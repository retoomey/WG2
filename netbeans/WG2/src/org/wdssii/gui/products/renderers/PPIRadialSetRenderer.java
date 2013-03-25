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
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.PPIRadialSet.PPIRadialSetQuery;
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

            // Bleh, still not happy with this....
            allocateMemory(aRadialSet);
            myQuadRenderer.begin();
            myQuadRenderer.setBatched(false);

            Array1DOpenGL verts = myQuadRenderer.getVerts();
            Array1DOpenGL colors = myQuadRenderer.getColors();
            Array1DOpenGL readout = myQuadRenderer.getReadout();
            GrowList<Integer> myOffsets = myQuadRenderer.getOffsets();

            // For now at least, pull fields from renderer..

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:
            myQuadRenderer.setCanDraw(true);
            setIsCreated();

            int idx = 0;
            int idy = 0;
            int idREAD = 0;

            // The four locations of the quad of the data cell
            Location loc0 = new Location(0, 0, 0);
            Location loc1 = new Location(0, 0, 0);
            Location loc2 = new Location(0, 0, 0);
            Location loc3 = new Location(0, 0, 0);
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
            Vec4 point0, point1, point2 = null, point3 = null;
            boolean startQuadStrip;
            int updateIndex = 0;
            float[] point01 = new float[6];
            float[] point23 = new float[6];
            float[] temp;
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
                Array1D<Float> values = r.getValues();
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
                            // Strip:
                            // v0  v2  v4  v6
                            // v1  v3  v5  v7
                            loc0 = loc2;
                            loc1 = loc3;
                            temp = point01;
                            point01 = point23;
                            point23 = temp;
                            startQuadStrip = false;
                        } else {
                            // Calculate the closet points to radar center, the bottom
                            // of the quadstrip.
                            RadialUtil.getAzRan1(loc0, radarLoc, sinEndAzRAD, cosEndAzRAD,
                                    rangeKms, sinElevAngle, cosElevAngle, c.heights[j],
                                    c.gcdSinCache[j], c.gcdCosCache[j]);
                            point0 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc0.getLatitude()),
                                    Angle.fromDegrees(loc0.getLongitude()),
                                    loc0.getHeightKms() * 1000);
                            point01[0] = (float) point0.x;
                            point01[1] = (float) point0.y;
                            point01[2] = (float) point0.z;
                            RadialUtil.getAzRan1(loc1, radarLoc, sinStartAzRAD,
                                    cosStartAzRAD, rangeKms, sinElevAngle, cosElevAngle,
                                    c.heights[j], c.gcdSinCache[j], c.gcdCosCache[j]);
                            point1 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc1.getLatitude()),
                                    Angle.fromDegrees(loc1.getLongitude()),
                                    loc1.getHeightKms() * 1000);
                            point01[3] = (float) point1.x;
                            point01[4] = (float) point1.y;
                            point01[5] = (float) point1.z;
                            startQuadStrip = true;
                        }
                        // Calculate the furthest two points 'top' of the quad
                        // from the radar center.                     
                        float endRangeKms = rangeKms + gateWidthKms;
                        RadialUtil.getAzRan1(loc2, radarLoc, sinEndAzRAD,
                                cosEndAzRAD, endRangeKms, sinElevAngle,
                                cosElevAngle, c.heights[j + 1],
                                c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);
                        RadialUtil.getAzRan1(loc3, radarLoc, sinStartAzRAD,
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
                            readout.set(idREAD++, value);
                            idy = out.putUnsignedBytes(colors, idy);

                            idx = verts.set(idx, point01);
                        }

                        // Always write the 'top' of the strip
                        // Push back last two vertices of quad
                        point2 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc2.getLatitude()),
                                Angle.fromDegrees(loc2.getLongitude()),
                                loc2.getHeightKms() * 1000);
                        point23[0] = (float) point2.x;
                        point23[1] = (float) point2.y;
                        point23[2] = (float) point2.z;
                        point3 = myGlobe.computePointFromPosition(
                                Angle.fromDegrees(loc3.getLatitude()),
                                Angle.fromDegrees(loc3.getLongitude()),
                                loc3.getHeightKms() * 1000);
                        point23[3] = (float) point3.x;
                        point23[4] = (float) point3.y;
                        point23[5] = (float) point3.z;

                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);
                        readout.set(idREAD++, value);
                        idy = out.putUnsignedBytes(colors, idy);

                        idx = verts.set(idx, point23);

                        // Update the offsets last...
                        if (startQuadStrip) {
                            myOffsets.add(updateIndex);
                        }
                    }
                    rangeKms += gateWidthKms;

                    // Update during render call...
                    updateCounter++;
                    if (updateCounter > 200) {
                        AnimateManager.updateDuringRender();  // These queue up anyway 
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

        myQuadRenderer.end();

        // System.out.println("********Ending radial set creation");
        AnimateManager.updateDuringRender();

        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    @Override
    public boolean canOverlayOtherData() {
        return false;
    }
}
