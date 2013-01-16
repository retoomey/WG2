/*
 * LLHAreaSetGUI.java
 *
 * @author Robert Toomey
 * 
 */
package org.wdssii.gui.volumes;

import com.jidesoft.swing.JideButton;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gov.nasa.worldwind.geom.LatLon;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.PointRemoveCommand;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.views.ChartView;
import org.wdssii.gui.volumes.LLHAreaController.ToolbarMode;
import org.wdssii.gui.volumes.LLHAreaSet.LLHAreaSetMemento;
import org.wdssii.properties.PropertyGUI;
import org.wdssii.properties.gui.ComboStringGUI;
import org.wdssii.properties.gui.IntegerGUI;

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

    private static org.slf4j.Logger log = LoggerFactory.getLogger(LLHAreaSetGUI.class);
    private LLHAreaFeature myFeature;
    private final LLHAreaSet myLLHAreaSet;
    private LLHAreaSetTableModel myLLHAreaSetTableModel;
    private RowEntryTable jObjects3DListTable;
    private javax.swing.JScrollPane jObjectScrollPane;
    private JPanel myContainer;
    private JButton myAddRemoveButton, myNormalButton;

    /**
     * Creates new form LLHAreaSetGUI
     */
    public LLHAreaSetGUI(LLHAreaFeature f, LLHAreaSet owner) {
        myFeature = f;
        myLLHAreaSet = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        LLHAreaSetMemento m = myLLHAreaSet.getMemento();
        updateToMemento(m);
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
        super.activateGUI(parent);
        // FIXME: Restore old mouse mode?
    }

    @Override
    public void deactivateGUI() {
        // Go back to normal mouse mode when we aren't the GUI...
        LLHAreaController l = CommandManager.getInstance().getLLHController();
        if (l != null) {
            l.setMouseMode(ToolbarMode.NORMAL);
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

        JButton export = new JideButton("Export...");
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jExportActionPerformed(e);
            }
        });

        Icon i = SwingIconFactory.getIconByName("plus.png");
        JButton aButton = new JideButton("");
        aButton.setIcon(i);
        aButton.setToolTipText("Add point with click, remove with shift-click");
        aButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LLHAreaController l = CommandManager.getInstance().getLLHController();
                if (l != null) {
                    l.setMouseMode(ToolbarMode.ADD_REMOVE);
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
                LLHAreaController l = CommandManager.getInstance().getLLHController();
                if (l != null) {
                    l.setMouseMode(ToolbarMode.NORMAL);
                    updateToolbar();
                }
            }
        });
        bButton.setToolTipText("Normal mode, drag points");
        myNormalButton = bButton;

        toolbar.add(bButton);
        toolbar.add(aButton);
        toolbar.addSeparator();
        toolbar.add(export);

        // Create container for stock property controls
        myContainer = new JPanel();
        myContainer.setLayout(new MigLayout(new LC(), null, null));
        JScrollPane s = new JScrollPane();
        s.setViewportView(myContainer);

        // Add a list of charts
        ComboStringGUI.ArrayListProvider listMaker = new ComboStringGUI.ArrayListProvider() {
            @Override
            public ArrayList<String> getList() {
                ArrayList<ChartView> charts = ChartView.getList();
                ArrayList<String> list = new ArrayList<String>();
                for (ChartView v : charts) {
                    String key = v.getTitle();  // FIXME: should be get key since title can change
                    list.add(key);
                }
                return list;
            }
        };

        add(new ComboStringGUI(myFeature, LLHAreaSetMemento.RENDERER, "3D", this, listMaker));

        add(new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_ROWS, "Rows", this,
                10, 100, 1, ""));

        add(new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_COLS, "Cols", this,
                10, 100, 1, ""));

        add(new IntegerGUI(myFeature, LLHAreaSetMemento.TOP_HEIGHT, "Top", this,
                1, 20000, 1, "Meters"));

        add(new IntegerGUI(myFeature, LLHAreaSetMemento.BOTTOM_HEIGHT, "Bottom", this,
                0, 19999, 1, "Meters"));


        // Create the final layout...
        // Stick into split pane....
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                s, initTable());
        split.setResizeWeight(.50);
        add(toolbar, new CC().growX().dockNorth());
        add(split, new CC().growX().growY());

        updateTable(null);
        updateToolbar();
    }

    public void updateToolbar() {
        LLHAreaController l = CommandManager.getInstance().getLLHController();
        ToolbarMode m = ToolbarMode.INACTIVE;
        if (l != null) {
            m = l.getMouseMode();
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

    /**
     * Here is how you can use a SimpleFeatureType builder to create the schema
     * for your shapefile dynamically. <p> This method is an improvement on the
     * code used in the main method above (where we used
     * DataUtilities.createFeatureType) because we can set a Coordinate
     * Reference System for the FeatureType and a a maximum field length for the
     * 'name' field dddd
     */
    private static SimpleFeatureType createFeatureType() {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        builder.add("Location", Point.class);
        builder.add("Height ASL", Integer.class);
        builder.add("Bottom ASL", Integer.class);
        // builder.length(15).add("Name", String.class); // <- 15 chars width for name field

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
        int ret = fileopen.showSaveDialog(null);
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
         * We use the DataUtilities class to create a FeatureType that
         * will describe the data in our shapefile.
         *
         * See also the createFeatureType method below for another, more
         * flexible approach.
         */
        try {
            // final SimpleFeatureType TYPE = DataUtilities.createType("Location",
            //         "location:Point:srid=4326," + // <- the geometry attribute: Point type
            //         "name:String," + // <- a String attribute
            //         "number:Integer" // a number attribute
            //         );

            final SimpleFeatureType TYPE = createFeatureType();

            /*
             * We create a FeatureCollection into which we will put
             * each Feature created from a record in the input csv
             * data file
             */
            SimpleFeatureCollection collection = FeatureCollections.newCollection();
            /*
             * GeometryFactory will be used to create the geometry
             * attribute of each feature (a Point object for the
             * location)
             */
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            List<LatLon> list = myLLHAreaSet.getLocations();
            int i = 0;
            int counter = 1;
            LLHAreaSetMemento m = myLLHAreaSet.getMemento();
            Integer h = ((Integer) m.getPropertyValue(LLHAreaSetMemento.TOP_HEIGHT));
            if (h == null) {
                h = 0;
            }
            Integer b = ((Integer) m.getPropertyValue(LLHAreaSetMemento.BOTTOM_HEIGHT));
            if (b == null) {
                b = 0;
            }
            for (LatLon l : list) {
                LLHAreaSetTableData d = new LLHAreaSetTableData();
                double latitude = l.latitude.degrees;
                double longitude = l.longitude.degrees;
                /*
                 * Longitude (= x coord) first !
                 */
                Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

                featureBuilder.add(point);
                featureBuilder.add(h);
                featureBuilder.add(b);
                //  featureBuilder.add(counter);
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
             * createFeatureType method (at end of class file)
             * rather than DataUtilities.createType
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
                    log.debug("Exception writing file: " + problem.toString());
                    transaction.rollback();

                } finally {
                    transaction.close();
                }

            } else {
                log.debug(typeName + " does not support read/write access");
            }
        } catch (Exception e) {
            log.debug("Exception trying to write file:" + e.toString());
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
    }

    private class LLHAreaSetTableModel extends RowEntryTableModel<LLHAreaSetTableData> {

        public static final int OBJ_NUMBER = 0;
        public static final int OBJ_LATITUDE = 1;
        public static final int OBJ_LONGITUDE = 2;
        private boolean isRebuilding = false;

        public LLHAreaSetTableModel() {
            super(LLHAreaSetTableData.class, new String[]{
                        "Point", "Latitude", "Longitude"
                    });
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
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof LLHAreaSetTableData) {
                LLHAreaSetTableData e = (LLHAreaSetTableData) value;

                switch (trueCol) {
                    case LLHAreaSetTableModel.OBJ_NUMBER:
                        info = Integer.toString(row + 1);
                        break;
                    case LLHAreaSetTableModel.OBJ_LATITUDE:
                        info = Double.toString(e.lat);
                        break;
                    case LLHAreaSetTableModel.OBJ_LONGITUDE:
                        info = Double.toString(e.lon);
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
        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        // IconHeaderRenderer r = new IconHeaderRenderer();
        for (int i = 0;
                i < count;
                i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            // col.setHeaderRenderer(r);
           /*
             * switch (i) { case LLHAreaTableModel.OBJ_LAT: {
             * IconHeaderInfo info = new
             * IconHeaderInfo("layervisible.png");
             * col.setHeaderValue(info); // FIXME: this isn't right,
             * how to do it with look + feel col.setWidth(2 *
             * d.width); col.setMaxWidth(2 * d.width);
             * col.setResizable(false); } break; case
             * FeatureListTableModel.OBJ_ONLY: { IconHeaderInfo info
             * = new IconHeaderInfo("picture.png");
             * col.setHeaderValue(info); // FIXME: this isn't right,
             * how to do it with look + feel col.setWidth(2 *
             * d.width); col.setMaxWidth(2 * d.width);
             * col.setResizable(false); } break; }
             */
        }

        jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                //  jObjects3DListTableValueChanged(e);
            }
        });

        jObjects3DListTable.addMouseListener(
                new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {
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
                        // for now.  Probably will need a custom JPopupMenu that has
                        // our Objects3DTableData in it.
                        ActionListener al = new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Item i = (Item) (e.getSource());
                                String text = i.getText();
                                int deleteIndex = i.getData().index;
                                if (text.startsWith("Delete all")) {
                                    deleteIndex = -1; // all
                                }
                                if (text.startsWith("Delete")) {
                                    PointRemoveCommand c = new PointRemoveCommand(myLLHAreaSet, deleteIndex);
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
        updateTable(
                null);
        return jObjectScrollPane;
        //updateLabel();
    }

    public void updateTable(Object info) {

        // We only want to change selection when the user directly
        // changes one, not from other updates like from looping
        boolean changeSelection = false;

        int currentLine = 0;
        int select = -1;
        int oldSelect = -1;

        //myLLHAreaSet.getLocationList(); GUI thread different
        ArrayList<LLHAreaSetTableData> aList = new ArrayList<LLHAreaSetTableData>();
        List<LatLon> list = myLLHAreaSet.getLocations();
        int i = 0;
        for (LatLon l : list) {
            LLHAreaSetTableData d = new LLHAreaSetTableData();
            d.lat = l.latitude.degrees;  // possible jitter...
            d.lon = l.longitude.degrees;
            d.index = i++;
            aList.add(d);
        }
        /*
         * for (Feature d : f) { FeatureListTableData d2 = new
         * FeatureListTableData(); d2.visibleName = d.getName();
         * d2.group = d.getFeatureGroup(); d2.checked = d.getVisible();
         * // methods allow internal locking d2.keyName = d.getKey();
         * d2.onlyMode = d.getOnlyMode(); d2.message = d.getMessage();
         * d2.candelete = d.getDeletable(); newList.add(d2); if
         * (topFeature == d) { select = currentLine; } if
         * (myLastSelectedFeature == d) { oldSelect = currentLine; }
         * currentLine++; }
         * myFeatureListTableModel.setDataTypes(newList);
         * myFeatureListTableModel.fireTableDataChanged();
         *
         * // Keep old selection unless it's gone... if
         * (!changeSelection) { // Use old selection if exists... if
         * (oldSelect > 0) { select = oldSelect; topFeature =
         * myLastSelectedFeature; } } else { //log.debug("CHANGE
         * SELECTION IS TRUE"); }
         *
         */
        myLLHAreaSetTableModel.setDataTypes(aList);
        myLLHAreaSetTableModel.fireTableDataChanged();
    }
}
