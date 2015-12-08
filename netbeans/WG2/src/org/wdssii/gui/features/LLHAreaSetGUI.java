/*
 * LLHAreaSetGUI.java
 *
 * @author Robert Toomey
 * 
 */
package org.wdssii.gui.features;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

// FIXME: remove geotools dependency to another lib
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wdssii.core.CommandManager;
import org.wdssii.geom.LLD_X;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.PointAddCommand;
import org.wdssii.gui.commands.PointRemoveCommand;
import org.wdssii.gui.commands.PointSelectCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.properties.DirectoryGUI;
import org.wdssii.gui.properties.PropertyGUI;
import org.wdssii.gui.properties.StringGUI;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.swing.TableUtil;
import org.wdssii.gui.volumes.LLHAreaController;
import org.wdssii.gui.volumes.LLHAreaController.ToolbarMode;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.gui.volumes.LLHAreaSet.LLHAreaSetMemento;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;
import org.wdssii.xml.PointSet;
import org.wdssii.xml.Util;

import com.jidesoft.swing.JideButton;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * LLHAreaSetGUI
 *
 * The gui that appears in the 3D Object list when a LLHAreaSet is selected.
 * This allows controls for a LLHAreaSet. All 'common' controls are in the
 * LLHAreaManager list, so for now at least we'll have to recreate height
 * controls for any subclass, but that's ok since we might want different layout
 * anyway.
 *
 * @author Robert Toomey
 */
public class LLHAreaSetGUI extends FeatureGUI {

	private final static Logger LOG = LoggerFactory.getLogger(LLHAreaSetGUI.class);

	/** Eventually this controller stuff all in us I think */
	private static LLHAreaController theController = null;

	private LLHAreaFeature myFeature;
	private final LLHAreaSet myLLHAreaSet;
	private LLHAreaSetTableModel myLLHAreaSetTableModel;
	private RowEntryTable jObjects3DListTable;
	private javax.swing.JScrollPane jObjectScrollPane;
	private JPanel myContainer;
	private JButton myAddRemoveButton, myNormalButton, myNewPolyButton;
	private JComponent myParent = null;
	private String lastAutoFile = "";

	private StringGUI myStringGUI = null;

	/** Set when we're reading in a point set to avoid too many GUI updates */
	private boolean myIsReading = false;
	private String myAutoDir = "";
	
	private DirectoryGUI myAutoGUI;

	// FIXME: Could be cool to subclass and make our own formatter
	private static Format myFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");

	/**
	 * Creates new form LLHAreaSetGUI
	 */
	public LLHAreaSetGUI(LLHAreaFeature f, LLHAreaSet owner) {

		myFeature = f;
		myLLHAreaSet = owner;
		setupComponents();
	}

	public static void setLLHAreaController(LLHAreaController c) {
		theController = c;
	}

	/**
	 * General update call
	 */
	@Override
	public void updateGUI() {
		// This pulls EVERY SETTING...different bug though...
		LLHAreaSetMemento m = myLLHAreaSet.getMemento();
		updateGUI(m);
	}

	@Override
	public void updateGUI(Memento m) {
		updateToMemento(m);
		updateProductInfo();
		updateTable(m);
		updateToolbar();
	}

	public JComponent getSecondaryControls() {
		return jObjects3DListTable;
	}

	/**
	 * Layout a newly added propety gui item, they go into our holder
	 */
	@Override
	public void layout(PropertyGUI g) {
		/*
		 * Default layout is mig
		 */
		g.addToMigLayout(myContainer);
	}

	@Override
	public void activateGUI(JComponent parent) {
		myParent = parent;
		super.activateGUI(parent);
		// FIXME: Restore old mouse mode?
	}

	@Override
	public void deactivateGUI() {
		// Go back to normal mouse mode when we aren't the GUI...
		if (theController != null) {
			theController.setMouseMode(ToolbarMode.NORMAL);
			updateToolbar();
		}
		super.deactivateGUI();
	}

