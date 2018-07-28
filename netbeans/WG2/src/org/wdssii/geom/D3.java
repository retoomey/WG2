package org.wdssii.geom;

import org.wdssii.log.LoggerFactory;

/* D3
 * @author Robert Toomey
 * 
 * In WDSSII we had CPoint, CVector, Location, displacement and DLHH
 * Don't think we actually need multiple classes.  This does a lot of
 * copying, etc. we don't really want.  I'm making a 'D3' which is
 * a dimension value of 3, which can store points, vectors and locations.
 * And I already have a triple V3...so I'll merge with that probably.
 * 
 * FIXME: make WDSSII use a combined D3 class as well.  This would make
 * the code really reduce down and save speed and memory.  Macros in the 
 * C++ might make the code clearer to read.
 * 
 * FIXME: Combine with the V3 class here in geom maybe.
 * 
 */
public class D3 {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(D3.class);

	/** Radius of earth in kilometers (matching WDSS2 projection) */
	public static final double EARTH_RADIUS_KMS = 6380;

	/** The x value of the point or vector */
	public double x;
	/** The y value of the point or vector */
	public double y;
	/** The z value of the point or vector */
	public double z;

	/** Assign a triple value point/vector */
	public D3(double a, double b, double c) {
		x = a;
		y = b;
		z = c;
	}

	/** Copy constructor */
	public D3(D3 o) {
		x = o.x;
		y = o.y;
		z = o.z;
	}

	/** Set value unconditionally */
	public D3 set(double a, double b, double c)
	{
		x = a;
		y = b;
		z = c;
		return this;
	}

	public D3 set(D3 o) {
		x = o.x;
		y = o.y;
		z = o.z;
		return this;
	}

	/** Subtract another point vector */
	public D3 minus(D3 p) {
		x = x - p.x;
		y = y - p.y;
		z = z - p.z;
		return this;
	}

	/** Add another point vector */
	public D3 plus(D3 p) {
		x = x + p.x;
		y = y + p.y;
		z = z + p.z;
		return this;
	}

	/** Multiple another point vector */
	public D3 times(D3 o) {
		x = x * o.x;
		y = y * o.y;
		z = z * o.z;
		return this;
	}

	/** Multiple by a constant */
	public D3 times(double d) {
		x = x * d;
		y = y * d;
		z = z * d;
		return this;
	}

	/** Divide by a constant */
	public D3 divide(double d) {
		x = x / d;
		y = y / d;
		z = z / d;
		return this;
	}

	/** Cross product with another D3 */
	public D3 cross(D3 o) {
		final double nx = y*o.z - z*o.y;
		final double ny = z*o.x - x*o.z;
		final double nz = x*o.y - y*o.x;
		x = nx;
		y = ny;
		z = nz;
		return this;
	}

	/** Dot product with another D3 */
	public double dot(D3 o) {
		return (x*o.x + y * o.y + z* o.z);
	}

	/**
	 * Return norm^2 of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	public double normSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * Return norm of PointVector. Note this value isn't cached.
	 * 
	 * @return norm
	 */
	public double norm() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** Get unit vector from point vector */
	public D3 newUnit() {
		// Turn point into unit
		final double len = Math.sqrt(x * x + y * y + z * z);
		if (len <= 0) {
			return new D3(this);
		}
		return new D3(x / len, y / len, z / len);
	}

	/**
	 * In place, turn point vector into its unit value. Can be used to save memory
	 */
	public D3 toUnit() {
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
	}

	/** As standing, is the point vector a valid lat lon location */
	public boolean isValidLocation() {
		final boolean r = ((x >= -90) && (x <= 90) && (y >= -180) && (y <= 180));
		if (!r) {
			LOG.error("Location is valid failed (" + x + "," + y + "," + z + ")");
		}
		return r;
	}

	/** Assuming a perfect sphere earth, give great circle
	 * distance between two locations.  Extremely accurate, if
	 * not precise.  Note height of the locations is meaningless here, we
	 * are assuming both locations are on the earth surface
	 * @param b Another location
	 * @return distance in kms
	 */
	public double getSurfaceDistanceToKMS(D3 b) 
	{
		final double alat = Math.toRadians(x);
		final double blat = Math.toRadians(b.x);
		final double longdiff = Math.toRadians(y-b.y);
		final double d = Math.sin(alat)*Math.sin(blat)+
				Math.cos(alat)*Math.cos(blat)*Math.cos(longdiff);
		return Math.acos(d)*EARTH_RADIUS_KMS;	
	}

	// More location math stuff (lots more routines)
	// azegll
	// RangeElev_to_Height
	// AzRange_to_XY
	// XY_to_AzRange
	// getLocation (length east, north, higher...)
	// XY_to_LL

}