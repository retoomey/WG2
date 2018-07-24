package org.wdssii.gui;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.jfree.util.Log;
import org.wdssii.log.LoggerFactory;

/* D3
 * @author Robert Toomey
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
 */
final class D3 {
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

/**
 * wg_CameraPlane
 *
 * A plane in 3d space
 * FIXME: Do we really need a class for this? lol
 * 
 * @author Robert Toomey
 *
 */
final class CameraPlane
{
	/** First variable of a plane equation */
	public float a;

	/** Second variable of a plane equation */
	public float b;

	/** Third variable of a plane equation */
	public float c;

	/** Fourth variable of a plane equation */
	public float d;
};

/**
 * Like the WG camera state, keeps info for looking on an earth ball projection
 * 
 * @author Robert Toomey
 *
 */
final class CameraState {
	/** Distance between reference point and view point. */
	float r;

	/** Minimum distance between reference point and view point. */
	float r_limit;

	/** code::Angle of declination of view point from z-axis. */
	float theta;

	/** Azimuthal angle of view point about reference point. */
	float phi;

	/** Distance in km from view point to near clipping plane. */
	float near;

	/** Distance in km from view point to far clipping plane. */
	float far;

	/** Reference object's x-axis direction. */
	D3 ref_ux = new D3(1, 0, 0);

	/** Reference object's y-axis direction. */
	D3 ref_uy = new D3(0, 1, 0);

	/** Reference object's z-axis direction. */
	D3 ref_uz = new D3(0, 0, 1);

	/** View point. */
	D3 view = new D3(0,0,0);

	/** Reference point. */
	D3 ref = new D3(0,0,0);

	/** Screen's up direction (for gluLookAt). */
	D3 up = new D3(0,0,0);

	/** Field of view. */
	double fovDegrees = 45.0;

}


public class GLBoxCamera  {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(GLBoxCamera.class);

	/** The point we are looking at on the earth's surface in orbit view. 
	 * (0,0,0) is the center of the earth in our coordinate system.
	 *  +++ This is wrong..needs to be an earth surface point 
	 *  currently goToLocation initializes
	 */
	D3 myLOP = new D3(0,0,0);

	/** The min distance we allow into the LOP (in kilometers).
	 * Let's stay above the earth surface with a bit extra so maps
	 * and textures don't disappear
	 */
	float myMinDistance= 1.0f;

	/** The max distance we allow from the LOP (in kilometers) 
	 * Let's not get too crazy far out.  Unless I really do
	 * put an openGL moon easter egg out there of course.. ;)
	 */
	float myMaxDistance=(float) (D3.EARTH_RADIUS_KMS*3);

	/** The tilt angle our true view point differs from true 90 */
	float myTilt = 0.00f;

	/** The compass heading angle in radians. 0 degrees is north,
	 * 180 is south. */
	float myCompass = 0.00f;

	/** Do we all tilt to work? */
	boolean myAllowTilt = true;

	/** Debugging stuff */
	D3 ux = new D3(0,0,0);
	D3 uy = new D3(0,0,0);
	D3 uz = new D3(0,0,0);
	D3 ut = new D3(0,0,0);

	CameraState myCameraState = new CameraState();

	CameraPlane[] myPlanes = new CameraPlane[6];

	private boolean myValidLastDown = false;
	private D3 myLastDown = new D3(0,0,0);
	private D3 myLastDownDelta = new D3(0,0,0);

	/** Wheel time.  Used to allow actions to 'wait' when a mouse wheel
	 * occurs.  Wheels happen in 'bursts' typically */
	// wg_TimeCheck myWheelTime;

	/** Camera time.  Used to allow actions to 'wait' when camera changes
	 * occur. Camera changes happen in 'bursts' typically  */
	// wg_TimeCheck myChangeTime;


	public GLBoxCamera(boolean enableTilt) {
		//super(enableTilt);

		// Permanent camera settings for now at least
		myCameraState.r_limit = myMinDistance;
		// myCameraState.fovDegrees =  45;
		//  myCameraState.ref_ux = new D3( 1, 0, 0 );
		//  myCameraState.ref_uy = new D3( 0, 1, 0 );
		// myCameraState.ref_uz = new D3( 0, 0, 1 );

	}

