package org.wdssii.gui.volumes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wdssii.geom.LLD_X;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.views.DataFeatureView;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;

/**
 * A collection of editable points in a line. Constant or increasing time. This
 * might merge with LLHArea. I'm refactoring/cleaning up some of the
 * LLHAreaSlice, etc..mess. Keeping things separate until proven they work.
 *
 * This should really be the LLHArea....
 *
 */
public class LLHAreaSet extends LLHArea {

	private final static Logger LOG = LoggerFactory.getLogger(LLHAreaSet.class);
	public int currentHeightMeters = (int) LLHArea.DEFAULT_HEIGHT_METERS;
	public int currentBottomMeters = 0;
	public int currentPolygonNumber = 1;
	public String currentNote = "";
	public String currentAuto = "";
	public String currentFile = "";
	public int myNumRows = 50;
	public int myNumCols = 100;
	private String myChartKey;

	/**
	 * Change to pass onto the LLHArea. All fields common to LLHArea are here
	 */
	public static class LLHAreaSetMemento extends LLHArea.LLHAreaMemento {

		public static final String TOP_HEIGHT = "LLHtopheightmeters";
		public static final String BOTTOM_HEIGHT = "LLHbottommeters";
		public static final String POLYGON = "LLHpolygon";
		public static final String NOTE = "LLHnote";
		public static final String AUTO = "LLHautofile";
		public static final String FILE = "LLHfile";
		public static final String GRID_ROWS = "LLHgridrows";
		public static final String GRID_COLS = "LLHgridcols";
		public static final String POINTS = "LLHpoints";
		public static final String RENDERER = "LLHrenderer";

		// Create a new default property LLHAreaSetMemento
		public LLHAreaSetMemento(LLHAreaSet a) {
			super(a); // this will go away
			initProperty(TOP_HEIGHT, 10000);
			initProperty(BOTTOM_HEIGHT, 0);
			initProperty(POLYGON, 0);
			initProperty(NOTE, "xxx");
			initProperty(AUTO, "");
			initProperty(FILE, "");
			initProperty(GRID_ROWS, 50);
			initProperty(GRID_COLS, 100);
			// Experimental...
			initProperty(POINTS, new ArrayList<LLD_X>());
			initProperty(RENDERER, "");
		}
	}

	/**
	 * Get the memento for this class
	 */
	@Override
	public LLHAreaSetMemento getMemento() {
		LLHAreaSetMemento m = new LLHAreaSetMemento(this);
		// hack into old way bleh...so bad. always set so thrash...
		m.setProperty(LLHAreaSetMemento.TOP_HEIGHT, currentHeightMeters);
		m.setMaxHeight(currentHeightMeters);
		m.setProperty(LLHAreaSetMemento.BOTTOM_HEIGHT, currentBottomMeters);
		m.setProperty(LLHAreaSetMemento.POLYGON, currentPolygonNumber);
		m.setProperty(LLHAreaSetMemento.NOTE, currentNote);
		m.setProperty(LLHAreaSetMemento.AUTO, currentAuto);
		m.setProperty(LLHAreaSetMemento.FILE, currentFile);
		m.setMinHeight(currentBottomMeters);
		m.setProperty(LLHAreaSetMemento.GRID_ROWS, myNumRows);
		m.setProperty(LLHAreaSetMemento.GRID_COLS, myNumCols);
		m.setProperty(LLHAreaSetMemento.RENDERER, myChartKey);

		// Stuff points into property copy each time could be bad later for >> N
		m.setProperty(LLHAreaSetMemento.POINTS, this.getArrayListCopyOfLocations());
		return m;
	}

	@Override
	public void setFromMemento(Memento m) {
		setVisOnly(m);
		
		// FIXME: Not liking this code here:
		Integer v = ((Integer) m.getPropertyValue(LLHAreaSetMemento.TOP_HEIGHT));
		if (v != null) {
			if (m instanceof LLHAreaMemento) {
				LLHAreaMemento cc = (LLHAreaMemento) (m);
				cc.setMaxHeight(v.intValue());
				currentHeightMeters = (int) cc.getMaxHeight();
			}
		}
		v = ((Integer) m.getPropertyValue(LLHAreaSetMemento.BOTTOM_HEIGHT));
		if (v != null) {
			if (m instanceof LLHAreaMemento) {
				LLHAreaMemento cc = (LLHAreaMemento) (m);
				cc.setMinHeight(v.intValue());
				currentBottomMeters = (int) cc.getMinHeight();
			}
		}
		currentPolygonNumber = m.get(LLHAreaSetMemento.POLYGON, currentPolygonNumber);
		currentNote = m.get(LLHAreaSetMemento.NOTE, currentNote);
		currentAuto = m.get(LLHAreaSetMemento.AUTO, currentAuto);
		currentFile = m.get(LLHAreaSetMemento.FILE, currentFile);
		myNumRows = m.get(LLHAreaSetMemento.GRID_ROWS, myNumRows);
		myNumCols = m.get(LLHAreaSetMemento.GRID_COLS, myNumCols);
		myChartKey = m.get(LLHAreaSetMemento.RENDERER, myChartKey);

		setAltitudes(m);

		// Whoops this caused a bug.  We get partial mementos now so
		// if the list is not in the properties, we don't want to set to null
		// it's different from others because we're copying the list
		//ArrayList<LLD_X> list = null;  This overwrites if points is missing in update.
		//list = m.get(LLHAreaSetMemento.POINTS, list);
		//this.setLocations(list);
		
		// Make sure and only change iff there is a NEW list...
		ArrayList<LLD_X> list = m.getPropertyValue(LLHAreaSetMemento.POINTS);
		if (list != null){
			this.setLocations(list);
		}
	}