	private void setupComponents() {

		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		setRootComponent(this);

		// Create toolbar and controls for point add/delete
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setMargin(new Insets(0, 0, 0, 0));

		Icon i = SwingIconFactory.getIconByName("plus.png");
		JButton aButton = new JideButton("");
		aButton.setIcon(i);
		aButton.setToolTipText("Add point with click, remove with shift-click");
		aButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (theController != null) {
					theController.setMouseMode(ToolbarMode.ADD_REMOVE);
					updateToolbar();
				}
			}
		});
		myAddRemoveButton = aButton;

		Icon i2 = SwingIconFactory.getIconByName("cursor.png");
		JButton bButton = new JideButton("");
		bButton.setIcon(i2);
		bButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (theController != null) {
					theController.setMouseMode(ToolbarMode.NORMAL);
					updateToolbar();
				}
			}
		});
		bButton.setToolTipText("Normal mode, drag points");
		myNormalButton = bButton;

		// New Polygon button
		i2 = SwingIconFactory.getIconByName("polyadd.png");
		JButton b = new JideButton("");
		b.setIcon(i2);
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<LLD_X> list = myLLHAreaSet.getLocations();

				// O(n) search for max polygon number... I'm not expected
				// billions of polygons
				// or we'll have to improve our refresh/search
				// Basically: Go into 'edit' mode and go to max polygon number
				// found + 1
				int max = 0;
				for (LLD_X x : list) {
					int v = x.getPolygon();
					if (v > max) {
						max = v;
					}
				}
				myLLHAreaSet.currentPolygonNumber = max + 1;
				if (theController != null) {
					theController.setMouseMode(ToolbarMode.ADD_REMOVE);
					updateToolbar();
				}
				updateGUI();
			}
		});
		b.setToolTipText("Start a new polygon");
		myNewPolyButton = b;

		// Delete Polygon button
		// Icon i2 = SwingIconFactory.getIconByName("cursor.png");
		JButton dd = new JideButton("");
		i2 = SwingIconFactory.getIconByName("polydel.png");
		dd.setIcon(i2);
		dd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Delete the selected items polygon...
				LLD_X x = myLLHAreaSet.getFirstSelected();
				if (x != null) {
					int p = x.getPolygon();
					PointRemoveCommand c = new PointRemoveCommand(myLLHAreaSet, p, true);
					CommandManager.getInstance().executeCommand(c, true);
				}
			}
		});
		dd.setToolTipText("Delete current polygon");

		// New Polygon button
		i2 = SwingIconFactory.getIconByName("save_edit.gif");
		JButton auto = new JideButton("");
		auto.setIcon(i2);
		auto.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				// Save to xml...
				JFileChooser j = new JFileChooser();
				j.setDialogTitle("Save XML file of points");
				// j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				Integer opt = j.showSaveDialog(LLHAreaSetGUI.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					File file = j.getSelectedFile();
					try {
						String temp = file.toString();
						if (!(temp.endsWith(".xml"))) {
							file = new File(temp + ".xml");
						}
						URL aURL = file.toURI().toURL();
						writeXMLFile(aURL);
					} catch (MalformedURLException ex) {
					}
				}

			}
		});
		auto.setToolTipText("Save XML file of data points");

		// New Polygon button
		i2 = SwingIconFactory.getIconByName("import_brkpts.gif");
		JButton lb = new JideButton("");
		lb.setIcon(i2);
		lb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				// Save to xml...
				JFileChooser j = new JFileChooser();
				j.setDialogTitle("Load XML file of points");
				Integer opt = j.showOpenDialog(LLHAreaSetGUI.this);
				if (opt == JFileChooser.APPROVE_OPTION) {
					File file = j.getSelectedFile();

					try {
						URL aURL = file.toURI().toURL();
						// String name = aURL.getFile();

						readXMLFile(aURL);

					} catch (Exception c) {
						LOG.error("Error loading pointset file: " + c.toString());
					}

				}

			}
		});
		lb.setToolTipText("Load XML file of data points");

		// Create 'Export' Button
		JButton export = new JideButton("Export...");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jExportActionPerformed(e);
			}
		});

		// Layout the buttons
		toolbar.add(bButton);
		toolbar.add(aButton);
		toolbar.addSeparator();
		toolbar.add(myNewPolyButton);
		toolbar.add(dd);
		toolbar.addSeparator();
		toolbar.add(auto);
		toolbar.add(lb);
		toolbar.add(export);

		JTabbedPane pane = new JTabbedPane();

		// Create container for stock property controls
		myContainer = new JPanel();
		myContainer.setLayout(new MigLayout(new LC(), null, null));
		JScrollPane s = new JScrollPane();
		s.setViewportView(myContainer);

		// Add a list of charts
		/*
		 * ComboStringGUI.ArrayListProvider listMaker = new
		 * ComboStringGUI.ArrayListProvider() {
		 * 
		 * @Override public ArrayList<String> getList() {
		 * ArrayList<DataFeatureView> charts = DataFeatureView.getList();
		 * ArrayList<String> list = new ArrayList<String>(); for
		 * (DataFeatureView v : charts) { String key = v.getTitle(); // FIXME:
		 * should be get key since title can change list.add(key); } return
		 * list; } };
		 * 
		 * add(new ComboStringGUI(myFeature, LLHAreaSetMemento.RENDERER, "3D",
		 * this, listMaker));
		 * 
		 * add(new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_ROWS, "Rows",
		 * this, 10, 100, 1, ""));
		 * 
		 * add(new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_COLS, "Cols",
		 * this, 10, 100, 1, ""));
		 * 
		 * add(new IntegerGUI(myFeature, LLHAreaSetMemento.TOP_HEIGHT, "Top",
		 * this, 1, 20000, 1, "Meters"));
		 * 
		 * add(new IntegerGUI(myFeature, LLHAreaSetMemento.BOTTOM_HEIGHT,
		 * "Bottom", this, 0, 19999, 1, "Meters"));
		 */
		// For debugging, allow manual changing of polygon number
		// add(new IntegerGUI(myFeature, LLHAreaSetMemento.POLYGON, "Polygon",
		// this, 1, 5000, 1, "Number"));

		myAutoGUI = new DirectoryGUI(myFeature, LLHAreaSetMemento.AUTO, LLHAreaSetMemento.FILE, "Auto:", "Dir...",
				"Choose directory to auto save/read XML files for top product timestamp", this);
		add(myAutoGUI);
		
		myStringGUI = new StringGUI(myFeature, LLHAreaSetMemento.NOTE, "Note:", this);
		add(myStringGUI);

		// Create the final layout...
		// Stick into split pane....
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, s, initTable());
		split.setResizeWeight(.50);
		add(toolbar, new CC().growX().dockNorth());
		add(split, new CC().growX().growY());

		updateTable(null);
		updateToolbar();
	}

	public void updateToolbar() {
		ToolbarMode m = ToolbarMode.INACTIVE;
		if (theController != null) {
			m = theController.getMouseMode();
		}
		if (m == ToolbarMode.ADD_REMOVE) {
			myAddRemoveButton.setSelected(true);
		} else {
			myAddRemoveButton.setSelected(false);
		}
		if (m == ToolbarMode.NORMAL) {
			myNormalButton.setSelected(true);
		} else {
			myNormalButton.setSelected(false);
		}
	}

	public void writeXMLFile(URL aURL) {
		if (myIsReading)
			return;

		// Create JAXB dataset for our LLH.
		// We 'could' just make LLH jaxb by directly annotating
		// right? lol
		// I debate keeping jaxb separate or not from our
		// working objects..
		PointSet ps = new PointSet();
		List<LLD_X> list = myLLHAreaSet.getLocations();
		for (LLD_X x : list) {
			ps.add(x.latDegrees(), x.lonDegrees(), x.getPolygon(), x.getNote());
		}

		String file2 = aURL.getFile();
		String error = Util.save(ps, file2, ps.getClass());
		if (error.isEmpty()) {
			LOG.info("Successfully saved point set to " + file2);
		} else {
			LOG.error("Couldn't save file");
		}
	}

	public void readXMLFile(URL aURL) {
		if (myIsReading)
			return;

		PointSet ps2 = Util.load(aURL, PointSet.class);
		if (ps2 != null) {

			ArrayList<LLD_X> newList = new ArrayList<LLD_X>();
			for (PointSet.Point p : ps2.points) {
				LLD_X x = new LLD_X(p.latitude, p.longitude);
				x.setNote(p.note);

				// For appending, need new unique number, or
				// the 'new polygon' value
				x.setPolygon(p.polygon);
				newList.add(x);
			}

			LOG.info("Loaded pointset from " + aURL.toString());
			myIsReading = true;
			PointAddCommand c = new PointAddCommand(myLLHAreaSet, newList, false, false);
			CommandManager.getInstance().executeCommand(c, true);
			myIsReading = false;
		} else {
			LOG.info("Couldn't load pointset " + aURL.toString());
		}
	}

	public void autoWrite(String dirName, String fileName) {
		if (myIsReading || dirName.isEmpty()){
			return;
		}
		File dir = new File(dirName);
		File file = new File(dir, fileName);
		try {
			URL aURL = file.toURI().toURL();

			if (dir.canWrite()) {
				if (file.exists()) {
					LOG.error("Found existing file at " + file.toPath());
					if (file.canWrite()) {
						writeXMLFile(aURL);
					} else {
						LOG.error("Can't overwrite file at " + file.toPath());
					}
				} else {
					writeXMLFile(aURL);
				}

			} else {
				LOG.error("Can't write to directory " + dir);
			}
		} catch (MalformedURLException e) {
			LOG.error("Error forming file URL.  Can't write data ");
			LOG.error("Exception is " + e.toString());
			return;
		}
	}

	public void autoRead(String dirName, String fileName) {
		if (myIsReading || dirName.isEmpty()){
			return;
		}
		
		File dir = new File(dirName);
		File file = new File(dir, fileName);
		try {
			URL aURL = file.toURI().toURL();

			if (dir.canRead()) {
				if (file.exists()) {
					LOG.error("Found existing file at " + file.toPath());
				}
				if (file.canRead()) {
					readXMLFile(aURL);
				} else {
					LOG.error("Can't read file at " + file.toPath());
				}

			} else {
				LOG.error("Can't read directory " + dir);
			}
		} catch (MalformedURLException e) {
			LOG.error("Error forming file URL.  Can't write data ");
			LOG.error("Exception is " + e.toString());
			return;
		}
	}

	/**
	 * Here is how you can use a SimpleFeatureType builder to create the schema
	 * for your shapefile dynamically.
	 * <p>
	 * This method is an improvement on the code used in the main method above
	 * (where we used DataUtilities.createFeatureType) because we can set a
	 * Coordinate Reference System for the FeatureType and a a maximum field
	 * length for the 'name' field dddd
	 */
	private static SimpleFeatureType createFeatureType() {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Location");
		builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference
		// system

		// add attributes in order
		builder.add("Location", Point.class);
		builder.add("Height ASL", Integer.class);
		builder.add("Bottom ASL", Integer.class);
		// builder.length(15).add("Name", String.class); // <- 15 chars width
		// for name field

		// build the type
		final SimpleFeatureType LOCATION = builder.buildFeatureType();

		return LOCATION;
	}

	/**
	 * Handle export.
	 */
	private void jExportActionPerformed(java.awt.event.ActionEvent evt) {
		JFileChooser fileopen = new JFileChooser();
		fileopen.setDialogType(JFileChooser.SAVE_DIALOG);
		fileopen.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				String t = f.getName().toLowerCase();
				return (f.isDirectory() || t.endsWith(".shp"));
			}

			@Override
			public String getDescription() {
				return "ERSI SHP File Format";
			}
		});
		fileopen.setDialogTitle("Export Data Points...");
		int ret = fileopen.showSaveDialog(myParent);
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = fileopen.getSelectedFile();
			if (file != null) {
				String name = file.getPath();
				if (!name.toLowerCase().endsWith(".shp")) {
					file = new File(file.getPath() + ".shp");
				}
				try {
					URL aURL = file.toURI().toURL();
					exportSHPPointData(aURL);
				} catch (MalformedURLException ex) {
				}
			}
		}
	}

	/**
	 * Export the points as a shp file and prj, etc... ESRI format
	 */
	public void exportSHPPointData(URL aURL) {

		/*
		 * We use the DataUtilities class to create a FeatureType that will
		 * describe the data in our shapefile.
		 *
		 * See also the createFeatureType method below for another, more
		 * flexible approach.
		 */
		try {
			final SimpleFeatureType TYPE = createFeatureType();

			/*
			 * We create a FeatureCollection into which we will put each Feature
			 * created from a record in the input csv data file
			 */
			// SimpleFeatureCollection collection =
			// FeatureCollections.newCollection();
			DefaultFeatureCollection collection = new DefaultFeatureCollection();
			/*
			 * GeometryFactory will be used to create the geometry attribute of
			 * each feature (a Point object for the location)
			 */
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

			List<LLD_X> list = myLLHAreaSet.getLocations();
			int i = 0;
			int counter = 1;
			LLHAreaSetMemento m = myLLHAreaSet.getMemento();
			Integer h = m.get(LLHAreaSetMemento.TOP_HEIGHT, 0);
			Integer b = m.get(LLHAreaSetMemento.BOTTOM_HEIGHT, 0);

			for (LLD_X l : list) {
				double latitude = l.latDegrees();
				double longitude = l.lonDegrees();
				/*
				 * Longitude (= x coord) first !
				 */
				Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

				featureBuilder.add(point);
				featureBuilder.add(h);
				featureBuilder.add(b);
				// featureBuilder.add(counter);
				SimpleFeature feature = featureBuilder.buildFeature(null);
				collection.add(feature);
			}

			/*
			 * Get an output file name and create the new shapefile
			 */
			File newFile = new File(aURL.getFile());

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);

			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(TYPE);

			/*
			 * You can comment out this line if you are using the
			 * createFeatureType method (at end of class file) rather than
			 * DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			/*
			 * Write the features to the shapefile
			 */
			Transaction transaction = new DefaultTransaction("create");

			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(collection);
					transaction.commit();

				} catch (Exception problem) {
					// problem.printStackTrace();
					LOG.debug("Exception writing file: " + problem.toString());
					transaction.rollback();

				} finally {
					transaction.close();
				}

			} else {
				LOG.debug(typeName + " does not support read/write access");
			}
		} catch (Exception e) {
			LOG.debug("Exception trying to write file:" + e.toString());
		}

	}

	// First pass at 'table' of lat/lon values to edit...
	/**
	 * Storage
	 */
	private static class LLHAreaSetTableData {

		public double lat;
		public double lon;
		public int index;
		public int polygon;
		public String note;
		public boolean selected;
	}

	private static class LLHAreaSetTableModel extends RowEntryTableModel<LLHAreaSetTableData> {

		public static final int OBJ_NUMBER = 0;
		public static final int OBJ_LATITUDE = 1;
		public static final int OBJ_LONGITUDE = 2;
		// public static final int OBJ_POLYGON = 3;
		public static final int OBJ_NOTE = 3;
		private boolean isRebuilding = false;

		public LLHAreaSetTableModel() {
			super(LLHAreaSetTableData.class, new String[] { "Point", "Latitude", "Longitude", "Note" });
		}

		@Override
		public boolean rebuilding() {
			return isRebuilding;
		}

		@Override
		public void setRebuilding(boolean value) {
			isRebuilding = value;
		}
	}

	/**
	 * Our custom renderer for drawing the table for the FeatureList
	 */
	private static class LLHAreaSetTableCellRenderer extends TableUtil.WG2TableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean cellHasFocus, int row, int col) {

			// Let super set all the defaults...
			super.getTableCellRendererComponent(table, "", isSelected, cellHasFocus, row, col);

			String info;
			int trueCol = table.convertColumnIndexToModel(col);

			// Each row uses a single LayerTableEntry...
			if (value instanceof LLHAreaSetTableData) {
				LLHAreaSetTableData e = (LLHAreaSetTableData) value;

				switch (trueCol) {
				case LLHAreaSetTableModel.OBJ_NUMBER:
					info = String.format("%04d-%d", row + 1, e.polygon);
					// info = Integer.toString(row +
					// 1)+"-"+Integer.toString(e.polygon);
					break;
				case LLHAreaSetTableModel.OBJ_LATITUDE:
					info = Double.toString(e.lat);
					break;
				case LLHAreaSetTableModel.OBJ_LONGITUDE:
					info = Double.toString(e.lon);
					break;
				case LLHAreaSetTableModel.OBJ_NOTE:
					info = e.note;
					break;
				default:
					info = "";
					break;
				}

				// For text...
				setText(info);
			} else {
				setText((String) (value));
			}
			return this;
		}
	}

	private JScrollPane initTable() {
		myLLHAreaSetTableModel = new LLHAreaSetTableModel();
		jObjects3DListTable = new RowEntryTable();
		final JTable myTable = jObjects3DListTable;
		jObjects3DListTable.setModel(myLLHAreaSetTableModel);
		final LLHAreaSetTableModel myModel = myLLHAreaSetTableModel;

		jObjects3DListTable.setFillsViewportHeight(true);
		jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jObjectScrollPane = new JScrollPane();
		jObjectScrollPane.setViewportView(jObjects3DListTable);

		LLHAreaSetTableCellRenderer p = new LLHAreaSetTableCellRenderer();
		jObjects3DListTable.setDefaultRenderer(LLHAreaSetTableData.class, p);

		int count = myTable.getColumnCount();
		TableColumnModel cm = myTable.getColumnModel();
		// Dimension d = aBox.getMinimumSize();
		// IconHeaderRenderer r = new IconHeaderRenderer();
		for (int i = 0; i < count; i++) {
			TableColumn col = cm.getColumn(i);
		}

		jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				jObjects3DListTableValueChanged(e);
			}
		});

		jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {
			class Item extends JMenuItem {

				private final LLHAreaSetTableData d;

				public Item(String s, LLHAreaSetTableData line) {
					super(s);
					d = line;
				}

				public LLHAreaSetTableData getData() {
					return d;
				}
			};

			@Override
			public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {

				// FIXME: Code a bit messy, we're just hacking the text value
				// for now. Probably will need a custom JPopupMenu that has
				// our Objects3DTableData in it.
				ActionListener al = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Item i = (Item) (e.getSource());
						String text = i.getText();
						int deleteIndex = i.getData().index;
						if (text.startsWith("Delete all")) {
							deleteIndex = -1; // all
							myLLHAreaSet.currentPolygonNumber = 1;
						}
						if (text.startsWith("Delete")) {
							PointRemoveCommand c = new PointRemoveCommand(myLLHAreaSet, deleteIndex, false);
							CommandManager.getInstance().executeCommand(c, true);
						}
					}
				};
				JPopupMenu popupmenu = new JPopupMenu();
				LLHAreaSetTableData entry = (LLHAreaSetTableData) (line);

				// Create Delete menu item
				Item i = new Item("Delete point " + (row + 1), entry);
				popupmenu.add(i);
				i.addActionListener(al);
				popupmenu.addSeparator();

				// Create Delete All menu item
				i = new Item("Delete all ", entry);
				popupmenu.add(i);
				i.addActionListener(al);
				return popupmenu;
			}

			@Override
			public void handleClick(Object stuff, int orgRow, int orgColumn) {

				if (stuff instanceof LLHAreaSetTableData) {
					LLHAreaSetTableData entry = (LLHAreaSetTableData) (stuff);

					switch (orgColumn) {

					default:
						break;
					}
				}
			}
		});

		// setUpSortingColumns();

		// Initial update (some stuff created on start up statically)
		updateTable(null);
		return jObjectScrollPane;
		// updateLabel();
	}

	/** Create the string used for 'auto-ing', or saving while navigating... */
	public void updateProductInfo() {
		String path = "";

		// FIXME: Be able to follow product of choice?
		String useKey = ProductManager.TOP_PRODUCT;
		ProductFeature tph = ProductManager.getInstance().getProductFeature(useKey);
		Product prod = null;
		if (tph != null) {
			prod = tph.getProduct();
			if (prod != null) {
				// DataType d = prod.getRawDataType();
				// Date aDate = d.getTime();
				Date aDate = prod.getTime();
				String dateString = myFormatter.format(aDate);

				// We could make the file output string format customizable
				path = String.format("%s-%s-%s.xml",
						// d.getTypeName(),
						// dateString);
						prod.getDataType(),
						// prod.getProductName(),
						prod.getSubType(), prod.getTimeStamp());


				//String dir = myLLHAreaSet.currentAuto;
				
				String oldAutoPath = myLLHAreaSet.currentFile;
				myLLHAreaSet.currentFile = path;
				
				// How to know if stuff changed?  Different path and/or directory...
				String compare1 = oldAutoPath+myAutoDir;
				String compare2 = path+myLLHAreaSet.currentAuto;
					
				// Only attempt to write/read data if the path has changed..
				if (!compare1.equals(compare2)) {
					LOG.debug("OLDPATH:"+oldAutoPath);
					LOG.debug("NEWPATH:"+path);
					// Write out the current stuff if we can...
					autoWrite(myAutoDir, oldAutoPath);
					// Read in new stuff if there...or keep the current point set for modification
					autoRead(myLLHAreaSet.currentAuto, path);
					// FIXME:
					// Really need to work on my property/update system, ugggh
					if (myAutoGUI != null){
					myAutoGUI.setText(myLLHAreaSet.currentAuto, myLLHAreaSet.currentFile);
					}
				}else{
					LOG.debug("PATHS NOT CHANGED:"+oldAutoPath);
				}
				myAutoDir = myLLHAreaSet.currentAuto;
			}
		}
		// LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>NAME IS "+path);
	}

	public void updateTable(Memento info) {

		// Current edit fields should be already synced to the list..
		String editNote = "";
		boolean fromGUI = false;
		if (info != null) {
			fromGUI = info.getEventSource() == this.myStringGUI;
			editNote = info.get(LLHAreaSetMemento.NOTE, editNote);
		}

		// Set the top product info. This isn't really the 'table' though...

		int currentLine = 0;
		int select = -1;

		ArrayList<LLHAreaSetTableData> aList = new ArrayList<LLHAreaSetTableData>();
		List<LLD_X> list = myLLHAreaSet.getLocations();
		int i = 0;
		for (LLD_X l : list) {

			// Make sure selected items data matches the editing fields
			// No only set on explicit enter key right...
			if (l.getSelected()) {
				if (fromGUI) {
					l.setNote(editNote);
				}
				myLLHAreaSet.currentNote = l.getNote();
				// Humm only set polygon on explicit selection so the 'new
				// polygon' works
				// myLLHAreaSet.currentPolygonNumber = l.getPolygon();
			}

			LLHAreaSetTableData d = new LLHAreaSetTableData();
			d.lat = l.latDegrees(); // possible jitter...
			d.lon = l.lonDegrees();
			d.index = i++;
			d.polygon = l.getPolygon();
			d.note = l.getNote();
			d.selected = l.getSelected();

			aList.add(d);
			if (l.getSelected()) {
				select = currentLine;
			}
			currentLine++;
		}

		// This code dups with NavView. Should have a class that handles/hides
		// all this stuff...
		myLLHAreaSetTableModel.setDataTypes(aList);
		myLLHAreaSetTableModel.fireTableDataChanged();

		if (select > -1) {
			select = jObjects3DListTable.convertRowIndexToView(select);

			// This of course fires an event, which calls
			// jObjects3DListTableValueChanged
			// which would send a command which would do this again in an
			// infinite loop. So we have a flag. We don't use isAdjusting
			// because it still fires and event when you set it false
			myLLHAreaSetTableModel.setRebuilding(true);
			jObjects3DListTable.setRowSelectionInterval(select, select);
			myLLHAreaSetTableModel.setRebuilding(false);
		}// else {
		//	LOG.info("*****>>SELECT IS -1.  Ok..");
		//}
	}

	private void jObjects3DListTableValueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}
		// We're in the updateTable and have set the selection to the old
		// value, we don't want to loop infinitely
		if (myLLHAreaSetTableModel.rebuilding()) {
			return;
		}
		int row = jObjects3DListTable.getSelectedRow();
		if (row > -1) {
			int dataRow = jObjects3DListTable.convertRowIndexToModel(row);
			LLHAreaSetTableData d = myLLHAreaSetTableModel.getDataForRow(dataRow);
			if (d != null) {
				// Set the values, the next memento will come from this.
				myLLHAreaSet.currentPolygonNumber = d.polygon;
				myLLHAreaSet.currentNote = d.note;

				PointSelectCommand c = new PointSelectCommand(myLLHAreaSet, row);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}
}
