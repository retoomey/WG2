package org.wdssii.gui.volumes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V3;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.properties.Memento;

/**
 * A root class for all of our volumes. Common functionality will go here.
 *
 * @author Robert Toomey
 *
 */
public class LLHArea {
	/**
	 * Default LLHArea height in meters
	 */
	public static final double DEFAULT_HEIGHT_METERS = 15000.0; // 15 km
	/**
	 * Default LLHArea length in meters
	 */
	public static final double DEFAULT_LENGTH_METERS = 100000.0; // 100 km

	/**
	 * Change to pass onto the LLHArea. All fields common to LLHArea are here
	 */
	public static class LLHAreaMemento extends FeatureMemento {

		private double maxHeight;
		private boolean useMaxHeight = false;
		private double minHeight;
		private boolean useMinHeight = false;

		public LLHAreaMemento(LLHArea a) {
			maxHeight = a.upperAltitude;
			minHeight = a.lowerAltitude;
		}

		public double getMinHeight() {
			return minHeight;
		}

		public void setMinHeight(double h) {
			final double a = getMinAllowedHeight();
			if (h >= a) {
				minHeight = h;
			} else {
				h = a;
			}

			// Minimum total size of slice
			final double m = maxHeight - getMinAllowedSize();
			if (h >= m) {
				minHeight = m;
			}
			useMinHeight = true;
		}

		public double getMaxHeight() {
			return maxHeight;
		}

		public void setMaxHeight(double h) {
			final double a = getMaxAllowedHeight();
			if (h <= a) {
				maxHeight = h;
			} else {
				h = a;
			}

			// Minimum total size of slice
			final double m = minHeight + getMinAllowedSize();
			if (h <= m) {
				maxHeight = m;
			}
			useMaxHeight = true;
		}

		public double getMaxAllowedHeight() {
			return 20000; // Meters
		}

		public double getMinAllowedHeight() {
			return -20000; // Meters
		}

		public double getMinAllowedSize() {
			return 100; // Meters
		}
	}

	/**
	 * The feature we belong to. All 3D objects such as slice, box, stick, etc.
	 * will belong to the LLHAreaFeature group
	 */
	private LLHAreaFeature myFeature;
	/**
	 * Is this LLHArea visible?
	 */
	private boolean visible = true;
	/**
	 * When selected do we allow others to show?
	 */
	private boolean only = false;
	/**
	 * Every LLHArea can follow a particular product
	 */
	protected String myProductFollow = ProductManager.TOP_PRODUCT;
	/**
	 * Do we use virtual volume or regular one?
	 */
	protected boolean myUseVirtualVolume = false;
	protected static final String SUBDIVISIONS = "Subdivisions";
	protected static final String VERTICAL_EXAGGERATION = "VerticalExaggeration";
	protected double lowerAltitude = 0.0;
	protected double upperAltitude = 1.0;
	private LLD_X groundReference;

	/**
	 * The list of locations that make us up
	 */
	private List<LLD_X> locations = new ArrayList<LLD_X>();

	public LLHArea(LLHAreaFeature f) {
		// this(new BasicAirspaceAttributes());
		myFeature = f;
	}

	public LLHAreaFeature getFeature() {
		return myFeature;
	}

	/**
	 * Get the memento for this class
	 */
	public LLHAreaMemento getMemento() {
		return new LLHAreaMemento(this);
	}

	public void setVisOnly(Memento m) {
		visible = m.get(FeatureMemento.VISIBLE, true);
		only = m.get(FeatureMemento.ONLY, false);
	}

	public void setAltitudes(Memento m) {
		if (m instanceof LLHAreaMemento) {
			LLHAreaMemento l = (LLHAreaMemento) (m);
			if (l.useMaxHeight) {
				setAltitudes(lowerAltitude, l.maxHeight);
			}
			if (l.useMinHeight) {
				setAltitudes(l.minHeight, upperAltitude);
			}
		}
	}

	/**
	 * Called by the Feature to change us
	 */
	public void setFromMemento(Memento m) {
		setVisOnly(m);
	}

