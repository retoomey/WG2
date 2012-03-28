package org.wdssii.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.geom.Location;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.util.RadialUtil;

/** A radial set is a collection of radials.
 * 
 * @author lakshman
 * 
 */
public class RadialSet extends DataType implements Table2DView {

    private static Logger log = LoggerFactory.getLogger(RadialSet.class);
    /** Elevation in degrees of this radial set */
    private final float elevDegs;
    /** Elevation in radians of this radial set */
    private final float elevRads;
    /** Tan of elevation, cached for the beam height distance function */
    private final float tanElevation;
    /** Sin of the elevation cached for getLocation and other needs */
    private final float sinElevation;
    /** Cos of the elevation cached for getLocation and other needs */
    private final float cosElevation;
    /** Our radials */
    protected Radial[] radials;
    /** Cache the radar location CPoint for speed */
    private final CPoint radarLocation;
    /** Cross product of z and y vector */
    private final CVector myUx;
    /** Y-azis vector north-ward relative to RadialSet center */
    private final CVector myUy;
    /** Z-axis perpendicular to the earth's surface */
    private final CVector myUz;
    /** Range to the first gate of the RadialSet in Kms */
    private final float rangeToFirstGate;
    /** The maximum number of gates of all Radial */
    private final int myMaxGateNumber;
    // This is a radial set lookup that finds an exact match for a radial given an azimuth.  Need this for the vslice/isosurface
    // in the GUI.
    /** A sorted array of end azimuth, each corresponding to azimuthRadials below, this gives us a o(nlogn) binary
     * search of radials given an angle (which for 360 radials is about 8 searches per angle .
     * Memory cost: one Float, one Radial reference per Radial, typically O(365) 
     * Speed cost: Typically 8 searches O(log(360)) */
    private float[] angleToRadial;
    /** Radials corresponding the angleToRadial above (these are sorted by increasing end azimuth). */
    private Radial[] azimuthRadials;

    /** Passed in by builder objects to use to initialize ourselves.
     * This allows us to have final field access from builders.
     */
    public static class RadialSetMemento extends DataTypeMemento {

        /** Elevation in degrees of this radial set */
        public float elevation;
        /** Our radials */
        public Radial[] radials;
        /** Cache the radar location CPoint for speed */
        public CPoint radarLocation;
        /** Cross product of z and y vector */
        public CVector myUx;
        /** Y-azis vector north-ward relative to RadialSet center */
        public CVector myUy;
        /** Z-axis perpendicular to the earth's surface */
        public CVector myUz;
        /** Range to the first gate of the RadialSet in Kms */
        public float rangeToFirstGate;
        /** The maximum number of gates of all Radial */
        public int maxGateNumber = 0;
    };

    /** The query object for RadialSets. */
    public static class RadialSetQuery extends DataTypeQuery {

        /** Query by SphericalLocation, has priority over inLocation.. Try to use this when
         * passing the same location to multiple radial sets of a volume...saves calculation, use
         * the locationToSpherical function */
        public SphericalLocation inSphere = null;
        /** Used by the GUI to tell which RadialSet in a RadialSetVolume query was in */
        public int outRadialSetNumber = -1;
        /** The radial index the last query found.  This is found by binary search of sorted end azimuth.
         * This may be outside the Radial azimuth range if outInAzimuth is false. 
         * Radial 1: 0-2 degrees, Radial 2: 5-8 degrees.
         * 4 degrees --> Radial 2, outInAzimuth = false
         * 5 degrees --> Radial 2, outInAzimuth = true
         */
        public int outHitRadialNumber;
        /** Our we within the azimuth of the hit radial number? Some stuff might need to know this,
         * do we in vslice smear azimuth or show blank gaps? */
        public boolean outInAzimuth;
        /** The radial gate number the last query found.  Note this is only set if data is HIT by location exactly */
        public int outHitGateNumber;
        /** Is the gate in range? The query assumes an infinite size RadialSet for gate calculation */
        public boolean outInRange;
        /** The azimuth for query in degrees */
        public float outAzimuthDegrees;
    };

    public RadialSet(RadialSetMemento m) {
        super(m);
        this.elevDegs = m.elevation;
        this.elevRads = (float) Math.toRadians(elevDegs);
        this.tanElevation = (float) Math.tan(elevRads);
        this.sinElevation = (float) Math.sin(elevRads);
        this.cosElevation = (float) Math.cos(elevRads);
        this.radials = m.radials;
        this.radarLocation = m.radarLocation;
        this.myUx = m.myUx;
        this.myUy = m.myUy;
        this.myUz = m.myUz;
        this.rangeToFirstGate = m.rangeToFirstGate;
        this.myMaxGateNumber = m.maxGateNumber;
        createAzimuthSearch();
    }

