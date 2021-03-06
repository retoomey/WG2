package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;

import org.wdssii.core.CommandManager;
import org.wdssii.core.StringUtil;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.builders.Builder.BuilderFileInfo;
import org.wdssii.gui.RadarInfo;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.IndexSourceAddParams;
import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.sources.IndexSourceParamsGUI;
import org.wdssii.gui.sources.IndexSourceParamsGUI.historyControl;
import org.wdssii.gui.swing.CONUSJPanel;
import org.wdssii.gui.swing.CONUSJPanel.CONUSJPanelListener;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.xml.SourceBookmarks;
import org.wdssii.xml.SourceBookmarks.BookmarkURLData;
import org.wdssii.xml.SourceBookmarks.BookmarkURLSource;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

/**
 *
 * WdssiiCatalog handles connecting to one of our xml catalog files, or a list
 * of available sources
 *
 * @author Robert Toomey
 *
 */
public class WdssiiCatalog extends JThreadPanel implements CONUSJPanelListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(WdssiiCatalog.class);
//	private ArrayList<BookmarkURLSource> mySourceList;
	private static final String ALLGROUPS = "All";
	private JTable jSourceListTable;
	private final String myDebugList = "Debug List";
	private BookmarkURLDataTableModel myModel;
	private final CONUSJPanel myCONUSPanel;
	private Date mySingleDate;
	private historyControl myHistoryControl;
	private int historyValue = 1000;

	@Override
	public void updateInSwingThread(Object info) {
		// Snag any new book marks, note that now myNewBookmarks could
		// be changed while we're drawing..or have been changed 10 times.
		// We grab the latest available.
		boolean requiredUpdateList = false;
		synchronized (workerLock) {

			if (myNewBookmarks != myBookmarks) {
				myBookmarks = myNewBookmarks;
				requiredUpdateList = true;
			}
		}

		if ((myBookmarks != null) && requiredUpdateList) {
			syncBookmarks();
		}
	}

	@Override
	public void radarClicked(String name) {
		if (jSourceListTable != null) {
			if (myModel != null) {
				Object a = myModel.getDataForKeyField(name);
				if (a != null) {
					BookmarkURLSource s = (BookmarkURLSource) (a);
					int index = myModel.getRowIndexOf(s);
					if (index >= 0) {
						// We want the event to fire
						// myBookmarkModel.setRebuilding(true);
						jSourceListTable.setRowSelectionInterval(index, index);
						// myBookmarkModel.setRebuilding(false);
					}
				}
			}
		}

	}

	@Override
	public void radarDoubleClicked(String name) {
		addNewSourceFromFields(true, true);
	}

	/**
	 * Filter for local files.
	 */
	private static class SingleProductFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			String t = f.getName().toLowerCase();
			// FIXME: need to get these from the Builders
			return (f.isDirectory() || t.endsWith(".netcdf") || t.endsWith(".nc") || t.endsWith(".netcdf.gz")
					|| t.endsWith(".xml") || t.endsWith(".xml.gz"));
		}

		@Override
		public String getDescription() {
			return "netcdf files or xml files";
		}
	}

	/**
	 * A class that uses a BookmarkURLData as its model
	 */
	private static class BookmarkURLDataTableModel extends RowEntryTableModel<BookmarkURLSource> {

		// FIXME: should be an enum class probably...
		public static final int BOOK_NAME = 0;
		public static final int BOOK_LOCATION = 1;
		public static final int BOOK_PATH = 2;
		public static final int BOOK_TIME = 3;
		public static final int BOOK_GROUP = 4;
		/**
		 * Group filter
		 */
		private BookmarkRowFilter myRowFilter = new BookmarkRowFilter();

		//private void selectLineOf(BookmarkURLSource s) {
		//	throw new UnsupportedOperationException("Not yet implemented");
		//}

		// Set up a row filter for the group combo box
		private static class BookmarkRowFilter extends RowFilter<Object, Object> {

			private String myGroupName;
			private boolean myFilterOn = false;

			@Override
			public boolean include(RowFilter.Entry entry) {

				// In our model, every column is the same object. 0 is first column.
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
			super(BookmarkURLSource.class, new String[] { "Name", "Location", "Path", "Latest", "Group" });
		}

		public TableRowSorter<WdssiiCatalog.BookmarkURLDataTableModel> getGroupModelSorter() {
			TableRowSorter<WdssiiCatalog.BookmarkURLDataTableModel> sorter = new TableRowSorter<WdssiiCatalog.BookmarkURLDataTableModel>(
					this);
			sorter.setRowFilter(myRowFilter);

			for (int i = 0; i < WdssiiCatalog.BookmarkURLDataTableModel.BOOK_GROUP; i++) {
				Comparator<BookmarkURLSource> c = null;
				switch (i) {

				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_NAME: {
					c = new Comparator<BookmarkURLSource>() {
						@Override
						public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
							return (o1.name).compareTo(o2.name);
						}
					};
				}
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_LOCATION: {
					c = new Comparator<BookmarkURLSource>() {
						@Override
						public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
							return (o1.location).compareTo(o2.location);
						}
					};
				}
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_PATH: {
					c = new Comparator<BookmarkURLSource>() {
						@Override
						public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
							return (o1.path).compareTo(o2.path);
						}
					};
				}
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_TIME: {
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
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_GROUP: {
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
			// this.fireTableStructureChanged();
			this.fireTableDataChanged();
		}

		public void showAllGroups() {
			myRowFilter.showAllGroups();
			// this.fireTableStructureChanged();
			this.fireTableDataChanged();
		}

		@Override
		public String getKeyField(BookmarkURLSource data) {
			return data.name;
		}
	}

	/**
	 * Our custom renderer for our product view table
	 */
	private static class BookmarkTableCellRenderer extends WG2TableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean cellHasFocus, int row, int col) {

			// Let super set all the defaults...
			super.getTableCellRendererComponent(table, "", isSelected, cellHasFocus, row, col);

			String info = "";
			int trueCol = table.convertColumnIndexToModel(col);

			if (value instanceof BookmarkURLSource) {
				BookmarkURLSource e = (BookmarkURLSource) value;

				switch (trueCol) {

				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_NAME:
					info = e.name;
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_LOCATION:
					info = e.location;
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_PATH:
					info = e.path;
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_TIME:
					info = e.time;
					break;
				case WdssiiCatalog.BookmarkURLDataTableModel.BOOK_GROUP:
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

	public WdssiiCatalog() {

		createIndexLoadPanel();
		boolean doit = true;
		if (doit) {
			myModel = new WdssiiCatalog.BookmarkURLDataTableModel();
			jSourceListTable = new JTable();
			jSourceListTable.setModel(myModel);

			jSourceListTable.setFillsViewportHeight(true);
			jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jSourceTableScrollPane.setViewportView(jSourceListTable);

			WdssiiCatalog.BookmarkTableCellRenderer p = new WdssiiCatalog.BookmarkTableCellRenderer();
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

					/**
					 * On double click, try to add the source
					 */
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

			// jBookmarkComboBox.setModel(new DefaultComboBoxModel(new String[]{
			// "http://tensor.protect.nssl/rindexv2.xml", "file:/D:/testing2.xml",
			// myDebugList}));
			jBookmarkComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {
					// myDebugList, "http://tensor.protect.nssl/rindexv2.xml",
					// "file:/D:/testing2.xml"}));
					// http://tensor.protect.nssl/rindexv2.xml
					// http://vmrms-ark/status.xml
					myDebugList, "http://vmrms-ark/status.xml", "file:/D:/testing2.xml" }));
			jSourceListTable.setRowSorter(myModel.getGroupModelSorter());
		}
		CONUSJPanel ipanel = new CONUSJPanel();
		myCONUSPanel = ipanel;
		JScrollPane holder = new JScrollPane();
		holder.setViewportView(ipanel);

		if (doit) {
			// jBookTabPane.addTab("CONUS", null, holder, "stuff");
			// myCONUSPanel.addCONUSJPanelListener(this);

			updateListToCurrent();
			// updateCurrentRadarInfo();
		}
	}

	private void createIndexLoadPanel() {

		// The load from index panel ------------------------------------------
		setLayout(new MigLayout("wrap 3", "[pref!][grow][align right]", "[pref!|pref!|pref!|pref!|pref!|pref!|grow]"));
		// Catalog section
		add(new JLabel("Catalog:"));
		jBookmarkComboBox = new JComboBox<String>();
		add(jBookmarkComboBox, new CC().growX().spanX(2).wrap());
		add(new JLabel("Group:"));
		jGroupComboBox = new JComboBox<String>();
		add(jGroupComboBox, new CC().growX().spanX(1));
		JButton refresh = new JButton("Refresh");
		add(refresh, new CC().wrap());

		add(new JSeparator(), new CC().growX().spanX(3).wrap());

		// Index information section
		add(new JLabel("Index URL:"));
		jURLTextField = new JTextField();
		add(jURLTextField, new CC().growX().spanX(2).wrap());
		add(new JLabel("Source Name:"));
		jNameTextField = new JTextField();
		add(jNameTextField, new CC().growX().spanX(2).wrap());
		ActionListener l = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				historyActionPerformed(evt);
			}
		};
		historyControl h = IndexSourceParamsGUI.createHistoryControl(l);
		myHistoryControl = h;
		historyValue = myHistoryControl.getHistory();
		add(h.getLabel());
		jComboBox2 = h.getDropDown();
		add(jComboBox2, new CC().growX());
		JButton add = new JButton("Add this index");
		add(add, new CC().wrap());

		jBookTabPane = new JTabbedPane();
		add(jBookTabPane, new CC().spanX(3).growX().growY());
		jSourceTableScrollPane = new JScrollPane();
		jBookTabPane.addTab("List", jSourceTableScrollPane);

		// Listener setup -----------------------------------------------------
		jURLTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jURLTextFieldActionPerformed(evt);
			}
		});
		jGroupComboBox.setModel(new DefaultComboBoxModel<String>(new String[] { "All" }));
		jGroupComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jGroupComboBoxActionPerformed(evt);
			}
		});
		jBookmarkComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jCatalogComboBoxActionPerformed(evt);
			}
		});
		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jLoadNewSourceButtonActionPerformed(evt);
			}
		});
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jRefreshButtonActionPerformed(evt);
			}
		});
	}

	private void jURLTextFieldActionPerformed(ActionEvent evt) {
		// TODO add your handling code here:
	}

	private void jRefreshButtonActionPerformed(ActionEvent evt) {
		updateListToCurrent();
		//updateCurrentRadarInfo();
		//myCONUSPanel.repaint();
	}

	private void jLoadNewSourceButtonActionPerformed(ActionEvent evt) {
		addNewSourceFromFields(false, true);
	}

	private void jCatalogComboBoxActionPerformed(ActionEvent evt) {
		// Refresh the list to the new selection...
		updateListToCurrent();

	}

	private void jGroupComboBoxActionPerformed(ActionEvent evt) {

		// Set the group to the current combo selection.
		String theItem = (String) jGroupComboBox.getSelectedItem();
		setShownGroup(theItem);

	}

	private void setToInfo(BuilderFileInfo info) {
		jSingleProductTextField.setText(info.TypeName);
		jSingleChoiceTextField.setText(info.Choice);
		setSingleTimeDate(info.Time);
		jDataTypeTextField.setText(info.DataType);
	}

	private void clearInfo() {
		jSingleURLTextField.setText("");
		jSingleProductTextField.setText("");
		jSingleChoiceTextField.setText("");
		setSingleTimeDate(null);
		jDataTypeTextField.setText("");
	}

	private void setSingleTimeDate(Date aDate) {
		mySingleDate = aDate;
		if (aDate != null) {
			jSingleTimeTextField.setText(aDate.toString());
		} else {
			jSingleTimeTextField.setText("");
		}
	}

	private void historyActionPerformed(ActionEvent evt) {
		if (myHistoryControl != null) {
			historyValue = myHistoryControl.getHistory();
		}
	}

	private JTabbedPane jBookTabPane;
	private JComboBox<String> jBookmarkComboBox;
	private JComboBox<String> jComboBox2;
	private JComboBox<String> jGroupComboBox;
	private JTextField jNameTextField;
	private JTextField jSingleChoiceTextField;
	private JTextField jSingleProductTextField;
	private JTextField jSingleTimeTextField;
	private JTextField jDataTypeTextField;
	private JTextField jSingleURLTextField;
	private JScrollPane jSourceTableScrollPane;
	private JTextField jURLTextField;
	// private JTabbedPane jRootTab;

	private final Object workerLock = new Object(); // Lock 1 (first)

	/** Current care about worker...locked by workerLock */
	private NetworkBookmarkWorker myWorker = null;

	/** Current new unprocessed bookmarks...locked by workerlock */
	private BookmarkURLData myNewBookmarks = null;

	/**
	 * Current bookmark data...note that this is not locked, but changed out with
	 * myNewBookmarks within a valid lock
	 */
	private BookmarkURLData myBookmarks = null;

	// End of variables declaration

	public static class NetworkBookmarkWorker extends WdssiiJob {

		private WdssiiCatalog myOwner;
		private URL myURL;

		public NetworkBookmarkWorker(String jobName, WdssiiCatalog owner, URL aURL) {
			super(jobName);
			myOwner = owner;
			myURL = aURL;
		}

		@Override
		public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
			BookmarkURLData bookmarks = SourceBookmarks.getBookmarksFromURL(myURL);
			myOwner.fromThreadSetBookmarks(bookmarks, this);
			return WdssiiJobStatus.OK_STATUS;
		}

	}

	/**
	 * Update the table of bookmarks to the current combo box selection. This spawns
	 * a separate thread to do the network part if needed.
	 */
	private void updateListToCurrent() {

		// Change out workers only in workerLock
		synchronized (workerLock) {
			if (myWorker != null) {
				// It doesn't matter really..any 'old' workers might even
				// call fromThreadSetBookmarks later..
				myWorker.cancel();
				myWorker = null;
			}
			String text = (String) jBookmarkComboBox.getSelectedItem();
			// Don't need a thread for this one...
			if (myDebugList.equals(text)) {
				BookmarkURLData bookmarks = SourceBookmarks.getFakeBookmarks(20, 5);
				fromThreadSetBookmarks(bookmarks, null);
			} else {
				URL aURL;
				try {
					aURL = new URL(text);
					myWorker = new NetworkBookmarkWorker("Network", this, aURL);
					myWorker.schedule();
				} catch (MalformedURLException e) {
					JOptionPane.showMessageDialog(null, "Bookmark must be a valid URL");
				}
			}
		}
	}

	/**
	 * Call back from thread to change out bookmarks for a new one. Called when
	 * bookmark reading job is complete
	 * 
	 * @param bookmarks
	 * @param aWorker
	 */
	public void fromThreadSetBookmarks(BookmarkURLData bookmarks, NetworkBookmarkWorker aWorker) {
		synchronized (workerLock) {
			// Important in case it's an old worker coming calling us.
			if (myWorker == aWorker) {
				myNewBookmarks = bookmarks;
				updateGUI(); // Update in GUI thread later...
			}
		}
	}

	/**
	 * This actually set our visual table list of bookmarks from the current
	 * bookmarks after thread work, etc.. Most things should call
	 * updateListToCurrent above
	 */
	private void syncBookmarks() {

		// mySourceList =
		WdssiiCatalog.BookmarkURLDataTableModel model = (WdssiiCatalog.BookmarkURLDataTableModel) jSourceListTable
				.getModel();
		// myVisibleSourceList = mySourceList; // Same for moment
		model.setDataTypes(myBookmarks.data);
		model.fireTableDataChanged();

		if (myCONUSPanel != null) {
			myCONUSPanel.setBookmarkList(myBookmarks.data);
		}
		// Add each unique group item to list
		jGroupComboBox.removeAllItems();
		jGroupComboBox.addItem(ALLGROUPS);
		Iterator<String> it = myBookmarks.groups.iterator();
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

	/**
	 * Try to update the image/radar info vcp latencies for the CONUS
	 */
	public final void updateCurrentRadarInfo() {

		RadarInfo info = new RadarInfo();
		try {
			URL aURL = new URL("http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/conus.png");
			BufferedImage img = ImageIO.read(aURL);
			myCONUSPanel.setImage(img);
			info.gatherRadarInfo();
		} catch (Exception e) {
		}

		// How many did we manage to get successfully? Note because HTML sucks and isn't
		// XHTML we might
		// have gotta exceptions the entire way...this is ok, we only add DivInfo on
		// success, so worst case
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

	/**
	 * From our manually added table, handle selection of a line by filling in the
	 * fields
	 */
	private void jSourceListTableValueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}

		WdssiiCatalog.BookmarkURLDataTableModel model = (WdssiiCatalog.BookmarkURLDataTableModel) jSourceListTable
				.getModel();
		if (model != null) {
			int row = jSourceListTable.getSelectedRow();
			if (row > -1) {
				row = jSourceListTable.convertRowIndexToModel(row);
				if (myModel != null) {
					BookmarkURLSource s = myModel.getDataForRow(row);
					if (s != null) {
						jNameTextField.setText(s.name);
						String p = StringUtil.convertToLabDomain(s.path);
						jURLTextField.setText(p);
					}
				}
			}
		}
	}

	/**
	 * Try to add a source to the display from the currently showing fields
	 */
	public void addNewSourceFromFields(boolean confirm, boolean report) {
		String path = jURLTextField.getText();
		String name = jNameTextField.getText();

		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Give this source a name, such as 'KTLX'");
		} else {
			// Assuming realtime? FIXME
			boolean realtime = true;
			boolean connect = true;
			try {
				URL aURL = new URL(path);
				SourceAddParams params = new IndexSourceAddParams(name, aURL, realtime, connect, historyValue);
				SourceAddCommand c = new SourceAddCommand(params);
				c.setConfirmReport(false, true, this);
				CommandManager.getInstance().executeCommand(c, false);
			} catch (MalformedURLException ex) {
				// Dialog or something....
			}
		}
	}

	/**
	 * Load an individual file into the ManualLoadIndex
	 */
	public URL doSingleProductOpenDialog() {

		URL pickedFile = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new WdssiiCatalog.SingleProductFileFilter());
		chooser.setDialogTitle("Add single product");
		// rcp chooiser.setFilterPath("D:/") ?

		int returnVal = chooser.showOpenDialog(this);
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
