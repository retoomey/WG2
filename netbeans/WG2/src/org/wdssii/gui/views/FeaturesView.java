package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;
import com.jidesoft.swing.JideSplitButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.swing.SwingGUIPlugInPanel;
import org.wdssii.gui.FeatureTableList;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.features.EarthBallFeature;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.features.LegendFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.MapGUI;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.views.WdssiiSDockedViewFactory.SDockView;

public class FeaturesView extends JThreadPanel implements SDockView, CommandListener {

    public static final String ID = "wdssii.FeaturesView";
    private final static Logger LOG = LoggerFactory.getLogger(FeaturesView.class);

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void FeatureCommandUpdate(FeatureCommand command) {
        updateGUI(command);
    }

    public void ProductCommandUpdate(ProductCommand command) {
        updateGUI(command);
    }

    @Override
    public void updateInSwingThread(Object command) {
        updateTable(command);
        updateLabel();
    }

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiSDockedViewFactory {

        public Factory() {
            super("Features", "brick_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new FeaturesView(myDockControls);
        }
    }
    private FeatureListTableModel myProductListTableModel;
    private RowEntryTable myProductListTable;
    private Feature myLastSelectedFeature = null;
    /**
     * Holds the regular controls of selected feature
     */
    private JPanel jFeatureGUIPanel;
    /**
     * Holds the optional table for selected feature
     */
    //private JPanel jFeatureGUITablePanel;
    JToolBar jEditToolBar;
 //   private javax.swing.JScrollPane jObjectScrollPane;
    private JComponent jControls;
    /**
     * Scroll bar for regular controls for feature
     */
    //private javax.swing.JScrollPane jControlScrollPane;
    /**
     * Scroll bar for features table, for example...attribute table for maps,
     * point table for the data point line
     */
    //private javax.swing.JScrollPane jTableScrollPane;
    JLabel jInfoLabel;
	private FeatureTableList myFeatureTableList;
	private FeatureTableList myFeatureTableList2;
	private FeatureTableList myFeatureTableList3;
	private FeatureTableList myFeatureTableList4;

    /**
     * Get the items for an individual view
     */
    @Override
    public void addGlobalCustomTitleBarComponents(List<Object> addTo) {

        Icon test = SwingIconFactory.getIconByName("plus.png");
        JideSplitButton button = new JideSplitButton("");
        button.setIcon(test);
        button.setToolTipText("Add a feature to the 3D window");
        button.setAlwaysDropdown(true);

        JMenuItem item;

        item = new JMenuItem("2 Points");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                addSetActionPerformed(evt, 2);
            }
        });
        button.add(item);

        item = new JMenuItem("1 Point");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                addSetActionPerformed(evt, 1);
            }
        });
        button.add(item);

        item = new JMenuItem("Map from ESRI shapefile...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        button.add(item);
        item = new JMenuItem("Polargrid");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                jPolarGridButtonActionPerformed(evt);
            }
        });
        button.add(item);
        addTo.add(button);
    }

    /**
     * Storage for displaying the current feature list
     */
    private static class FeatureListTableData {

        public String visibleName; // Name shown in list
        public String group; // The feature group such as 'maps'
        public String keyName; // The key used to select this handler
        public boolean checked;
        public boolean onlyMode;
        // Used by the navigator right...
        public String type;
        public String timeStamp;
        public String subType;
        public String message;
        public boolean candelete;
    }

    private static class FeatureListTableModel extends RowEntryTableModel<FeatureListTableData> {

        public static final int OBJ_VISIBLE = 0;
        public static final int OBJ_ONLY = 1;
        public static final int OBJ_NAME = 2;
        public static final int OBJ_GROUP = 3;
        public static final int OBJ_MESSAGE = 4;
        private boolean isRebuilding = false;

        public FeatureListTableModel() {
            super(FeatureListTableData.class, new String[]{
                "Visible", "Only", "Name", "Type", "Message"
            });
        }
    }

    /**
     * Our custom renderer for drawing the table for the FeatureList
     */
    private static class FeatureListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof FeatureListTableData) {
                FeatureListTableData e = (FeatureListTableData) value;

                switch (trueCol) {

                    case FeatureListTableModel.OBJ_VISIBLE:
                        return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);
                    case FeatureListTableModel.OBJ_ONLY:
                        return getJCheckBoxIcon(table, e.onlyMode, "picture.png", "pictures.png", isSelected, cellHasFocus, row, col);
                    case FeatureListTableModel.OBJ_NAME:
                        info = e.visibleName;
                        break;
                    case FeatureListTableModel.OBJ_MESSAGE:
                        info = e.message;
                        break;
                    case FeatureListTableModel.OBJ_GROUP:
                        info = e.group;
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

    public FeaturesView(boolean dockControls) {
        initComponents(dockControls);

        CommandManager.getInstance().addListener(FeaturesView.ID, this);
    }

    private void initTable() {
    	
    	// Fields...
        myProductListTableModel = new FeatureListTableModel();
        myProductListTable = new RowEntryTable();
        
        final RowEntryTable t = myProductListTable;
        final FeatureListTableModel m = myProductListTableModel;

        t.setModel(m);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
       // jObjectScrollPane.setViewportView(myProductListTable);

        FeatureListTableCellRenderer p = new FeatureListTableCellRenderer();
        t.setDefaultRenderer(FeatureListTableData.class, p);

        TableColumnModel cm = t.getColumnModel();
        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        IconHeaderRenderer r = new IconHeaderRenderer();

        final int count = t.getColumnCount();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(r);
            switch (i) {
                case FeatureListTableModel.OBJ_VISIBLE: {
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
                case FeatureListTableModel.OBJ_ONLY: {
                    IconHeaderInfo info = new IconHeaderInfo("picture.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
            }
        }

        t.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                jObjects3DListTableValueChanged(e);
            }
        });

        t.addMouseListener(new RowEntryTableMouseAdapter(t, m) {
            class Item extends JMenuItem {

                private final FeatureListTableData d;

                public Item(String s, FeatureListTableData line) {
                    super(s);
                    d = line;
                }

                public FeatureListTableData getData() {
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
                        if (text.startsWith("Delete")) {
                            FeatureDeleteCommand del = new FeatureDeleteCommand(i.getData().keyName);
                            CommandManager.getInstance().executeCommand(del, true);
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                FeatureListTableData entry = (FeatureListTableData) (line);
                if (entry.candelete) {
                    String name = "Delete " + entry.visibleName;
                    Item i = new Item(name, entry);
                    popupmenu.add(i);
                    i.addActionListener(al);
                } else {
                    String name = "This feature cannot be deleted";
                    Item i = new Item(name, entry);
                    popupmenu.add(i);
                }
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {
               // LOG.debug("Clicking here " + stuff + ", " + orgRow);

                if (stuff instanceof FeatureListTableData) {
                    FeatureListTableData entry = (FeatureListTableData) (stuff);

                    switch (orgColumn) {
                        case FeatureListTableModel.OBJ_VISIBLE: {
                            Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
                            if (f != null) {
                                FeatureMemento m = f.getNewMemento();
                                m.setProperty(FeatureMemento.VISIBLE, !entry.checked);
                                FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        case FeatureListTableModel.OBJ_ONLY: {
                            Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
                            if (f != null) {
                                FeatureMemento m = f.getNewMemento();
                                m.setProperty(FeatureMemento.ONLY, !entry.onlyMode);
                                FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }
            }
        });

        setUpSortingColumns();

        // Initial update (some stuff created on start up statically)
        updateTable(null);
        updateLabel();
    }
    // Disable for now since this does nothing yet

    /**
     * Set up sorting columns if wanted
     */
    private void setUpSortingColumns() {
    }

    private void jObjects3DListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        // We're in the updateTable and have set the selection to the old
        // alue, we don't want to loop infinitely
        if (myProductListTableModel.rebuilding()) {
            return;
        }
        int row = myProductListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = myProductListTable.convertRowIndexToModel(row);
            FeatureListTableData d = (FeatureListTableData) (myProductListTableModel.getDataForRow(dataRow));
            if (d != null) {
                FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
    }
    
    public void updateTable(Object info) {
        myFeatureTableList.updateList(info);
        myFeatureTableList2.updateList(info);
        myFeatureTableList3.updateList(info);
        myFeatureTableList4.updateList(info);

        // We only want to change selection when the user directly
        // changes one, not from other updates like from looping
        boolean changeSelection = false;
        Feature fromSelect = null;
        if (info instanceof FeatureSelectCommand) {
            FeatureSelectCommand c = (FeatureSelectCommand) (info);
            changeSelection = true;
            fromSelect = c.getFeature();
            //LOG.debug("******SELECTCOMMAND " + fromSelect);

        }
        final FeatureList flist = FeatureList.theFeatures;

        /**
         * Static for now...
         */
        List<Feature> forg = flist.getFeatures();
        ArrayList<Feature> f = new ArrayList<Feature>(forg);
        // Sort this list....might be better to keep a sorted list within
        // the FeatureList...we'll see how much this gets 'hit'
        Collections.sort(f,
                new Comparator<Feature>() {
            @Override
            public int compare(Feature o1, Feature o2) {
                String k1 = o1.getFeatureGroup();
                String k2 = o2.getFeatureGroup();
                if (k1.equals(ProductFeature.ProductGroup)) {
                    k1 = "0";
                }
                if (k2.equals(ProductFeature.ProductGroup)) {
                    k2 = "0";
                }
                int c = k1.compareTo(k2);
                if (c == 0) { // same group, sort by key name...
                    c = o1.getKey().compareTo(o2.getKey());
                }
                return c;
            }
        });

        int currentLine = 0;
        int select = -1;
        int oldSelect = -1;
        ArrayList<FeatureListTableData> newList = new ArrayList<FeatureListTableData>();
        Feature topFeature = flist.getTopSelected();
        //LOG.debug("Top selected in was "+topFeature);
        if (changeSelection) {
            if (fromSelect != topFeature) {
                //	LOG.error("**********NOT THE SAME "+fromSelect+", "+topFeature);
            }
        }

        // Fill in the table(s).  Split by group name...
        for (Feature d : f) {
            FeatureListTableData d2 = new FeatureListTableData();
            d2.visibleName = d.getName();
            d2.group = d.getFeatureGroup();
            d2.checked = d.getVisible();  // methods allow internal locking
            d2.keyName = d.getKey();
            d2.onlyMode = d.getOnlyMode();
            d2.message = d.getMessage();
            d2.candelete = d.getDeletable();
            newList.add(d2);
            if (topFeature == d) {
                select = currentLine;
            }
            if (myLastSelectedFeature == d) {
                oldSelect = currentLine;
            }
            currentLine++;
        }
        myProductListTableModel.setDataTypes(newList);
        myProductListTableModel.fireTableDataChanged();
        // Keep old selection unless it's gone...
        if (!changeSelection) {
            // Use old selection if exists...
            if (oldSelect > 0) {
                select = oldSelect;
                topFeature = myLastSelectedFeature;
            }
        } else {
            //LOG.debug("CHANGE SELECTION IS TRUE");
        }

        if (select > -1) {
            select = myProductListTable.convertRowIndexToView(select);

            // This of course fires an event, which calls jProductsListTableValueChanged
            // which would send a command which would do this again in an
            // infinite loop.  So we have a flag.  We don't use isAdjusting
            // because it still fires and event when you set it false
            myProductListTableModel.setRebuilding(true);

            myProductListTable.setRowSelectionInterval(select, select);

            // Swap from old controls to new controls
            FeatureGUI newControls = null;
            if (topFeature != null) {
                newControls = topFeature.getControls();
            }
            FeatureGUI lastControls = null;
            if (myLastSelectedFeature != null) {
                lastControls = myLastSelectedFeature.getControls();
            }
            boolean swapped = SwingGUIPlugInPanel.swapToPanel(jFeatureGUIPanel, lastControls, newControls);
            myLastSelectedFeature = topFeature;
            if (swapped) {
                myLastSelectedFeature = topFeature;
            }

            myProductListTableModel.setRebuilding(false);

        } else {
            setEmptyControls();
            jFeatureGUIPanel.validate();
            jFeatureGUIPanel.repaint();
            myLastSelectedFeature = null;
        }
        myProductListTable.repaint();
        
        // Hack updates...need a better system for this
        if (myLastSelectedFeature != null){
            myLastSelectedFeature.getControls().updateGUI();
        }
        // Notification for product changed...
         FeatureList.theFeatures.sendMessage("product");

    }

    private void updateLabel() {
        jInfoLabel.setText(FeatureList.theFeatures.getGUIInfoString());
    }

    private JComponent initFeatureControlGUI() {
        // Single no table area split
        // jFeatureGUIPanel = new JPanel();
        // jControlScrollPane = new JScrollPane();
        // jControlScrollPane.setViewportView(jFeatureGUIPanel);
        // jFeatureGUIPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        // setEmptyControls();
        // return jControlScrollPane;

        // JSplitPane split = new JSplitPane();
        //split.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        // The top part of feature controls
        jFeatureGUIPanel = new JPanel();
        jFeatureGUIPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        // jControlScrollPane = new JScrollPane();
        //jControlScrollPane.setViewportView(jFeatureGUIPanel);
        //split.setTopComponent(jControlScrollPane);

        // The optional bottom part of feature controls
        //jFeatureGUITablePanel = new JPanel();
        //jFeatureGUITablePanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        //  jTableScrollPane = new JScrollPane();
        //  jTableScrollPane.setViewportView(jFeatureGUITablePanel);
        // split.setBottomComponent(jTableScrollPane);
        //split.setBottomComponent(jFeatureGUITablePanel);
        //split.setDividerLocation(100);

        setEmptyControls();
        return jFeatureGUIPanel;

    }

    /**
     * The part of the GUI that deals with selection of a individual feature
     */
 /*   private JComponent initFeatureSelectGUIoldcode() {

// only done once...
        jObjectScrollPane = new JScrollPane();
        jObjectScrollPane.setBorder(null);
        jInfoLabel = new JLabel("---");
        return jObjectScrollPane;
    }
*/
    
    private JComponent tabForGroup(Color aColor, String groupName, FeatureTableList list )
    {
    	 JComponent w3 = new JPanel();
         w3.setBackground(aColor);
         w3.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
       //  myFeatureTableList = new FeatureTableList(groupName);
         javax.swing.JScrollPane testScroll = new JScrollPane();
         testScroll.setBorder(null);
         //	add(toolbar, new CC().dockNorth());
         w3.add(new JLabel(groupName), new CC().dockNorth().growX());
         w3.add(testScroll, new CC().growX().growY());
         testScroll.setViewportView(list);
         return w3;
    }
    
    private void initComponents(boolean dockControls) {

    	// Create a tab per group type.  For the moment hard coded..
    	// but could be done dynamically.
        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
         
        // Tab panel for each feature group
        JComponent w = new JPanel();
        w.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        jInfoLabel = new JLabel("---");
        w.add(jInfoLabel, new CC().dockNorth().growX());
        JScrollPane jObjectScrollPane = new JScrollPane();
        w.add(jObjectScrollPane, new CC().growX().growY());
        
        JTabbedPane t = new javax.swing.JTabbedPane();
        t.addTab("All", w);
        myFeatureTableList3 = new FeatureTableList(new String[] {ProductFeature.ProductGroup});
        t.addTab("Products", tabForGroup(Color.GREEN, "PRODUCTS!", myFeatureTableList3));   
        myFeatureTableList = new FeatureTableList(new String[] {EarthBallFeature.MapGroup, MapFeature.MapGroup});
        t.addTab("Maps", tabForGroup(Color.GREEN, "MAPS!", myFeatureTableList));      
        myFeatureTableList2 = new FeatureTableList(
        		new String[] {LegendFeature.LegendGroup});
        t.addTab("2D Overlays", tabForGroup(Color.GREEN, "2D!", myFeatureTableList2));
        myFeatureTableList4 = new FeatureTableList(
        		new String[] {LLHAreaFeature.LLHAreaGroup, PolarGridFeature.PolarGridGroup});
        t.addTab("3D Overlays", tabForGroup(Color.GREEN, "3D!", myFeatureTableList4));
      
        // Controls for latest selected feature
        // This split pane could be done by our new generic window manager
        jControls = initFeatureControlGUI();
        JSplitPane s = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                t, jControls);
        s.setResizeWeight(.50);
        add(s, new CC().growX().growY());
 
        // to size correct, init table last, nope not it
        initTable();
        myFeatureTableList.updateList(null);
        myFeatureTableList2.updateList(null);
        myFeatureTableList3.updateList(null);
        myFeatureTableList4.updateList(null);
        
        // Have to have table object before assigning to scroll pane
        myProductListTable.setFillsViewportHeight(true);
        jObjectScrollPane.setViewportView(myProductListTable);
    }

    @Override
    public Component getControlComponent() {
        return jControls;
    }

    @Override
    public String getControlTitle() {
        return "Feature Controls";
    }

    public JComponent getToolBar() {
        return jEditToolBar;
    }

    public JComponent getInfoLabel() {
        return jInfoLabel;
    }

    private void setEmptyControls() {
        jFeatureGUIPanel.removeAll();
        JTextField t = new javax.swing.JTextField();
        t.setText("Controls for selected feature");
        t.setEditable(false);
        jFeatureGUIPanel.setLayout(new java.awt.BorderLayout());
        jFeatureGUIPanel.add(t, java.awt.BorderLayout.CENTER);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        URL newMap = MapGUI.doSingleMapOpenDialog(this);
        if (newMap != null) {
            Feature testOne = new MapFeature(FeatureList.theFeatures, newMap);
            FeatureList.theFeatures.addFeature(testOne);
        }
        updateGUI();
    }

    private void addSetActionPerformed(java.awt.event.ActionEvent evt, int count) {
        FeatureCreateCommand doit = new FeatureCreateCommand("Set", Integer.valueOf(count));
        CommandManager.getInstance().executeCommand(doit, true);
        //FeatureList.theFeatures.addFeature(new LLHAreaFeature(0));
    }

    private void jPolarGridButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // FIXME: need to generize the feature create command...
        PolarGridFeature pg = new PolarGridFeature(FeatureList.theFeatures);
        FeatureList.theFeatures.addFeature(pg);
        updateGUI();
    }
}
