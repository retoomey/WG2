package org.wdssii.gui;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.wdssii.geom.D3;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.charts.W2DataView;

/** GLWorld object for drawing openGL into a W2 standard view
 * 
 * @author Robert Toomey
 *
 */
public class W2GLWorld extends GLWorld {
	private final W2DataView myWorld;
	private GLU myGLU = new GLU();
	
	double myModel[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
	double myProj[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
	int myView[] = {1, 2, 3, 4};
	private GLBoxCamera myCamera;
	
	public W2GLWorld(GL aGL, GLBoxCamera currentCam, int width, int height, W2DataView w) {
		super(aGL, width, height);
		final GL2 gl = aGL.getGL2();
		myWorld = w;
		myCamera = currentCam;

		gl.getContext().makeCurrent();
		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, myModel, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, myProj, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, myView, 0);
	}

	@Override
	public V2 project(V3 a3d) {
		// Use GLU to project...assuming GL context is current.  You need a current one
		// for result to make sense...
		double xyz[] = {1, 2, 3};
        myGLU.gluProject(a3d.x, a3d.y, a3d.z, myModel, 0, myProj, 0, myView, 0, xyz, 0);
        return new V2(xyz[0], xyz[1]);
	}

	@Override
	public V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters) {	
		double r = D3.EARTH_RADIUS_KMS + (heightMeters / 1000.0f);
		double phi = Math.toRadians(lonDegrees);
		double beta = Math.toRadians(latDegrees);
		double cos_beta = Math.cos(beta);
		double x = r * Math.cos(phi) * cos_beta;
		double y = r * Math.sin(phi) * cos_beta;
		double z = r * Math.sin(beta);
		return new V3(x, y, z);

	}

	@Override
	public V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters) {
		latDegrees = 20;

		double r = D3.EARTH_RADIUS_KMS + (heightMeters / 1000.0f);
		double phi = Math.toRadians(lonDegrees);
		double beta = Math.toRadians(latDegrees);
		double cos_beta = Math.cos(beta);
		double x = r * Math.cos(phi) * cos_beta;
		double y = r * Math.sin(phi) * cos_beta;
		double z = r * Math.sin(beta);
		
	/*	D3 aD3 = new D3(x,y,z);
		// We can pull back out the r since x,y,z is in earth distance space from center:
		//double r2 = (Math.sqrt(x*x+y*y+z*z)); //-D3.EARTH_RADIUS_KMS); // *1000.0;
		double r2 = aD3.norm();
		double heightMeters2 = (r2-D3.EARTH_RADIUS_KMS) *1000.0;
		
		// What is this thing?
		D3 bD3 = new D3(0,-1,0);
		double top = bD3.dot(aD3);
		double bottom = r2*bD3.norm();
		double cosangle = top/bottom;
		double someRad = Math.acos(cosangle);
		double someDeg = Math.toDegrees(someRad);	
		
		// double x = r * Math.cos(lonRadians) * Math.cos(latRadians);
		// x/r = cos(lonRadians) * cos(latRadians);
		// y/r = sin(lonRadians) * cos(latRadians);
		
		// x/(r*cos(lonRadians)) = y/(r*sin(lonRadians));
		// 
		
		// 

		// Just need calculate the angle vs a unit vector of the earth ball...I think...
		double lon = Math.atan(y/x);
		double lat = Math.atan(x/z);
		
		double t1 = Math.toDegrees(Math.atan(y/x));
		double t2 = Math.toDegrees(Math.atan(y/z));
		double t3 = Math.toDegrees(Math.atan(x/y));  // lon
		double t4 = Math.toDegrees(Math.atan(x/z));
		double t5 = Math.toDegrees(Math.atan(z/x));
		double t6 = Math.toDegrees(Math.atan(-z/y)); //lat is 0-90


		//double lonDegrees2 = someDeg;
		double lonDegrees2 = Math.toDegrees(lon);
		double latDegrees2 = Math.toDegrees(lat);
		// How to get lon degrees...
		//(cos(angle) = u dot v/(unitU*unitV) right?
		// x = 
		// To avoid atan infinity, get hypoo
		// xy,
		// xz,
		// yx,
		// yz,
		// zx,
		// zy
		
		double v1 = y;  // Lat
		double v2 = z;
		double h = Math.sqrt(v1*v1+v2*v2);  // if both are zero, angle is zero...need to check
		double lat2 = Math.asin(v2/h);  // H could be zero!
		latDegrees2 = Math.toDegrees(lat2);	
		
		double v3 = y;
		double v4 = x;
		double h2 = Math.sqrt(v3*v3+v4*v4);
		double lon2 = Math.asin(v4/h2);
		lonDegrees2 = Math.toDegrees(lon2);
		
		
		//System.out.println("LAT/LON/HEIGHT "+latDegrees+", "+lonDegrees+", "+heightMeters);
		//System.out.println("FOR POINT X, Y, Z "+x+", "+y+", "+z);
		//System.out.println("R was  "+r+ " and R back is "+r2);
		//System.out.println("LAT/LON/HEIGHT2 "+latDegrees2+", "+lonDegrees2+", "+heightMeters2);
		System.out.println("x and y "+x+", "+y);
		System.out.println("LON "+lonDegrees+", "+lonDegrees2);
		System.out.println("LAT "+latDegrees+", "+latDegrees2);
		System.out.println("GOOP "+t1+","+t2+","+t3+","+t4+","+t5+","+t6);
		//System.exit(1);
		 * 
		 */
		return new V3(x, y, z);
	}

	@Override
	public V3 projectV3ToLLH(V3 in) {
		
		// Need to do this backwards, can we?
	/*	double r = D3.EARTH_RADIUS_KMS + (heightMeters / 1000.0f);
		double phi = Math.toRadians(lonDegrees);
		double beta = Math.toRadians(latDegrees);
		double cos_beta = Math.cos(beta);
		double x = r * Math.cos(phi) * cos_beta;
		double y = r * Math.sin(phi) * cos_beta;
		double z = r * Math.sin(beta);
		*/
		// We have x, y, z need lat, lon, height...
		//
		// x = r * Math.cos(lonRad) * Math.cos(latRad);
		// y = r * Math.sin(lonRad) * Math.cos(latRad);
		// z = r * Math.sin(latRad);
		// r = K+(h/1000);
		
		// The height is the length from point to zero...we're in meter
		// space
		// d = Math.sqrt(x*x+y*y+z*z);
		
		
return null;
		
		
		
	
	}

	@Override
	public V3 project2DToEarthSurface(double x, double y, double elevation) {
		// TODO Auto-generated method stub
		double xyz[] = {1, 2, 3};
        myGLU.gluUnProject(x, y, 0.0, myModel, 0, myProj, 0, myView, 0, xyz, 0);
        return new V3(xyz[0], xyz[1], xyz[2]);
	}

	@Override
	public float getElevation(float latDegrees, float lonDegrees) {
		return 0; // WG is flat as a pancake sphere
	}

	@Override
	public double getElevation(double latDegrees, double lonDegrees) {
		return 0; // Flat
	}

	@Override
	public boolean isPickingMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getVerticalExaggeration() {
		return 0;
	}

	@Override
	public boolean inView(V3 a3d) {
		// TODO Auto-generated method stub
		//return false;
		return true;
	}

	@Override
	public void redraw() {
		myWorld.repaint();
	}

	/** The camera math for current render */
	public GLBoxCamera getCamera() {
		return myCamera;
	}
}
