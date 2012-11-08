package org.wdssii.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.geom.Location;

/**
 * A RadialSet. Root class of all RadialSet
 *
 * @author Robert Toomey
 */
public class RadialSet extends DataType {

    private static Logger log = LoggerFactory.getLogger(RadialSet.class);
    /**
     * Fixed angle in degrees of this radial set. This is the unchanging angle
     * of the RadialSet, it will be elevation for PPI and azimuth for RHI
     */
    private final float fixedAngleDegs;
    /**
     * Fixed angle in radians of this radial set
     */
    private final float fixedAngleRads;
    /**
     * Tan of fixed angle, cached for the beam height distance function
     */
    private final float fixedAngleTan;
    /**
     * Sin of the fixed angle cached for getLocation and other needs
     */
    private final float fixedAngleSin;
    /**
     * Cos of the fixed angle cached for getLocation and other needs
     */
    private final float fixedAngleCos;
    /**
     * The array of Radial that we hold. These are in load order, not sorted in
     * any way.
     */
    protected Radial[] radials;
    /**
     * Cache the radar location CPoint for speed
     */
    private final CPoint radarLocation;
    /**
     * Cross product of z and y vector
     */
    private final CVector myUx;
    /**
     * Y-azis vector north-ward relative to RadialSet center
     */
    private final CVector myUy;
    /**
     * Z-axis perpendicular to the earth's surface
     */
    private final CVector myUz;
    /**
     * Range to the first gate of the RadialSet in Kms
     */
    private final float rangeToFirstGate;
    /**
     * The maximum number of gates of all Radial
     */
    private final int myMaxGateNumber;

    /**
     * Passed in by builder objects to use to initialize ourselves. This allows
     * us to have final field access from builders.
     */
    public static class RadialSetMemento extends DataTypeMemento {

        /**
         * The fixed angle in degrees of this radial set, for PPI this is the
         * elevation of the RadialSet
         */
        public float fixedAngleDegs;
        /**
         * Our radials
         */
        public Radial[] radials;
        /**
         * Cache the radar location CPoint for speed
         */
        public CPoint radarLocation;
        /**
         * Cross product of z and y vector
         */
        public CVector myUx;
        /**
         * Y-azis vector north-ward relative to RadialSet center
         */
        public CVector myUy;
        /**
         * Z-axis perpendicular to the earth's surface
         */
        public CVector myUz;
        /**
         * Range to the first gate of the RadialSet in Kms
         */
        public float rangeToFirstGate;
        /**
         * The maximum number of gates of all Radial
         */
        public int maxGateNumber = 0;
    };

    /**
     * Create a DataType given a DataTypeMemento object
     */
    public RadialSet(RadialSetMemento m) {
        super(m);
        this.fixedAngleDegs = m.fixedAngleDegs;
        this.fixedAngleRads = (float) Math.toRadians(fixedAngleDegs);
        this.fixedAngleTan = (float) Math.tan(fixedAngleRads);
        this.fixedAngleSin = (float) Math.sin(fixedAngleRads);
        this.fixedAngleCos = (float) Math.cos(fixedAngleRads);
        this.radials = m.radials;
        this.radarLocation = m.radarLocation;
        this.myUx = m.myUx;
        this.myUy = m.myUy;
        this.myUz = m.myUz;
        this.rangeToFirstGate = m.rangeToFirstGate;
        this.myMaxGateNumber = m.maxGateNumber;
    }

    /**
     * The maximum number of gates of any Radial in us
     */
    public int getNumGates() {
        return myMaxGateNumber;
    }

    /**
     * Get the number of Radial objects in this RadialSet
     */
    public int getNumRadials() {
        if (radials != null) {
            return radials.length;
        } else {
            return 0;
        }
    }

    /**
     * Get the fixed angle of the RadialSet in degrees
     */
    public float getFixedAngleDegs() {
        return fixedAngleDegs;
    }

    /**
     * Get the fixed angle of the RadialSet in radians
     */
    public float getFixedAngleRads() {
        return fixedAngleRads;
    }

    /**
     * Get the cached Sin of fixed angle of the RadialSet
     */
    public float getFixedAngleSin() {
        return fixedAngleSin;
    }

    /**
     * Get the cached Cos of fixed angle of the RadialSet
     */
    public float getFixedAngleCos() {
        return fixedAngleCos;
    }

    /**
     * Get the cached Tan of fixed angle of the RadialSet
     */
    public float getFixedAngleTan() {
        return fixedAngleTan;
    }

    /**
     * Get given radial number
     */
    public Radial getRadial(int index) {
        return radials[index];
    }

    /**
     * Get the range to the beginning of the first gate in Kms for all Radial
     */
    public float getRangeToFirstGateKms() {
        return rangeToFirstGate;
    }

    /**
     * Return a double used to sort a volume of this DataType. For example, for
     * RadialSets this would normally be fixed angle of the RadialSet. For
     * example, PPIRadialSets will sort by elevation. For example, RHIRadialSets
     * will sort by azimuth.
     *
     * @return value in volume
     */
    @Override
    public double sortInVolume() {
        return getFixedAngleDegs();
    }

    /**
     * String debugging output
     */
    @Override
    public String toStringDB() {
        String s = "RadialSet " + getTypeName() + " at " + fixedAngleDegs + " has "
                + radials.length + " radials." + " the first:\n";
        return s;
    }

