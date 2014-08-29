package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

/**
 *  ProductGroupView shows a list of all product collections in the
 * GUI.  Data display windows usually point to one of these.
 * 
 * Actually this will be products, maps, some other stuff too eventually...
 * 
 * @author Robert Toomey
 */
public class ProductGroupView extends JPanel {
   
        /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {
        public Factory() {
             super("Groups", "application_cascade.png");   
        }
        @Override
        public Component getNewComponent(){
            return new ProductGroupView();
        }
    }
    
    /** The object 3D list shows the list of 3d objects in the window
     */
    private DataGroupTableModel myDataGroupTableModel;
    private RowEntryTable jObjects3DListTable;
    
    /** Storage for the current product list */
    private static class DataGroupTableData {

        public String visibleName;
        public String keyName;
    }

    private static class DataGroupTableModel extends RowEntryTableModel<DataGroupTableData> {

        public static final int GROUP_NAME = 0;
        private boolean isRebuilding = false;

        public DataGroupTableModel() {
            super(DataGroupTableData.class, new String[]{
                        "Name"
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

    /** Our custom renderer for our product view table */
    private static class DataGroupTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof DataGroupTableData) {
                DataGroupTableData e = (DataGroupTableData) value;

                switch (trueCol) {

                    case DataGroupTableModel.GROUP_NAME:
                        info = e.visibleName;
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
    
    public ProductGroupView(){
        setLayout(new MigLayout("fill", "", ""));  
        initTable();
    }
    
    public final void initTable() {
        
        JScrollPane s = new JScrollPane();
        add(s, "grow");
        myDataGroupTableModel = new DataGroupTableModel();
        jObjects3DListTable = new RowEntryTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(myDataGroupTableModel);
        final DataGroupTableModel myModel = myDataGroupTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        s.setViewportView(jObjects3DListTable);

        DataGroupTableCellRenderer p = new DataGroupTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(DataGroupTableData.class, p);

        jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
              // @todo  jObjects3DListTableValueChanged(e);
            }
        });

        jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {

            class Item extends JMenuItem {

                private final DataGroupTableData d;

                public Item(String s, DataGroupTableData line) {
                    super(s);
                    d = line;
                }

                public DataGroupTableData getData() {
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
                      //  Item i = (Item) (e.getSource());
                      //  String text = i.getText();
                      //  if (text.startsWith("Delete")) {
                     //      LLHAreaDeleteCommand del = new LLHAreaDeleteCommand(i.getData().keyName);
                      //      CommandManager.getInstance().executeCommand(del, true);
                       // }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                DataGroupTableData entry = (DataGroupTableData) (line);
                String name = "Delete " + entry.visibleName;
                Item i = new Item(name, entry);
                popupmenu.add(i);
                i.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                /*if (stuff instanceof DataGroupTableData) {
                    DataGroupTableData entry = (DataGroupTableData) (stuff);

                    switch (orgColumn) {
                        case DataGroupTableModel.GROUP_NAME: {
                           
                        }
                        break;
                        default:
                            break;
                    }
                }*/
            }
        });

        setUpSortingColumns();

        updateTable();
    }
    
    public void updateTable() {
      
    }

    /** Set up sorting columns if wanted */
    private void setUpSortingColumns() {
    }
}
