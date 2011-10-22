package org.wdssii.geom;

/**
 * Subtracting two CPoints gives rise to a CVector Based on Thomas Vaughan's
 * CVector class in the C++ version.
 * 
 * @author Lakshman
 * @version $Id: CVector.java,v 1.2 2009/06/02 20:18:30 lakshman Exp $
 */
public class CVector {

    public double x;
    public double y;
    public double z;

    /**
     * everything in kilometers. the z direction is the polar axis. x is from
     * center through the Greenwich meridian y is from the center of earth
     * through the Caribbean.
     */
    public CVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CVector unit() {
        double len = norm();
        if (len <= 0) {
            return this; // zero vector
        }
        return multiply(1.0 / len);
    }

    public double norm() {
        return Math.sqrt(normSquared());
    }

    public double normSquared() {
        return (x * x + y * y + z * z);
    }

    public CVector multiply(double f) {
        return new CVector(x * f, y * f, z * f);
    }

    public CVector plus(CVector v) {
        return new CVector(x + v.x, y + v.y, z + v.z);
    }

    public CVector minus(CVector v) {
        return new CVector(x - v.x, y - v.y, z - v.z);
    }

    public double dotProduct(CVector v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public CVector crossProduct(CVector v) {
        return new CVector(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y
                * v.x);
    }

    @Override
    public String toString() {
        return "(vector: " + x + "," + y + "," + z + ")";
    }
}
