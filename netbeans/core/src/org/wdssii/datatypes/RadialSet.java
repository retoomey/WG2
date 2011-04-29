package org.wdssii.datatypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.geom.Location;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array2Dfloat;
import org.wdssii.util.RadialUtil;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/** A radial set
 * 
 * @author lakshman
 * 
 */
public class RadialSet extends DataType implements Table2DView {

    private static Log log = LogFactory.getLog(RadialSet.class);
    /** Elevation in degrees of this radial set */
    private float elevation;
    /** Our radials */
    protected Radial[] radials;
    /** Cache the radar location CPoint for speed */
    private CPoint radarLocation;
    /** Cross product of z and y vector */
    private CVector myUx;
    /** Y-azis vector north-ward relative to RadialSet center */
    private CVector myUy;
    /** Z-axis perpendicular to the earth's surface */
    private CVector myUz;
    /** Range to the first gate of the RadialSet in Kms */
    private float rangeToFirstGate;
    /** The maximum number of gates of all Radial */
    private int myMaxGateNumber = 0;
    // This is a radial set lookup that finds an exact match for a radial given an azimuth.  Need this for the vslice/isosurface
    // in the GUI.
    /** A sorted array of end azimuth, each corresponding to azimuthRadials below, this gives us a o(nlogn) binary
     * search of radials given an angle (which for 360 radials is about 8 searches per angle .
     * Memory cost: one Float, one Radial reference per Radial, typically O(365) 
     * Speed cost: Typically 8 searches O(log(360)) */
    private float[] angleToRadial;
    /** Radials corresponding the angleToRadial above (these are sorted by increasing end azimuth). */
    private Radial[] azimuthRadials;

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

    /** Try to create a RadialSet by reflection.  This is called from NetcdfBuilder by reflection	
     * @param ncfile	the netcdf file to read from
     * @param sparse 	did we come from a "SparseRadialSet"?
     */
    public RadialSet(NetcdfFile ncfile, boolean sparse) {

        super(ncfile, sparse); // Let DataType fill in the basics

        try {
            Variable v_az = ncfile.findVariable("Azimuth");
            Variable v_bw = ncfile.findVariable("BeamWidth");
            Variable v_as = ncfile.findVariable("AzimuthalSpacing");
            Variable v_gw = ncfile.findVariable("GateWidth");
            Variable v_ny = ncfile.findVariable("NyquistVelocity");
            float elev = ncfile.findGlobalAttribute("Elevation").getNumericValue().floatValue();
            float distToFirstGate = ncfile.findGlobalAttribute("RangeToFirstGate").getNumericValue().floatValue();
            float nyquist = DataType.MissingData;

            if (attributes.containsKey("Nyquist_Vel")) {
                nyquist = Float.parseFloat(attributes.get("Nyquist_Vel"));
            }
            Array az_values = v_az.read();
            Array bw_values = v_bw.read();
            Array gw_values = v_gw.read();

            // optional
            Array as_values = null;
            if (v_as != null) {
                as_values = v_as.read();
            }
            Array ny_values = null;
            if (v_ny != null) {
                ny_values = v_ny.read();
            }

            // Valid for all info but the radials
            this.elevation = elev;
            this.rangeToFirstGate = distToFirstGate / 1000;
            // set up the co-ordinate system
            this.radarLocation = originLocation.getCPoint();
            this.myUz = radarLocation.minus(new CPoint(0, 0, 0)).unit();
            this.myUx = new CVector(0, 0, 1).crossProduct(myUz).unit();
            this.myUy = myUz.crossProduct(myUx);

            Array2Dfloat values = null;
            int num_radials = 0;
            try {
                values = sparse ? NetcdfBuilder.readSparseArray2Dfloat(ncfile, typeName, myDataTypeMetric)
                        : NetcdfBuilder.readArray2Dfloat(ncfile, typeName, myDataTypeMetric);
                num_radials = values.getX();
            } catch (Exception e) {
                // If we can't get the values for any reason, 
                // just make a zero size radial set (nice recovery)
                log.warn("Couldn't create radials of radial set, leaving as empty");
            }

            this.radials = new Radial[num_radials];
            Index radial_index = az_values.getIndex();
            for (int i = 0; i < num_radials; ++i) {
                radial_index.set(i);
                float az = az_values.getFloat(radial_index);
                float bw = bw_values.getFloat(radial_index);
                float as = (as_values == null) ? bw : as_values.getFloat(radial_index);
                float gw = gw_values.getFloat(radial_index) / 1000; // meters to kms
                float ny = (ny_values == null) ? nyquist : ny_values.getFloat(radial_index);

                // This wraps around the column of the 2D array, _not_ a copy
                Array1Dfloat col = values.getCol(i);
                if (col != null) {
                    if (myMaxGateNumber < col.size()) {
                        myMaxGateNumber = col.size();
                    }
                }
                radials[i] = new Radial(az, bw, as, gw, ny, col, i);
            }
        } catch (Exception e) { // FIXME: what to do if anything?
            log.warn("Couldn't create radial set from netcdf file");
        }
        createAzimuthSearch();
    }

