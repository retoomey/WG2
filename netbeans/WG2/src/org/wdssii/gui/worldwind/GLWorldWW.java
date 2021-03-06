package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import org.wdssii.gui.GLWorld;

import org.wdssii.geom.V2;
import org.wdssii.geom.V3;

/**
 * A GLWorld that has a Worldwind DrawContext
 *
 * @author Robert Toomey
 */
public class GLWorldWW extends GLWorld {

	private final View v;
	private final Globe g;
	private final DrawContext adc;
	private final WorldWindDataView world;
	
	public GLWorldWW(DrawContext dc) {
		super(dc.getGL(), dc.getView().getViewport().width, dc.getView().getViewport().height);
		adc = dc;
		v = dc.getView();
		g = dc.getGlobe();
		world = null;
	}

	public WorldWindDataView getWWWorld(){
		world.repaint();
		return world;
	}
	public GLWorldWW(DrawContext dc, WorldWindDataView w) {
		super(dc.getGL(), dc.getView().getViewport().width, dc.getView().getViewport().height);
		adc = dc;
		v = dc.getView();
		g = dc.getGlobe();
		world = w;
	}

	public DrawContext getDC() {
		return adc;
	}

	/**
	 * Project from 3d to 2D in the current world
	 */
	@Override
	public V2 project(V3 a3D) {
		// Two news, yay.
		Vec4 vv = v.project(new Vec4(a3D.x, a3D.y, a3D.z));
		return new V2(vv.x, vv.y);
	}

	/**
	 * Project a lat, lon, height into a 3D model point...
	 */
	@Override
	public V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters) {
		final Vec4 v = g.computePointFromPosition(
				Angle.fromDegrees(latDegrees),
				Angle.fromDegrees(lonDegrees),
				heightMeters);
		return new V3(v.x, v.y, v.z);
	}

	@Override
	public V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters) {
		final Vec4 v = g.computePointFromPosition(
				Angle.fromDegrees(latDegrees),
				Angle.fromDegrees(lonDegrees),
				heightMeters);
		return new V3(v.x, v.y, v.z);
	}

	/** Project a x,y,z in 3D model space to a lat, lon, height  in degrees and meters */
	@Override
	public V3 projectV3ToLLH(V3 in){
		Position p = g.computePositionFromPoint(new Vec4(in.x, in.y, in.z));
		return new V3(p.latitude.getDegrees(), p.longitude.getDegrees(), p.getAltitude());
	}
	
	/** Project from 2D point to earth surface at elevation above surface in meters.
	 *   To just hit the ball use zero for elevation.
	 */
	public V3 project2DToEarthSurface(double x, double y, double elevation){
		Line ray = v.computeRayFromScreenPoint(x, y);
		Vec4 newPoint = intersectGlobeAt(elevation, ray);
		if (newPoint == null) {
			return null;
		}
		V3 np = new V3(newPoint.x, newPoint.y, newPoint.z);
		return np;
	}
	
	/**
	 * Get the elevation in meters at a given latitude, longitude location
	 */
	@Override
	public float getElevation(float latDegrees, float lonDegrees) {
		Globe myGlobe = adc.getGlobe();
		ElevationModel e = myGlobe.getElevationModel();
		return (float) e.getElevation(Angle.fromDegrees(latDegrees), Angle.fromDegrees(lonDegrees));

	}

	/**
	 * Get the elevation in meters at a given latitude, longitude location
	 */
	@Override
	public double getElevation(double latDegrees, double lonDegrees) {
		Globe myGlobe = adc.getGlobe();
		ElevationModel e = myGlobe.getElevationModel();
		return e.getElevation(Angle.fromDegrees(latDegrees), Angle.fromDegrees(lonDegrees));
	}

	@Override
	public boolean isPickingMode() {
		return adc.isPickingMode();
	}

	@Override
	public double getVerticalExaggeration() {
		return adc.getVerticalExaggeration();
	}

	@Override
	public boolean inView(V3 a3D) {
		return(v.getFrustumInModelCoordinates().contains(new Vec4(a3D.x, a3D.y, a3D.z)));
	}
	
	@Override
	public void redraw(){
		world.repaint();
	}
	
	private  Vec4 intersectGlobeAt(double elevation, Line ray) {
		Intersection[] intersections = g.intersect(ray, elevation);
		if (intersections == null || intersections.length == 0) {
			return null;
		}

		return nearestIntersectionPoint(ray, intersections);
	}

	private static Vec4 nearestIntersectionPoint(Line line, Intersection[] intersections) {
		Vec4 intersectionPoint = null;

		// Find the nearest intersection that's in front of the ray origin.
		double nearestDistance = Double.MAX_VALUE;
		for (Intersection intersection : intersections) {
			// Ignore any intersections behind the line origin.
			if (!isPointBehindLineOrigin(line, intersection.getIntersectionPoint())) {
				double d = intersection.getIntersectionPoint().distanceTo3(line.getOrigin());
				if (d < nearestDistance) {
					intersectionPoint = intersection.getIntersectionPoint();
					nearestDistance = d;
				}
			}
		}

		return intersectionPoint;
	}
	
	private static boolean isPointBehindLineOrigin(Line line, Vec4 point) {
		double dot = point.subtract3(line.getOrigin()).dot3(line.getDirection());
		return dot < 0.0;
	}
}
