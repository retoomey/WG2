package org.wdssii.gui.views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.infonode.docking.*;
import net.infonode.docking.properties.DockingWindowProperties;
import net.infonode.docking.properties.ViewProperties;
import net.infonode.docking.properties.ViewTitleBarProperties;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.builders.Builder;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.datatypes.builders.XMLBuilder;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.DockWindow;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.index.IndexFactory;

/**
 * SourcesView. Lots of duplicate code with FeaturesView, should probably break
 * up the code refactor here...
 *
 * @author Robert Toomey
 */
public class SourcesView extends JThreadPanel implements CommandListener {

    public static final String ID = "wdssii.SourcesView";
    private static Logger log = LoggerFactory.getLogger(SourcesView.class);
    private Source myLastSelectedSource = null;

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void SourceCommandUpdate(SourceCommand command) {
        updateGUI(command);
    }

    // FIXME: probably not needed....
    public void FeatureCommandUpdate(FeatureCommand command) {
        updateGUI(command);
    }

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiDockedViewFactory {

        /**
         * Create a sub-bock for gui controls, or a split pane
         */
        public static final boolean myDockControls = true;

        public Factory() {
            super("Sources", "brick_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new SourcesView(myDockControls);
        }

        @Override
        public DockingWindow getNewDockingWindow() {
            if (!myDockControls) {
                // Get a single non-docked FeatureView component
                return super.getNewDockingWindow();
            } else {
                Icon i = getWindowIcon();
                String title = getWindowTitle();

                SourcesView f = new SourcesView(myDockControls);

                // Create a RootWindow in the view.  Anything added to this
                // will be movable.
                RootWindow root = DockWindow.createARootWindow();
                View topWindow = new View(title, i, root);

                // Inside the root window we'll add two views...
                View controls = new View("Source Controls", i, f.getControlComponent());
                View select = new View("Selection", i, f);

                // The select is our 'root', so make it non-dragable.  Basically
                // the main view will be the actual holder for this.  By making it a view,
                // we allow the other view to be docked above it, etc..
                ViewProperties vp = select.getViewProperties();
                ViewTitleBarProperties tp = vp.getViewTitleBarProperties();
                tp.setVisible(false);

                // Since menu allows changing, make a new window properties
                DockingWindowProperties org = controls.getWindowProperties();
                org.setCloseEnabled(false);
                org.setMaximizeEnabled(false);
                org.setMinimizeEnabled(false);

                SplitWindow w = new SplitWindow(false, select, controls);
                w.setDividerLocation(.25f);
                root.setWindow(w);

                // Add a 'close' listener to our internal root so that if the
                // control window is closed we redock instead.. (we could close
                // it but we'll need some control to get it back then)
                // Add a listener which shows dialogs when a window is closing or closed.
                root.addListener(new DockingWindowAdapter() {

                    @Override
                    public void windowClosing(DockingWindow window)
                            throws OperationAbortedException {
                        window.dock();
                    }
                });

                return topWindow;
            }
        }
    }
    private SourceListTableModel mySourceListTableModel;
    private RowEntryTable jObjects3DListTable;
    private JPanel jSourceGUIPanel;
    private javax.swing.JToolBar jEditToolBar;
    private javax.swing.JScrollPane jObjectScrollPane;
    // private javax.swing.JScrollPane jControlScrollPane;
    private javax.swing.JLabel jInfoLabel;

    @Override
    public void updateInSwingThread(Object command) {
        updateTable();
        updateInfoLabel();
    }

    public SourcesView(boolean dockControls) {
        initComponents(dockControls);
        CommandManager.getInstance().addListener(SourcesView.ID, this);
    }

    /**
     * Storage for displaying the current feature list
     */
    private static class SourceListTableData {

        public String visibleName; // Name shown in list
        public String sourceKey;   // Key for the source lookup...
        public boolean realtime;  // true if 'realtime' updating source
        public String typeName;   // The 'type' this source thinks it is
        public String urlLocation; // location of the source
        public boolean connected;  // connection success
        public boolean connecting; // connection being attempted
        public String message;
    }

    private class SourceListTableModel extends RowEntryTableModel<SourceListTableData> {

        public static final int SOURCE_STATUS = 0;
        public static final int SOURCE_NAME = 1;
        public static final int SOURCE_TYPE = 2;
        public static final int SOURCE_PATH = 3;
        private boolean isRebuilding = false;

        public SourceListTableModel() {
            super(SourceListTableData.class, new String[]{
                        "Connected", "Source", "Type", "Path"
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
    private static class SourceListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof SourceListTableData) {
                SourceListTableData e = (SourceListTableData) value;

                switch (trueCol) {
                    case SourceListTableModel.SOURCE_STATUS:
                        String icon = "link_break.png"; // Not connected
                        if (e.connecting) {
                            icon = "link_go.png";
                        }
                        if (e.connected) {
                            icon = "link.png";
                        }
                        return getIcon(table, icon, isSelected, cellHasFocus, row, col);
                    case SourceListTableModel.SOURCE_NAME:
                        info = e.visibleName;
                        break;
                    case SourceListTableModel.SOURCE_TYPE:
                        info = e.typeName;
                        break;
                    case SourceListTableModel.SOURCE_PATH:
                        info = ">" + e.urlLocation;
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

    private void initTable() {
        mySourceListTableModel = new SourceListTableModel();
        jObjects3DListTable = new RowEntryTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(mySourceListTableModel);
        final SourceListTableModel myModel = mySourceListTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jObjectScrollPane.setViewportView(jObjects3DListTable);

        SourceListTableCellRenderer p = new SourceListTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(SourceListTableData.class, p);

        int count = myTable.getColumnCount();
        TableColumnModel cm = myTable.getColumnModel();
        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        TableUtil.IconHeaderRenderer r = new TableUtil.IconHeaderRenderer();

        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(r);
            switch (i) {
                case SourceListTableModel.SOURCE_STATUS: {
                    IconHeaderInfo info = new IconHeaderInfo("link.png");
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

                private final SourceListTableData d;

                public Item(String s, SourceListTableData line) {
                    super(s);
                    d = line;
                }

                public SourceListTableData getData() {
                    return d;
                }
            };

            @Override
            public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {

                // FIXME: Code a bit messy, we're just hacking the text value
                // for now.  Probably will need a custom JPopupMenu that has
                // our Objects3DTableData in it.
                // FIXME: Really need a cleaner way to do this...
                ActionListener al = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Object z = e.getSource();
                        if (z instanceof Item) {
                            Item i = (Item) (e.getSource());
                            String text = i.getText();
                            String key = i.getData().sourceKey;
                            if (text.startsWith("Delete")) {
                                SourceDeleteCommand del = new SourceDeleteCommand(key);
                                CommandManager.getInstance().executeCommand(del, true);
                            } else if (text.startsWith("Connect")) {
                                SourceConnectCommand r = new SourceConnectCommand(key);
                                CommandManager.getInstance().executeCommand(r, true);
                            } else if (text.startsWith("Disconnect")) {
                                SourceDisconnectCommand r = new SourceDisconnectCommand(key);
                                CommandManager.getInstance().executeCommand(r, true);
                            }
                        } else {
                            JMenuItem i = (JMenuItem) (z);
                            String text = i.getText();
                            if (text.startsWith("Delete All Sources")) {
                                SourceDeleteCommand.SourceDeleteAllCommand del = new SourceDeleteCommand.SourceDeleteAllCommand();
                                CommandManager.getInstance().executeCommand(del, true);
                            }
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                SourceListTableData entry = (SourceListTableData) (line);

                String vis = entry.visibleName;
                // Disconnect/Reconnect command...
                if (entry.connected) {
                    String name = "Disconnect " + vis;
                    Item i = new Item(name, entry);
                    popupmenu.add(i);
                    i.addActionListener(al);
                } else {
                    if (!entry.connecting) {
                        String name = "Connect " + vis;
                        Item i = new Item(name, entry);
                        popupmenu.add(i);
                        i.addActionListener(al);
                    }
                }

                // Delete 'ktlx'
                String name = "Delete " + vis;
                Item i = new Item(name, entry);
                popupmenu.add(i);
                i.addActionListener(al);

                popupmenu.add(new JSeparator());

                // Delete all
                name = "Delete All Sources";
                JMenuItem z = new JMenuItem(name, null);
                popupmenu.add(z);
                z.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof SourceListTableData) {
                    SourceListTableData entry = (SourceListTableData) (stuff);

                    switch (orgColumn) {
                        default:
                            break;
                    }
                }
            }
        });

        setUpSortingColumns();

        // updateTable();
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
        // value, we don't want to loop infinitely
        if (mySourceListTableModel.rebuilding()) {
            return;
        }
        int row = jObjects3DListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = jObjects3DListTable.convertRowIndexToModel(row);
            if (mySourceListTableModel != null) {
                SourceListTableData d = (SourceListTableData) (mySourceListTableModel.getDataForRow(dataRow));
                if (d != null) {
                    SourceSelectCommand c = new SourceSelectCommand(d.sourceKey);
                    CommandManager.getInstance().executeCommand(c, true);
                }
            }
        }
    }

    public void updateTable() {

        final SourceList flist = SourceList.theSources;

        /**
         * Static for now...
         */
        List<Source> forg = flist.getSources();
        ArrayList<Source> f = new ArrayList<Source>(forg);

        /**
         * Sort by visible name
         */
        Collections.sort(f,
                new Comparator<Source>() {

                    @Override
                    public int compare(Source o1, Source o2) {
                        int c = o1.getVisibleName().compareTo(o2.getVisibleName());
                        return c;
                    }
                });

        int currentLine = 0;
        int select = -1;
        ArrayList<SourceListTableData> newList = new ArrayList<SourceListTableData>();
        Source selectedSource = null;
        Source topSource = flist.getTopSelected();

        for (Source d : f) {
            SourceListTableData d2 = new SourceListTableData();
            d2.connected = d.isConnected();
            d2.connecting = d.isConnecting();
            d2.realtime = d.isRealtime();
            d2.visibleName = d.getVisibleName();
            d2.typeName = d.getShownTypeName();
            d2.sourceKey = d.getKey();
            d2.urlLocation = d.getURLString();
            d2.message = "";

            newList.add(d2);
            if (topSource == d) {
                select = currentLine;
                selectedSource = d;
            }
            currentLine++;
        }
        mySourceListTableModel.setDataTypes(newList);
        mySourceListTableModel.fireTableDataChanged();

        if (select > -1) {
            select = jObjects3DListTable.convertRowIndexToView(select);

            // This of course fires an event, which calls jProductsListTableValueChanged
            // which would send a command which would do this again in an
            // infinite loop.  So we have a flag.  We don't use isAdjusting
            // because it still fires and event when you set it false
            mySourceListTableModel.setRebuilding(true);
            jObjects3DListTable.setRowSelectionInterval(select, select);

            if (myLastSelectedSource != selectedSource) {
                jSourceGUIPanel.removeAll();
                selectedSource.setupSourceGUI(jSourceGUIPanel);
                jSourceGUIPanel.validate();
                jSourceGUIPanel.repaint();
                myLastSelectedSource = selectedSource;
            } else {
                selectedSource.updateGUI();
            }

            mySourceListTableModel.setRebuilding(false);

        } else {
            setEmptyControls();
            jSourceGUIPanel.validate();
            jSourceGUIPanel.repaint();
            myLastSelectedSource = null;
        }
        jObjects3DListTable.repaint();
    }

    public void updateInfoLabel() {
        Source s = SourceList.theSources.getTopSelected();
        if (s != null) {
            String info = s.getSourceDescription();
            jInfoLabel.setText(info);
        } else {
            jInfoLabel.setText(" ");
        }
    }

    private JComponent initControlGUI() {
        //JPanel holder = new JPanel();
        //holder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));      
        jSourceGUIPanel = new JPanel();
        jSourceGUIPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        setEmptyControls();
        return jSourceGUIPanel;
    }

    /**
     * The part of the GUI that deals with selection of a individual feature
     */
    private JComponent initSelectGUI() {

        jObjectScrollPane = new JScrollPane();
        jInfoLabel = new JLabel("---");
        return jObjectScrollPane;
    }

    private JToolBar initToolBar() {
        jEditToolBar = new javax.swing.JToolBar();
        jEditToolBar.setFloatable(false);
        jEditToolBar.setRollover(true);

        JButton b = new JButton("+Source");
        b.setFocusable(false);
        b.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        b.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });
        jEditToolBar.add(b);

        return jEditToolBar;
    }

    private void addActionPerformed(java.awt.event.ActionEvent evt) {
        // Open dialog for single file adding.....

        // FeatureCreateCommand doit = new FeatureCreateCommand("VSlice");
        // CommandManager.getInstance().executeCommand(doit, true);
        //FeatureList.theFeatures.addFeature(new LLHAreaFeature(0));
        doSingleProductOpenDialog();
    }

    private void initComponents(boolean dockControls) {

        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        jInfoLabel = new JLabel("---");
        JComponent controls = initControlGUI();
        JComponent selection = initSelectGUI();
        JComponent toolbar = initToolBar();
        JComponent rootComponent;
        if (!dockControls) {
            JSplitPane s = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    selection, controls);
            s.setResizeWeight(.50);
            rootComponent = s;
        } else {
            rootComponent = jObjectScrollPane;
            jObjectScrollPane.setBorder(null);
        }

        add(toolbar, new CC().dockNorth());
        add(jInfoLabel, new CC().dockNorth().growX());
        add(rootComponent, new CC().growX().growY());
        // growX().growY());

        // to size correct, init table last, nope not it
        initTable();
    }

    public JComponent getControlComponent() {
        return jSourceGUIPanel;
    }

    public JComponent getToolBar() {
        return jEditToolBar;
    }

    public JComponent getInfoLabel() {
        return jInfoLabel;
    }

    private void setEmptyControls() {
        jSourceGUIPanel.removeAll();
        JTextField t = new javax.swing.JTextField();
        t.setText("Controls for selected feature");
        t.setEditable(false);
        jSourceGUIPanel.setLayout(new java.awt.BorderLayout());
        jSourceGUIPanel.add(t, java.awt.BorderLayout.CENTER);
    }

    // Single source dialog.  We're gonna have to make it handle ANY generic
    // Source type. FIXME
    /**
     * Filter for local files.
     */
    private class SingleProductFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            String t = f.getName().toLowerCase();
            // FIXME: need to get these from the Sources, some sort of
            // Factory
            return (f.isDirectory()
                    || t.endsWith(".netcdf") || t.endsWith(".nc") || t.endsWith(".netcdf.gz")
                    || t.endsWith(".xml") || t.endsWith(".xml.gz"));
        }

