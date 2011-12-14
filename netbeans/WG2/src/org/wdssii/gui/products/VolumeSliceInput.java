package org.wdssii.gui.products;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

/** VolumeSliceInput is a holder class for the unique layout in 3D space of a vertical or cappi slice.
 * VolumeProducts use this to create visual sliced representations of their data.
 * 
 * FIXME: we're coupled to worldwind right now for projection...should subclass this for input in the worldwind part.
 * Not sure the compute location should even BE here..
 * 
 * @author Robert Toomey
 *
 */
public class VolumeSliceInput {

    public VolumeSliceInput(int r, int c, double slat, double slon, double elat, double elon, double bh, double th,
            Globe aGlobe, int uniqueID) {
        rows = r;
        cols = c;
        startLat = slat;
        startLon = slon;
        endLat = elat;
        endLon = elon;
        bottomHeight = bh;
        topHeight = th;
        gb = aGlobe;
        iterationCount = uniqueID;  // do we stil need this?
    }

    public VolumeSliceInput(VolumeSliceInput o) {
        rows = o.rows;
        cols = o.cols;
        startLat = o.startLat;
        startLon = o.startLon;
        endLat = o.endLat;
        endLon = o.endLon;
        bottomHeight = o.bottomHeight;
        topHeight = o.topHeight;
        gb = o.gb;
        iterationCount = o.iterationCount;
    }
    public int rows;
    public int cols;
    public double startLat;
    public double startLon;
    public double endLat;
    public double endLon;
    public double bottomHeight;
    public double topHeight;
    public int iterationCount;
    public Globe gb;

    /** Compute the x,y,z actual point in the current projection system */
    public Vec4 computePoint(Globe globe, Angle latitude, Angle longitude, double elevation,
            boolean terrainConformant) {
        //   double newElevation = elevation;

        //   if (terrainConformant)
        //   {
        //        newElevation += this.computeElevationAt(dc, latitude, longitude);
        //   }

        return gb.computePointFromPosition(latitude, longitude, elevation);
    }
}
