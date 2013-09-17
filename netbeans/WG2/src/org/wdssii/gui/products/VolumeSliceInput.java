package org.wdssii.gui.products;

/**
 * VolumeSliceInput is a holder class for the unique layout in 3D space of a
 * vertical or cappi slice. VolumeProducts use this to create visual sliced
 * representations of their data.
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

    public double getPercentOfLat(double aLat) {
        double percent = (aLat - startLat) / (endLat - startLat);
        return percent;
    }

    public double getPercentOfHeight(double h) {
        double percent = (h - bottomHeight) / (topHeight - bottomHeight);
        return percent;
    }

    /**
     * Get the latitude for given row and column in the grid FIXME: math shared
     * with ProductVolume
     */
    public double getLatDegrees(int col) {
        double deltaLat = (endLat - startLat) / cols;
        double lat = startLat + (col * deltaLat) + (deltaLat / 2.0);
        return lat;
    }

    /**
     * Get the longitude for given column in the grid FIXME: math shared with
     * ProductVolume
     */
    public double getLonDegrees(int col) {
        double deltaLon = (endLon - startLon) / cols;
        double lon = startLon + (col * deltaLon) + (deltaLon / 2.0);

        return lon;
    }

    /**
     * Get the height in slice for given row
     */
    public double getHeightKMS(int row) {
        double startHeight = topHeight;
        double deltaHeight = (topHeight - bottomHeight) / (1.0 * rows);
        double h = startHeight - (row * deltaHeight);
        return h;
    }

    public double getDeltaLat() {
        return (endLat - startLat) / cols;
    }

    public double getDeltaLon() {
        return (endLon - startLon) / cols;
    }

    public double getStartHeight() {
        return topHeight;
    }

    public double getDeltaHeight() {
        return (topHeight - bottomHeight) / (1.0 * rows);
    }
}