    public RadialSet(float elevation, Location radarLoc, Date scanTime,
            float rangeToFirstGate,
            String typeName, Radial[] radials) {
        super(radarLoc, scanTime, typeName);
        this.radials = radials;
        this.elevation = elevation;
        this.rangeToFirstGate = rangeToFirstGate;
        // set up the co-ordinate system
        this.radarLocation = originLocation.getCPoint();
        myUz = radarLocation.minus(new CPoint(0, 0, 0)).unit();
        myUx = new CVector(0, 0, 1).crossProduct(myUz).unit();
        myUy = myUz.crossProduct(myUx);
        createAzimuthSearch();
    }

    /** Create a sorted list of end azimuth numbers, which allows us to binary
     * search for a Radial by azimuth very quickly
     */
    protected void createAzimuthSearch() {
        // This assumes no two radials have the same end angle, even if they do,
        // should still work, just indeterminate which of the 2 radials you'll get
        angleToRadial = new float[radials.length];
        azimuthRadials = Arrays.copyOf(radials, radials.length);

        // Sort the azimuth radials by end angle...
        Arrays.sort(azimuthRadials, new Comparator<Radial>() {

            @Override
            public int compare(Radial o1, Radial o2) {
                double u1 = o1.getEndAzimuth();
                double u2 = o2.getEndAzimuth();
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
            angleToRadial[i] = azimuthRadials[i].getEndAzimuth();
        }
    }

    /** Return a double used to sort a volume of this DataType.  For example,
     * for RadialSets this would be the elevation value.
     * @return value in volume
     */
    @Override
    public double sortInVolume() {
        return elevation;
    }

    public float getElevation() {
        return elevation;
    }

    /** meters */
    public float getRangeToFirstGateKms() {
        return rangeToFirstGate;
    }

    /**
     * beamwidth of first radial, or 1 degree if there is no radial.
     */
    public float getBeamWidth() {
        if (radials.length > 0) {
            return getRadial(0).getBeamWidth();
        }
        return 1;
    }

    /**
     * nyquist of first radial, or 0 if there is no radial.
     */
    public float getNyquist() {
        if (radials.length > 0) {
            return getRadial(0).getNyquist();
        }
        return 0;
    }

    /**
     * mean azimuthal spacing of all radials.
     */
    public float getAzimuthalSpacing() {
        float as = 0;
        for (Radial r : radials) {
            as += r.getAzimuthalSpacing();
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
        return radials.length;
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
        public double azimuth;
        /** Angle in degrees */
        public double elev;
        /** Distance in Kms */
        public double range;
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
        out.azimuth = Radial.normalizeAzimuth((float) az);

        double dotz = coneX * myUz.x + coneY * myUz.y + coneZ * myUz.z;
        out.elev = Math.asin(dotz / out.range) * 180 / Math.PI;
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

            q.outAzimuthDegrees = (float) a.azimuth;

            // Get the elevation of the point (estimate)
            if (q.inUseHeight) {
                Radial first = getRadial(0);
                double bw = first.getBeamWidth();
                //double elev = Math.asin(fromConeApex.dotProduct(myUz) / norm) * 180 / Math.PI;
                double elev_diff = a.elev - elevation;

                // Beam width filter.  Outside beam width in the vertical?
                if (Math.abs(elev_diff) > bw / 2.0) {
                    q.outDataValue = MissingData;

                    // FIXME: Should we still get this stuff anyway?  Will slow down volumes like vslice
                    // Probably need more in flags
                    q.outHitRadialNumber = -1;
                    q.outHitGateNumber = -1;
                    q.outInAzimuth = false;
                    q.outInRange = false;
                    return;
                }
            }

            // Search radials by end azimuth
            int index = Arrays.binarySearch(angleToRadial, (float) a.azimuth);
            int radialIndex = (index < 0) ? -(index + 1) : index;

            if ((radialIndex >= 0) && (radialIndex < angleToRadial.length)) { // within all radial end values
                Radial candidate = azimuthRadials[radialIndex];
                q.outHitRadialNumber = candidate.getIndex();
                q.outInAzimuth = candidate.contains((float) a.azimuth);

                // Gate calculation.  Assumes all gates the same width.
                int gateNumber = (int) Math.floor((a.range - this.getRangeToFirstGateKms()) / candidate.getGateWidthKms());
                q.outHitGateNumber = gateNumber;

                // Is the query within range for this Radial?  Range is up to the last piece of available data.
                Array1Dfloat gates = candidate.getValues();
                q.outInRange = (gateNumber >= 0) && (gateNumber < gates.size());

                // Valid data must be in azimuth and range...? Note for vslice this will make black gaps in azimuth
                //if ((q.outInAzimuth) && (q.outInRange)){
                if (q.outInRange) {
                    q.outDataValue = gates.get(gateNumber);
                    return;
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
        for (Radial r : azimuthRadials) {
            float aglRadians = (float) Math.toRadians((r.getMidAzimuth() - dirDegrees));
            float vr = (float) (Math.cos(aglRadians) * speedMS);
            vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
            srmDeltas.add(vr);
        }
        return srmDeltas;
    }

    /** debugging output */
    @Override
    public String toStringDB() {
        String s = "RadialSet " + getTypeName() + " at " + elevation + " has "
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
                float azDegs = aRadial.getStartAzimuth();
                return (String.format("%6.2f", azDegs));
            }
        }
        return ("");
    }

    @Override
    public float getCellValue(int row, int col) {
        float value = 0;
        Radial r = getRadial((col));
        if (r != null) {
            int count = getNumGates();
            if (count > 0) {
                value = r.getValue((count - row - 1));
            }
        }
        return value;
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
                double elevationRAD = getElevation() * RAD; // Make
                Location center = getRadarLocation();
                double sinElevAngle = Math.sin(elevationRAD);
                double cosElevAngle = Math.cos(elevationRAD);
                int irow = getNumRows() - row - 1;
                float rangeKms = getRangeToFirstGateKms();
                float w = r.getGateWidthKms();
                float startRAD = r.getStartAzimuth() * RAD;
                float endRAD = r.getEndAzimuth() * RAD;

                // FIXME clean this up.
                switch (type) {
                    case CENTER: {
                        float fullRangeKms = rangeKms + (w * ((irow) + .5f));

                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin((startRAD + endRAD) / 2.0f), // Precomputed azimuth sin and cos around	
                                Math.cos((startRAD + endRAD) / 2.0f), fullRangeKms, // at range in kilometers
                                sinElevAngle, // sin of the elevation (precomputed)
                                cosElevAngle // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                sinElevAngle, // sin of the elevation (precomputed)
                                cosElevAngle // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                sinElevAngle, // sin of the elevation (precomputed)
                                cosElevAngle // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case BOTTOM_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                sinElevAngle, // sin of the elevation (precomputed)
                                cosElevAngle // cos of the elevation (precomputed)
                                );
                    }
                    break;

                    case BOTTOM_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                sinElevAngle, // sin of the elevation (precomputed)
                                cosElevAngle // cos of the elevation (precomputed)
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