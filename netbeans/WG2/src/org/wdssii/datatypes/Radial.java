package org.wdssii.datatypes;

import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.storage.Array1Dfloat;

/**
 * Radial holds a set of gate data that share an azimuth range.
 * 
 * @author lakshman
 * 
 */
public class Radial {

    /** The 1D float that stores the radial */
    private Array1Dfloat array;
    /** The start Azimuth in degrees.  In the 'sweep' of the radar the beginning of the beam */
    private final float startAzimuthDegs;
    /** The width of the beam in degrees */
    private final float beamWidthDegs;
    /** The total degrees in azimuth of the beam */
    private final float azimuthalSpacingDegs;
    /** The nyquist of this radial in meters per second */
    private final float nyquistMetersPerSecond;
    /** The constant length in Kms from radar center that each gate is */
    private final float gateWidthKms;
    /** Cache the sin of the radial elevation */
   // private final sinElevation;
    /** Cache the cos of the radial elevation */
   // private final cosElevation;
    /** Unit vector of radial.  FIXME: kinda want to get rid of this */
    private CVector unitVector;
    /** The index of this radial inside of a RadialSet in creation order */
    private int index = -1;

    /** in degrees and kilometers. Does not copy array 
     * @param ny 
     * @param  */
    public Radial(float startAz, float beamWidthDegs,
            float azimuthalSpacing, float gateWidth,
            float nyquist, Array1Dfloat values, int i) {
        this.gateWidthKms = gateWidth;
        this.array = values;
        this.startAzimuthDegs = startAz;
        this.beamWidthDegs = beamWidthDegs;
        this.azimuthalSpacingDegs = azimuthalSpacing;
        this.nyquistMetersPerSecond = nyquist;
        this.index = i;
    }

    /** puts the given angle in the range [0,360) */
    public static float normalizeAzimuthDegs(float az) {
        // in range [0,360)
        if (az < 0) {
            az += 360;
        } else if (az >= 360) {
            az -= 360;
        }
        return az;
    }

    public boolean contains(float az) {
        // returns range [0,360)
        float diff = normalizeAzimuthDegs(az - startAzimuthDegs);
        return (diff < azimuthalSpacingDegs);
    }

    public Array1Dfloat getValues() {
        return array;
    }

    public void setIndex(int i) {
        index = i;
    }

    public int getIndex() {
        return index;
    }

    /** may not be normalized */
    public float getStartAzimuthDegs() {
        return startAzimuthDegs;
    }

    /**
     * the end azimuth is not normalized in range [0,360) -- the rule instead is
     * that the azimuth will be greater than the start azimuth.
     */
    public float getEndAzimuthDegs() {
        return (startAzimuthDegs + azimuthalSpacingDegs);
    }

    /**
     * the mid azimuth is not normalized in range [0,360) -- the rule instead is
     * that the azimuth will be greater than the start azimuth.
     */
    public float getMidAzimuthDegs() {
        return (float) (startAzimuthDegs + 0.5 * azimuthalSpacingDegs);
    }

    /** always positive */
    public float getBeamWidth() {
        return beamWidthDegs;
    }

    public float getAzimuthalSpacingDegs() {
        return azimuthalSpacingDegs;
    }

    /** in meters */
    public float getGateWidthKms() {
        return gateWidthKms;
    }

    public float getValue(int index) {
        return array.get(index);
    }

    public int getNumGates() {
        return array.size();
    }

    /** in m/s */
    public float getNyquistMetersPerSecond() {
        return nyquistMetersPerSecond;
    }

    /**
     * get the unit vector along the direction of this radial. This method will
     * do the computation only on the first call, and return old values after
     * that, so the elevation is not used after that.
     */
    public CVector getUnitVector(float elevation) {
        if (unitVector == null) {
            // unit vector in co-ordinate system tangential to
            // earth at the radar location.
            double angle_to_xdir = (90 - getMidAzimuthDegs()) * Math.PI / 180.0;
            unitVector = new CVector(Math.cos(angle_to_xdir), Math.sin(angle_to_xdir), Math.sin(elevation * Math.PI / 180.0)).unit();
            return unitVector;
        }
        return unitVector;
    }

    /*
     * @param elevation of the RadialSet @param gateno of point @param radar
     * location of radar @param ux x-axis of local co-ordinate system tangential
     * to earth's surface at radar location
     */
    public CPoint getLocation(float elevation, int gateno, CPoint radar,
            CVector ux, CVector uy, CVector uz) {
        CVector disp = getUnitVector(elevation).multiply(
                gateno * getGateWidthKms());
        CVector disp_global = ux.multiply(disp.x).plus(uy.multiply(disp.y)).plus(uz.multiply(disp.z));
        CPoint result = radar.plus(disp_global);
        return result;
    }

    /** debugging output */
    public String toStringDB() {
        String s = "Radial " + startAzimuthDegs + " to " + getEndAzimuthDegs() + " deg"
                + " " + gateWidthKms + "km " + " first 10 values: \n";
        for (int i = 0; i < 10; ++i) {
            s += getValue(i) + " ";
        }
        return (s + "\n");
    }
}