    /**
     * In volumes, a location is used to query multiple radial sets that share
     * the same center location, ux, uy, uz, etc. This takes a location in space
     * and caches all of the calculations in relation to the volume. This speeds
     * up VSlice/Isosurface rendering calculations.
     *
     * This then is passed into the query function.
     *
     * @FIXME: This might only apply to PPI RadialSet, not sure yet..
     */
    public void locationToSphere(Location l, PPIRadialSet.SphericalLocation out) {

        // The direct way, but slower...crazy amounts of newing and memory fluctuation
        // during render
	/*
         * CPoint pt = l.getCPoint(); CVector fromConeApex =
         * pt.minus(radarLocation); CVector vxy =
         * fromConeApex.crossProduct(myUz); double norm =
         * fromConeApex.norm(); out.elev =
         * Math.asin(fromConeApex.dotProduct(myUz) / norm) * 180 /
         * Math.PI; out.range = norm; // Get azimuth for a location
         * (always for filters in GUI) // The atan2 gives us 0 at north,
         * 90 to west. Radar is 0 to north, 90 to east double dotx2 =
         * vxy.dotProduct(myUx); double doty2 = vxy.dotProduct(myUy);
         * double az2 = (float)(360.0 -
         * (Math.toDegrees(Math.atan2(doty2, dotx2)))); float azimuth =
         * Radial.normalizeAzimuth((float) az2); out.azimuth = azimuth;
         */

        // All the math expanded (avoids new allows some short cuts for speed)
        // Remember in a GUI volume render we typically call this in a monster loop
        // while the user is dragging so every ms counts...

        // pt = q.inLocation.getCPoint();
        double r = Location.EarthRadius + l.getHeightKms(); // kms
        double phi = l.getLongitude() * Math.PI / 180.0; // radians();
        double beta = l.getLatitude() * Math.PI / 180.0; // .radians();
        double x = r * Math.cos(phi) * Math.cos(beta);
        double y = r * Math.sin(phi) * Math.cos(beta);
        double z = r * Math.sin(beta);

        //CVector fromConeApex = pt.minus(radarLocation);
        double coneX = x - radarLocation.x;  // Cheaper
        double coneY = y - radarLocation.y;
        double coneZ = z - radarLocation.z;

        //CVector vxy = fromConeApex.crossProduct(myUz);
        double crossX = coneY * myUz.z - coneZ * myUz.y;
        double crossY = coneZ * myUz.x - coneX * myUz.z;
        double crossZ = coneX * myUz.y - coneY * myUz.x;

        //double norm = fromConeApex.norm();
        out.range = Math.sqrt(coneX * coneX + coneY * coneY + coneZ * coneZ);

        // Calculate the azimuth of the location in respect to the radar center
        //double dotx = vxy.dotProduct(myUx);
        double dotx = crossX * myUx.x + crossY * myUx.y + crossZ * myUx.z;
        double doty = crossX * myUy.x + crossY * myUy.y + crossZ * myUy.z;
        double az = (360.0 - (Math.toDegrees(Math.atan2(doty, dotx))));
        out.azimuthDegs = Radial.normalizeDegrees((float) az);

        double dotz = coneX * myUz.x + coneY * myUz.y + coneZ * myUz.z;
        out.elevDegs = Math.asin(dotz / out.range) * 180 / Math.PI;
    }

    /**
     * beamwidth of first radial, or 1 degree if there is no radial.
     */
    public float getBeamWidthKms() {
        if (radials.length > 0) {
            radials[0].getBeamWidthDegrees();
        }
        return 1;
    }

    /**
     * gatewidth of first radial, or 1 km if there is no radial.
     */
    public float getGateWidthKms() {
        if (radials.length > 0) {
            return radials[0].getGateWidthKms();
        }
        return 1.0f;
    }

    /**
     * nyquist of first radial, or 0 if there is no radial.
     */
    public float getNyquistMetersPerSecond() {
        if (radials.length > 0) {
            return radials[0].getNyquistMetersPerSecond();
        }
        return 0;
    }

    /**
     * The center location of the RadialSet
     */
    public Location getRadarLocation() {
        return originLocation;
    }

    /**
     * @return 0 if vcp is unknown.
     */
    public int getVCP() {
        try {
            return Integer.parseInt(getAttribute("vcp").toString());
        } catch (Exception e) {
            // number format, null, etc.
            return 0;
        }
    }

    /**
     * mean azimuthal spacing of all radials. Not used it seems...
     */
    /*
     public float getAzimuthalSpacing() {
     float as = 0;
     for (Radial r : radials) {
     as += r.getAzimuthalSpacingDegs();
     }
     float result = (radials.length <= 1) ? as : (as / radials.length);
     return result;
     }
     */
    /**
     * Given speed in meters per second, a direction in degrees (for the SRM
     * vector), and an azimuth, generates the SRM value
     *
     * @param speedMS Meters per second of SRM vector
     * @param dirDegs Direction in degs of SRM vector from radar center
     * @param azimuthDegs Azimuth of Radial wanting SRM delta for
     * @return the SRM delta value (added to gate value)
     */
    public static float getSRMDelta(float speedMS, float dirDegs, float azimuthDegs) {
        float aglRadians = (float) Math.toRadians(azimuthDegs - dirDegs);
        float vr = (float) (Math.cos(aglRadians) * speedMS);
        vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
        return vr;
    }
}