	/**
	 * Number of rows...this is really X resolution...
	 */
	public int getNumRows() {
		return myNumRows;
	}

	/**
	 * Number of cols...this is really Y or height resolution
	 */
	public int getNumCols() {
		return myNumCols;
	}

	public LLHAreaSet(LLHAreaFeature f) {
		super(f);
	}

	/**
	 * Get our chart object, if any that we use to fill in our stuff
	 */
	public DataView get3DRendererChart() {
		DataFeatureView v = getChartView();
		if (v != null) {
			return v.getChart();
		}
		return null;
	}

	public void setChartView(DataFeatureView v0) {
		// We don't hold the object in case name changes or it is destroyed
		myChartKey = v0.getKey();
	}

	public DataFeatureView getChartView() {
		ArrayList<DataFeatureView> list = DataFeatureView.getList();
		for (DataFeatureView c : list) {
			if (c.getKey().equals(myChartKey)) {
				return c;
			}
		}
		// if not found and there's a list, use the first one....
		if (!list.isEmpty()) {
			DataFeatureView fallBack = list.get(0);
			myChartKey = fallBack.getKey();
			return fallBack;
		}
		return null;
	}

	/**
	 * Get a key that represents the GIS location of this slice
	 */
	public String getGISKey() {

		// Add location and altitude...
		List<LLD_X> locations = getLocationList();
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < locations.size(); i++) {
			LLD_X l = locations.get(i);
			buf.append(l.latDegrees() + ":");
			buf.append(l.lonDegrees() + ":");
		}
		// newKey = newKey + myCurrentGrid.bottomHeight;
		// newKey = newKey + myCurrentGrid.topHeight;
		double[] altitudes = this.getAltitudes();
		buf.append(altitudes[0]);
		buf.append(altitudes[1]);
		String newKey = buf.toString();
		return newKey;
	}

	/**
	 * Get a volume key for this slice, either virtual or nonvirtual volume
	 */
	public String getVolumeKey(String follow, boolean useVirtual) {
		// Add the key of the volume....
		String newKey = "";
		ProductVolume volume = ProductManager.getCurrentVolumeProduct(follow, useVirtual);
		newKey += volume.getKey(); // java 6 StringBuilder is internally used...
		return newKey;
	}

	/**
	 * Get a unique key representing all states. Used by charts and 3d slice to
	 * tell unique state for recalculation.
	 *
	 * @param virtual
	 *            -- shouldn't pass these params probably.
	 * @param useFilters
	 * @return
	 */
	public String getKey(String follow, boolean virtual, FilterList list, boolean useFilters) {
		String newKey = getGISKey();
		return newKey;
	}

	public double getBottomHeightKms() {
		double[] altitudes = this.getAltitudes();
		return altitudes[0];
	}

	public double getTopHeightKms() {
		return upperAltitude;
	}

	/**
	 * Update the current grid that is the GIS location of the slice
	 */
	@Override
	public void updateCurrentGrid() {
	}

	/**
	 * The default location for a newly created LLHArea
	 */
	@Override
	protected List<LLD_X> getDefaultLocations(Object params) {

		int count = 2;
		if (params instanceof Integer) {
			Integer c = (Integer) (params);
			count = c.intValue();
		}
		LLD_X[] locations;

		switch (count) {
		case 1:
			locations = new LLD_X[] {
					// LatLon.fromDegrees(35.8, -98.4)
					new LLD_X(35.8, -98.4) };

			break;
		default:
		case 2:
			locations = new LLD_X[] {
					// LatLon.fromDegrees(35.8, -98.4),
					new LLD_X(35.8, -98.4),
					// LatLon.fromDegrees(34.9, -96.4),};
					new LLD_X(34.9, -96.4), };
		}
		return Arrays.asList(locations);
	}
}