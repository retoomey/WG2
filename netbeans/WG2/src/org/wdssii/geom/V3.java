package org.wdssii.geom;

import org.wdssii.log.LoggerFactory;

/**
 * Our generic point/vector object for the display.
 * 
 * Going to move to this to reduce some of the library coupling. This type of
 * object is created in pretty much every library out there. In WDSSII we had
 * CPoint, CVector, Location, displacement and DLHH. Don't think we actually
 * need multiple classes. This does a lot of copying, etc. we don't really want.
 * I'm making a 'V3' which is a dimension value of 3, which can store points,
 * vectors and locations. Note not using templates because Float more memory
 * than float.
 * 
 * The advantage to multiple classes is you know exactly what you are. However,
 * since we transform a lot between say spherical and cartesian this slows us a
 * lot, so I think having a debug mode where we check the calls would be better
 * in this case.
 * 
 * @author Robert Toomey
 */
public class V3 extends V2 {

	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(V3.class);

	/** Radius of earth in kilometers (matching WDSS2 projection) */
	public static final double EARTH_RADIUS_KMS = 6380;

	/** The z value of the point or vector */
	public double z;

	/** Assign a triple value point/vector/location */
	public V3(double a, double b, double c) {
		super(a, b);
		z = c;
	}

	/** Copy constructor */
	public V3(V3 o) {
		super(o.x, o.y);
		z = o.z;
	}

	/** Set value unconditionally */
	public V3 set(double a, double b, double c) {
		x = a;
		y = b;
		z = c;
		return this;
	}

	public V3 set(V3 o) {
		x = o.x;
		y = o.y;
		z = o.z;
		return this;
	}

	/** Subtract values */
	public V3 minus(double a, double b, double c) {
		x = x - a;
		y = y - b;
		z = z - c;
		return this;
	}

	/** Subtract another point vector */
	public V3 minus(V3 p) {
		x = x - p.x;
		y = y - p.y;
		z = z - p.z;
		return this;
	}

	/** Add values */
	public V3 plus(double a, double b, double c) {
		x = x + a;
		y = y + b;
		z = z + c;
		return this;
	}

	/** Add another point vector */
	public V3 plus(V3 p) {
		x = x + p.x;
		y = y + p.y;
		z = z + p.z;
		return this;
	}

	/** Multiple another point vector */
	public V3 times(V3 o) {
		x = x * o.x;
		y = y * o.y;
		z = z * o.z;
		return this;
	}

	/** Multiple by a constant */
	public V3 times(double d) {
		x = x * d;
		y = y * d;
		z = z * d;
		return this;
	}

	/** Divide by a constant */
	public V3 divide(double d) {
		x = x / d;
		y = y / d;
		z = z / d;
		return this;
	}

	/** Cross product with another V3 */
	public V3 cross(V3 o) {
		final double nx = y * o.z - z * o.y;
		final double ny = z * o.x - x * o.z;
		final double nz = x * o.y - y * o.x;
		x = nx;
		y = ny;
		z = nz;
		return this;
	}

	/** Dot product with another V3 */
	public double dot(V3 o) {
		return (x * o.x + y * o.y + z * o.z);
	}

	/**
	 * Return norm^2 of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	@Override
	public double normSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Return norm of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	@Override
	public double norm() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** Get unit vector from point vector */
	public V3 newUnit() {
		// Turn point into unit
		final double len = Math.sqrt(x * x + y * y + z * z);
		if (len <= 0) {
			return new V3(this);
		}
		return new V3(x / len, y / len, z / len);
	}

	/**
	 * In place, turn point vector into its unit value. Can be used to save memory
	 */
	public V3 toUnit() {
		final double len = Math.sqrt(x * x + y * y + z * z);
		if (len != 0) {
			x = x / len;
			y = y / len;
			z = z / len;
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
	public void toLocation() {
		final double r = Math.sqrt(x * x + y * y + z * z);
		final double lat = Math.toDegrees(Math.asin(z / r));
		final double lon = Math.toDegrees(Math.atan2(y, x));
		final double h = r - EARTH_RADIUS_KMS;
		x = lat;
		y = lon;
		z = h;

		/** Force lat in range -90 to 89 */
		x = (mod(x + 90, 180)) - 90;

		/** Force lon in range -180 to 179 */
		y = (mod(y + 180, 360)) - 180.0;
	}

	/**
	 * In place, turn a Location vector into a point reference (opposite of
	 * toLocation)
	 * 
	 */
	public void toPoint() {
		double r = V3.EARTH_RADIUS_KMS + z;
		double lonRadians = Math.toRadians(y);
		double latRadians = Math.toRadians(x);
		double cosLat = Math.cos(latRadians);
		x = r * Math.cos(lonRadians) * cosLat;
		y = r * Math.sin(lonRadians) * cosLat;
		z = r * Math.sin(latRadians);
	}

	/** Initialize as a lat/lon trimmed location. FIXME: bad function name */
	public final void init(double lat, double lon, double ht) {

		/** Force lat in range -90 to 89 */
		x = (mod(lat + 90, 180)) - 90;

		/** Force lon in range -180 to 179 */
		y = (mod(lon + 180, 360)) - 180.0;
		z = ht;

		// Contours has location objects with height 0
		// if (lat < -90 || lat > 90 || lon < -180 || lon > 180 || ht < -0.01)
		// if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
		// throw new IndexOutOfBoundsException("Invalid earth location" + this);
		// }

	}

	/** True modulus */
	protected final double mod(double x, double y) {
		double result = x % y;
		if (result < 0) {
			result += y;
		}
		return result;
	}

	/**
	 * Get the height in kilometers. Note we should be used a a lat, lon, height in
	 * this case
	 */
	public double getHeightKms() {
		return z;
	}

	@Override
	public String toString() {
		return "[ " + x + ", " + y + ", " + z + "]";
	}

	// More location math stuff (lots more routines)
	// azegll
	// RangeElev_to_Height
	// AzRange_to_XY
	// XY_to_AzRange
	// getLocation (length east, north, higher...)
	// XY_to_LL
}