	/**
	 * Get the range between two given index points. This is not cumulative
	 * range
	 */
	public double getRangeKms(int point1, int point2) {
		// FIXME: cleaner way of this?....fetch radius of current globe..
		// This should clean up later...bad design on my part.
		// FIXME: MULTIVIEW
		/*
		 * WorldWindView v = FeatureList.theFeatures.getWWView(); if (v != null)
		 * { double radius = v.getWwd().getModel().getGlobe().getRadius();
		 * List<LatLon> l = this.getLocationList(); double length; if ((l.size()
		 * > point1) && (l.size() > point2)) { length =
		 * LatLon.greatCircleDistance(l.get(point1), l.get(point2)).radians *
		 * radius; } else { length = 0.0d; } return length; }
		 */

		return 1.0f; // bleh
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isOnly() {
		return this.only;
	}

	public void setOnly(boolean only) {
		this.only = only;
	}

	public int getVertexCount() {
		if (this.locations != null) {
			return this.locations.size();
		}
		return 0;
	}

	// More ww strangeness, 'have' to copy or the dragging stuff breaks like
	// mad.
	// FIXME: really need to make our own
	public ArrayList<LLD_X> getArrayListCopyOfLocations() {
		ArrayList<LLD_X> newList = new ArrayList<LLD_X>();
		for (LLD_X l : locations) {
			// newList.add(new LatLon(l.getLatitude(), l.getLongitude()));
			LLD_X t = new LLD_X(l);
			t.setPolygon(l.getPolygon());
			t.setNote(l.getNote());
			newList.add(t);
		}
		return newList;
	}

	public List<LLD_X> getLocations() {
		return Collections.unmodifiableList(this.locations);
	}

	public double[] getAltitudes() {
		double[] array = new double[2];
		array[0] = this.lowerAltitude;
		array[1] = this.upperAltitude;
		return array;
	}

	protected double[] getAltitudes(double verticalExaggeration) {
		double[] array = this.getAltitudes();
		array[0] = array[0] * verticalExaggeration;
		array[1] = array[1] * verticalExaggeration;
		return array;
	}

	public void setAltitudes(double lowerAltitude, double upperAltitude) {
		this.lowerAltitude = lowerAltitude;
		this.upperAltitude = upperAltitude;
		// this.setExtentOutOfDate();
	}

	public void setAltitude(double altitude) {
		this.setAltitudes(altitude, altitude);
	}

	public LLD_X getGroundReference() {
		return this.groundReference;
	}

	public void setGroundReference(LLD_X groundReference) {
		this.groundReference = groundReference;
	}

	public boolean isAirspaceVisible(GLWorld w) {
		// Not bothering to clip it. Noones gonna be drawing vslices all over
		// the earth at the
		// same time..I think...lol. Well, at least not the weather people here.
		// I can write a better clip method later if needed
		return true;
	}

	public void renderGeometry(GLWorld w, String drawStyle) {
		this.doRenderGeometry(w, drawStyle);
	}

	/*
	 * @Override public void move(Position position) {
	 * this.moveTo(this.getReferencePosition().add(position)); }
	 * 
	 * @Override public void moveTo(Position position) { Position oldRef =
	 * this.getReferencePosition(); this.doMoveTo(oldRef, position); }
	 * 
	 * protected void doMoveTo(Position oldRef, Position newRef) { double[]
	 * altitudes = this.getAltitudes(); double elevDelta = newRef.getElevation()
	 * - oldRef.getElevation(); this.setAltitudes(altitudes[0] + elevDelta,
	 * altitudes[1] + elevDelta);
	 * 
	 * // Update all locations...
	 * 
	 * int count = this.locations.size(); LLD_X[] newLocations = new
	 * LLD_X[count]; for (int i = 0; i < count; i++) { LLD_X lld =
	 * this.locations.get(i); LatLon ll = new
	 * LatLon(Angle.fromDegrees(lld.latDegrees()),
	 * Angle.fromDegrees(lld.lonDegrees())); double distance =
	 * LatLon.greatCircleDistance(oldRef, ll).radians; double azimuth =
	 * LatLon.greatCircleAzimuth(oldRef, ll).radians; // newLocations[i] =
	 * LatLon.greatCircleEndPosition(newRef, azimuth, // distance); LatLon
	 * newLoc = LatLon.greatCircleEndPosition(newRef, azimuth, distance);
	 * newLocations[i] = new LLD_X(newLoc.latitude.degrees,
	 * newLoc.longitude.degrees); }
	 * this.setLocations(Arrays.asList(newLocations)); }
	 */
	public void setLocations(Iterable<? extends LLD_X> locations) {
		this.locations.clear();
		this.addLocations(locations);
	}

	public void selectLocation(int x, boolean forceOne) {
		for (int i = 0; i < locations.size(); i++) {
			LLD_X l = locations.get(i);
			if (i == x) {
				l.setSelected(true);
			} else if (forceOne) {
				l.setSelected(false);
			}
		}
	}

	public LLD_X getFirstSelected() {
		for (LLD_X l : locations) {
			if (l.getSelected()) {
				return l;
			}
		}
		return null;
	}

	protected List<LLD_X> getLocationList() {
		return this.locations;
	}

	protected void addLocations(Iterable<? extends LLD_X> newLocations) {
		if (newLocations != null) {
			for (LLD_X ll : newLocations) {
				if (ll != null) {
					this.locations.add(ll);
				}
			}
		}
		updateCurrentGrid();
	}

	public void updateCurrentGrid() {
	}

	public V3 getReferencePosition() {
		return this.computeReferencePosition(this.locations, this.getAltitudes());
	}

	protected V3 computeReferencePosition(List<? extends LLD_X> locations, double[] altitudes) {
		int count = locations.size();
		if (count == 0) {
			return null;
		}

		LLD_X ll;
		if (count < 3) {
			ll = locations.get(0);
		} else {
			ll = locations.get(count / 2);
		}

		return new V3(ll.latDegrees(), ll.lonDegrees(), altitudes[0]);
	}

	protected void doRenderGeometry(GLWorld w, String drawStyle) {
		this.doRenderGeometry(w, drawStyle, getLocationList(), null);
	}

	protected void doRenderGeometry(GLWorld w, String drawStyle, List<LLD_X> locations, List<Boolean> edgeFlags) {
	}

	/**
	 * Set the product that we follow in the display
	 */
	public void setProductFollow(String f) {
		myProductFollow = f;
	}

	/**
	 * Get the product that we follow in the display
	 */
	public String getProductFollow() {
		return myProductFollow;
	}

	/**
	 * Set if we use a virtual or regular volume
	 */
	public void setUseVirtualVolume(boolean current) {
		myUseVirtualVolume = current;
	}

	/**
	 * Get if we use a virtual or regular volume
	 */
	public boolean getUseVirtualVolume() {
		return myUseVirtualVolume;
	}

	/**
	 * Get the current control points for this area
	 */

	public ArrayList<LLHAreaControlPoint> getControlPoints(GLWorld w) {
		ArrayList<LLHAreaControlPoint> points = new ArrayList<LLHAreaControlPoint>();
		List<LLD_X> l = getLocations();
		// int numLocations = l.size();
		int count = 0;
		for (LLD_X x : l) {
			addPolygonControlPoint(points, w, count++, 0, x);
		}
		// for (int locationIndex = 0; locationIndex < numLocations;
		// locationIndex++) {

		// Just add the bottom control point for now...
		// addPolygonControlPoint(points, dc, locationIndex, 0);
		// Add the upper altitude control points.
		// addPolygonControlPoint(points, dc, locationIndex, 1);
		// }
		return points;

	}

	/**
	 * Add a control point
	 */
	protected void addPolygonControlPoint(ArrayList<LLHAreaControlPoint> list, GLWorld w, int locationIndex,
			int altitudeIndex, LLD_X location) {
		double altitude = getAltitudes()[altitudeIndex];
		double vert = w.getVerticalExaggeration();
		V3 p = w.projectLLH(location.latDegrees(), location.lonDegrees(), altitude * vert);

		LLHAreaControlPoint controlPoint = new LLHAreaControlPoint(locationIndex, p, location);
		list.add(controlPoint);
	}

	/**
	 * The default location for a newly created LLHArea
	 */
	protected List<LLD_X> getDefaultLocations(Object params) {
		return null;
	}

	/**
	 * Return info for a segment Not sure where to put this function
	 */
	public VolumeSliceInput getSegmentInfo(VolumeSliceInput buffer, int segment, int rows, int cols) {
		java.util.List<LLD_X> l = getLocations();
		if (l.size() > segment + 1) {
			double[] altitudes = getAltitudes();
			ArrayList<LLD_X> list = getVSliceLocations(getLocations());
			if (buffer != null) {
				buffer.set(rows, cols, list.get(0).latDegrees(), list.get(0).lonDegrees(), list.get(1).latDegrees(),
						list.get(1).lonDegrees(), altitudes[0], altitudes[1]);
				return buffer;
			} else {
				return new VolumeSliceInput(rows, cols, list.get(0).latDegrees(), list.get(0).lonDegrees(),
						list.get(1).latDegrees(), list.get(1).lonDegrees(), altitudes[0], altitudes[1]);
			}
		} else {
			return null;
		}
	}

	/**
	 * Given a lat lon list, return vslice order Not sure where to put this
	 * function
	 */
	protected static ArrayList<LLD_X> getVSliceLocations(java.util.List<LLD_X> input) {

		ArrayList<LLD_X> out = null;
		if (input.size() > 1) {
			out = new ArrayList<LLD_X>();
			LLD_X l1 = input.get(0);
			LLD_X l2 = input.get(1);
			LLD_X leftBottom, rightBottom;
			if (l1.lonDegrees() < l2.lonDegrees()) {
				leftBottom = l1;
				rightBottom = l2;
			} else {
				leftBottom = l2;
				rightBottom = l1;
			}
			out.add(leftBottom);
			out.add(rightBottom);
		}
		return out;
	}
}