        @Override
        public String getDescription() {
            return "netcdf files or xml files";
        }
    }

    /**
     * Load an individual file into the ManualLoadIndex
     */
    public URL doSingleProductOpenDialog() {

        if (false) {
            URL pickedFile = null;
            JFileChooser chooser = new JFileChooser();

            chooser.setAccessory(new LabelAccessory(chooser));

            chooser.setFileFilter(new SingleProductFileFilter());
            chooser.setDialogTitle("Add single source");

            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    pickedFile = f.toURI().toURL();
                } catch (MalformedURLException ex) {
                    // We assume that chooser knows not to return
                    // malformed urls...
                }
            }
            return pickedFile;
        } else {
            URLLoadDialog myDialog = new URLLoadDialog(null, true, "Open Source");
            return null;
        }
    }

    public String figureOutSelectedFileType(URL aURL) {
        String type = "Unknown";
        if (aURL != null) {
            
            // Check for wdssii index??
            boolean isIndex = IndexFactory.checkURLForIndex(aURL);
            if (isIndex){
                type = "WDSS2 Index";
                return type;
            }
            
            String text = aURL.toString();
            //jSingleURLTextField.setText(text);

            // FIXME: Generalize these across Sources.  Sources might
            // 'share' a builder...should Source do it or Builder?

            // See if we can build from netcdf (a Wdssii netcdf file)
            Builder.BuilderFileInfo info = NetcdfBuilder.getBuilderFileInfo(aURL);
            if (info.success) {
                log.debug("FOUND a WDSS2 Netcdf format file...");
                type = "WDSS2 Netcdf File";
                // setToInfo(info);
            } else {

                // Try to get header from xml
                info = XMLBuilder.getBuilderFileInfo(aURL);
                if (info.success) {
                    // setToInfo(info);
                    log.debug("FOUND a WDSSII XML format file....");
                    type = "WDSS2 XML File";
                }
            }
        }
        return type;
    }

    class LabelAccessory extends JLabel implements PropertyChangeListener {

        private static final int PREFERRED_WIDTH = 125;
        private static final int PREFERRED_HEIGHT = 100;

        public LabelAccessory(JFileChooser chooser) {
            setVerticalAlignment(JLabel.CENTER);
            setHorizontalAlignment(JLabel.CENTER);
            chooser.addPropertyChangeListener(this);
            setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        }

        @Override
        public void propertyChange(PropertyChangeEvent changeEvent) {
            String changeName = changeEvent.getPropertyName();
            if (changeName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                File file = (File) changeEvent.getNewValue();
                if (file != null) {
                    String type = "Internal Error";
                    try {
                        URL aURL = file.toURI().toURL();
                        type = figureOutSelectedFileType(aURL);
                    } catch (MalformedURLException ex) {
                        // We assume that chooser knows not to return
                        // malformed urls...
                    }
                    setText(type);
                    /*
                     * ImageIcon icon = new ImageIcon(file.getPath()); if
                     * (icon.getIconWidth() > PREFERRED_WIDTH) { icon = new
                     * ImageIcon(icon.getImage().getScaledInstance(
                     * PREFERRED_WIDTH, -1, Image.SCALE_DEFAULT)); if
                     * (icon.getIconHeight() > PREFERRED_HEIGHT) { icon = new
                     * ImageIcon(icon.getImage().getScaledInstance( -1,
                     * PREFERRED_HEIGHT, Image.SCALE_DEFAULT)); } }
                     * setIcon(icon);
                     *
                     *
                     */

                }
            }
        }
    }

    public class CustomDialog extends JDialog implements ActionListener {

        private JPanel myPanel = null;
        private JButton yesButton = null;
        private JButton noButton = null;
        private boolean answer = false;

        public boolean getAnswer() {
            return answer;
        }

        public CustomDialog(JFrame frame, boolean modal, String myMessage) {
            super(frame, modal);
            Container content = getContentPane();

            myPanel = new JPanel();
            myPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

            content.add(myPanel, new CC().width("min:pref:"));
            myPanel.add(new JLabel(myMessage), new CC().growX().wrap());

            JFileChooser fileChooser = new JFileChooser(".");
            // fileChooser.setControlButtonsAreShown(false);
            myPanel.add(fileChooser, new CC().growX().growY().wrap());

            yesButton = new JButton("Yes");
            yesButton.addActionListener(this);
            myPanel.add(yesButton, new CC().growX().wrap());

            noButton = new JButton("No");
            noButton.addActionListener(this);
            myPanel.add(noButton, new CC().growX().wrap());

            pack();
            setLocationRelativeTo(frame);
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (yesButton == e.getSource()) {
                System.err.println("User chose yes.");
                answer = true;
                setVisible(false);
            } else if (noButton == e.getSource()) {
                System.err.println("User chose no.");
                answer = false;
                setVisible(false);
            }
        }
    }

    /**
     * A dialog that loads a URL with the ability to prefetch url information
     */
    public class URLLoadDialog extends JDialog implements ActionListener {

        private JPanel myPanel = null;
        private JTextField myURLTextField;
        private JTextField myNameTextField;
        private JPanel mySubPanel;
        private JButton myValidateURLButton;
        private JButton myCancelButton;
        private JPanel myGUIHolder;
        private boolean myValidURLSource = false;

        /**
         * Get a default name of the form 'Source#', making sure that name isn't
         * already being used
         *
         * @return a default source name
         */
        private String getDefaultName() {
            int counter = 0;
            boolean done = false;
            List<Source> list = SourceList.theSources.getSources();
            String candidateName = "Source";
            while (!done) {
                candidateName = "Source" + (++counter);
                // See if candidate matches something there....
                boolean alreadyHaveThatName = false;
                for (Source S : list) {
                    if (S.getVisibleName().equals(candidateName)) {
                        counter++;
                        alreadyHaveThatName = true;
                        break;
                    }
                }
                if (!alreadyHaveThatName) {
                    done = true;
                }
            }
            return candidateName;
        }

        public URLLoadDialog(JFrame frame, boolean modal, String myMessage) {
            super(frame, modal);

            setTitle("Open Source");
            Container content = getContentPane();

            JPanel p;
            myPanel = p = new JPanel();
            p.setLayout(new MigLayout("fillx, wrap 3", "[pref!][][pref!]", ""));

            // The URL field...
            p.add(new JLabel("URL:"));
            myURLTextField = new JTextField();
            p.add(myURLTextField, new CC().growX().width("300"));

            // The browse local file button...
            JButton b = new JButton("Browse...");
            p.add(b, new CC().alignX("right"));

            // Source name
            p.add(new JLabel("Name:"));
            myNameTextField = new javax.swing.JTextField();
            myNameTextField.setText(getDefaultName());
            p.add(myNameTextField, new CC().growX().spanX(2));

            // Validate button...
            myValidateURLButton = new JButton("Validate");
            p.add(myValidateURLButton, new CC().skip(1).alignX("right"));

            myCancelButton = new JButton("Cancel");
            p.add(myCancelButton, new CC().alignX("right"));

            // The extra information panel...
            myGUIHolder = new JPanel();
            myGUIHolder.setSize(200, 50);
            setUpBadSource(myGUIHolder);
            p.add(myGUIHolder, new CC().growX().growY().spanX(3));

            content.add(myPanel);
            pack();
            setLocationRelativeTo(frame);

            myURLTextField.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(DocumentEvent e) {
                    invalidate();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    invalidate();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    invalidate();
                }

                public void invalidate() {
                    setValidURL(false);
                }
            });


            b.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    //jBrowseButtonActionPerformed(evt);
                    URL aURL = doSourceOpenDialog();
                    if (aURL != null) {
                        myURLTextField.setText(aURL.toString());
                        setValidURL(true);
                        setUpGUIForURL(myGUIHolder, aURL);
                    }
                }
            });

            myCancelButton.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setVisible(false);
                }
            });

            myValidateURLButton.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    boolean valid = validateURL();
                    setValidURL(valid);
                }
            });

            setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
        }

        public void setValidURL(boolean flag) {
            myValidURLSource = flag;
            myValidateURLButton.setVisible(!myValidURLSource);
        }
    }

    public boolean validateURL() {
        return true;

    }

    public void setUpBadSource(JComponent holder) {
        holder.removeAll();
        holder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        JPanel myStuff = new JPanel();
        myStuff.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        myStuff.setBackground(Color.red);
        myStuff.add(new JLabel("invalid URL"), new CC().growX());
        holder.add(myStuff, new CC().growX().growY());
    }
    
    public void setUpGUIForURL(JComponent holder, URL aURL){
        String type = figureOutSelectedFileType(aURL);
        holder.removeAll();
        holder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        JPanel myStuff = new JPanel();
        myStuff.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        myStuff.setBackground(Color.green);
        myStuff.add(new JLabel(type), new CC().growX());
        holder.add(myStuff, new CC().growX().growY());
    }

    /** FIXME: move to factory.  When an Index is found....set it up */
    public boolean setUpIndexGUIForURL(JTextField urlDisplay, JComponent holder, URL aURL){
        return false;
    }
    
    /**
     * For the moment a simple file dialog
     */
    public URL doSourceOpenDialog() {

        URL pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        //chooser.setFileFilter(new LocalDataFilter());
        chooser.setDialogTitle("Open local file");

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL();
            } catch (MalformedURLException ex) {
                pickedFile = null;
            }

        }

        return pickedFile;
    }
}
