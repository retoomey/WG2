package org.wdssii.gui.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.navigators.ProductNavigator;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

/**
 * NavView shows the time/volume navigation controls for the current
 * top ProductFeature
 * 
 * @author Robert Toomey
 */
public final class NavView extends JThreadPanel implements CommandListener {

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void ProductCommandUpdate(ProductCommand command) {
        updateGUI(command);
    }

    public void FeatureCommandUpdate(FeatureCommand command) {
        updateGUI(command);
    }

    //public void ProductSelectCommandUpdate(ProductSelectCommand command) {
    //    updateGUI(command);
    //}
    public void SourceCommandUpdate(SourceCommand command) {
        updateGUI(command);
    }

    public void AnimateCommandUpdate(AnimateCommand command) {
        updateGUI(command);
    }
    private static final int myGridRows = 4;
    private static final int myGridCols = 4;
    private static final int myGridCount = myGridRows * myGridCols;
    private ArrayList<NavButton> myNavControls = new ArrayList<NavButton>();
    private RowEntryTable jProductsListTable;
    /**
     * The product list shows the list of products in the window
     */
    private ProductsListTableModel myProductsListTableModel;
    private JLabel jProductInfoLabel;
    private JPanel jNavPanel;
    private JPanel jLoopPanel;
    private JScrollPane jProductsScrollPane;

