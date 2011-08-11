package org.wdssii.gui.nbm.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import org.wdssii.core.RadarInfo;
import org.wdssii.core.RadarInfo.ARadarInfo;

import org.wdssii.core.SourceBookmarks.*;
import org.wdssii.core.SourceBookmarks;
import org.wdssii.datatypes.builders.Builder;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.datatypes.builders.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Sources//EN",
autostore = false)
@TopComponent.Description(preferredID = "SourcesTopComponent",
iconBase = "org/wdssii/gui/nbm/views/cart_add.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.SourcesTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_SourcesAction",
preferredID = "SourcesTopComponent")
/** The source view is what allows us to browse to various data sources, 
 * by URL, disk location (url too), etc...
 * 
 * @author Robert Toomey
 * 
 */
public final class SourcesTopComponent extends TopComponent {

    private ArrayList<BookmarkURLSource> mySourceList;
    //private ArrayList<BookmarkURLSource> myVisibleSourceList;
    private static final String ALLGROUPS = "All";
    private javax.swing.JTable jSourceListTable;
    private final String myDebugList = "Debug List";
    private BookmarkURLDataTableModel myModel;
    private ArrayList<ARadarInfo> myRadarInfo;
    private final ImagePanel myCONUSPanel;

    /** Filter to looks for local data files.  We can make this more 
     * advanced
     */
    private class LocalDataFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
        }

        @Override
        public String getDescription() {
            return "XML Data files";
        }
    }

    /** Filter for local files.
     */
    private class SingleProductFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            String t = f.getName().toLowerCase();
            // FIXME: need to get these from the Builders
            return (t.endsWith(".netcdf") || (t.endsWith(".netcdf.gz"))
                    || (t.endsWith(".xml")) || (t.endsWith(".xml.gz")));
        }

        @Override
        public String getDescription() {
            return "netcdf files or xml files";
        }
    }

    /** A class that uses a BookmarkURLData as its model */
    private class BookmarkURLDataTableModel extends RowEntryTableModel {

        // FIXME: should be an enum class probably...
        public static final int BOOK_NAME = 0;
        public static final int BOOK_LOCATION = 1;
        public static final int BOOK_PATH = 2;
        public static final int BOOK_TIME = 3;
        public static final int BOOK_GROUP = 4;
        /** Group filter */
        private BookmarkRowFilter myRowFilter = new BookmarkRowFilter();

        private void selectLineOf(BookmarkURLSource s) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        // Set up a row filter for the group combo box
        private class BookmarkRowFilter extends RowFilter<Object, Object> {

            private String myGroupName;
            private boolean myFilterOn = false;

            @Override
            public boolean include(Entry entry) {

                // In our model, every column is the same object.  0 is first column.
                if (myFilterOn) {
                    Object e = entry.getValue(0);
                    if (e instanceof BookmarkURLSource) {
                        BookmarkURLSource s = (BookmarkURLSource) (e);
                        if (s.group.equals(myGroupName)) {
                            return true;
                        }
                        return false;
                    }
                }
                return true;
            }

            public void setGroupToShow(String filter) {
                myGroupName = filter;
                myFilterOn = true;
            }

            public void showAllGroups() {
                myFilterOn = false;
            }
        }

        public BookmarkURLDataTableModel() {
            super(BookmarkURLSource.class, new String[]{
                        "Name", "Location", "Path", "Latest", "Group"});
        }

        public TableRowSorter<BookmarkURLDataTableModel> getGroupModelSorter() {
            TableRowSorter<BookmarkURLDataTableModel> sorter =
                    new TableRowSorter<BookmarkURLDataTableModel>(this);
            sorter.setRowFilter(myRowFilter);

            for (int i = 0; i < BookmarkURLDataTableModel.BOOK_GROUP; i++) {
                Comparator<BookmarkURLSource> c = null;
                switch (i) {

                    case BookmarkURLDataTableModel.BOOK_NAME: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.name).compareTo(o2.name);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_LOCATION: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.location).compareTo(o2.location);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_PATH: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.path).compareTo(o2.path);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_TIME: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                if (o1.date.after(o2.date)) {
                                    return 1;
                                }
                                return -1;
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_GROUP: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.group).compareTo(o2.group);
                            }
                        };
                        break;
                    }
                }
                sorter.setComparator(i, c);
            }
            return sorter;
        }

        public void setGroupToShow(String filter) {
            myRowFilter.setGroupToShow(filter);
            //this.fireTableStructureChanged();
            this.fireTableDataChanged();
        }

        public void showAllGroups() {
            myRowFilter.showAllGroups();
            // this.fireTableStructureChanged();
            this.fireTableDataChanged();
        }

        @Override
        public String getKeyField(Object data) {
            // From generic, so it better match
            BookmarkURLSource s = (BookmarkURLSource) (data);
            return s.name;
        }
    }

    /** Our custom renderer for our product view table */
    private static class BookmarkTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            if (value instanceof BookmarkURLSource) {
                BookmarkURLSource e = (BookmarkURLSource) value;

                switch (trueCol) {

                    case BookmarkURLDataTableModel.BOOK_NAME:
                        info = e.name;
                        break;
                    case BookmarkURLDataTableModel.BOOK_LOCATION:
                        info = e.location;
                        break;
                    case BookmarkURLDataTableModel.BOOK_PATH:
                        info = e.path;
                        break;
                    case BookmarkURLDataTableModel.BOOK_TIME:
                        info = e.time;
                        break;
                    case BookmarkURLDataTableModel.BOOK_GROUP:
                        info = e.group;
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

    public static class ImagePanel extends JPanel {

        private BufferedImage image;
        private RadarInfo myRadarInfo;
        private String myDragging;
        
        // private Map<String, ARadarInfo> divs;
        String hitName;
        private ArrayList<BookmarkURLSource> mySourceList;
        // Table for bookmarks...
        private javax.swing.JTable myTable;
        private BookmarkURLDataTableModel myBookmarkModel;

        private void setBookmarkTable(JTable t, BookmarkURLDataTableModel m) {
            myTable = t;
            myBookmarkModel = m;
        }

        public void setBookmarkList(ArrayList<BookmarkURLSource> list) {
            mySourceList = list;
        }

        public ImagePanel() {
            mouser m = new mouser();
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        public void setImage(BufferedImage aImage) {
            image = aImage;
        }

        public void setRadarInfo(RadarInfo r) {
            myRadarInfo = r;
        }

        @Override
        public void paintComponent(Graphics g) {
            if (image != null) {
                g.drawImage(image, 0, 0, null);

                if (myRadarInfo != null) {

                    // Gather font information
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    FontRenderContext frc = g2.getFontRenderContext();
                    Font f = Font.decode("Arial-PLAIN-12");

                    // Draw all the boxes first before mouse hilite..
                    Map<String, ARadarInfo> divs = myRadarInfo.getRadarInfos();

                    // Update the rectangles for the curent text size
                    int m = 5;
                    for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                        ARadarInfo d = entry.getValue();
                        // Use the current VCP rectangle as the new center
                        TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                        Rectangle2D r = t1.getBounds();
                        d.width = (int) r.getWidth()+m+m; 
                        d.height = (int) r.getHeight()+m+m;
                    }

                    // Draw the non-hit radar entries in 'background'
                    for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                        ARadarInfo d = entry.getValue();
                        Color c = d.getColor();
                        String key = entry.getKey();
                        Rectangle2D r = d.getRect();
                        TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                        if (!key.equals(hitName)) {
                            g.setColor(c);
                            g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
                            g.setColor(Color.BLACK);
                            t1.draw(g2, (float)r.getX()+m, (float)(r.getY()+r.getHeight()-m));
                        }
                    }

                    // Mouse hit pass....
                    for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                        //Gonna have to draw em...
                        ARadarInfo d = entry.getValue();
                        Color c = d.getColor();
                        String key = entry.getKey();
                        int left = d.getLeft();
                        int top = d.getTop();

                        if (key.equals(hitName)) {

                            // Cross reference to the current source list...
                            if (mySourceList != null) {
                                for (BookmarkURLSource s : mySourceList) {
                                    if (s.name.equals(key)) {
                                        key += ": ";
                                        key += d.id;
                                    }
                                }
                            }

                            // Hilight the actual radar color rectangle..
                            TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                            Rectangle2D r = d.getRect();

                            // Fill     
                            g.setColor(c.brighter());
                            g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

                            // Outline
                            g.setColor(Color.WHITE);
                            g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

                            // Text
                            g.setColor(Color.BLACK);
                            t1.draw(g2, (float) r.getX() + m, (float) (r.getY() + r.getHeight() - m));

                            // Draw the floating text for hovering ----------------------------------
                            TextLayout t2 = new TextLayout(key, f, frc);
                            Rectangle2D b = t2.getBounds();
                            int hLeft = left;
                            int hTop = (int) (top+r.getHeight()+m+b.getHeight());
                            b.setRect(b.getX() + hLeft - 2, b.getY() + hTop - 2, b.getWidth() + 4, b.getHeight() + 4);

                            // Try to move text to left if past right edge....
                            int over = (int) ((b.getX() + b.getWidth()) - image.getWidth());
                            if (over > 0) {
                                b.setRect(b.getX() - over - 5, b.getY(), b.getWidth(), b.getHeight());
                                hLeft -= over + 5;
                            }

                            g2.setClip(0, 0, image.getWidth(), image.getHeight());
                            g2.setColor(Color.WHITE);
                            g2.fillRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
                            g2.setColor(Color.black);
                            t2.draw(g2, hLeft, hTop);
                            g2.drawRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
                            // End draw hover text -------------------------------------
                        }
                    }

                }
            }
        }

        /** A cheezy outline behind the text that doesn't require an outline
         * font to render.  It shadows by shifting the text 1 pixel in every
         * direction.  Not very fast, but color keys are more about looks.
         */
        public void cheezyOutline(Graphics2D g, int x, int y, TextLayout t) {

            // Draw a 'grid' of background to shadow the character....
            // We can get away with this because there aren't that many labels
            // in a color key really. Draw 8 labels shifted to get outline.
            g.setColor(Color.black);
            t.draw(g, x + 1, y + 1);
            t.draw(g, x, y + 1);
            t.draw(g, x - 1, y + 1);
            t.draw(g, x - 1, y);
            t.draw(g, x - 1, y - 1);
            t.draw(g, x, y - 1);
            t.draw(g, x + 1, y - 1);
            t.draw(g, x + 1, y);

            g.setColor(Color.white);
            t.draw(g, x, y);
        }

        @Override
        public Dimension getPreferredSize() {
            if (image != null) {
                return new Dimension(image.getWidth(), image.getHeight());
            }
            return new Dimension(100, 100);
        }

        private class mouser extends MouseAdapter {

            @Override
            public void mousePressed(MouseEvent e) {
                if (myRadarInfo != null) {
                    int x = e.getX();
                    int y = e.getY();
                    String newHit = "";
                    newHit = myRadarInfo.findRadarAt(x, y);
                    if (newHit != null) {
                        myDragging = newHit;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                myDragging = "";
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // On release..find it in the current source list and 
                // select the line in the bookmark table....
                if (myRadarInfo != null) {
                    int x = e.getX();
                    int y = e.getY();
                    String newHit = "";
                    newHit = myRadarInfo.findRadarAt(x, y);
                    if (newHit != null) {
                        if (myTable != null) {
                            if (myBookmarkModel != null) {
                                Object a = myBookmarkModel.getDataForKeyField(newHit);
                                if (a != null) {
                                    BookmarkURLSource s = (BookmarkURLSource) (a);
                                    int index = myBookmarkModel.getRowIndexOf(s);
                                    if (index >= 0) {
                                        // We want the event to fire
                                        // myBookmarkModel.setRebuilding(true);
                                        myTable.setRowSelectionInterval(index, index);
                                        //myBookmarkModel.setRebuilding(false);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (myRadarInfo != null) {
                    int x = e.getX();
                    int y = e.getY();
                    String newHit = "";
                    newHit = myRadarInfo.findRadarAt(x, y);
                    if (!newHit.equals(hitName)) {
                        hitName = newHit;
                        repaint();
                    }
                }
            }
        }
    }

    public SourcesTopComponent() {
        initComponents();

        myModel = new BookmarkURLDataTableModel();
        jSourceListTable = new javax.swing.JTable();
        jSourceListTable.setModel(myModel);

        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourceTableScrollPane.setViewportView(jSourceListTable);

        BookmarkTableCellRenderer p = new BookmarkTableCellRenderer();
        jSourceListTable.setDefaultRenderer(BookmarkURLSource.class, p);
        jSourceListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jSourceListTableValueChanged(e);
            }
        });

        jSourceListTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                /** On double click, try to add the source */
                if (e.getClickCount() == 2) {
                    addNewSourceFromFields(true, true);
                }
            }
        });
        jSourceListTable.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                /** Have to do enter pressed to snag before the table scroll */
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNewSourceFromFields(true, true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        jBookmarkComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[]{
                    "http://tensor.protect.nssl/rindexv2.xml", "file:/D:/testing2.xml", myDebugList}));
        jSourceListTable.setRowSorter(myModel.getGroupModelSorter());

        // Try to fill in table with web image?
        // experimental
        ImagePanel ipanel = new ImagePanel();
        myCONUSPanel = ipanel;
        javax.swing.JScrollPane holder = new javax.swing.JScrollPane();
        holder.setViewportView(ipanel);
        jBookTabPane.addTab("CONUS", null, holder, "stuff");
        myCONUSPanel.setBookmarkTable(jSourceListTable, myModel);

        updateListToCurrent();
        updateCurrentRadarInfo();

        setName(NbBundle.getMessage(SourcesTopComponent.class, "CTL_SourcesTopComponent"));
        setToolTipText(NbBundle.getMessage(SourcesTopComponent.class, "HINT_SourcesTopComponent"));

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jIndexPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jNameTextField = new javax.swing.JTextField();
        jURLTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jGroupComboBox = new javax.swing.JComboBox();
        jBrowseButton = new javax.swing.JButton();
        jRefreshButton = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox();
        jLoadNewSourceButton = new javax.swing.JButton();
        jBookmarkComboBox = new javax.swing.JComboBox();
        jBookTabPane = new javax.swing.JTabbedPane();
        jSourceTableScrollPane = new javax.swing.JScrollPane();
        jSeparator1 = new javax.swing.JSeparator();
        jSinglePanel = new javax.swing.JPanel();
        jSingleLoad = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jSingleChoiceTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jSingleProductTextField = new javax.swing.JTextField();
        jAddLocalButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jSingleURLTextField = new javax.swing.JTextField();
        jSingleTimeTextField = new javax.swing.JTextField();

        setToolTipText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.toolTipText")); // NOI18N
        setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel3.text")); // NOI18N

        jNameTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jNameTextField.text")); // NOI18N

        jURLTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jURLTextField.text")); // NOI18N
        jURLTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jURLTextFieldActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel5.text")); // NOI18N

        jGroupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All" }));
        jGroupComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jGroupComboBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jBrowseButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jBrowseButton.text")); // NOI18N
        jBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jRefreshButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jRefreshButton.text")); // NOI18N
        jRefreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRefreshButtonActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "30", "60", "90", "120" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLoadNewSourceButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLoadNewSourceButton.text")); // NOI18N
        jLoadNewSourceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadNewSourceButtonActionPerformed(evt);
            }
        });

        jBookmarkComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBookmarkComboBoxActionPerformed(evt);
            }
        });

        jBookTabPane.addTab(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSourceTableScrollPane.TabConstraints.tabTitle_1"), jSourceTableScrollPane); // NOI18N

        javax.swing.GroupLayout jIndexPanelLayout = new javax.swing.GroupLayout(jIndexPanel);
        jIndexPanel.setLayout(jIndexPanelLayout);
        jIndexPanelLayout.setHorizontalGroup(
            jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
            .addGroup(jIndexPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBookmarkComboBox, 0, 410, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jIndexPanelLayout.createSequentialGroup()
                        .addComponent(jGroupComboBox, 0, 333, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRefreshButton)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jIndexPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jIndexPanelLayout.createSequentialGroup()
                        .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)))
                    .addGroup(jIndexPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jURLTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jBrowseButton)
                    .addComponent(jLoadNewSourceButton))
                .addContainerGap())
            .addComponent(jBookTabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
        );
        jIndexPanelLayout.setVerticalGroup(
            jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jIndexPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jURLTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(4, 4, 4)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLoadNewSourceButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jBookmarkComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jGroupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRefreshButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBookTabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jIndexPanel.TabConstraints.tabTitle"), jIndexPanel); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSingleLoad, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSingleLoad.text")); // NOI18N
        jSingleLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSingleLoadActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel6.text")); // NOI18N

        jSingleChoiceTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSingleChoiceTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel7.text")); // NOI18N

        jSingleProductTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSingleProductTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jAddLocalButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jAddLocalButton.text")); // NOI18N
        jAddLocalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddLocalButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel8.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel10.text")); // NOI18N

        jSingleURLTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSingleURLTextField.text")); // NOI18N

        jSingleTimeTextField.setEditable(false);
        jSingleTimeTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSingleTimeTextField.text")); // NOI18N

        javax.swing.GroupLayout jSinglePanelLayout = new javax.swing.GroupLayout(jSinglePanel);
        jSinglePanel.setLayout(jSinglePanelLayout);
        jSinglePanelLayout.setHorizontalGroup(
            jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSinglePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jSinglePanelLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(22, 22, 22)
                        .addComponent(jSingleURLTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSingleLoad))
                    .addGroup(jSinglePanelLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSingleChoiceTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE))
                    .addGroup(jSinglePanelLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(18, 18, 18)
                        .addComponent(jSingleTimeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE))
                    .addGroup(jSinglePanelLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSingleProductTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE))
                    .addComponent(jAddLocalButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jSinglePanelLayout.setVerticalGroup(
            jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSinglePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jSingleURLTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSingleLoad))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jSingleProductTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jSingleChoiceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSinglePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jSingleTimeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jAddLocalButton)
                .addContainerGap(169, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jSinglePanel.TabConstraints.tabTitle"), jSinglePanel); // NOI18N

        add(jTabbedPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jURLTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jURLTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jURLTextFieldActionPerformed

    private void jBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowseButtonActionPerformed
        String file = doSourceOpenDialog();
        if (file != null) {

            jURLTextField.setText(file + "?p=xml");
        }
    }//GEN-LAST:event_jBrowseButtonActionPerformed

    private void jRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRefreshButtonActionPerformed
        updateListToCurrent();
        updateCurrentRadarInfo();
        myCONUSPanel.repaint();
    }//GEN-LAST:event_jRefreshButtonActionPerformed

    private void jLoadNewSourceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadNewSourceButtonActionPerformed
        addNewSourceFromFields(false, true);
    }//GEN-LAST:event_jLoadNewSourceButtonActionPerformed

    private void jBookmarkComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBookmarkComboBoxActionPerformed
        // Refresh the list to the new selection...
        updateListToCurrent();

    }//GEN-LAST:event_jBookmarkComboBoxActionPerformed

    private void jGroupComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jGroupComboBoxActionPerformed

        // Set the group to the current combo selection.
        String theItem = (String) jGroupComboBox.getSelectedItem();
        setShownGroup(theItem);

    }//GEN-LAST:event_jGroupComboBoxActionPerformed

    private void jSingleLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSingleLoadActionPerformed

        URL aURL = doSingleProductOpenDialog();
        if (aURL != null) {
            String text = aURL.toString();
            jSingleURLTextField.setText(text);

            // Now get the file from the URL
            File aFile = Builder.getFileFromURL(aURL);
            String absolutePath = aFile.getAbsolutePath();

            // FIXME: add stuff for xml file
            NetcdfFileInfo info = NetcdfBuilder.getNetcdfFileInfo(absolutePath);
            if (info.success) {
                jSingleProductTextField.setText(info.TypeName);
                jSingleChoiceTextField.setText(info.Choice);
                jSingleTimeTextField.setText(info.Time.toString());
            }
        }
    }//GEN-LAST:event_jSingleLoadActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jAddLocalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddLocalButtonActionPerformed

        // Check URL format first...
        String text = jSingleURLTextField.getText();
        URL aURL = null;
        try {
            aURL = new URL(text);
        } catch (MalformedURLException ex) {
            String message = "Can't parse the URL.\nNeed something like: 'file:/C:/data.netcdf'\n" + ex.toString();
            JOptionPane.showMessageDialog(null,
                    message,
                    "URL malformed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check product name...
        String product = jSingleProductTextField.getText();

        if (product.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Need a Product name to add this in the products under",
                    "Missing:",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check choice name...
        String choice = jSingleChoiceTextField.getText();
        if (choice.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Need a Choice name to add this in the choices under",
                    "Missing:",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Use the current time for product.  FIXME: could we open it up
        // and get the time?  Maybe
        Calendar cal = Calendar.getInstance();
        Date d = cal.getTime();

        // Ugggh. Params for netcdf differ from params for xml files, so we
        // fudge it for now...
        // Ok we'll get it from the builder I think....
        String[] params = null;
        String s = aURL.toString().toLowerCase();
        if (s.endsWith(".netcdf") || s.endsWith(".netcdf.gz")) {
            params = new String[]{"netcdf", "", product, choice, ""};
        } else if (s.endsWith(".xml") || s.endsWith(".xml.gz")) {
            // Params 0 are of this form for a regular index:
            // 0 - builder name 'W2ALGS'
            // 1 - 'GzippedFile' or some other storage type
            // 2 - Base path such as "http://www/warnings"
            // 3 - 'xmldata' formatter_name
            // 4 - short file such as '1999_ktlx.netcdf.gz'
            params = new String[]{"W2ALGS", "", "", "", ""};
        } else {
            // can't load it...
        }
        // Finally try to add it.
        if (params != null) {
            SourceManager.getInstance().addSingleURL(aURL, product, choice, d, params);
            JOptionPane.showMessageDialog(null, "Added file to local file index",
                                "Add success", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_jAddLocalButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddLocalButton;
    private javax.swing.JTabbedPane jBookTabPane;
    private javax.swing.JComboBox jBookmarkComboBox;
    private javax.swing.JButton jBrowseButton;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jGroupComboBox;
    private javax.swing.JPanel jIndexPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JButton jLoadNewSourceButton;
    private javax.swing.JTextField jNameTextField;
    private javax.swing.JButton jRefreshButton;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jSingleChoiceTextField;
    private javax.swing.JButton jSingleLoad;
    private javax.swing.JPanel jSinglePanel;
    private javax.swing.JTextField jSingleProductTextField;
    private javax.swing.JTextField jSingleTimeTextField;
    private javax.swing.JTextField jSingleURLTextField;
    private javax.swing.JScrollPane jSourceTableScrollPane;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jURLTextField;
    // End of variables declaration//GEN-END:variables

    /** Update the table of bookmarks to the current combo box selection */
    private void updateListToCurrent() {
        try {
            // FIXME: should be from GUI, not hardcoded
            String text = (String) jBookmarkComboBox.getSelectedItem();
            BookmarkURLData bookmarks = null;
            if (myDebugList.equals(text)) {
                bookmarks = SourceBookmarks.getFakeBookmarks(20, 5);
            } else {
                URL aURL = new URL(text);
                bookmarks = SourceBookmarks.getBookmarksFromURL(aURL);
            }
            setBookmarks(bookmarks);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, "Bookmark must be a valid URL");
        }
    }

    /** Try to update the image/radar info vcp latencies for the CONUS */
    public void updateCurrentRadarInfo() {

        RadarInfo info = new RadarInfo();
        try {
            URL aURL = new URL("http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/conus.png");
            BufferedImage img = ImageIO.read(aURL);
            myCONUSPanel.setImage(img);
            info.gatherRadarInfo();
        } catch (Exception e) {
        }

        // How many did we manage to get successfully?  Note because HTML sucks and isn't XHTML we might
        // have gotta exceptions the entire way...this is ok, we only add DivInfo on success, so worst case
        // we don't have them all.
        myCONUSPanel.setRadarInfo(info);

    }

    public void setShownGroup(String theItem) {
        if (theItem != null) {
            if ("All".equals(theItem)) {
                myModel.showAllGroups();
            } else {
                myModel.setGroupToShow(theItem);
            }
        } else {
            myModel.showAllGroups();
        }
    }

    /** From our manually added table, handle selection of a line by filling
     * in the fields
     */
    private void jSourceListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        BookmarkURLDataTableModel model = (BookmarkURLDataTableModel) jSourceListTable.getModel();
        if (model != null) {
            int row = jSourceListTable.getSelectedRow();

            if (myModel != null) {
                BookmarkURLSource s = (BookmarkURLSource) (myModel.getDataForRow(row));
                if (s != null) {
                    jNameTextField.setText(s.name);
                    jURLTextField.setText(s.path);
                }
            }
        }
    }

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

    /** Set our visual table list of bookmarks from a given data set of them */
    public void setBookmarks(BookmarkURLData bookmarks) {

        BookmarkURLDataTableModel model = (BookmarkURLDataTableModel) jSourceListTable.getModel();
        mySourceList = bookmarks.data;
        //myVisibleSourceList = mySourceList; // Same for moment
        model.setDataTypes(bookmarks.data);
        model.fireTableDataChanged();

        if (myCONUSPanel != null) {
            myCONUSPanel.setBookmarkList(bookmarks.data);
        }
        // Add each unique group item to list
        jGroupComboBox.removeAllItems();
        jGroupComboBox.addItem(ALLGROUPS);
        Iterator<String> it = bookmarks.groups.iterator();
        int i = 0;
        int select = 0;
        while (it.hasNext()) {
            String current = it.next();
            jGroupComboBox.addItem(current);
            if (current.equalsIgnoreCase("Realtime")) {
                select = i;
            }
            i++;
        }
        jGroupComboBox.setSelectedIndex(select);

        String theItem = (String) jGroupComboBox.getSelectedItem();
        setShownGroup(theItem);
    }

    /** Try to add a source to the display from the currently showing
     * fields
     */
    public void addNewSourceFromFields(boolean confirm, boolean report) {
        String path = jURLTextField.getText();
        String name = jNameTextField.getText();

        // Assuming realtime?  FIXME
        boolean realtime = true;
        boolean connect = true;
        CommandManager.getInstance().executeCommand(
                new SourceAddCommand(name, path, confirm, report, realtime, connect), false);
    }

    public String doSourceOpenDialog() {

        String pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new LocalDataFilter());
        chooser.setDialogTitle("Open an Index file");
        // rcp chooiser.setFilterPath("D:/") ?

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // pickedFile = chooser.getSelectedFile().getName();
            pickedFile = chooser.getSelectedFile().getAbsolutePath();
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL().toString();
            } catch (MalformedURLException ex) {
                pickedFile = "Couldn't parse file location";
            }

        }
        return pickedFile;
    }

    /** Load an individual file into the ManualLoadIndex */
    public URL doSingleProductOpenDialog() {

        URL pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new SingleProductFileFilter());
        chooser.setDialogTitle("Add single product");
        // rcp chooiser.setFilterPath("D:/") ?

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
    }
}