	//public boolean cameraAction(wg_MouseEvent e) {};
	/** Jumps the 3D view to this location.  -1 for any param means to 
	 * ignore changing the setting for that field */
	public void goToLocation(float lon, float lat, float length,
			float rotateRadians, float tiltRadians ) {

		// Any or all fields can be changed by goToLocation.  Use -1.0 for
		// fields not wanted

		// Length of viewing stick
		if (length != -1.0) { myCameraState.r  = length; }

		// Latitude
		if (lat != -1.0){ myCameraState.theta = (float) ((90.0 - lat)*Math.PI/180.0); }

		// Longitude
		if (lon != -1.0){ myCameraState.phi   = (float) (lon *Math.PI/180.0); }

		// Back tilt
		if (tiltRadians != -1.0){ myTilt   = tiltRadians; }

		// Rotation from north
		if (rotateRadians != -1.0){ myCompass = rotateRadians; }

		// This VALIDATES all current settings as well as sets the camera
		cameraChanged();

	}

	/** Drag Pan the world camera (like google earth) */
	public boolean dragPan(int dx, int dy, boolean shift) {
		//LOG.error("Drag pan BY " +dx+", " +dy);

		pan(dx, dy, shift);
		return false;
	}

	/** Pan the world camera */
	public void pan(int dx, int dy, boolean shift) {

		//LOG.error("PANNING BY " +dx+", " +dy);
		float lon, lat, length, rotateRadians, tiltRadians;
		//getCamera(lon, lat, length, rotateRadians, tiltRadians);
		lat = (float) (90.0-(myCameraState.theta*180.0)/Math.PI);
		lon = (float) ((myCameraState.phi*180.0 )/Math.PI);
		int factor =(int) lon / 360;
		lon = (float) (lon - factor * 360.0);
		if( lon <= -180 ) lon += 360;
		if( lon > +180 ) lon -= 360;
		length = myCameraState.r;
		rotateRadians = myCompass;
		tiltRadians = myTilt;

		dx = (int) ((Math.abs(dx) <= 10) ? dx: (dx / Math.log10(Math.abs(dx*1000.0))));
		dy = (int) ((Math.abs(dy) <= 10) ? dy: (dy / Math.log10(Math.abs(dy*1000.0))));
		// mouse sensitivity speed
		float speedX = 0.01f;

		//LOG.error("Shift and allowtilt "+shift +"," + myAllowTilt);
		// With the shift key, we allow changing of compass and tilt
		if (shift && myAllowTilt) {
			myCompass += (-dx*speedX);
			myTilt += (dy*speedX);
			cameraChanged();  // Seems inconsistent from below...
		}else {
			if (myValidLastDown) {
				//LOG.error("Moving location right? ");
				float lon1 = (float) myLastDown.y;
				float lon2 = (float) myLastDownDelta.y;
				float lat1 = (float) myLastDown.x;
				float lat2 = (float) myLastDownDelta.x;

				goToLocation(lon+(lon1-lon2), lat+(lat1-lat2), -1, myCompass, myTilt);
				// ^^^^  calls cameraChanged()..  
			}

		}
	}

	/** Zoom the world camera */
	boolean zoom(int dx, int dy, boolean shift) {
		// dx is ignored for now, so we don't need to adjust it.
		dy = (int) ((Math.abs(dy) <= 10) ? dy :
			(dy / Math.log10(Math.abs(dy*10))));
//LOG.error("STATIC " +dy + ", "+ Math.abs(dy*10) +", "+ (dy / Math.log10(Math.abs(dy*10)))); 
		// We use an exponential zoom for easier UI, but make sure
		// we can't just keep zooming in forever, never reaching
		// myCameraState.r_limit.
		float newdistance = (float) ((myCameraState.r) * Math.exp( -dy/100.0 ));

		//  Pin myCameraState.r to be no less than myCameraState.r_limit.
		myCameraState.r = (newdistance <= myCameraState.r_limit) ?
				myCameraState.r_limit : newdistance;
		cameraChanged(); 
		return true;


	}

