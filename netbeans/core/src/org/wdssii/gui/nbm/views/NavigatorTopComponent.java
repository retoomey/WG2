package org.wdssii.gui.nbm.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.commands.ProductSelectCommand;
import org.wdssii.gui.commands.WdssiiCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.ProductNavigator;
import org.wdssii.gui.views.NavView;

@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Navigator//EN",
autostore = false)
@TopComponent.Description(preferredID = "NavigatorTopComponent",
iconBase = "org/wdssii/gui/nbm/views/eye.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.NavigatorTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_NavigatorAction",
preferredID = "NavigatorTopComponent")
/** Navigator allows us to move forward/back in time and up/down elevation
 * for a particular product.
 * 
 * @author Robert Toomey
 */
public final class NavigatorTopComponent extends TopComponent implements NavView {

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void ProductCommandUpdate(ProductCommand command) {
        testing(command);
    }

    public void ProductSelectCommandUpdate(ProductSelectCommand command) {
        testing(command);
    }

    public void SourceCommandUpdate(SourceCommand command) {
        testing(command);
    }

    public void AnimateCommandUpdate(AnimateCommand command) {
        testing(command);
    }
    private String SWING_THREAD;
    
    // Just like swt, Swing isn't thread safe. Soooo, we'll end up using the
    // same trick for any window updates...just snag the Swing thread name
    // and either 1. update immediately if thread is the Swing one, or
    // 2.  Invoke later (which will run in the swing thread)
    // FIXME: needs to be in a root class like in the RCP version.
    public void testing(WdssiiCommand command) {
        Thread t = Thread.currentThread();
        String currentName = t.getName();
        if (currentName.equals(SWING_THREAD)){
            // Go ahead and update swing widgets....
            updateNavButtons();
            updateProductList(command);
        }else{
            // Invoke it later in the swing thread....
            // FIXME: Maybe we should make sure only one of these is in there
            // at a time (so queue doesn't fill up)
            final WdssiiCommand aCommand = command;
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                   updateNavButtons();
                   updateProductList(aCommand);
                }
                
            });
        }
}
    private static final int myGridRows = 4;
    private static final int myGridCols = 4;
    private static final int myGridCount = myGridRows * myGridCols;
    private ArrayList<NavButton> myNavControls = new ArrayList<NavButton>();
    private javax.swing.JTable jProductsListTable;
    /** The product list shows the list of products in the window
     */
    private ProductsListTableModel myProductsListTableModel;

    /** Storage for the current product list */
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

    private class ProductsListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<ProductsTableData> myDataTypes;
        public static final int NAV_VISIBLE = 0;
        public static final int NAV_ONLY = 1;
        public static final int NAV_TIME = 2;
        public static final int NAV_TYPE = 3;
        public static final int NAV_SUBTYPE = 4;
        public static final int NAV_NAME = 5;
        public static final int NAV_MESSAGE = 6;

        public ProductsListTableModel() {

            this.headers = new String[]{
                "Visible", "Only", "Time", "Type", "Subtype", "Name", "Message"
            };
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myDataTypes != null) {
                size = myDataTypes.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            if (rowIndex == -1) {
                return "H";
            }
            if (myDataTypes != null) {
                if (rowIndex < myDataTypes.size()) {
                    return myDataTypes.get(rowIndex);
                }
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        public void setDataTypes(ArrayList<ProductsTableData> n) {
            myDataTypes = n;
            // Wow causes a null pointer exception in Swing...probably
            // because of changing the data out on the fly.  Just call
            // table.repaint after setDataTypes to force a full redraw.
            // this.fireTableDataChanged();
        }

        private ProductsTableData getProductTableDataForRow(int row) {
            ProductsTableData s = null;
            if (myDataTypes != null) {
                if ((row >= 0) && (row < myDataTypes.size())) {
                    s = myDataTypes.get(row);
                }
            }
            return s;
        }
    }

    /** Our custom renderer for our product view table */
    private static class ProductTableCellRenderer extends DefaultTableCellRenderer {

        protected ImageIcon createImageIcon(String path,
                String description) {
            java.net.URL imgURL = getClass().getResource(path);
            if (imgURL != null) {
                return new ImageIcon(imgURL, description);
            } else {
                return null;
            }
        }

        /** Code taken from open source example on web:
         * http://tips4java.wordpress.com/2009/02/27/default-table-header-cell-renderer/
         * 
         */
        protected Icon getIcon(JTable table, int column) {
            SortKey sortKey = getSortKey(table, column);
            if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
                switch (sortKey.getSortOrder()) {
                    case ASCENDING:
                        return UIManager.getIcon("Table.ascendingSortIcon");
                    case DESCENDING:
                        return UIManager.getIcon("Table.descendingSortIcon");
                }
            }
            return null;
        }

        protected String getSortText(JTable table, int column) {
            SortKey sortKey = getSortKey(table, column);
            if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
                switch (sortKey.getSortOrder()) {
                    case ASCENDING:
                        return "-";
                    case DESCENDING:
                        return "+";
                }
            }
            return "";
        }

        protected SortKey getSortKey(JTable table, int column) {
            RowSorter rowSorter = table.getRowSorter();
            if (rowSorter == null) {
                return null;
            }

            List sortedColumns = rowSorter.getSortKeys();
            if (sortedColumns.size() > 0) {
                return (SortKey) sortedColumns.get(0);
            }
            return null;
        }
        /** A shared JCheckBox for rendering every check box in the list */
        private JCheckBox checkbox = new JCheckBox();
        /** The icon for the 'visible' checkbox column of table */
        private ImageIcon p1 = createImageIcon("layervisible.png", "Layer is visible");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Only our headers are type 'string'
            if (value instanceof String) {
                info = (String) (value);

                // First, set the basic label stuff...
                super.getTableCellRendererComponent(table, info,
                        isSelected, cellHasFocus, row, col);

                // Change the needed drawing for a table 'header'
                // Note state is KEPT, so must undo this for other rows
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(BOTTOM);
                setHorizontalTextPosition(LEFT);
                setOpaque(false);
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                if (info.equals("Visible")) {
                    setIcon(p1);
                    setText(getSortText(table, col));
                } else {
                    setIcon(getIcon(table, col));

                }
                return this;

            }

            // Each row uses a single LayerTableEntry...
            if (value instanceof ProductsTableData) {
                ProductsTableData e = (ProductsTableData) value;

                switch (trueCol) {

                    case ProductsListTableModel.NAV_VISIBLE:

                        // We have to make sure we set EVERYTHING we use everytime,
                        // since we are just using a single checkbox.        
                        checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                        checkbox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                        checkbox.setEnabled(isEnabled());

                        checkbox.setSelected(e.checked);

                        checkbox.setFont(getFont());
                        checkbox.setFocusPainted(false);
                        checkbox.setBorderPainted(true);
                        checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
                                : noFocusBorder);
                        return checkbox;

                    case ProductsListTableModel.NAV_ONLY:

                        // We have to make sure we set EVERYTHING we use everytime,
                        // since we are just using a single checkbox.        
                        checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                        checkbox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                        checkbox.setEnabled(isEnabled());

                        checkbox.setSelected(e.checked);

                        checkbox.setFont(getFont());
                        checkbox.setFocusPainted(false);
                        checkbox.setBorderPainted(true);
                        checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
                                : noFocusBorder);
                        return checkbox;


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

                }

            }

            // Default is to render a stock label.
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(BOTTOM);
            //  setHorizontalTextPosition(LEFT);
            setOpaque((row != -1));
            setBorder(null);
            setIcon(null);
            return super.getTableCellRendererComponent(table, info,
                    isSelected, cellHasFocus, row, col);


        }
    }

    @Override
    public void update() {
        updateNavButtons();
        updateProductList(null);
    }

    /** Our special class for drawing the grid controls */
    public static class NavButton extends JButton {

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
                setEnabled(p.getValidRecord());
                Color c;
                if (p.getUseColor()) {
                    c = new Color(p.getRed(), p.getGreen(), p.getBlue());
                } else {
                    c = new Color(255, 0, 0);
                }

                // Everytime we update status we recreate the icon, which is kinda
                // messy..but then
                // again we could do things like change icon colors/animate? Lots of
                // possibilities.
                // Need to read SWT well make sure we aren't leaking here.
                //setImage(p.getIcon(myDisplay));
                setBackground(c);
                setEnabled(p.getEnabled());

            }
        }
    }

    public NavigatorTopComponent() {
        
        // Assume this is the Swing thread....
        // Looks like we are having thread issues updating the GUI, just
        // like swt it's not thread safe so we'll have to make it thread safe
    
        Thread t = Thread.currentThread();
        SWING_THREAD = t.getName();
        
        initComponents();
        initButtonGrid();
        initProductTable();

        updateNavButtons();
        updateProductList(null);

        CommandManager.getInstance().registerView(NavView.ID, this);
        setName(NbBundle.getMessage(NavigatorTopComponent.class, "CTL_NavigatorTopComponent"));
        setToolTipText(NbBundle.getMessage(NavigatorTopComponent.class, "HINT_NavigatorTopComponent"));

    }

    private void initButtonGrid() {
        GridLayout l = new GridLayout(myGridRows, myGridCols);
        jNavPanel.setLayout(l);
        l.setHgap(2);
        for (int i = 0; i < myGridCount; i++) {
            NavButton b = new NavButton("Test" + i, i);
            b.setBackground(Color.red);
            if (i == 0) {
                b.setVisible(false);
            }
            jNavPanel.add(b);
            myNavControls.add(b);
        }
    }

    private void initProductTable() {
        myProductsListTableModel = new ProductsListTableModel();
        jProductsListTable = new javax.swing.JTable();
        final JTable myTable = jProductsListTable;
        jProductsListTable.setModel(myProductsListTableModel);
        final ProductsListTableModel myModel = myProductsListTableModel;

        jProductsListTable.setFillsViewportHeight(true);
        jProductsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jProductsScrollPane.setViewportView(jProductsListTable);

        ProductTableCellRenderer p = new ProductTableCellRenderer();
        jProductsListTable.setDefaultRenderer(ProductsTableData.class, p);
        // Set each column individually, since above doesn't work.
        int count = jProductsListTable.getColumnCount();
        TableColumnModel cm = jProductsListTable.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            col.setCellRenderer(p);
            col.setHeaderRenderer(p);
        }

        jProductsListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jProductsListTableValueChanged(e);
            }
        });

        /** Add the mouse listener that handles clicking in any cell of our
         * custom Layer table
         */
        jProductsListTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // You actually want the single AND the double clicks so
                // that you always toggle even if they are clicking fast,
                // so we don't check click count.
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON2) {
                    // updateProductList();
                    return;
                }
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON1/* && e.getClickCount() == 1*/) {
                    Point p = e.getPoint();
                    int row = myTable.rowAtPoint(p);
                    int column = myTable.columnAtPoint(p);
                    int orgColumn = myTable.convertColumnIndexToModel(column);
                    int orgRow = myTable.convertRowIndexToModel(row);
                    Object stuff = myModel.getValueAt(orgRow, orgColumn);
                    if (stuff instanceof ProductsTableData) {
                        ProductsTableData entry = (ProductsTableData) (stuff);


                        /** a click on visible checkbox toggles layer visibility */
                          if (orgColumn == ProductsListTableModel.NAV_VISIBLE) {
                              
                        //     LayerList list = getLayerList();
                        //      Layer l = list.getLayerByName(entry.name);
                        //     l.setEnabled(!l.isEnabled());
                        //     updateProductList();
                          }
                    }
                }
            }
        });

    }

    private void updateProductList(WdssiiCommand command) {

        CommandManager m = CommandManager.getInstance();
        ProductHandlerList p = m.getProductOrderedSet();

        if (p != null) {
            ArrayList<ProductsTableData> sortedList = null;
            int currentLine = 0;
            int select = -1;

            // Create a new list only if not from a selection command
            sortedList = new ArrayList<ProductsTableData>();
            Iterator<ProductHandler> iter = p.getIterator();
            while (iter.hasNext()) {
                ProductHandler h = iter.next();

                ProductsTableData theData = new ProductsTableData();
                theData.visibleName = h.getListName();
                theData.keyName = h.getKey();
                theData.checked = h.getIsVisible();
                theData.onlyMode = h.getOnlyMode();
                theData.type = h.getProductType();
                theData.timeStamp = h.getTimeStamp();
                theData.subType = h.getSubType();
                theData.message = h.getMessage();
                sortedList.add(theData);

                if (p.getTopProductHandler() == h) {
                    select = currentLine;
                }
                currentLine++;
            }

      
try{
                myProductsListTableModel.setDataTypes(sortedList);
           
                //jProductsListTable.rev
                //jProductsListTable.updateUI();
}catch(Exception z){
    System.out.println("set exception "+z.toString());
}
             try {
                if (select > -1) {
                    jProductsListTable.setRowSelectionInterval(select, select);
                }


            } catch (Exception e) {
                Thread t = Thread.currentThread();
                String name = t.getName();
                System.out.println("DEBUG THIS:" + name + ", " + SWING_THREAD+", "+jProductsListTable + ", " + sortedList);
            }
                  jProductsListTable.repaint();
        }
    }

    private Product updateNavButtons() {
        // Update the navigation button array
        CommandManager m = CommandManager.getInstance();
        ProductHandlerList l = m.getProductOrderedSet();
        ProductHandler h = l.getTopProductHandler();
        Product d = null;
        if (h != null) {
            d = h.getProduct();
        }

        ProductNavigator n = null;
        if (d != null) {
            n = d.getNavigator();
        }

        // Update the button grid to the current ProductNavigator
        // product.  A product sets the commands/output of these
        // buttons depending on the product type
        for (NavButton b : myNavControls) {
            WdssiiCommand w = null;
            if (n != null) {
                w = n.getGridCommand(b.getGridIndex());
            }
            b.setCommand(w);
        }
        return d;

    }

    private void jProductsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = jProductsListTable.getSelectedRow();
        if (myProductsListTableModel != null) {
            ProductsTableData d = myProductsListTableModel.getProductTableDataForRow(row);
            ProductSelectCommand c = new ProductSelectCommand(d.keyName);
            CommandManager.getInstance().executeCommand(c, true);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jNavPanel = new javax.swing.JPanel();
        jLoopPanel = new javax.swing.JPanel();
        jProductsScrollPane = new javax.swing.JScrollPane();

        jNavPanel.setBackground(new java.awt.Color(0, 102, 51));
        jNavPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 0)));
        jNavPanel.setLayout(null);

        jLoopPanel.setBackground(new java.awt.Color(153, 0, 153));
        jLoopPanel.setLayout(null);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jNavPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
            .addComponent(jLoopPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
            .addComponent(jProductsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jNavPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLoopPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProductsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jLoopPanel;
    private javax.swing.JPanel jNavPanel;
    private javax.swing.JScrollPane jProductsScrollPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
