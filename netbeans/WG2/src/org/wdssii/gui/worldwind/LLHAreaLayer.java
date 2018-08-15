package org.wdssii.gui.worldwind;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.wdssii.core.CommandManager;
import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.commands.PointAddCommand;
import org.wdssii.gui.commands.PointRemoveCommand;
import org.wdssii.gui.commands.PointSelectCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.renderers.LLHAreaControlPointRenderer;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaControlPoint;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * A layer for all of our LLHArea collection, this handles editing a 'list' of
 * objects. A lot of this code came from Airspace work in the WorldWind library,
 * but at the time it was changing too fast and was missing features we needed
 * for vslice and isosurfaces, so it was ported and modified. -- Merged editor
 * layer and drawing layer (simpler for our purposes)
 *
 * @author Robert Toomey
 * @author dcollins
 *
 *         Merged Editor ability into this class, since we have a single
 *         'selected' object that is the currently edited one.
 */
public class LLHAreaLayer {
				// implements WWCategoryLayer {

	private final static Logger LOG = LoggerFactory.getLogger(LLHAreaLayer.class);

	// Editor fields...
	private LLHAreaControlPointRenderer controlPointRenderer;
	// List of control points from the last call to draw().
	private ArrayList<LLHAreaControlPoint> currentControlPoints = new ArrayList<LLHAreaControlPoint>();
	
	// Altitude constants.
	protected static final int LOWER_ALTITUDE = 0;
	protected static final int UPPER_ALTITUDE = 1;
	private static final double DEFAULT_POLYGON_HEIGHT = 10.0;

	//@Override
	//public String getCategory() {
	//	return WDSSII_CATEGORY;
	//}

	/**
	 * Creates a new <code>Airspace</code> with an empty collection of
	 * Airspaces.
	 */
	public LLHAreaLayer() {
		this.controlPointRenderer = new LLHAreaControlPointRenderer();
	}

	public void pick(GLWorld w, java.awt.Point pickPoint){
		this.drawControlPoints(w, pickPoint);
	}
	
	public void draw(GLWorld w){
		this.drawControlPoints(w, null);
	}

	public LLHAreaControlPointRenderer getControlPointRenderer() {
		return this.controlPointRenderer;
	}

	private void drawControlPoints(GLWorld w, Point pickPoint) {

		// Assemble points ---------------------------------------------
		currentControlPoints.clear();			
		Feature f = FeatureList.getFeatureList().getSelected(LLHAreaFeature.LLHAreaGroup);
		if ((f == null) || (!f.getVisible())) {
			return;
		}
		LLHAreaFeature l = (LLHAreaFeature) (f);
		LLHArea area = l.getLLHArea();

		if (area == null) {
			return;
		}

		List<LLHAreaControlPoint> list = area.getControlPoints(w);
		
		// ------------------------------------------------------
		// Add control points
		for (LLHAreaControlPoint p : list) {
			currentControlPoints.add(p);
		}
		
		if (w.isPickingMode()) {
			this.getControlPointRenderer().pick(w, currentControlPoints, pickPoint);
		} else {
			this.getControlPointRenderer().render(w, currentControlPoints);
		}
	}

	public LLHAreaControlPoint addControlPoint(GLWorld w, LLHArea airspace, Point mousePoint, boolean shiftDown) {
		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null || this.getAirspace() != airspace) {
			return null;
		}

		if ( mousePoint == null) {
			return null;
		}