	/** Get the current camera state */
	/* Harder in java well kinda
	 *    void getCamera( float& lon, float& lat, float& length,
	 *        float& rotateRadians, float& tiltRadians ) const;
	 *        */
	public void cameraChanged() {

		// --------------------------------------------------------------------------
		// Camera validation.  Check all independent variables
		//
		if( myCameraState.r > myMaxDistance){
			myCameraState.r = (float) (myMaxDistance-.001);
			LOG.error("Zoomed out too far");
		}
		if( myCameraState.r < myMinDistance){
			myCameraState.r = (float) (myMinDistance+.001);
			LOG.error("Zoomed in too close");
		}

		// Keep the rotation value between 0 and 360 degrees

		if (myCompass < 0) { myCompass = (float) (myCompass+2*Math.PI); }
		if (myCompass > 2*Math.PI) { myCompass = (float) (myCompass-2*Math.PI); }

		// The distance we can tilt back is 0 to 90 degrees
		if (myTilt < 0.0) { myTilt = 0.0f; }
		if (myTilt > Math.PI/2) { myTilt = (float) (Math.PI/2); }

		// Keep theta and phi in value ranges north/south, east/west
		if ( myCameraState.theta < 0.01 ) { myCameraState.theta = (float) 0.01; }
		if ( myCameraState.theta > Math.PI-0.01) { myCameraState.theta = (float) (Math.PI-0.01); }

		if ( myCameraState.phi < 0) { myCameraState.phi += 2*Math.PI; }else
			if ( myCameraState.phi > 2*Math.PI) { myCameraState.phi -= 2*Math.PI; }

		//--------------------------------------------------------------------------
		// Rotation of view due to Earth theta (north/south) and phi (east/west)
		//
		float er = (float) D3.EARTH_RADIUS_KMS;
		float t = myCameraState.theta;
		float p = myCameraState.phi;
		float height = myCameraState.r;

		final double first = Math.sin(t)*Math.cos(p);
		final double second = Math.sin(t)*Math.sin(p);
		final double third = Math.cos(t);

		final D3 ruv = new D3(first, second, third);
		final D3 erv = new D3(ruv).times(er);
		// erv.times(er);

		// The surface point is out on this vector a distance of er.
		// We could add to this here if needed to 'float' a bit
		// code::CPoint surfacePoint = code::CPoint(0,0,0) + (er*ruv);
		// myLOP = surfacePoint;
		// myCameraState.ref = myLOP; // Always look at this point
		//

		// Look at the surface point on the earth
		//myCameraState.ref = myLOP = code::CPoint(erv.x, erv.y, erv.z);
		myCameraState.ref.set(erv);
		myLOP.set(erv);

		//  Construct the coordinate system associated with the reference point
		uz.set(erv).toUnit();
		//ux = ( code::CVector( 0, 0, 1 ) % uz ).unit();
		ux.set(0, 0, 1).cross(uz).toUnit();
		// uy = uz % ux; // Y up on screen at center of screen
		uy.set(uz).cross(ux);

		//--------------------------------------------------------------------------
		// Rotation of compass (myCompass 0 to 2*M_PI)
		//
		//myCameraState.up = uy  (Use just this line to disable rotating)

		// Rotate in y/x direction
		//  ut = uy*Math.cos( myCompass ) + ux*Math.sin( myCompass );
		//final D3 uxc = new D3(ux).times(Math.sin(myCompass));
		ut.set(uy).times(Math.cos(myCompass)).plus(new D3(ux).times(Math.sin(myCompass))).toUnit();

		myCameraState.up.set(ut);

		//--------------------------------------------------------------------------
		// Tilt back. (myTilt 0 to 90)
		//
		//code::CVector ub = ut*cos( myTilt ) + uz*sin( myTilt );
		final double cosT = Math.cos(myTilt);
		final double sinT = Math.sin(myTilt);

		final D3 ub = new D3(ut).times(cosT).plus(new D3(uz).times(sinT));

		myCameraState.up.set(ub);// new up vector tilts forward..
		// tilt view vector as well
		//code::CVector uv2 = uz*cos( myTilt ) + -ut*sin( myTilt );
		final D3 uv2 = new D3(uz).times(cosT).minus(new D3(ut).times(sinT)).times(height);

		// Finally, set the view point and the wg_GLBox camera to us
		//myCameraState.view = code::CPoint(0,0,0)
		//        + (er*ruv)      // Move view to the earth's surface
		//        + (height*uv2); // ...then move along tilt line to height
		myCameraState.view.set(erv).plus(uv2);

		calculateClippingPlanes();

		// Reset camera change time
		//myChangeTime = wg_TimeCheck();
		//myChangeTime.startAlways();

	}

