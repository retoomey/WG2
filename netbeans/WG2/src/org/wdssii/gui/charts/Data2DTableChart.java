package org.wdssii.gui.charts;

import org.wdssii.gui.worldwind.WorldWindDataView;
import com.jidesoft.swing.JideButton;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.globes.ElevationModel;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.axis.ValueAxis;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.geom.LLD;
import org.wdssii.geom.Location;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.volumes.LLHAreaFeature;
import org.wdssii.gui.products.Product2DTable;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.SimpleTable.ToolbarMode;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * Chart which displays a product tracking table
 *
 * @author Robert Toomey
 */
public class Data2DTableChart extends DataView {

    private final static Logger LOG = LoggerFactory.getLogger(Data2DTableChart.class);
    /**
     * The scroll panel we use to show chart
     */
    private JScrollPane jDataTableScrollPane;
    /**
     * Current mouse mode (editing) for this data table
     */
    private ToolbarMode myMouseMode = ToolbarMode.MOVE;
    /**
     * The main widget
     */
    private JComponent myPanel = null;
    /**
     * The selection label
     */
    private JLabel selectionLabel;
    /**
     * Current Product2DTable (created by product)
     */
    private Product2DTable myTable;
    private ProductFeature myLastProductFeature = null;

    /**
     * Static method to create a chart, called by reflection
     */
    public static Data2DTableChart create() {

        return new Data2DTableChart();
    }
    private JButton myMoveButton;
    private JButton mySelectButton;

    @Override
    public Object getNewGUIForChart(Object myChartBox) {

        // or should it be JComponent?
        JPanel holder = new JPanel();
        myPanel = holder;
        holder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        jDataTableScrollPane = new javax.swing.JScrollPane();
        holder.add(jDataTableScrollPane, new CC().growX().growY());

        JToolBar bar = initToolBar();
        holder.add(bar, new CC().dockNorth());
        // initTable();
        jDataTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jDataTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        updateDataTable();

        return holder;
    }

    /**
     * Update chart when needed (check should be done by chart)
     */
    @Override
    public void updateChart(boolean force) {
        updateDataTable();
    }

    /**
     * Readout XYZDataset is a JFreeChart dataset where we sample the product
     * value along the range.
     *
     * Would be nice to enhance for some feedback on how close we are to the
     * true readout 'point'
     */
    public static class ReadoutXYZDataset extends DynamicXYZDataset {

        public ReadoutXYZDataset() {
            super("Readout", 201);
        }
        /*
         * Resample data depending on zoom
         * level. This is called with the current lat/lon of the chart
         * so that the terrain can be resampled by zoom
         */

        public void syncToRange(ValueAxis x,
                double startLat,
                double startLon,
                double endLat,
                double endLon) {

            // Clear range
            clearRange();

            // Sample and fill in with new values
           // WorldWindView eb = FeatureList.theFeatures.getWWView();
           // Globe globe = eb.getWwd().getModel().getGlobe();
           // ElevationModel m = globe.getElevationModel();
            
            ElevationModel m = WorldWindDataView.getElevationModel();
            int size = getSampleSize();
            double deltaLat = (endLat - startLat) / (size - 1);
            double deltaLon = (endLon - startLon) / (size - 1);
            double lat = startLat;
            double lon = startLon;
            for (int i = 0; i < size; i++) {
                setSample(i, m.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon)) / 1000.0d);
                lat += deltaLat;
                lon += deltaLon;
            }

