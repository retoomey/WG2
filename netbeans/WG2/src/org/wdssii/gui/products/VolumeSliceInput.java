package org.wdssii.gui.products;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

/** VolumeSliceInput is a holder class for the unique layout in 3D space of a vertical or cappi slice.
 * VolumeProducts use this to create visual sliced representations of their data.
 * 
 * @author Robert Toomey
 *
 */
public class VolumeSliceInput {

    public VolumeSliceInput(int r, int c, double slat, double slon, double elat, double elon, double bh, double th) {
        rows = r;
        cols = c;
        startLat = slat;
        startLon = slon;
        endLat = elat;
        endLon = elon;
        bottomHeight = bh;
        topHeight = th;
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
    }
    public int rows;
    public int cols;
    public double startLat;
    public double startLon;
    public double endLat;
    public double endLon;
    public double bottomHeight;
    public double topHeight;   

    public void set(int r, int c, double slat, double slon, double elat, double elon, double bh, double th) {
        rows = r;
        cols = c;
        startLat = slat;
        startLon = slon;
        endLat = elat;
        endLon = elon;
        bottomHeight = bh;
        topHeight = th;       
    }
    
    /** Compute the x,y,z actual point in the current projection system */
   /* public Vec4 computePoint(Globe globe, Angle latitude, Angle longitude, double elevation,
            boolean terrainConformant) {
        //   double newElevation = elevation;

        //   if (terrainConformant)
        //   {
        //        newElevation += this.computeElevationAt(dc, latitude, longitude);
        //   }

        return gb.computePointFromPosition(latitude, longitude, elevation);
    }*/
}