	/** Calculate the clipping box of a GL view */
	public void calculateClippingPlanes()
	{
		//  distance from view point to reference point
		//double dvr = ( myCameraState.view - myCameraState.ref).norm();
		D3 temp = new D3(myCameraState.view).minus(myCameraState.ref);
		final double dvr = temp.norm();

		//  distance from center of earth to reference point
		//double dcr = ( myCameraState.ref - code::CPoint( 0, 0, 0 ) ).norm();

		//  distance from center of earth to reference point
		final double dcr = myCameraState.ref.norm();

		//  earth radius
		//const double er = code::Constants::EarthRadius().kilometers();
		final double er = D3.EARTH_RADIUS_KMS;

		if( dcr < 1 && dvr < 2*er )
		{
			//  The reference point is at the center of the earth, and the view
			//  point is nearer to the surface of the earth than it is to the
			//  center of the earth.

			double h = dvr - er;

			myCameraState.near = (float) (h/2.0);
			myCameraState.far  = (float) (h*2.0);
		}else{
			//  Either the reference point is not at the center of the earth,
			//  or the view point is farther from the surface of the earth than
			//  it is from the center of the earth.

			myCameraState.near = (float) (dvr/32);
			myCameraState.far  = (float) (dvr*4);
		}

		// LOG.error("CAMERASTATE " +myCameraState.near +", "+myCameraState.far);

	}

	public void prepMouseDownFlags(GL glold, int x, int y, int dx, int dy)
	{
		// If left button down, need current earth surface as well as the delta
		// projected point, if any.  This is for 'google' panning
		// FIXME:  This is all flawed, projection to sphere is perfect, but
		// we get 'drift' of the drag location because of the conversions
		// internally between radians/degrees, etc.  You can see this by printing
		// out the lat/lon
		// if (e->leftButton){
		// FIXME:  really should update locationOnSphere to return fail/success,
		// but will require modifying more code, so for the moment check nan
		double gMeters = 0;
		//const code::Length g = code::Length::Meters( 0 );
		// code::Location loc = glc->locationOnSphere( g, e->x, e->y );
		D3 loc = locationOnSphere(glold, gMeters, x, y);

		//code::Location loc2 = glc->locationOnSphere(
		//  g, e->x+(e->dx), e->y+(e->dy));
		D3 loc2 = locationOnSphere(glold, gMeters, x+dx, y+dy);

		//LOG.error("POINT ONE " +loc.x +", " +loc.y+", "+loc.z);
		//LOG.error("POINT TWO " +loc2.x +", " +loc2.y+", "+loc2.z);

		myLastDown = loc;
		myLastDownDelta = loc2;
		//float lon1 = (myLastDown.getLongitude()).degrees();
		//float lon2 = (myLastDownDelta.getLongitude()).degrees();
		// float lat1 = (myLastDown.getLatitude()).degrees();
		float lat1 = (float) myLastDown.x;
		// float lat2 = (myLastDownDelta.getLatitude()).degrees();
		float lat2 = (float) myLastDownDelta.x;
		myValidLastDown = !((Float.isNaN(lat2) || Float.isNaN(lat1)));

		// }
	}
	public D3 locationOnSphere(GL glold, double heightKMS, int mousex, int mousey) {
		D3[] points = getPointsOnClippingPlanes(glold, mousex, mousey);
		// LOG.error("NEARA FAR " + points[0].x + ","+points[0].y + ", "+points[0].z + " and "+points[1].x
		//	      + ", " +points[1].y+", "+points[1].z);  
		//  points[0].x = -580; points[0].y = -5434; points[0].z = 3594.3;
		//  points[1].x = -872.73743; points[1].y = -4517.3978; points[1].z = 3664.2586;

		//LOG.error("NEAR FAR " + points[0].x + ","+points[0].y + ", "+points[0].z + " and "+points[1].x
		//		+ ", " +points[1].y+", "+points[1].z);
		// wg_GeometricCalc...kill me please it never ends...
		// ADD GeometricCalc
		D3 d = GeometricCalc.pointOnSphere(points[0], points[1], heightKMS);
		d.toLocation();  // Make the point a location
		//LOG.error("POINT ON SPHERE IS " + d.x +", " +d.y+", "+d.z);
		return d;
		//return new D3(0,0,0);
	}

