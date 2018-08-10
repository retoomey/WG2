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
	
	public W2GLWorld(GL aGL, int width, int height, W2DataView w) {
		super(aGL, width, height);
		final GL2 gl = aGL.getGL2();
		myWorld = w;

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
	public V3 projectV3ToLLH(V3 in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V3 project2DToEarthSurface(double x, double y, double elevation) {
		// TODO Auto-generated method stub
		return null;
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
		return false;
	}

	@Override
	public void redraw() {
		// GOOP
	}

}