    @Override
    public void updateInSwingThread(Object command) {
        WdssiiCommand w = null;

        // Update the navigation button array
        ArrayList<ProductFeature> l = getProductFeatures();
        ProductFeature top =
                (ProductFeature) FeatureList.theFeatures.getSelected(ProductFeature.ProductGroup);

        updateContentDescription(l, top);
        updateNavButtons(l, top);
        updateProductList(w);
    }

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Navigator", "eye.png");
        }

        @Override
        public Component getNewComponent() {
	    NavView justOne = new NavView();
            CommandManager.getInstance().addListener("Navigation", justOne);
            return justOne;
        }
    }

    /**
     * Storage for the current product list
     */
    private static class ProductsTableData {

        public String visibleName; // Name shown in list
        public String keyName; // The key used to select this handler
        public boolean checked;
        public boolean onlyMode;
        public String type;
        public String timeStamp;
        public String subType;
        public String message;
    }

    private class ProductsListTableModel extends RowEntryTableModel<ProductsTableData> {

        // FIXME: should be an enum class probably...
        public static final int NAV_VISIBLE = 0;
        public static final int NAV_ONLY = 1;
        public static final int NAV_TIME = 2;
        public static final int NAV_NAME = 3;
        public static final int NAV_TYPE = 5;
        public static final int NAV_SUBTYPE = 4;
        public static final int NAV_MESSAGE = 6;

        public ProductsListTableModel() {
            super(ProductsTableData.class, new String[]{
                        "Visible", "Only", "Time", "Type", "Subtype", "Name", "Message"});
        }
    }

    /**
     * Our custom renderer for our product view table
     */
    private static class ProductTableCellRenderer extends WG2TableCellRenderer {

        /**
         * A shared JCheckBox for rendering every check box in the list
         */
        private JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof ProductsTableData) {
                ProductsTableData e = (ProductsTableData) value;

                switch (trueCol) {

                    case ProductsListTableModel.NAV_VISIBLE:
                        return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);

                    case ProductsListTableModel.NAV_ONLY:
                        // return getJCheckBox(table, e.onlyMode, isSelected, cellHasFocus, row, col);
                        return getJCheckBoxIcon(table, e.onlyMode, "picture.png", "pictures.png", isSelected, cellHasFocus, row, col);
                    case ProductsListTableModel.NAV_TIME:
                        info = e.timeStamp;
                        break;
                    case ProductsListTableModel.NAV_SUBTYPE:
                        info = e.subType;
                        break;
                    case ProductsListTableModel.NAV_TYPE:
                        info = e.type;
                        break;
                    case ProductsListTableModel.NAV_NAME:
                        info = e.visibleName;
                        break;
                    case ProductsListTableModel.NAV_MESSAGE:
                        info = e.message;
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

    /**
     * Our special class for drawing the grid controls
     */
    public static class NavButton extends SimplerJButton {

        private int myGridIndex;
        private WdssiiCommand myCommand = null;

        public NavButton(String title, int index) {
            super(title);
            myGridIndex = index;
            this.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    handleActionPerformed(e);
                }
            });
        }

        public void handleActionPerformed(ActionEvent e) {
            CommandManager m = CommandManager.getInstance();
            m.executeCommand(myCommand, true);
        }

        public int getGridIndex() {
            return myGridIndex;
        }

        public void setCommand(WdssiiCommand c) {
            myCommand = c;

            ProductButtonStatus p = null;
            if (myCommand == null) {
                setVisible(false);
            } else {
                setVisible(true);
                p = c.getButtonStatus();
            }
            updateNavButton(p);

        }

        public void updateNavButton(ProductButtonStatus p) {
            if (p == null) {
                setVisible(false);
            } else {
                setVisible(true);
                setText(p.getButtonText());
                setToolTipText(p.getToolTip());
                Color background;
                if (p.getUseColor()) {
                    background = new Color(p.getRed(), p.getGreen(), p.getBlue());
                    // FIXME: maybe should be contrasting color instead...
                    setForeground(Color.BLACK);
                } else {
                    setForeground(Color.BLACK);
                    background = Color.WHITE;
                }

                setIcon(SwingIconFactory.getIconByName(p.getIconString()));
                setBackground(background);
                setEnabled(p.getEnabled());
            }
        }
    }

    public NavView() {

        setLayout(new MigLayout("insets 0",
		"[grow]",               // 1 column 
		"[][pref!][][grow]"));  // 4 rows, last scroll so grow

        jProductInfoLabel = new javax.swing.JLabel(" ");
        jNavPanel = new javax.swing.JPanel();
        jNavPanel.setBackground(Color.BLACK);
        jLoopPanel = new javax.swing.JPanel();
        jProductsScrollPane = new javax.swing.JScrollPane();

        add(jProductInfoLabel, new CC().wrap());
        add(jNavPanel, new CC().growX().wrap());
        add(jLoopPanel, new CC().wrap());
        add(jProductsScrollPane, new CC().growX().growY());

        //  initComponents();
        initButtonGrid();
        initProductTable();

        // For box layout, have to align label center...
        jProductInfoLabel.setAlignmentX(LEFT_ALIGNMENT);

        updateInSwingThread(null);
    }

    private void initButtonGrid() {
        jNavPanel.setLayout(new MigLayout(new LC().fill().wrapAfter(4), null, null));
                                        
        // We want a fixed size for buttons so they don't jitter as 
        NavButton sizer = new NavButton("00:00:00  ", 0);
        Dimension pref = sizer.getPreferredSize();

        for (int i = 0; i < myGridCount; i++) {
            NavButton b = new NavButton("Test" + i, i);
            b.setPreferredSize(pref);
            b.setMinimumSize(pref);
            //  b.setBackground(Color.red);
            if (i == 0) {
                b.setVisible(false);
            }
            jNavPanel.add(b, "w min:pref:, growx");
            jNavPanel.add(b, new CC().width("min:pref:").growX());
            myNavControls.add(b);
        }
    }

    private void initProductTable() {
        myProductsListTableModel = new ProductsListTableModel();
        jProductsListTable = new RowEntryTable();
        final JTable myTable = jProductsListTable;
        jProductsListTable.setModel(myProductsListTableModel);
        final ProductsListTableModel myModel = myProductsListTableModel;

        jProductsListTable.setFillsViewportHeight(true);
        jProductsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jProductsScrollPane.setViewportView(jProductsListTable);

        ProductTableCellRenderer p = new ProductTableCellRenderer();
        jProductsListTable.setDefaultRenderer(ProductsTableData.class, p);

        int count = jProductsListTable.getColumnCount();
        TableColumnModel cm = jProductsListTable.getColumnModel();
        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        IconHeaderRenderer r = new IconHeaderRenderer();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            col.setHeaderRenderer(r);
            switch (i) {
                case ProductsListTableModel.NAV_VISIBLE: {
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
                case ProductsListTableModel.NAV_ONLY: {
                    IconHeaderInfo info = new IconHeaderInfo("picture.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                    break;
                }
            }
        }

        jProductsListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jProductsListTableValueChanged(e);
            }
        });

        /**
         * Add the mouse listener that handles clicking in any cell of our
         * custom Layer table
         */
        /*
         * jProductsListTable.addMouseListener(new MouseAdapter() {
         *
         * @Override public void mouseClicked(MouseEvent e) { // You actually
         * want the single AND the double clicks so // that you always toggle
         * even if they are clicking fast, // so we don't check click count. if
         * (e.getComponent().isEnabled() && e.getButton() == MouseEvent.BUTTON2)
         * { // updateProductList(); return; } if (e.getComponent().isEnabled()
         * && e.getButton() == MouseEvent.BUTTON1// && e.getClickCount() == 1//)
         * { Point p = e.getPoint(); int row = myTable.rowAtPoint(p); int column
         * = myTable.columnAtPoint(p);
         *
         * if ((row > -1) && (column > -1)) { int orgColumn =
         * myTable.convertColumnIndexToModel(column); int orgRow =
         * myTable.convertRowIndexToModel(row); Object stuff =
         * myModel.getValueAt(orgRow, orgColumn); if (stuff instanceof
         * ProductsTableData) { ProductsTableData entry = (ProductsTableData)
         * (stuff);
         *
         * switch (orgColumn) { case ProductsListTableModel.NAV_VISIBLE: {
         * ProductVisibleCommand c = new ProductVisibleCommand(entry.keyName,
         * !entry.checked); CommandManager.getInstance().executeCommand(c,
         * true); } break; case ProductsListTableModel.NAV_ONLY: {
         * ProductOnlyCommand c = new ProductOnlyCommand(entry.keyName,
         * !entry.onlyMode); CommandManager.getInstance().executeCommand(c,
         * true); } break; default: break; } } } } } });
         */
        jProductsListTable.addMouseListener(new RowEntryTableMouseAdapter(jProductsListTable, myModel) {

            class Item extends JMenuItem {

                private final ProductsTableData d;

                public Item(String s, ProductsTableData line) {
                    super(s);
                    d = line;
                }

                public ProductsTableData getData() {
                    return d;
                }
            };

            @Override
            public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {

                ActionListener al = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Item i = (Item) (e.getSource());
                        String text = i.getText();
                        if (text.startsWith("Delete")) {
                            ProductDeleteCommand del = new ProductDeleteCommand();
                            del.ProductDeleteByKey(i.getData().keyName);
                            CommandManager.getInstance().executeCommand(del, true);
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                ProductsTableData entry = (ProductsTableData) (line);
                String name = "Delete " + entry.visibleName;
                Item i = new Item(name, entry);
                popupmenu.add(i);
                i.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof ProductsTableData) {
                    ProductsTableData entry = (ProductsTableData) (stuff);

                    switch (orgColumn) {
                        case ProductsListTableModel.NAV_VISIBLE: {
                            Feature f = ProductManager.getInstance().getNamedFeature(entry.keyName);
                            if (f != null) {
                                FeatureMemento m = f.getNewMemento();
				m.setProperty(FeatureMemento.VISIBLE, !entry.checked);
                                FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        case ProductsListTableModel.NAV_ONLY: {
                            Feature f = ProductManager.getInstance().getNamedFeature(entry.keyName);
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
    }

    /**
     * Set up sorting columns if wanted
     */
    private void setUpSortingColumns() {

        /**
         * Set the sorters for each column
         */
        TableRowSorter<ProductsListTableModel> sorter =
                new TableRowSorter<ProductsListTableModel>(myProductsListTableModel);
        jProductsListTable.setRowSorter(sorter);

        for (int i = 0; i < ProductsListTableModel.NAV_MESSAGE; i++) {
            Comparator<ProductsTableData> c;
            switch (i) {
                case ProductsListTableModel.NAV_VISIBLE: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return Boolean.valueOf(o1.checked).compareTo(Boolean.valueOf(o2.checked));
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_ONLY: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return Boolean.valueOf(o1.onlyMode).compareTo(Boolean.valueOf(o2.onlyMode));
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_TIME: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            // FIXME: use Date object?
                            return (o1.timeStamp).compareTo(o2.timeStamp);
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_NAME: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            // FIXME: use Date object?
                            return (o1.visibleName).compareTo(o2.visibleName);
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_TYPE: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return (o1.type).compareTo(o2.type);
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_SUBTYPE: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return (o1.subType).compareTo(o2.subType);
                        }
                    };
                }
                break;
                case ProductsListTableModel.NAV_MESSAGE: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return (o1.message).compareTo(o2.message);
                        }
                    };
                }
                break;
                default: {
                    c = new Comparator<ProductsTableData>() {

                        @Override
                        public int compare(ProductsTableData o1, ProductsTableData o2) {
                            return (o1.visibleName).compareTo(o2.visibleName);
                        }
                    };
                }
                break;

            }
            sorter.setComparator(i, c);
        }

        jProductsListTable.getRowSorter().toggleSortOrder(ProductsListTableModel.NAV_NAME);
    }

    /**
     * Get our product handler list. For now at least, this is global
     */
    private ArrayList<ProductFeature> getProductFeatures() {

        List<ProductFeature> forg = ProductManager.getInstance().getProductFeatures();   
        ArrayList<ProductFeature> f = new ArrayList<ProductFeature>(forg);
        // Sort a copy of this list....might be better to keep a sorted list within
        // the FeatureList...we'll see how much this gets 'hit'
        Collections.sort(f,
                new Comparator<ProductFeature>() {

                    @Override
                    public int compare(ProductFeature o1, ProductFeature o2) {
                        return (o1.getKey().compareTo(o2.getKey()));
                    }
                });
        return f;
    }

    /**
     * Regenerate the list of products in the navigator
     */
    private void updateProductList(WdssiiCommand command) {

        ArrayList<ProductFeature> p = getProductFeatures();

        if (p != null) {
            ArrayList<ProductsTableData> sortedList;
            int currentLine = 0;
            int select = -1;

            // Create a new list only if not from a selection command
            sortedList = new ArrayList<ProductsTableData>();
            Iterator<ProductFeature> iter = p.iterator();
            ProductFeature top =
                    (ProductFeature) FeatureList.theFeatures.getSelected(ProductFeature.ProductGroup);
            while (iter.hasNext()) {
                ProductFeature h = iter.next();

                ProductsTableData theData = new ProductsTableData();
                theData.visibleName = h.getListName();
                theData.keyName = h.getKey();
                theData.checked = h.getVisible();
                theData.onlyMode = h.getOnlyMode();
                theData.type = h.getProductType();
                theData.timeStamp = h.getTimeStamp();
                theData.subType = h.getSubType();
                theData.message = h.getMessage();
                sortedList.add(theData);

                if (h == top) {
                    select = currentLine;
                }
                currentLine++;
            }
            myProductsListTableModel.setDataTypes(sortedList);
            myProductsListTableModel.fireTableDataChanged();


            if (select > -1) {
                select = jProductsListTable.convertRowIndexToView(select);

                // This of course fires an event, which calls jProductsListTableValueChanged
                // which would send a command which would do this again in an
                // infinite loop.  So we have a flag.  We don't use isAdjusting
                // because it still fires and event when you set it false
                myProductsListTableModel.setRebuilding(true);
                jProductsListTable.setRowSelectionInterval(select, select);
                myProductsListTableModel.setRebuilding(false);

            }
            jProductsListTable.repaint();
        }
    }

    private Product updateNavButtons(ArrayList<ProductFeature> l, ProductFeature top) {

        Product d = null;
        ProductNavigator n = null;
        if (top != null) {
            d = top.getProduct();
            n = top.getNavigator();
        }

        // Update the button grid to the current ProductNavigator
        // product.  A product sets the commands/output of these
        // buttons depending on the product type
        if (n != null) {
            for (NavButton b : myNavControls) {
                WdssiiCommand w = null;
                if (n != null) {
                    w = n.getGridCommand(b.getGridIndex());
                }
                b.setCommand(w);

            }
        } else {
            for (NavButton b : myNavControls) {
                b.setVisible(false);
            }
        }
        return d;
    }

    private void updateContentDescription(ArrayList<ProductFeature> l, ProductFeature top) {
        Product d = null;
        if (top != null) {
            d = top.getProduct();
        }
        String text;
        if (d == null) {
            text = "No Current Product";
        } else {
            text = d.getProductInfoString(true);
        }
        jProductInfoLabel.setText(text);
    }

    private void jProductsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        // We're in the updateTable and have set the selection to the old
        // value, we don't want to loop infinitely
        if (myProductsListTableModel.rebuilding()) {
            return;
        }
        int row = jProductsListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = jProductsListTable.convertRowIndexToModel(row);
            if (myProductsListTableModel != null) {
                ProductsTableData d = myProductsListTableModel.getDataForRow(dataRow);
                if (d != null) {
                    FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                    CommandManager.getInstance().executeCommand(c, true);
                }
            }
        }
    }
}
