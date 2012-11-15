package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.WMSSourceAddParams;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

/**
 * WMS Catalog has a list of Web Map Service providers
 *
 * @author Robert Toomey
 */
public class WMSCatalog extends JPanel {
    
    private static Logger log = LoggerFactory.getLogger(WMSCatalog.class);
    private javax.swing.JTable jCatalogTable;
    private CatalogListTableModel myModel;
    private javax.swing.JScrollPane jScrollPane;

    /**
     * Storage for displaying the current feature list
     */
    private static class CatalogListData {

        public String visibleName; // Name shown in list       
        public String urlLocation; // location of the source
    }

    private class CatalogListTableModel extends RowEntryTableModel<CatalogListData> {

        public static final int CATALOG_NAME = 0;
        public static final int CATALOG_URL = 1;
        private boolean isRebuilding = false;

        public CatalogListTableModel() {
            super(CatalogListData.class, new String[]{
                        "Name", "URL"
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
    private static class CatalogListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof CatalogListData) {
                CatalogListData e = (CatalogListData) value;

                switch (trueCol) {
                    case CatalogListTableModel.CATALOG_NAME:
                        info = e.visibleName;
                        break;
                    case CatalogListTableModel.CATALOG_URL:
                        info = e.urlLocation;
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

    public WMSCatalog() {
        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        myModel = new CatalogListTableModel();
        jCatalogTable = new javax.swing.JTable();
        jCatalogTable.setModel(myModel);

        jCatalogTable.setFillsViewportHeight(true);
        jCatalogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane = new javax.swing.JScrollPane();
        jScrollPane.setViewportView(jCatalogTable);

        CatalogListTableCellRenderer p = new CatalogListTableCellRenderer();
        jCatalogTable.setDefaultRenderer(CatalogListData.class, p);

        add(jScrollPane, new CC().growX().growY());

        jCatalogTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                jTableValueChanged(e);
            }
        });

        jCatalogTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                /**
                 * On double click, try to add the source
                 */
                if (e.getClickCount() == 2) {
                    addNewSourceFromFields(true, true);
                }
            }
        });
        jCatalogTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                /**
                 * Have to do enter pressed to snag before the table scroll
                 */
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNewSourceFromFields(true, true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        updateListToCurrent();
    }

    /**
     * Update the table of bookmarks to the current combo box selection
     */
    private void updateListToCurrent() {

        ArrayList<CatalogListData> e = new ArrayList<CatalogListData>();
        CatalogListData n = new CatalogListData();
        n.visibleName = "Nasa";
        n.urlLocation = "http://neowms.sci.gsfc.nasa.gov/wms/wms";
        e.add(n);
      /*  n = new CatalogListData();
        n.visibleName = "Broken";
        n.urlLocation = "http://sedac.ciesin.columbia.edu/geoserver/gwc/service/wms";
        e.add(n);
        n = new CatalogListData();
        n.visibleName = "Goop";
        n.urlLocation = "http://terraserver-usa.com/ogccapabilities.ashx?version=1.1.1&request=getcapabilities&service=wms";
        e.add(n);
      */
        myModel.setDataTypes(e);
    }

    /**
     * From our manually added table, handle selection of a line by filling in
     * the fields
     */
    private void jTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            // return;
        }
    }

    /**
     * Try to add a source to the display from the currently showing fields
     */
    public void addNewSourceFromFields(boolean confirm, boolean report) {
        // String path = jURLTextField.getText();
        // String name = jNameTextField.getText();

        int row = jCatalogTable.getSelectedRow();
        if (row > -1) {
            row = jCatalogTable.convertRowIndexToModel(row);
            //  int column = target.getSelectedColumn();
            try {
                CatalogListData data = myModel.getDataForRow(row);
                
                WMSSourceAddParams p = new WMSSourceAddParams(data.visibleName, data.urlLocation, true);
                SourceAddCommand c = new SourceAddCommand(p);
                c.setConfirmReport(false, true, this);
                CommandManager.getInstance().executeCommand(c, false);
            } catch (Exception ex) {
                // recover by just doing nothing...
                log.debug("Couldn't create WMS source, "+ex.toString());
            }
        }



    }
}
