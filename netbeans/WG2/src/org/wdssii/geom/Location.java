package org.wdssii.geom;

/**
 * @author lakshman
 * 
 */
public class Location extends V3 {
	public static final double EarthRadius = V3.EARTH_RADIUS_KMS;

	/** lat, lon in degrees and height in kilometers. */
	public Location(double lat, double lon, double ht) {
		super(lat, lon, ht);

		/** Force lat in range -90 to 89 */
		x = (mod(x + 90, 180)) - 90;

		/** Force lon in range -180 to 179 */
		y = (mod(y + 180, 360)) - 180.0;
	}
}