	/** Given x and y in current view coordinates */
	public D3[] getPointsOnClippingPlanes(GL glold, int viewx, int viewy){
		//LOG.error("X AND Y IS " +viewx +", " +viewy);
		D3[] points = new D3[2];
		points[0] = new D3(0,0,0);
		points[1] = new D3(0,0,0);
		final GL2 gl = glold.getGL2();

		GLU glu = new GLU();
		// Think static is slightly faster...
		double model[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
		//double model[] = new double[16];
		double proj[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
		//double proj[] = new double[16];
		double xyz[] = {1, 2, 3};
		int view[] = {1, 2, 3, 4};

		gl.getContext().makeCurrent();
		//LOG.error("IS CURRENT " +gl.getContext().isCurrent());

		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, model, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, proj, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, view, 0);

		//LOG.error(model[0]+","+model[1]+","+model[2]+","+model[3]+","+model[4]);
		// The projection is always zero based, even if viewport is
		// translated...
		view[0] = view[1] = 0;

		boolean ret = glu.gluUnProject(viewx, viewy, 0.0, model, 0, proj, 0, view, 0, xyz, 0);
		//LOG.error("PROJECT BOOLEAN IS " + ret );
		points[0].set(xyz[0], xyz[1], xyz[2]);
		// LOG.error("PROJECT 1 "+xyz[0]+", "+xyz[1]+"," +xyz[2]);

		glu.gluUnProject(viewx, viewy, 1.0, model, 0, proj, 0, view, 0, xyz, 0);
		points[1].set(xyz[0], xyz[1], xyz[2]);
		//LOG.error("PROJECT 2 "+xyz[0]+", "+xyz[1]+"," +xyz[2]);
		return points;
	}

	public void normalizePlane(
			CameraPlane plane
			)
	{
		float mag = (float) Math.sqrt(plane.a*plane.a+plane.b*plane.b+plane.c*plane.c);
		plane.a = plane.a/mag;
		plane.b = plane.b/mag;
		plane.c = plane.c/mag;
		plane.d = plane.d/mag;
	}

	public void setUpCamera(GL glold, GLU glu, int x, int y, int width, int height)
	{
		// need near/far..not sure 'when' this should be done
		calculateClippingPlanes();

		GL2 gl = glold.getGL2();

		gl.glMatrixMode( GL2.GL_PROJECTION );
		gl.glLoadIdentity();
		double ratio = (double)width/(double)height;
		glu.gluPerspective( myCameraState.fovDegrees,
				ratio,
				myCameraState.near, myCameraState.far );

		// ---------------------------------------------------------------------
		// Look at a point in 3d space
		gl.glMatrixMode( GL2.GL_MODELVIEW );
		gl.glLoadIdentity();

		// Forward vector (Point your finger at place on earth ball)
		//code::CVector f = (myCameraState.ref-myCameraState.view).unit();
		D3 f = new D3(myCameraState.ref).minus(myCameraState.view).toUnit();

		// Right vector...(Stick your arm out to the right)
		//code::CVector s = (f%myCameraState.up).unit();
		D3 s = new D3(f).cross(myCameraState.up).toUnit();

		// Recalculate up vector (Stick your arm straight up in the air)
		// (gl docs say do this, probably ensures proper sign)
		//code::CVector u = s%f;
		D3 u = new D3(s).cross(f);

		// The matrix that does the work
		double m[] =
			{
					s.x, u.x, -f.x, 0,
					s.y, u.y, -f.y, 0,
					s.z, u.z, -f.z, 0,
					0, 0, 0, 1
			};
		gl.glMultMatrixd(m, 0);

		gl.glTranslated(-myCameraState.view.x, -myCameraState.view.y,
				-myCameraState.view.z);
		/* glu dependent code.
		  gluLookAt(
		      myCameraState.view.x, myCameraState.view.y, myCameraState.view.z,
		      myCameraState.ref.x,  myCameraState.ref.y,  myCameraState.ref.z,
		      myCameraState.up.x,   myCameraState.up.y,   myCameraState.up.z
		  );
		 */
		// ---------------------------------------------------------------------
		// Generate frustum planes
		// double proj[16], modl[16], clip[16];
		//  float[] proj = new float[16];
		//  float[] modl = new float[16];
		//  float[] clip = new float[16];
		float modl[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
		float proj[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
		float clip[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};

		gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, proj, 0);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modl, 0);

		// Faster than looping
		clip[0] = modl[0] * proj[0] + modl[1] * proj[ 4]
				+ modl[2] * proj[8] + modl[3] * proj[12];
		clip[1] = modl[0] * proj[1] + modl[1] * proj[5]
				+ modl[2] * proj[9] + modl[3] * proj[13];
		clip[2] = modl[0] * proj[2] + modl[1] * proj[ 6]
				+ modl[2] * proj[10] + modl[3] * proj[14];
		clip[3] = modl[0] * proj[3] + modl[1] * proj[7]
				+ modl[2] * proj[11] + modl[3] * proj[15];
		clip[4] = modl[4] * proj[0] + modl[5] * proj[4]
				+ modl[6] * proj[8] + modl[7] * proj[12];
		clip[5] = modl[4] * proj[1] + modl[5] * proj[5]
				+ modl[6] * proj[9] + modl[7] * proj[13];
		clip[6] = modl[4] * proj[2] + modl[5] * proj[6]
				+ modl[6] * proj[10] + modl[7] * proj[14];
		clip[7] = modl[4] * proj[3] + modl[5] * proj[7]
				+ modl[6] * proj[11] + modl[7] * proj[15];
		clip[8] = modl[8] * proj[0] + modl[9] * proj[4]
				+ modl[10] * proj[8] + modl[11] * proj[12];
		clip[9] = modl[8] * proj[1] + modl[9] * proj[5]
				+ modl[10] * proj[9] + modl[11] * proj[13];
		clip[10] = modl[8] * proj[2] + modl[9] * proj[6]
				+ modl[10] * proj[10] + modl[11] * proj[14];
		clip[11] = modl[8] * proj[3] + modl[9] * proj[7]
				+ modl[10] * proj[11] + modl[11] * proj[15];
		clip[12] = modl[12] * proj[0] + modl[13] * proj[4]
				+ modl[14] * proj[8] + modl[15] * proj[12];
		clip[13] = modl[12] * proj[1] + modl[13] * proj[5]
				+ modl[14] * proj[9] + modl[15] * proj[13];
		clip[14] = modl[12] * proj[2] + modl[13] * proj[6]
				+ modl[14] * proj[10] + modl[15] * proj[14];
		clip[15] = modl[12] * proj[3] + modl[13] * proj[7]
				+ modl[14] * proj[11] + modl[15] * proj[15];

		if (myPlanes[0] == null) {
			myPlanes[0] = new CameraPlane();
		}
		// left clipping plane
		myPlanes[0].a = clip[3]+clip[0];
		myPlanes[0].b = clip[7]+clip[4];
		myPlanes[0].c = clip[11]+clip[8];
		myPlanes[0].d = clip[15]+clip[12];
		normalizePlane(myPlanes[0]);

		// right clipping plane
		if (myPlanes[1] == null) {
			myPlanes[1] = new CameraPlane();
		}
		myPlanes[1].a = clip[3]-clip[0];
		myPlanes[1].b = clip[7]-clip[4];
		myPlanes[1].c = clip[11]-clip[8];
		myPlanes[1].d = clip[15]-clip[12];
		normalizePlane(myPlanes[1]);

		// right clipping plane
		if (myPlanes[2] == null) {
			myPlanes[2] = new CameraPlane();
		}
		// top clipping plane
		myPlanes[2].a = clip[3]-clip[1];
		myPlanes[2].b = clip[7]-clip[5];
		myPlanes[2].c = clip[11]-clip[9];
		myPlanes[2].d = clip[15]-clip[13];
		normalizePlane(myPlanes[2]);
		// right clipping plane
		if (myPlanes[3] == null) {
			myPlanes[3] = new CameraPlane();
		}
		// bottom clipping plane
		myPlanes[3].a = clip[3]+clip[1];
		myPlanes[3].b = clip[7]+clip[5];
		myPlanes[3].c = clip[11]+clip[9];
		myPlanes[3].d = clip[15]+clip[13];
		normalizePlane(myPlanes[3]);

		// right clipping plane
		if (myPlanes[4] == null) {
			myPlanes[4] = new CameraPlane();
		}
		// far clipping plane
		myPlanes[4].a = clip[3]-clip[2];
		myPlanes[4].b = clip[7]-clip[6];
		myPlanes[4].c = clip[11]-clip[10];
		myPlanes[4].d = clip[15]-clip[14];
		normalizePlane(myPlanes[4]);
		// right clipping plane
		if (myPlanes[5] == null) {
			Log.error("Wow plane 5 was null how?");
			myPlanes[5] = new CameraPlane();
		}
		// near clipping plane (this plane should be last calculated)
		myPlanes[5].a = clip[3]+clip[2];
		myPlanes[5].b = clip[7]+clip[6];
		myPlanes[5].c = clip[11]+clip[10];
		myPlanes[5].d = clip[15]+clip[14];
		normalizePlane(myPlanes[5]);	  


	}

}
