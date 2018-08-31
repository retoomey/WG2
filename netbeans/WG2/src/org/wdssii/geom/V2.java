package org.wdssii.geom;

import org.wdssii.log.LoggerFactory;

/**
 *
 * @author Robert Toomey
 */
public class V2 {

	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(V2.class);

	/** Radius of earth in kilometers (matching WDSS2 projection) */
	public static final double EARTH_RADIUS_KMS = V3.EARTH_RADIUS_KMS;

	/** The x value of the point or vector */
	public double x;
	/** The y value of the point or vector */
	public double y;

	/** Assign a triple value point/vector */
	public V2(double a, double b) {
		x = a;
		y = b;
	}

	/** Copy constructor */
	public V2(V2 o) {
		x = o.x;
		y = o.y;
	}

	/** Set value unconditionally */
	public V2 set(double a, double b) {
		x = a;
		y = b;
		return this;
	}

	public V2 set(V2 o) {
		x = o.x;
		y = o.y;
		return this;
	}

	/** Subtract from V2 */
	public V2 minus(double a, double b) {
		x = x - a;
		y = y - b;
		return this;
	}

	/** Subtract another point vector */
	public V2 minus(V2 p) {
		x = x - p.x;
		y = y - p.y;
		return this;
	}

	/** Add to V2 */
	public V2 plus(double a, double b) {
		x = x + a;
		y = y + b;
		return this;
	}

	/** Add another point vector */
	public V2 plus(V2 p) {
		x = x + p.x;
		y = y + p.y;
		return this;
	}

	/** Multiple to V2 */
	public V2 times(double a, double b) {
		x = x * a;
		y = y * b;
		return this;
	}

	/** Multiple another point vector */
	public V2 times(V2 o) {
		x = x * o.x;
		y = y * o.y;
		return this;
	}

	/** Multiple by a constant */
	public V2 times(double d) {
		x = x * d;
		y = y * d;
		return this;
	}

	/** Divide by a constant */
	public V2 divide(double d) {
		x = x / d;
		y = y / d;
		return this;
	}

	/** Cross product with another V2 */
	/*
	 * V2 cross(V2 o) { final double nx = y * o.z - z * o.y; final double ny = z *
	 * o.x - x * o.z; final double nz = x * o.y - y * o.x; x = nx; y = ny; z = nz;
	 * return this; }
	 */

	/** Dot product with another V2 */
	public double dot(V2 o) {
		return (x * o.x + y * o.y);
	}

	/**
	 * Return norm^2 of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	public double normSquared() {
		return x * x + y * y;
	}

	/**
	 * Return norm of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	public double norm() {
		return Math.sqrt(x * x + y * y);
	}

	/** Get unit vector from point vector */
	public V2 newUnit() {
		// Turn point into unit
		final double len = Math.sqrt(x * x + y * y);
		if (len <= 0) {
			return new V2(this);
		}
		return new V2(x / len, y / len);
	}

	/**
	 * In place, turn point vector into its unit value. Can be used to save memory
	 */
	public V2 toUnit() {
		final double len = Math.sqrt(x * x + y * y);
		if (len != 0) {
			x = x / len;
			y = y / len;
		}
		return this;
	}

	// Location methods

	/**
	 * In place, turn a point vector (a point reference) into a Location where we
	 * have lat, lon and height. Lat in Degrees, Lon in Degrees and height in
	 * Kilometers. Note the cost of this is that you have to keep track mentally of
	 * what you have...
	 */
	/*
	 * public void toLocation() { final double r = Math.sqrt(x * x + y * y + z * z);
	 * final double lat = Math.toDegrees(Math.asin(z / r)); final double lon =
	 * Math.toDegrees(Math.atan2(y, x)); final double h = r - EARTH_RADIUS_KMS; x =
	 * lat; y = lon; z = h; }
	 */
	/**
	 * In place, turn a Location vector into a point reference (opposite of
	 * toLocation)
	 * 
	 */
	/*
	 * public void toPoint() { double r = V3.EARTH_RADIUS_KMS + z; double lonRadians
	 * = Math.toRadians(y); double latRadians = Math.toRadians(x); double cosLat =
	 * Math.cos(latRadians); x = r * Math.cos(lonRadians) * cosLat; y = r *
	 * Math.sin(lonRadians) * cosLat; z = r * Math.sin(latRadians); }
	 */

	/** As standing, is the point vector a valid lat lon location */
	public boolean isValidLocation() {
		final boolean r = ((x >= -90) && (x <= 90) && (y >= -180) && (y <= 180));
		if (!r) {
			LOG.error("Location is valid failed (" + x + "," + y + ")");
		}
		return r;
	}

	/**
	 * Assuming a perfect sphere earth, give great circle distance between two
	 * locations. Extremely accurate, if not precise. Note height of the locations
	 * is meaningless here, we are assuming both locations are on the earth surface
	 * 
	 * Note: we are storing location here
	 * 
	 * @param b Another location
	 * @return distance in kms
	 */
	public double getSurfaceDistanceToKMS(V2 b) {
		final double alat = Math.toRadians(x);
		final double blat = Math.toRadians(b.x);
		final double longdiff = Math.toRadians(y - b.y);
		final double d = Math.sin(alat) * Math.sin(blat) + Math.cos(alat) * Math.cos(blat) * Math.cos(longdiff);
		return Math.acos(d) * EARTH_RADIUS_KMS;
	}

	public double latDegrees() {
		return x;
	}

	public double lonDegrees() {
		return y;
	}

	@Override
	public String toString() {
		return "[ " + x + ", " + y + "]";
	}

	// More location math stuff (lots more routines)
	// azegll
	// RangeElev_to_Height
	// AzRange_to_XY
	// XY_to_AzRange
	// getLocation (length east, north, higher...)
	// XY_to_LL
}
