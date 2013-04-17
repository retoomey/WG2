package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonHeightGrid;
import org.wdssii.datatypes.LatLonHeightGrid.LatLonHeightGridQuery;
import org.wdssii.geom.Location;
import org.wdssii.gui.AnimateManager;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.products.ColorMapFloatOutput;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.Array3D;
import org.wdssii.storage.GrowList;

/**
 * Render a CAPPI layer of a LatLonHeightGrid. Since a LatLonHeightGrid is a
 * cube of data, the LatLonHeightProduct creates multiple of these.
 *
 * @author Robert Toomey
 */
public class LatLonHeightGridRenderer extends ProductRenderer {

    private final static Logger LOG = LoggerFactory.getLogger(LatLonHeightGridRenderer.class);
    protected int updateCounter = 0;
    
    protected QuadStripRenderer myQuadRenderer = new QuadStripRenderer();
    
    public LatLonHeightGridRenderer() {
        super(true);
    }
    
    public LatLonHeightGridRenderer(boolean asBackgroundJob) {
        super(asBackgroundJob);
    }

    public void allocateMemory(LatLonHeightGrid aLLHG) {
        // "Counter" loop. Faster to calculate than reallocate memory,
        int counter = 0;
        int ccounter = 0;

        // Not sure I should be directly accessing this here, but
        // for now we'll deal with it...
        // data here should be fully loaded by now...
        int rendererIndex = 0;
        int numLats = aLLHG.getNumLats();
        int numLons = aLLHG.getNumLons();
        Array3D<Float> data = aLLHG.getData();
        boolean startQuadStrip;
        for (int y = 0; y < numLats; y++) {
            int lastJWithData = -2;
            for (int x = 0; x < numLons; x++) {
                float value = data.get(rendererIndex, y, x);
                if (value == DataType.MissingData) {
                    // This new way we don't have to calculate anything
                    // with missing data.  Much better for long bursts of
                    // missing...
                } else {

                    // Calculate the two points closest 'bottom' to the radar center
                    // if last written then we have this cached from the 
                    // old two 'top' points...
                    if (lastJWithData == (x - 1)) {
                        startQuadStrip = false;
                    } else {
                        startQuadStrip = true;
                    }
                    lastJWithData = x;
                    if (startQuadStrip) {
                        counter += 6; // 2 * 3
                        ccounter += 8; // 2*4
                    }
                    counter += 6;
                    ccounter += 8;
                }
            }
        }

        // --------------End counter loop

        myQuadRenderer.allocate(counter, ccounter);
    }

    @Override
    public WdssiiJob.WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJob.WdssiiJobMonitor monitor) {
        //long start = System.currentTimeMillis();

        try {
            // Make sure and always start monitor
            LatLonHeightGrid aLLHG = (LatLonHeightGrid) aProduct.getRawDataType();
            monitor.beginTask("LatLonHeightGridRenderer", aLLHG.getNumHeights());
            Globe myGlobe = dc.getGlobe(); // FIXME: says may be null???
            FilterList aList = aProduct.getFilterList();

            // Bleh, still not happy with this....
            allocateMemory(aLLHG);
            myQuadRenderer.begin();
            Array1DOpenGL verts = myQuadRenderer.getVerts();
            Array1DOpenGL colors = myQuadRenderer.getColors();
            Array1DOpenGL readout = myQuadRenderer.getReadout();
            GrowList<Integer> myOffsets = myQuadRenderer.getOffsets();

            // Once buffers exist and myOffsets exists, we 'turn on' the drawing thread:
            myQuadRenderer.setCanDraw(true);
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

            LatLonHeightGridQuery lq = new LatLonHeightGridQuery();
            int numLats = aLLHG.getNumLats();
            int numLons = aLLHG.getNumLons();

            Vec4 point, point1, point2 = null, point3 = null;
            boolean startQuadStrip;
            int updateIndex = 0;

            // Not sure I should be directly accessing this here, but
            // for now we'll deal with it...
            // data here should be fully loaded by now...
            int rendererIndex = 0;
            // Bleh different per rendererIndex right?
            float height = (float) aLLHG.getLocation().getHeightKms();


            Array3D<Float> data = aLLHG.getData();
            // Northwest corner...
            Location origin = aLLHG.getLocation();
            float startLat = (float) origin.getLatitude();
            float startLon = (float) origin.getLongitude();
            float latDelta = aLLHG.getLatResDegrees();
            float lonDelta = aLLHG.getLonResDegrees();

            float curLat;
            float curLon;

            // FIXME: Create an Array2Dfloat wrapper?
            curLat = startLat;
            for (int y = 0; y < numLats; y++) {
                monitor.subTask("Row " + y + "/" + numLats);
                monitor.worked(1);   // Do it first to ensure it's called

                // Move alone a single 'row' of grid..increasing lon..
                curLon = startLon;
                int lastJWithData = -2;
                for (int x = 0; x < numLons; x++) {
                    float value = data.get(rendererIndex, y, x);
                    if (value == DataType.MissingData) {
                        // This new way we don't have to calculate anything
                        // with missing data.  Much better for long bursts of
                        // missing...
                    } else {

                        // Calculate the two points closest 'bottom' to the radar center
                        // if last written then we have this cached from the 
                        // old two 'top' points...
                        if (lastJWithData == (x - 1)) {
                            // The previous 'top' is our bottom,
                            // we don't need a new strip in this case...
                            loc = loc3;
                            loc2 = loc4;
                            point = point3;
                            point1 = point2;
                            startQuadStrip = false;
                        } else {
                            // Calculate the closet points to 'left' and 'bottom'
                            loc.init(curLat, curLon, height);
                            loc2.init(curLat - latDelta, curLon, height);
                            point = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc.getLatitude()),
                                    Angle.fromDegrees(loc.getLongitude()),
                                    loc.getHeightKms() * 1000);
                            point1 = myGlobe.computePointFromPosition(
                                    Angle.fromDegrees(loc2.getLatitude()),
                                    Angle.fromDegrees(loc2.getLongitude()),
                                    loc2.getHeightKms() * 1000);
                            startQuadStrip = true;
                        }
                        // Calculate the furthest two points 'right' of the quad  
                        loc3.init(curLat - latDelta, curLon + lonDelta, height);
                        loc2.init(curLat, curLon + lonDelta, height);
                        lastJWithData = x;

                        // Filler data value...
                        lq.inDataValue = value;
                        lq.outDataValue = value;
                        aList.fillColor(out, lq, false);

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
                    curLat += latDelta;
                    // Update during render call...
                    updateCounter++;
                    if (updateCounter > 200) {
                        AnimateManager.updateDuringRender();  // These queue up anyway 
                        //Thread.sleep(50);
                        updateCounter = 0;
                    }

                }
                curLon += lonDelta;
            }

        } catch (Exception e) {
            LOG.error("3D gen error" + e.toString());
            return WdssiiJob.WdssiiJobStatus.CANCEL_STATUS; // We should make this a 'cleaner' exception/catch FIXME
        }
        //long end = System.currentTimeMillis() - start;
        //	float seconds = (float) ((end * 1.0) / 1000.0);
        // System.out.println("RADIAL SET SECONDS " + seconds + " for "
        // + counter);

        myQuadRenderer.end();

        // System.out.println("********Ending radial set creation");
        AnimateManager.updateDuringRender();

        setIsCreated();
        return WdssiiJob.WdssiiJobStatus.OK_STATUS;
    }

    @Override
    public void draw(DrawContext dc) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean canOverlayOtherData() {
        return true;
    }
}
