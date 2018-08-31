package org.wdssii.gui;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.wdssii.geom.V3;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.charts.W2DataView;
import org.wdssii.log.LoggerFactory;

/**
 * GLWorld object for drawing openGL into a W2 standard view
 * 
 * @author Robert Toomey
 *
 */
public class W2GLWorld extends GLWorld {
	private final W2DataView myWorld;
	private GLU myGLU = new GLU();
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(W2GLWorld.class);

	double myModel[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
	double myProj[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
	int myView[] = { 1, 2, 3, 4 };
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

	/**
	 * Project from model space to the screen 2D location of it.
	 */
	@Override
	public V2 project(V3 a3d) {
		// Use GLU to project...assuming GL context is current. You need a current one
		// for result to make sense...
		double xyz[] = { 1, 2, 3 };
		myGLU.gluProject(a3d.x, a3d.y, a3d.z, myModel, 0, myProj, 0, myView, 0, xyz, 0);
		return new V2(xyz[0], xyz[1]);
	}

	@Override
	public V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters) {
	/*	// FIXME: Should probably stick in the merged V3/V3 class since it's just math
		double r = V3.EARTH_RADIUS_KMS + (heightMeters / 1000.0f);
		double lonRadians = Math.toRadians(lonDegrees);
		double latRadians = Math.toRadians(latDegrees);
		double cosLat = Math.cos(latRadians);
		double x = r * Math.cos(lonRadians) * cosLat;
		double y = r * Math.sin(lonRadians) * cosLat;
		double z = r * Math.sin(latRadians);
		*/
		V3 aV3 = new V3(latDegrees, lonDegrees, heightMeters/1000.0);
		aV3.toPoint();
		return new V3(aV3.x, aV3.y, aV3.z);
	}

	@Override
	public V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters) {
		/*// FIXME: Should probably stick in the merged V3/V3 class since it's just math
		double r = V3.EARTH_RADIUS_KMS + (heightMeters / 1000.0f);
		double lonRadians = Math.toRadians(lonDegrees);
		double latRadians = Math.toRadians(latDegrees);
		double cosLat = Math.cos(latRadians);
		double x = r * Math.cos(lonRadians) * cosLat;
		double y = r * Math.sin(lonRadians) * cosLat;
		double z = r * Math.sin(latRadians);
		return new V3(x, y, z);*/
		
		V3 aV3 = new V3(latDegrees, lonDegrees, heightMeters/1000.0);
		aV3.toPoint();
		return new V3(aV3.x, aV3.y, aV3.z);
	}

	@Override
	public V3 projectV3ToLLH(V3 in) {

		V3 aV3 = new V3(in.x, in.y, in.z); // FIXME: merge V3/V3 duh
		aV3.toLocation();

		// FIXME: merge V3/V3 duh
		return new V3(aV3.x, aV3.y, aV3.z);
	}

	@Override
	public V3 project2DToEarthSurface(double x, double y, double elevation) {

		// FIXME: This shares code with GLCamera clipping planes and projection stuff
		// GLCamera could 'use' glworld for this...
		V3[] points = new V3[2];
		points[0] = new V3(0, 0, 0);
		points[1] = new V3(0, 0, 0);
		double xyz[] = { 1, 2, 3 };

		myGLU.gluUnProject(x, y, 0.0, myModel, 0, myProj, 0, myView, 0, xyz, 0);
		points[0].set(xyz[0], xyz[1], xyz[2]);
		myGLU.gluUnProject(x, y, 1.0, myModel, 0, myProj, 0, myView, 0, xyz, 0);
		points[1].set(xyz[0], xyz[1], xyz[2]);

		V3 d = GeometricCalc.pointOnSphere(points[0], points[1], 0);
		return new V3(d.x, d.y, d.z);
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
		// return false;
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