    /** Create a sorted list of end azimuth numbers, which allows us to binary
     * search for a Radial by azimuth very quickly.  Note that this doesn't
     * mean the RadialSet is sorted.
     */
    protected final void createAzimuthSearch() {
        // This assumes no two radials have the same end angle, even if they do,
        // should still work, just indeterminate which of the 2 radials you'll get
        if (radials != null && (radials.length > 0)) {
            angleToRadial = new float[radials.length];
            azimuthRadials = Arrays.copyOf(radials, radials.length);

            // Sort the azimuth radials by end angle...
            Arrays.sort(azimuthRadials, new Comparator<Radial>() {

                @Override
                public int compare(Radial o1, Radial o2) {
                    double u1 = o1.getEndAzimuthDegs();
                    double u2 = o2.getEndAzimuthDegs();
                    if (u1 < u2) {
                        return -1;
                    }
                    if (u1 > u2) {
                        return 1;
                    }
                    return 0;
                }
            });

            // Create the angle list from the sorted radials...
            for (int i = 0; i < angleToRadial.length; i++) {
                angleToRadial[i] = azimuthRadials[i].getEndAzimuthDegs();
            }
        }
    }

    /** Return a double used to sort a volume of this DataType.  For example,
     * for RadialSets this would be the elevation value.
     * @return value in volume
     */
    @Override
    public double sortInVolume() {
        return elevDegs;
    }

    public float getElevationDegs() {
        return elevDegs;
    }

    public float getElevationRads() {
        return elevRads;
    }

    public float getElevationSin() {
        return sinElevation;
    }

    public float getElevationCos() {
        return cosElevation;
    }

    public float getElevationTan() {
        return tanElevation;
    }

    /** meters */
    public float getRangeToFirstGateKms() {
        return rangeToFirstGate;
    }

    /**
     * beamwidth of first radial, or 1 degree if there is no radial.
     */
    public float getBeamWidthKms() {
        if (radials.length > 0) {
            return getRadial(0).getBeamWidth();
        }
        return 1;
    }

    /**
     * nyquist of first radial, or 0 if there is no radial.
     */
    public float getNyquistMetersPerSecond() {
        if (radials.length > 0) {
            return getRadial(0).getNyquistMetersPerSecond();
        }
        return 0;
    }

    /**
     * mean azimuthal spacing of all radials.
     */
    public float getAzimuthalSpacing() {
        float as = 0;
        for (Radial r : radials) {
            as += r.getAzimuthalSpacingDegs();
        }
        float result = (radials.length <= 1) ? as : (as / radials.length);
        return result;
    }

    /**
     * gatewidth of first radial, or 1 km if there is no radial.
     */
    public float getGateWidthKms() {
        if (radials.length > 0) {
            return getRadial(0).getGateWidthKms();
        }
        return 1.0f;
    }

    /**
     * number of gates of first radial, or 0 if there is no radial.
     * This is set to the maximum gate number of all read in radials.
     */
    public int getNumGates() {
        //	if (radials.length > 0)
        //		return getRadial(0).getNumGates();
        //	return 0;
        return myMaxGateNumber;
    }

    public int getNumRadials() {
        if (radials != null) {
            return radials.length;
        } else {
            return 0;
        }
    }

    public Location getRadarLocation() {
        return originLocation;
    }

    public Radial[] getSortedAzimuthRadials() {
        return azimuthRadials;
    }

    public Radial[] getRadials() {
        return radials;
    }

    public Radial getRadial(int index) {
        return radials[index];
    }

    /** @return 0 if vcp is unknown. */
    int getVCP() {
        try {
            return Integer.parseInt(getAttribute("vcp").toString());
        } catch (Exception e) {
            // number format, null, etc.
            return 0;
        }
    }

    /** A Spherical coordinate location centered around our RadialSet center.
     * Used for speed in VSlice/Isosurface calculations.  Filled in by our locationToSphere method
     *  The trick is that all radial sets in a volume will give the same result for a given location.
     */
    public static class SphericalLocation {

        /** Angle in degrees */
        public double azimuthDegs;
        /** Angle in degrees */
        public double elevDegs;
        /** Distance in Kms */
        public double range;
        /** True if we cached this stuff. Will be the same value for all
         * RadialSets for a particular sample location
         */
        private boolean cachedSinCos = false;
        private double elevSin;
        private double elevCos;

