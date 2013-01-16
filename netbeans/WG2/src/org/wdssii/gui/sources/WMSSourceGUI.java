package org.wdssii.gui.sources;

import java.awt.Component;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.features.WorldwindStockFeature;
import org.wdssii.gui.sources.WMSSource.LayerInfo;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

/**
 * The GUI that handles controls for the WMS source
 *
 * @author Robert Toomey
 */
public class WMSSourceGUI extends javax.swing.JPanel implements SourceGUI {

    private static Logger log = LoggerFactory.getLogger(WMSSourceGUI.class);
    private WMSListTableModel myWMSListTableModel;
    private RowEntryTable myWMSTable;

    private static class WMSListTableData {

        public String title; // Name shown in list
        public String name;
        public String aabstract;
    }

    private class WMSListTableModel extends RowEntryTableModel<WMSListTableData> {

        public static final int WMS_TITLE = 0;
        public static final int WMS_NAME = 1;
        public static final int WMS_ABSTRACT = 2;
        private boolean isRebuilding = false;

        public WMSListTableModel() {
            super(WMSListTableData.class, new String[]{
                        "Title",
                        "Name",
                       // "Abstract"
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
     * Our custom renderer for drawing the Products table
     */
    private static class WMSListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof WMSListTableData) {
                WMSListTableData e = (WMSListTableData) value;

                switch (trueCol) {
                    case WMSListTableModel.WMS_TITLE:
                        info = e.title;
                        break;
                    case WMSListTableModel.WMS_NAME:
                        info = e.name;
                        break;
                    case WMSListTableModel.WMS_ABSTRACT:
                        info = e.aabstract;
                        break;
                    default:
                        info = Integer.toString(trueCol) + "," + col;
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

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        // Selection changed to us...fill the list...
        updateTables();
    }

    //private JScrollPane myScrollPane;
    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        parent.add(this, new CC().growX().growY());
        doLayout();
    }

    @Override
    public void deactivateGUI() {
    }
    /**
     * The WMSSource we are using
     */
    private WMSSource mySource;

    public WMSSourceGUI(WMSSource owner) {
        mySource = owner;
        setupComponents();
    }

    private void setupComponents() {
        setLayout(new MigLayout("insets 0",
		"[grow]",      // 1 column 
		"[][grow]"));  // 2 rows, last scroll so grow

        JLabel info = new JLabel("Double click or enter to add to basemaps");
        add(info, new CC().growX().wrap());
        
        JScrollPane myScrollPane = new JScrollPane();
        myScrollPane.setBorder(null);
        myWMSListTableModel = new WMSListTableModel();
        myWMSTable = new RowEntryTable();
        myWMSTable.setModel(myWMSListTableModel);
        myWMSTable.setFillsViewportHeight(true);
        myWMSTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myScrollPane.setViewportView(myWMSTable);
        myWMSTable.setDefaultRenderer(WMSListTableData.class, new WMSListTableCellRenderer());

        add(myScrollPane, new CC().growX().growY());

        myWMSTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                /**
                 * On double click, try to add the source
                 */
                if (e.getClickCount() == 2) {
                    addNewSource(e);
                }
            }
        });
        myWMSTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                /**
                 * Have to do enter pressed to snag before the table scroll
                 */
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNewSource(e);
                    e.consume();  // Don't scroll, super add multiple items
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
    }

    public void addNewSource(InputEvent e) {
        int row = myWMSTable.getSelectedRow();
        if (row > -1) {
            row = myWMSTable.convertRowIndexToModel(row);
            try {
                // FIXME: Should be a command....
                WMSListTableData data = myWMSListTableModel.getDataForRow(row);
                Object component = mySource.getLayerFromName(data.name);
                WorldwindStockFeature theBasemapsFeature = mySource.getWMSFeature();
                theBasemapsFeature.addWWComponent(this, data.name, component);
                
                // Fire generic FeatureCommand to update basemap table
                CommandManager.getInstance().executeCommand(new FeatureCommand(), true);
            } catch (Exception ex) {
                // recover by just doing nothing...
                log.error("Error adding layer " + ex.toString());
            }
        }
    }

    public void updateTables() {
        // Get a copy (synced so no worries here)
        ArrayList<LayerInfo> list = mySource.getLayers();
        ArrayList<WMSListTableData> dList = new ArrayList<WMSListTableData>();
        for (LayerInfo i : list) {
            i.getTitle();
            WMSListTableData data = new WMSListTableData();
            data.title = i.getTitle();
            data.name = i.getName();
            data.aabstract = i.getAbstract();
            dList.add(data);
        }
        myWMSListTableModel.setDataTypes(dList);
    }
}
