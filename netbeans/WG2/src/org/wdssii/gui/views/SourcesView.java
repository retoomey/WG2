package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceFactory;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.views.WdssiiSDockedViewFactory.SDockView;
import org.wdssii.xml.Tag;
import org.wdssii.xml.config.Tag_sources;

/**
 * SourcesView.
 *
 * @author Robert Toomey
 */
public class SourcesView extends JThreadPanel implements SDockView, CommandListener {

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
	public static class Factory extends WdssiiSDockedViewFactory {

		public Factory() {
			super("Sources", "brick_add.png");
		}

		@Override
		public Component getNewComponent() {
			return new SourcesView(myDockControls);
		}
	}
	private SourceListTableModel mySourceListTableModel;
	private RowEntryTable jObjects3DListTable;
	private JPanel jSourceGUIPanel;
	private javax.swing.JToolBar jEditToolBar;
	private javax.swing.JScrollPane jObjectScrollPane;
	private javax.swing.JLabel jInfoLabel;

	/**
	 * Get the items for an individual view
	 */
	@Override
	public void addGlobalCustomTitleBarComponents(List addTo) {

		// Interpolation button
		Icon test = SwingIconFactory.getIconByName("cart_add.png");
		JPopupMenu menu = new JPopupMenu();

		JMenuItem item;
		item = new JMenuItem("Add Source...");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addSourceFromDialog(evt);
			}
		});
		menu.add(item);

		item = new JMenuItem("Export Source List...");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				exportSourceFromDialog(evt);
			}
		});
		menu.add(item);


		JwgDropDownButton b1 = new JwgDropDownButton(test);
		b1.setToolTipText("Menu options");
		b1.setMenu(menu);
		addTo.add(b1);
	}

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

		updateTable();
		updateInfoLabel();
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
		return null;
	}

	private void addSourceFromDialog(java.awt.event.ActionEvent evt) {
		// Open dialog for single file adding.....

		// FeatureCreateCommand doit = new FeatureCreateCommand("VSlice");
		// CommandManager.getInstance().executeCommand(doit, true);
		//FeatureList.theFeatures.addFeature(new LLHAreaFeature(0));
		doSingleProductOpenDialog();
	}

	private void exportSourceFromDialog(java.awt.event.ActionEvent evt) {
		// Open dialog for single file adding.....
		doExportSourceListDialog();
	}

	private void initComponents(boolean dockControls) {

		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		jInfoLabel = new JLabel("---");
		JComponent controls = initControlGUI();
		JComponent selection = initSelectGUI();
		//JComponent toolbar = initToolBar();
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

		//add(toolbar, new CC().dockNorth());
		add(jInfoLabel, new CC().dockNorth().growX());
		add(rootComponent, new CC().growX().growY());
		// growX().growY());

		// to size correct, init table last, nope not it
		initTable();
	}

	@Override
	public Component getControlComponent() {
		return jSourceGUIPanel;
	}

	@Override
	public String getControlTitle() {
		return "Selection";
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
	// Source type.
	/**
	 * Filter for local files.
	 */
	private class SourceFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			String t = f.getName().toLowerCase();
			// FIXME: need to get these from the Sources, some sort of
			// Factory
			boolean canBeHandled = SourceFactory.canAnyHandleFileType(f);
			return (f.isDirectory() || canBeHandled);

//							|| t.endsWith(".netcdf") || t.endsWith(".nc") || t.endsWith(".netcdf.gz")
//							|| t.endsWith(".xml") || t.endsWith(".xml.gz"));
		}

		@Override
		public String getDescription() {
			Set<String> types = SourceFactory.getAllHandledFileDescriptions();
			String d = "";
			for (String s : types) {
				d += s;
				d += " ";
			}
			d += "Files";
			return d;
		}
	}

	public void doExportSourceListDialog() {
		// Experimental....begin ability to save stuff...
		// For the moment, regular java save dialog...
		// this will be hacked at first, lots to do low level to get this working...

		JFileChooser fileopen = new JFileChooser();
		fileopen.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				String t = f.getName().toLowerCase();
				// FIXME: need to get these from the Builders
				return (f.isDirectory() || t.endsWith(".xml"));
			}

			@Override
			public String getDescription() {
				return "XML file containing list of sources";
			}
		});
		fileopen.setDialogTitle("Export Table Selection");
		int ret = fileopen.showSaveDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = fileopen.getSelectedFile();
			try {
				String temp = file.toString();
				if (!(temp.endsWith(".xml"))) {
					file = new File(temp + ".xml");
				}
				URL aURL = file.toURI().toURL();
				log.debug("Trying to write output to " + aURL.toString());
				Tag root = SourceList.theSources.getTag();
				if (root != null) {
					root.writeAsRoot(aURL);
				}

			} catch (MalformedURLException ex) {
			}
		}

	}

	/**
	 * Load an individual file into the ManualLoadIndex
	 */
	public URL doSingleProductOpenDialog() {

		SourcesURLLoadDialog myDialog = new SourcesURLLoadDialog(null, true, "Open Source");
		return null;
	}

	/**
	 * FIXME: move to factory. When an Index is found....set it up
	 */
	public boolean setUpIndexGUIForURL(JTextField urlDisplay, JComponent holder, URL aURL) {
		return false;
	}
}
