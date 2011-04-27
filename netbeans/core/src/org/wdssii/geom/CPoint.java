package org.wdssii.geom;

/**
 * location in a cartesian grid situated at the earth's center. Based on Thomas
 * Vaughan's CPoint class in the C++ version.
 * 
 * @author Lakshman
 */
public class CPoint {

    public double x;
    public double y;
    public double z;

    /**
     * everything in kilometers. the z direction is the polar axis. x is from
     * center through the Greenwich meridian y is from the center of earth
     * through the Caribbean.
     */
    public CPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CVector minus(CPoint pt) {
        return new CVector(x - pt.x, y - pt.y, z - pt.z);
    }

    public Location getLocation() {
        double r = Math.sqrt(x * x + y * y + z * z);
        double lat = Math.asin(z / r) * 180 / Math.PI;
        double lon = Math.atan2(y, x) * 180 / Math.PI;
        double h = (r - Location.EarthRadius);
        return new Location(lat, lon, h);
    }

    public CPoint plus(CVector v) {
        return new CPoint(x + v.x, y + v.y, z + v.z);
    }

    @Override
    public String toString() {
        return "(point: " + x + "," + y + "," + z + ")";
    }
}
