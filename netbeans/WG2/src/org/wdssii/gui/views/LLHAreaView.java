package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.commands.LLHAreaCreateCommand;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.commands.LLHAreaChangeCommand;
import org.wdssii.gui.commands.LLHAreaCommand;
import org.wdssii.gui.commands.LLHAreaDeleteCommand;
import org.wdssii.gui.commands.LLHAreaSelectCommand;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHArea.LLHAreaMemento;

public class LLHAreaView extends JThreadPanel implements CommandListener {

    public static final String ID = "wdssii.LLHAreaView";
    private static Logger log = LoggerFactory.getLogger(LLHAreaView.class);

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void LLHAreaCommandUpdate(LLHAreaCommand command) {
        updateGUI(command);
    }

    @Override
    public void updateInSwingThread(Object command) {
        updateTable();
    }

    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("3D Objects", "brick_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new LLHAreaView();
        }
    }
    /** The object 3D list shows the list of 3d objects in the window
     */
    private Object3DListTableModel myObject3DListTableModel;
    private RowEntryTable jObjects3DListTable;
    private LLHArea myLastSelected = null;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private JPanel jControlsToolBar;
    private javax.swing.JToolBar jEditToolBar;
    private javax.swing.JScrollPane jObjectScrollPane;
    private javax.swing.JScrollPane jControlScrollPane;

    /** Storage for the current product list */
    private static class Objects3DTableData {

        public String visibleName; // Name shown in list
        public String keyName; // The key used to select this handler
        public boolean checked;
        public boolean onlyMode;
        public String type;
        public String timeStamp;
        public String subType;
        public String message;
    }

    private class Object3DListTableModel extends RowEntryTableModel<Objects3DTableData> {

        public static final int OBJ_VISIBLE = 0;
        public static final int OBJ_ONLY = 1;
        public static final int OBJ_NAME = 2;
        public static final int OBJ_MESSAGE = 3;
        private boolean isRebuilding = false;

        public Object3DListTableModel() {
            super(Objects3DTableData.class, new String[]{
                        "Visible", "Only", "Name", "Message"
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
    private static class Objects3DTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof Objects3DTableData) {
                Objects3DTableData e = (Objects3DTableData) value;

                switch (trueCol) {

                    case Object3DListTableModel.OBJ_VISIBLE:
                        return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);
                    case Object3DListTableModel.OBJ_ONLY:
                        return getJCheckBoxIcon(table, e.onlyMode, "picture.png", "pictures.png", isSelected, cellHasFocus, row, col);
                    case Object3DListTableModel.OBJ_NAME:
                        info = e.visibleName;
                        break;
                    case Object3DListTableModel.OBJ_MESSAGE:
                        info = e.message;
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

    public LLHAreaView() {
        initComponents();
        initTable();

        CommandManager.getInstance().addListener(LLHAreaView.ID, this);
    }

    private void initTable() {
        myObject3DListTableModel = new Object3DListTableModel();
        jObjects3DListTable = new RowEntryTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(myObject3DListTableModel);
        final Object3DListTableModel myModel = myObject3DListTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jObjectScrollPane.setViewportView(jObjects3DListTable);

        Objects3DTableCellRenderer p = new Objects3DTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(Objects3DTableData.class, p);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();

        int count = myTable.getColumnCount();
        TableColumnModel cm = myTable.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case Object3DListTableModel.OBJ_VISIBLE: {
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
                case Object3DListTableModel.OBJ_ONLY: {
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

        jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jObjects3DListTableValueChanged(e);
            }
        });

        jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {

            class Item extends JMenuItem {

                private final Objects3DTableData d;

                public Item(String s, Objects3DTableData line) {
                    super(s);
                    d = line;
                }

                public Objects3DTableData getData() {
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
                            LLHAreaDeleteCommand del = new LLHAreaDeleteCommand(i.getData().keyName);
                            CommandManager.getInstance().executeCommand(del, true);
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                Objects3DTableData entry = (Objects3DTableData) (line);
                String name = "Delete " + entry.visibleName;
                Item i = new Item(name, entry);
                popupmenu.add(i);
                i.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof Objects3DTableData) {
                    Objects3DTableData entry = (Objects3DTableData) (stuff);

                    switch (orgColumn) {
                        case Object3DListTableModel.OBJ_VISIBLE: {
                            LLHArea a = LLHAreaManager.getInstance().getLLHArea(entry.keyName);
                            if (a != null) {
                                LLHAreaMemento m = a.getMemento();
                                m.useVisible = true;
                                m.visible = !entry.checked;
                                LLHAreaChangeCommand c = new LLHAreaChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        case Object3DListTableModel.OBJ_ONLY: {
                            LLHArea a = LLHAreaManager.getInstance().getLLHArea(entry.keyName);
                            if (a != null) {
                                LLHAreaMemento m = a.getMemento();
                                m.useOnly = true;
                                m.only = !entry.onlyMode;
                                LLHAreaChangeCommand c = new LLHAreaChangeCommand(entry.keyName, m);
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

        // updateTable();
    }

    /** Set up sorting columns if wanted */
    private void setUpSortingColumns() {
    }

    private void jObjects3DListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        // We're in the updateTable and have set the selection to the old
        // value, we don't want to loop infinitely
        if (myObject3DListTableModel.rebuilding()) {
            return;
        }
        int row = jObjects3DListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = jObjects3DListTable.convertRowIndexToModel(row);
            if (myObject3DListTableModel != null) {
                Objects3DTableData d = (Objects3DTableData) (myObject3DListTableModel.getDataForRow(dataRow));
                if (d != null) {
                    LLHAreaSelectCommand c = new LLHAreaSelectCommand(d.keyName);
                    CommandManager.getInstance().executeCommand(c, true);
                }
            }
        }
    }

    public void updateTable() {

        ArrayList<VolumeTableData> v = LLHAreaManager.getInstance().getVolumes();
        VolumeTableData s = LLHAreaManager.getInstance().getSelection();
        int currentLine = 0;
        int select = -1;
        LLHArea selectedArea = null;
        ArrayList<Objects3DTableData> newList = new ArrayList<Objects3DTableData>();
        for (VolumeTableData d : v) {
            Objects3DTableData d2 = new Objects3DTableData();
            d2.visibleName = d.visibleName;
            d2.checked = d.checked;
            d2.keyName = d.keyName;
            d2.onlyMode = d.onlyMode;
            d2.message = d.message;
            newList.add(d2);
            if (s == d) {
                select = currentLine;
                selectedArea = d.airspace;
            }
            currentLine++;
        }
        myObject3DListTableModel.setDataTypes(newList);
        myObject3DListTableModel.fireTableDataChanged();

        if (select > -1) {
            select = jObjects3DListTable.convertRowIndexToView(select);

            // This of course fires an event, which calls jProductsListTableValueChanged
            // which would send a command which would do this again in an
            // infinite loop.  So we have a flag.  We don't use isAdjusting
            // because it still fires and event when you set it false
            myObject3DListTableModel.setRebuilding(true);
            jObjects3DListTable.setRowSelectionInterval(select, select);

            if (myLastSelected != selectedArea) {
                jControlsToolBar.removeAll();
                selectedArea.activateGUI(jControlsToolBar);
                jControlsToolBar.validate();
                jControlsToolBar.repaint();
                jControlScrollPane.revalidate();
                myLastSelected = selectedArea;
            } else {
                selectedArea.updateGUI();
            }

            myObject3DListTableModel.setRebuilding(false);

        } else {
            setEmptyControls();
            jControlsToolBar.validate();
            jControlsToolBar.repaint();
            myLastSelected = null;
        }
        jObjects3DListTable.repaint();
    }

    private void initComponents() {

        jEditToolBar = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton("Slice");
        jButton2 = new javax.swing.JButton("Box");
        jControlsToolBar = new JPanel();
        jObjectScrollPane = new JScrollPane();
        jControlScrollPane = new JScrollPane();
        jControlScrollPane.add(jControlsToolBar);
        jControlsToolBar.setLayout(new MigLayout(new LC().insetsAll("3"), null, null));

        JSplitPane northSouth = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                jObjectScrollPane, jControlScrollPane);
        northSouth.setResizeWeight(.50);
        jControlScrollPane.setViewportView(jControlsToolBar);

        setLayout(new java.awt.BorderLayout());

        jEditToolBar.setRollover(true);

        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jEditToolBar.add(jButton1);

        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jEditToolBar.add(jButton2);

        add(jEditToolBar, java.awt.BorderLayout.NORTH);

        setEmptyControls();

        add(northSouth, java.awt.BorderLayout.CENTER);
    }

    private void setEmptyControls() {
        jControlsToolBar.removeAll();
        JTextField t = new javax.swing.JTextField();
        t.setText("Controls for selected 3DObject");
        t.setEditable(false);
        jControlsToolBar.setLayout(new java.awt.BorderLayout());
        jControlsToolBar.add(t, java.awt.BorderLayout.CENTER);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        LLHAreaCreateCommand doit = new LLHAreaCreateCommand("Slice");
        CommandManager.getInstance().executeCommand(doit, true);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        LLHAreaCreateCommand doit = new LLHAreaCreateCommand("Box");
        CommandManager.getInstance().executeCommand(doit, true);
    }
}