            // Set to new range
            setRange(x.getRange());
        }
    }

    /**
     * Set the key of the product to follow Should be called only within GUI
     * thread
     */
    @Override
    public void setUseProductKey(String p) {
        super.setUseProductKey(p);
        updateDataTable();
    }

    private JToolBar initToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        Icon i = SwingIconFactory.getIconByName("stock-tool-move-16.png");
        JButton first = new JideButton("");
        first.setIcon(i);
        first.setToolTipText("Move table with mouse");
        first.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMouseMode(ToolbarMode.MOVE);
            }
        });
        bar.add(first);
        myMoveButton = first;

        Icon i2 = SwingIconFactory.getIconByName("stock-tool-rect-select-16.png");
        JButton second = new JideButton("");
        second.setIcon(i2);
        second.setToolTipText("Select table with mouse");
        second.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMouseMode(ToolbarMode.SELECT);
            }
        });
        bar.add(second);
        mySelectButton = second;

        /* Don't need?  Not 100% sure yet
        JButton export = new JButton("Export INP...");
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jExportINPActionPerformed(e);
            }
        });
        bar.add(export);
        */

        JButton export2 = new JButton("Export CSV...");
        export2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jExportCSVActionPerformed(e);
            }
        });
        bar.add(export2);
        selectionLabel = new JLabel("");
        bar.add(selectionLabel);

        setMouseMode(ToolbarMode.MOVE);


        return bar;
    }

    public void setMouseMode(ToolbarMode m) {
        myMouseMode = m;
        if (myTable != null) {
            myTable.setMode(myMouseMode);
        }
        updateToolbar();
        updateMouseCursor();
    }

    public void updateToolbar() {

        if (myMoveButton != null) {
            myMoveButton.setSelected(myMouseMode == ToolbarMode.MOVE);
        }
        if (mySelectButton != null) {
            mySelectButton.setSelected(myMouseMode == ToolbarMode.SELECT);
        }
    }

    private void updateMouseCursor() {
        switch (myMouseMode) {
            case MOVE: // Mode mode
                myPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            default:
                break;
            case SELECT:  // Hand mode
                myPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                break;
        }

    }

    private void updateDataTable() {
        if (myPanel != null) {  // GUI might not exist yet
           // LOG.debug("UPDATE TABLE CALLED....");
            Product2DTable newTable;
            Product2DTable oldTable = myTable;

            // Check for ProductFeature change first...this means table type changed
            ProductFeature f = ProductManager.getInstance().getProductFeature(getUseProductKey());
            boolean changed = (f != myLastProductFeature);
            myLastProductFeature = f;

            // Different feature, get a new table...
            // this 'could' be better since if it's same product type we don't really need
            // a new table..but it could be loading and still null...this is easiest.
            // Also, we assume ALL products have a 2DTable class so we check null (once it's
            // loaded this will become not null)
            if ((f != null) && (changed || (oldTable == null))) {
                newTable = f.getNew2DTable();
            } else {
                newTable = myTable; // Keep same table
            }

            // Always check and replace table.  There may be no ProductFeature,
            // there may be no table...
            if (oldTable != newTable) {

                // Remove any old stuff completely
                myPanel.remove(jDataTableScrollPane);
                jDataTableScrollPane = new javax.swing.JScrollPane();
                myPanel.add(jDataTableScrollPane, new CC().growX().growY());

                // Add new stuff if there
                if (newTable != null) {
                    newTable.createInScrollPane(jDataTableScrollPane, f, myMouseMode);
                    //	LOG.debug("Installed 2D table " + newTable);
                }
                LOG.debug("Installed 2D table " + myTable + " --> " + newTable);
                myTable = newTable;

                // Link 3DRenderer (usually outline of product) to current stick...
                // Revalidate is delayed, we need it NOW because the GridVisibleArea
                // calculation needs a valid ViewRect
                //this.revalidate();
                myPanel.validate(); // update now
            }

            // Always register..bleh..this is because you can add a stick without changing table..
            // bleh...guess it's cheap enough to do for now
            // Ok we need 'Table Features' that are linked to data tables... FIXME
          // GOOP  FeatureList.theFeatures.remove3DRenderer(oldTable);
            LLHAreaFeature s = getTrackFeature();
            if (s != null) {
             // GOOP  s.addRenderer(newTable);
                LOG.debug("Added table to tracking feature "+newTable);
            }else{
                LOG.debug("Tracking feature was null");
            }

            // Feature update, move table to stick experiment...
            if (myTable != null) {
                Location l = getTrackLocation();
                if (l != null) {
                    myTable.centerToLocation(l);
                }
                // Product update, redraw current table...
                myTable.updateTable();
            }
        }
    }

    /**
     * Get the feature we are tracking
     */
    public LLHAreaFeature getTrackFeature() {
        // -------------------------------------------------------------------------
        // Snag the top stick for the moment
        LLHAreaFeature f = FeatureList.theFeatures.getTopMatch(new StickFilter());
        if (f != null) {
            return f;
        }
        return null;
    }

    /**
     * Return the Location we are currently tracking
     */
    public Location getTrackLocation() {
        Location L = null;
        LLHAreaSet stick = getTrackStick();
        if (stick != null) {
            // FIXME: would be nice to have a Position ability
            List<LLD> list = stick.getLocations();
            double[] alts = stick.getAltitudes();
            if (!list.isEmpty()) {
                LLD l = list.get(0);
                double a = alts[0]; // assuming correct
                L = new Location(l.latDegrees(), l.lonDegrees(), a);
            }
        }
        return L;
    }

    /**
     * Get the stick we are tracking
     */
    public LLHAreaSet getTrackStick() {

        // -------------------------------------------------------------------------
        // Snag the top stick for the moment
        LLHAreaSet stick = null;
        LLHAreaFeature f = getTrackFeature();
        if (f != null) {
            stick = (LLHAreaSet) f.getLLHArea();
        }
        return stick;
    }

    /**
     * Return an LLHAreaFeature that contains a LLHAreaHeightStick
     */
    private static class StickFilter implements FeatureList.FeatureFilter {

        @Override
        public boolean matches(Feature f) {
            if (f instanceof LLHAreaFeature) {
                LLHAreaFeature a = (LLHAreaFeature) f;
                LLHArea area = a.getLLHArea();
                if (area instanceof LLHAreaSet) {
                    if (area.getLocations().size() > 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private URL exportDialog(String end, String description, String title) {
        JFileChooser fileopen = new JFileChooser();
        fileopen.setDialogType(JFileChooser.SAVE_DIALOG);
        final String fend = end;
        final String fdescription = description;

        fileopen.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                // FIXME: need to get these from the Builders
                return (f.isDirectory() || t.endsWith(fend));
            }

            @Override
            public String getDescription() {
                return fdescription;
            }
        });

        fileopen.setDialogTitle(title);
        int ret = fileopen.showSaveDialog(myPanel);
        URL aURL = null;
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            String name = file.getPath();
            if (!name.toLowerCase().endsWith(fend)) {
                file = new File(file.getPath() + fend);
            }
            try {
                aURL = file.toURI().toURL();
                // if (myTable != null) {
                //     myTable.exportToURL(aURL);
                // }
            } catch (MalformedURLException ex) {
                // it's ok..return NULL;
            }
        }
        return aURL;
    }

    /**
     * Handle export of CSV (for now, will eventually need a factory, etc. )
     */
    private void jExportCSVActionPerformed(java.awt.event.ActionEvent evt) {
        URL aURL = exportDialog(".csv", "CSV format", "Export table as CSV");
        if (aURL != null) {
            if (myTable != null) {
                myTable.exportToURL(aURL, "CSV");
            }
        }
    }

    /**
     * Handle export.
     */
    private void jExportINPActionPerformed(java.awt.event.ActionEvent evt) {
        URL aURL = exportDialog(".inp", "INP Bim Wood format", "Export table as INP");
        if (aURL != null) {
            if (myTable != null) {
                myTable.exportToURL(aURL, "INP");
            }
        }
    }
}