        /** Given the tan of an elevation, get the weight in height from the
         * actual beam at our elevDegs.  For example, if inElev == elevDegs, 
         * then the value is zero, if inElev < elevDegs the value is -,
         * if inElev > elevDegs the value is +
         * 
         * @param inElevTan The tan of the other elevation
         * @return 
         */
        public double getHeightWeight(double inElevTan) {
            // Trig so slow, cache it...
            if (!cachedSinCos) {
                double rads = Math.toRadians(elevDegs);
                elevCos = Math.cos(rads);
                elevSin = Math.sin(rads);
                cachedSinCos = true;
            }
            return -(range * (elevCos * inElevTan - elevSin));
        }
    }

    /** In volumes, a location is used to query multiple radial sets that share the
     * same center location, ux, uy, uz, etc.  This takes a location in space
     * and caches all of the calculations in relation to the volume. This speeds
     * up VSlice/Isosurface rendering calculations.
     * 
     * This then is passed into the query function.
     */
    public void locationToSphere(Location l, SphericalLocation out) {

        // The direct way, but slower...crazy amounts of newing and memory fluctuation
        // during render
	/*	CPoint pt = l.getCPoint();
        CVector fromConeApex = pt.minus(radarLocation);
        CVector vxy = fromConeApex.crossProduct(myUz);
        double norm = fromConeApex.norm();
        out.elev = Math.asin(fromConeApex.dotProduct(myUz) / norm) * 180 / Math.PI;
        out.range = norm;
        // Get azimuth for a location (always for filters in GUI)
        // The atan2 gives us 0 at north, 90 to west. Radar is 0 to north, 90 to east
        double dotx2 = vxy.dotProduct(myUx);
        double doty2 = vxy.dotProduct(myUy);
        double az2 = (float)(360.0 - (Math.toDegrees(Math.atan2(doty2, dotx2))));
        float azimuth = Radial.normalizeAzimuth((float) az2);
        out.azimuth = azimuth;
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
        out.azimuthDegs = Radial.normalizeAzimuthDegs((float) az);

        double dotz = coneX * myUz.x + coneY * myUz.y + coneZ * myUz.z;
        out.elevDegs = Math.asin(dotz / out.range) * 180 / Math.PI;
    }

    /** For a given location, get the information into a data object.
     * For speed the data object is typically pre-created and reused
     * Note: This method is overloading DataType's function, not overriding.
     * The object has to actually be a RadialSetQuery or cast to it for this
     * to get called instead.
     */
    public void queryData(RadialSetQuery q) {

        boolean haveLocation = false;

        SphericalLocation a = null;
        if (q.inSphere != null) {
            a = q.inSphere;
            haveLocation = true;
        } else {
            if (q.inLocation != null) {
                a = new SphericalLocation();
                this.locationToSphere(q.inLocation, a);
                haveLocation = true;
            }
        }

        if (haveLocation) {

            q.outAzimuthDegrees = (float) a.azimuthDegs;

            // Get the elevation of the point (estimate)
            if (q.inUseHeight) {
                Radial first = getRadial(0);
                double bw = first.getBeamWidth();
                //double elev = Math.asin(fromConeApex.dotProduct(myUz) / norm) * 180 / Math.PI;
                double elev_diff = a.elevDegs - elevDegs;

                // We want the interpolation weight (for bi/tri-linear)
                if (q.inNeedInterpolationWeight) {
                    // FIXME: math could be cached in a for speed in volumes

                    /* Math for height calculation....
                    This is first part of the lat/lon/height weights so I can do true
                    bilinear/trilinear interpolation.  Bleh..  Here's the math:
                     * 
                    Line 1:  Line from sample point straight down to ground...The height
                    Polar to cartesion of the 'sample' point...
                    e = a.elev;
                    (r*cos(e), r*sin(e));
                    Point 2:
                    (r*cos(e), 0)
                    ==> X = r*cos(e); the vertical line....
                    
                    Line 2 (radial beam)
                    Point 1:
                    (0,0);
                    Point 2:
                    (R*cos(elevation), R*sin(elevation);
                    
                    y = mx+b where
                    z = elevation;
                    m = tan(z);
                    b = 0;
                    ==> y = x*tan(z); // For radar beam line...
                    
                    x1 = r*cos(e);              // Sample point
                    y1 = r*sin(e);
                    x2 = r*cos(e);              // Point on the 'beam'
                    y2 = r*cos(e)*tan(z);
                    
                    // Now the distance from the beam point to the sample point...
                    (x2-x1)^2 = 0;
                    (y2-y1)^2 = (r*cos(e)*tan(z)-(r*sin(e)))^2
                    distance = abs(r*(cos(e)*tan(z)-sin(e)));
                     */
                    // Calculate the weight for interpolation in height
                    // We convert from radial polar space to cartesian and get 
                    // the distance from sample point to the beam line.

                    // Projection in Height of the sampled point onto the beam
                    // of the radar....or is it? rofl..
                    // double eR = Math.toRadians(a.elevDegs);
                    // double d2R = elevRads;
                    // double h = a.range * (Math.cos(eR) * Math.sin(d2R) / Math.cos(d2R) - Math.sin(eR));
                    // double h = a.range * (Math.cos(eR) * Math.tan(d2R) - Math.sin(eR));
                    double h = a.getHeightWeight(tanElevation);

                    // if (h < 0) {
                    //     h = -h;
                    // } // sqrt of square is abs         
                    // if (a.elevDegs < elevDegs) {
                    //     h = -h;
                    //  }
                    q.outDistanceHeight = (float) (h);
                    // Ignore beam width filter, we want
                    // the value AND the weight for interpolation
                    //elev_diff = 0;
                }

                // Beam width filter.  Outside beam width in the vertical?
                if (Math.abs(elev_diff) > bw / 2.0) {

                    // FIXME: Should we still get this stuff anyway?  Will slow down volumes like vslice
                    // Probably need more in flags
                    q.outHitRadialNumber = -1;
                    q.outHitGateNumber = -1;
                    q.outInAzimuth = false;
                    q.outInRange = false;
                    if (!q.inNeedInterpolationWeight) {
                        q.outDataValue = MissingData;
                        return;
                    }
                }
            }

            // Search radials by end azimuth
            if (radials != null) {
                int index = Arrays.binarySearch(angleToRadial, (float) a.azimuthDegs);
                int radialIndex = (index < 0) ? -(index + 1) : index;

                if ((radialIndex >= 0) && (radialIndex < angleToRadial.length)) { // within all radial end values
                    Radial candidate = azimuthRadials[radialIndex];
                    q.outHitRadialNumber = candidate.getIndex();
                    q.outInAzimuth = candidate.contains((float) a.azimuthDegs);

                    final double gate = (a.range - this.getRangeToFirstGateKms()) / candidate.getGateWidthKms();
                    final int gateNumber = (int) Math.floor(gate);
                    q.outHitGateNumber = gateNumber;

                    // Is the query within range for this Radial?  Range is up to the last piece of available data.
                    Array1Dfloat gates = candidate.getValues();
                    q.outInRange = (gateNumber >= 0) && (gateNumber < gates.size());

                    // Valid data must be in azimuth and range...? Note for vslice this will make black gaps in azimuth
                    //if ((q.outInAzimuth) && (q.outInRange)){
                    if (q.outInRange) {
                        /* Experiment, playing with range interpolation to see how it looks...
                        float dataCore = gates.get(gateNumber);
                        q.outDataValue = dataCore;
                        
                        if (DataType.isRealDataValue(q.outDataValue)){
                        if (q.inNeedInterpolationWeight) {
                        double weight1 = gate - gateNumber; // .5 = center, 0 floor, 1 to
                        q.outDataValue = dataCore;
                        if (weight1 >= .5) {
                        if (gateNumber + 1 < gates.size()) {
                        float dataUp1 = gates.get(gateNumber + 1);
                        if (DataType.isRealDataValue(dataUp1)){
                        // at weight == 1, 50% up, 50% core   0
                        // at weight = .5, 0% up, 100% core.  .5
                        // y2 = 1
                        // y1 = .5
                        // y = weight1
                        // R1 core
                        // R2 .5*dataUp1;
                        
                        float i1 = (float) ((1 - weight1) / .5) * dataCore;
                        //float i2 = (float) (((weight1 - .5) / .5) * (.5 * dataUp1));
                        float i2 = (float) (((weight1 - .5) / .5) * (dataUp1));
                        q.outDataValue = i1 + i2;
                        //q.outDataValue = 1000;
                        return;
                        }
                        } else {
                        //  q.outDataValue = dataCore;
                        return;
                        }
                        } else {
                        if (gateNumber - 1 > 0) {
                        float dataDown = gates.get(gateNumber - 1);
                        if (DataType.isRealDataValue(dataDown)){
                        // y2 = .5
                        // y1 = 0
                        // y = weight1
                        // R2 core
                        // R1 .5*dataDown;
                        
                        float i1 = (float) (((.5 - weight1) / .5) * (.5 * dataDown));
                        float i2 = (float) (((weight1) / .5) * (dataCore));
                        // q.outDataValue = i1 + i2;
                        
                        return;
                        }
                        } else {
                        // q.outDataValue = dataCore;
                        return;
                        }
                        
                        }
                        } 
                        }
                        }else{
                         */
                        q.outDataValue = gates.get(gateNumber);
                        return;
                    }
                }
            }
        }
        q.outDataValue = MissingData;
        return;
    }

    /** Create an SRM delta value for each radial we have. This method is called by the 
     * GUI Storm Relative Motion filter. Keeping the logic in RadialSet.  The GUI filter
     * allows us to show SRM without modifying the original RadialSet.
     */
    public ArrayList<Float> createSRMDeltas(float speedMS, float dirDegrees) {
        ArrayList<Float> srmDeltas = new ArrayList<Float>();
        if (azimuthRadials != null) {
            for (Radial r : azimuthRadials) {
                float aglRadians = (float) Math.toRadians((r.getMidAzimuthDegs() - dirDegrees));
                float vr = (float) (Math.cos(aglRadians) * speedMS);
                vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
                srmDeltas.add(vr);
            }
        }
        return srmDeltas;
    }

    /** debugging output */
    @Override
    public String toStringDB() {
        String s = "RadialSet " + getTypeName() + " at " + elevDegs + " has "
                + radials.length + " radials." + " the first:\n"
                + getRadial(0).toStringDB() + super.toStringDB();
        return s;
    }

    // Table2D implementation --------------------------------------------------------
    @Override
    public int getNumCols() {
        return (getNumRadials());
    }

    @Override
    public int getNumRows() {
        return (getNumGates());
    }

    @Override
    public String getRowHeader(int row) {

        // The row assumes every Radial in the set has gates with lined up gate width.
        int index = getNumRows() - (row) - 1;
        float firstGateKms = getRangeToFirstGateKms();
        float perGateKms = getGateWidthKms();
        float rangeKms = firstGateKms + (index * perGateKms);
        return (String.format("%6.2f", rangeKms));
    }

    @Override
    public String getColHeader(int col) {
        if (col < getNumRadials()) { // Do we need this check?
            Radial aRadial = getRadial((col));
            if (aRadial != null) {
                float azDegs = aRadial.getStartAzimuthDegs();
                return (String.format("%6.2f", azDegs));
            }
        }
        return ("");
    }

    @Override
    public boolean getCellValue(int row, int col, CellQuery output) {
        float value = 0;
        Radial r = getRadial((col));
        if (r != null) {
            int count = getNumGates();
            if (count > 0) {
                value = r.getValue((count - row - 1));
            }
        }
        output.value = value;
        return true;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col,
            Location output) {
        if ((col >= getNumCols()) || (row >= getNumRows())) {
            log.error("Table out of bounds : (" + col + "," + row + ") bounds [" + getNumCols() + "," + getNumRows());
            return false;
        }
        boolean success = false;

        Radial r = getRadial((col));
        if (r != null) {
            int count = getNumGates();
            if (count > 0) {
                final float RAD = 0.017453293f;
                Location center = getRadarLocation();
                int irow = getNumRows() - row - 1;
                float rangeKms = getRangeToFirstGateKms();
                float w = r.getGateWidthKms();
                float startRAD = r.getStartAzimuthDegs() * RAD;
                float endRAD = r.getEndAzimuthDegs() * RAD;

                // FIXME clean this up.
                switch (type) {
                    case CENTER: {
                        float fullRangeKms = rangeKms + (w * ((irow) + .5f));

                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin((startRAD + endRAD) / 2.0f), // Precomputed azimuth sin and cos around	
                                Math.cos((startRAD + endRAD) / 2.0f), fullRangeKms, // at range in kilometers
                                sinElevation, // sin of the elevation (precomputed)
                                cosElevation // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                sinElevation, // sin of the elevation (precomputed)
                                cosElevation // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                sinElevation, // sin of the elevation (precomputed)
                                cosElevation // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case BOTTOM_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                sinElevation, // sin of the elevation (precomputed)
                                cosElevation // cos of the elevation (precomputed)
                                );
                    }
                    break;

                    case BOTTOM_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                sinElevation, // sin of the elevation (precomputed)
                                cosElevation // cos of the elevation (precomputed)
                                );
                    }
                    break;
                }

                success = true;
            }
        }

        return success;
    }

    @Override
    public boolean getCell(Location input, Cell output) {
        RadialSetQuery q = new RadialSetQuery();
        q.inLocation = input;
        q.inUseHeight = false;
        queryData(q);
        boolean success = false;
        int row = q.outHitGateNumber;
        int col = q.outHitRadialNumber;
        // Humm this is already done in query, right?
        success = ((row >= 0) && (row < getNumRows()) && (col >= 0) && (col < getNumCols()));
        if (success) {
            output.row = getNumRows() - row - 1;
            output.col = col;
        }
        return success;
    }
}