		return this.doAddControlPoint(w, airspace, mousePoint, shiftDown);
	}
	
	public V3 airspaceOrigin(GLWorld w, LLHArea airspace, Point mousePoint) {
		
		V3 refPos = airspace.getReferencePosition();
		V3 clickedPosition = mouseToPosition(w, refPos.z, mousePoint);
		if (clickedPosition == null) {
			return null;
		}

		return clickedPosition;

	}

	public ArrayList<LLD_X> originList(LLHArea area, Point mousePoint) {
		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			ArrayList<LLD_X> newList = new ArrayList<LLD_X>();
			ArrayList<LLD_X> list = null;
			list = m.get(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
			for (LLD_X l : list) {
				LLD_X copy = new LLD_X(l);
				newList.add(copy);
			}
			return newList;
		}
		return null;
	}

	/**
	 * Calculate mouse to position on airspace..
	 */
	private V3 mouseToPosition(GLWorld w, double elevation, Point mousePoint) {
		// Find new 3D point on earth surface using this elevation...
		V3 a3D = w.project2DToEarthSurface(mousePoint.getX(), mousePoint.getY(), elevation);
		if (a3D == null) {
			return null;
		}
		// And make it lat, lon, height...
		V3 p = w.projectV3ToLLH(a3D);
		return p;
	}
	
	public void doMoveAirspaceLaterally(GLWorld w, LLHArea airspace, ArrayList<LLD_X> originList, V3 llh,
			Point mousePoint) {

		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null || this.getAirspace() != airspace) {
			return;
		}

		// Use elevation of surface, or existing elevation of point....
		V3 refPos = airspace.getReferencePosition();
		//double elevation = refPos.getElevation();

		// Project to earth 3D point at given elevation
		V3 a3D = w.project2DToEarthSurface(mousePoint.getX(), mousePoint.getY(), refPos.z);
		if (a3D == null) {
			return;
		}
		// And make it lat, lon, height...
		V3 newPosition = w.projectV3ToLLH(a3D);
		
		if (originList == null) {
			return;
		}

		LLHArea area = this.getAirspace();
		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			ArrayList<LLD_X> list = null;
			list = m.get(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
			if (list != null) {

				// Delta from original point to current point...
				//double dlat = newPosition.latitude.degrees - origin.latitude.degrees;
				//double dlon = newPosition.longitude.degrees - origin.longitude.degrees;
				double dlat = newPosition.x - llh.x;
				double dlon = newPosition.y - llh.y;
				if (!originList.isEmpty()) { // Need at least one existing point
												// for reference...
					int index = 0;
					for (LLD_X l : originList) {

						LLD_X l2 = new LLD_X(l.latDegrees() + dlat, l.lonDegrees() + dlon, l);
						list.set(index, l2);

						index++;

					}
					// list.add(newPosition);
					// m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, list);
					// same list
					FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), m);
					CommandManager.getInstance().executeCommand(c, true);
				}
			}
		}
		// eturn;
	}

	private void pinAltitudes(double[] altitudes) {
		if (altitudes[LOWER_ALTITUDE] < 0.0) {
			altitudes[LOWER_ALTITUDE] = 0.0;
		}
		if (altitudes[UPPER_ALTITUDE] < 0.001) {
			altitudes[UPPER_ALTITUDE] = 0.001;
		}
	}

	private LLHAreaControlPoint doAddControlPoint(GLWorld w, LLHArea airspace, Point mousePoint, boolean shiftDown) {

		V3 np = w.project2DToEarthSurface(mousePoint.getX(), mousePoint.getY(), 0.0d);
		if (np == null){ return null; }
		V3 newP = w.projectV3ToLLH(np);  // Project new point to lat lon

		double[] altitudes = new double[2];
		altitudes[LOWER_ALTITUDE] = newP.z;
		altitudes[UPPER_ALTITUDE] = newP.z + DEFAULT_POLYGON_HEIGHT;
		pinAltitudes(altitudes);
		this.getAirspace().setAltitudes(altitudes[LOWER_ALTITUDE], altitudes[UPPER_ALTITUDE]);

		LLHAreaControlPoint controlPoint = null;
		LLHArea area = this.getAirspace();
		if (area instanceof LLHAreaSet) {

			LLHAreaSet set = (LLHAreaSet) (area);

			// New control point from the location
			LLD_X newLLD = new LLD_X(newP.x, newP.y);
			PointAddCommand c = new PointAddCommand(set, newLLD, -1, shiftDown);
			CommandManager.getInstance().executeCommand(c, true);

			int aIndex = c.getFinalIndex();
			controlPoint = new LLHAreaControlPoint(aIndex, np, newLLD);
		}
		return controlPoint;
	}

	public LLHArea getAirspace() {
		LLHArea ap = null;

		Feature f = FeatureList.getFeatureList().getSelected(LLHAreaFeature.LLHAreaGroup);
		if (f != null) {
			LLHAreaFeature l = (LLHAreaFeature) (f);
			ap = l.getLLHArea();
		}
		return ap;
	}
	
	/**
	 * Delete a single point
	 */
	public void doRemoveControlPoint(LLHAreaControlPoint controlPoint) {
		if (controlPoint != null) {
			int index = controlPoint.getLocationIndex();
			LLHArea area = this.getAirspace();
			if (area instanceof LLHAreaSet) {
				LLHAreaSet set = (LLHAreaSet) (area);
				PointRemoveCommand c = new PointRemoveCommand(set, index, false);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}

	// Fire point select command
	public void doSelectControlPoint(LLHAreaControlPoint controlPoint) {
		if (controlPoint != null) {
			int index = controlPoint.getLocationIndex();
			LLHArea area = this.getAirspace();
			if (area instanceof LLHAreaSet) {
				LLHAreaSet set = (LLHAreaSet) (area);
				PointSelectCommand c = new PointSelectCommand(set, index);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}

	/**
	 * Move a single point
	 */
	public void doMoveControlPoint(GLWorld w, LLHAreaControlPoint controlPoint, Point mousePoint) {

		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null) {
			return;
		}

		// Get current 3d point in lat, lon, height....this seems to be to get the current elevation
		V3 controlPointL = w.projectV3ToLLH(controlPoint.getPoint());
		
		// Find new 3D point on earth surface using this elevation...
		V3 newPoint = w.project2DToEarthSurface(mousePoint.getX(), mousePoint.getY(), controlPointL.z);
		if (newPoint == null) {
			return;
		}

		// And make it lat, lon, height...
		V3 newPosition = w.projectV3ToLLH(newPoint);
		
		int index = controlPoint.getLocationIndex();
		LLHArea area = this.getAirspace();

		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			@SuppressWarnings("unchecked")
			ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
			if (list != null) {

				// Copy the location info at least...
				LLD_X oldOne = list.get(index);
				LLD_X newOne = new LLD_X(newPosition.x, newPosition.y, oldOne);

				list.set(index, newOne);

				FeatureMemento fm = (FeatureMemento) (m); // Check it
				FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}
}
