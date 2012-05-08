package org.wdssii.gui.views;

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
import net.infonode.docking.*;
import net.infonode.docking.properties.DockingWindowProperties;
import net.infonode.docking.properties.ViewProperties;
import net.infonode.docking.properties.ViewTitleBarProperties;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.DockWindow;
import org.wdssii.gui.MapGUI;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.features.*;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

public class FeaturesView extends JThreadPanel implements CommandListener {

	public static final String ID = "wdssii.FeaturesView";
	private static Logger log = LoggerFactory.getLogger(FeaturesView.class);

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
		updateTable();
		updateLabel();
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
			super("Features", "brick_add.png");
		}

		@Override
		public Component getNewComponent() {

			// return the thing that is docked....
			// This is actually not called since we bypass getNewDockingWindow
			return new FeaturesView(myDockControls);
		}

		@Override
		public DockingWindow getNewDockingWindow() {
			if (!myDockControls) {
				// Get a single non-docked FeatureView component
				return super.getNewDockingWindow();
			} else {
				Icon i = getWindowIcon();
				String title = getWindowTitle();

				FeaturesView f = new FeaturesView(myDockControls);

				// Create a RootWindow in the view.  Anything added to this
				// will be movable.
				RootWindow root = DockWindow.createARootWindow();
				View topWindow = new View(title, i, root);

				// Inside the root window we'll add two views...
				View controls = new View("Feature Controls", i, f.getControlComponent());
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
	private FeatureListTableModel myFeatureListTableModel;
	private RowEntryTable jObjects3DListTable;
	private Feature myLastSelectedFeature = null;
	private javax.swing.JButton jNewMapButton;
	private javax.swing.JButton jNewVSliceButton;
	private javax.swing.JButton jNewStickButton;
	private javax.swing.JButton jNewPolarGridButton;
	private JPanel jFeatureGUIPanel;
	private javax.swing.JToolBar jEditToolBar;
	private javax.swing.JScrollPane jObjectScrollPane;
	private javax.swing.JScrollPane jControlScrollPane;
	private javax.swing.JLabel jInfoLabel;

	/**
	 * Storage for displaying the current feature list
	 */
	private static class FeatureListTableData {

		public String visibleName; // Name shown in list
		public String group; // The feature group such as 'maps'
		public String keyName; // The key used to select this handler
		public boolean checked;
		public boolean onlyMode;
		public String type;
		public String timeStamp;
		public String subType;
		public String message;
	}

	private class FeatureListTableModel extends RowEntryTableModel<FeatureListTableData> {

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
		myFeatureListTableModel = new FeatureListTableModel();
		jObjects3DListTable = new RowEntryTable();
		final JTable myTable = jObjects3DListTable;
		jObjects3DListTable.setModel(myFeatureListTableModel);
		final FeatureListTableModel myModel = myFeatureListTableModel;

		jObjects3DListTable.setFillsViewportHeight(true);
		jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jObjectScrollPane.setViewportView(jObjects3DListTable);

		FeatureListTableCellRenderer p = new FeatureListTableCellRenderer();
		jObjects3DListTable.setDefaultRenderer(FeatureListTableData.class, p);

		int count = myTable.getColumnCount();
		TableColumnModel cm = myTable.getColumnModel();
		JCheckBox aBox = new JCheckBox();
		Dimension d = aBox.getMinimumSize();
		IconHeaderRenderer r = new IconHeaderRenderer();

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

		jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				jObjects3DListTableValueChanged(e);
			}
		});

		jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {

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
				String name = "Delete " + entry.visibleName;
				Item i = new Item(name, entry);
				popupmenu.add(i);
				i.addActionListener(al);
				return popupmenu;
			}

			@Override
			public void handleClick(Object stuff, int orgRow, int orgColumn) {

				if (stuff instanceof FeatureListTableData) {
					FeatureListTableData entry = (FeatureListTableData) (stuff);

					switch (orgColumn) {
						case FeatureListTableModel.OBJ_VISIBLE: {
							Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
							if (f != null) {
								FeatureMemento m = f.getNewMemento();
								m.setVisible(!entry.checked);
								FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
								CommandManager.getInstance().executeCommand(c, true);
							}
						}
						break;
						case FeatureListTableModel.OBJ_ONLY: {
							Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
							if (f != null) {
								FeatureMemento m = f.getNewMemento();
								m.setOnly(!entry.onlyMode);
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
		updateTable();
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
		// value, we don't want to loop infinitely
		if (myFeatureListTableModel.rebuilding()) {
			return;
		}
		int row = jObjects3DListTable.getSelectedRow();
		if (row > -1) {
			int dataRow = jObjects3DListTable.convertRowIndexToModel(row);
			if (myFeatureListTableModel != null) {
				FeatureListTableData d = (FeatureListTableData) (myFeatureListTableModel.getDataForRow(dataRow));
				if (d != null) {
					FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
					CommandManager.getInstance().executeCommand(c, true);
				}
			}
		}
	}

	public void updateTable() {

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
		ArrayList<FeatureListTableData> newList = new ArrayList<FeatureListTableData>();
		Feature selectedFeature = null;
		Feature topFeature = flist.getTopSelected();

		for (Feature d : f) {
			FeatureListTableData d2 = new FeatureListTableData();
			d2.visibleName = d.getName();
			d2.group = d.getFeatureGroup();
			d2.checked = d.getVisible();  // methods allow internal locking
			d2.keyName = d.getKey();
			d2.onlyMode = d.getOnlyMode();
			d2.message = d.getMessage();
			newList.add(d2);
			if (topFeature == d) {
				select = currentLine;
				selectedFeature = d;
			}
			currentLine++;
		}
		myFeatureListTableModel.setDataTypes(newList);
		myFeatureListTableModel.fireTableDataChanged();

		if (select > -1) {
			select = jObjects3DListTable.convertRowIndexToView(select);

			// This of course fires an event, which calls jProductsListTableValueChanged
			// which would send a command which would do this again in an
			// infinite loop.  So we have a flag.  We don't use isAdjusting
			// because it still fires and event when you set it false
			myFeatureListTableModel.setRebuilding(true);
			jObjects3DListTable.setRowSelectionInterval(select, select);

			if (myLastSelectedFeature != selectedFeature) {
				jFeatureGUIPanel.removeAll();
				selectedFeature.setupFeatureGUI(jFeatureGUIPanel);
				jFeatureGUIPanel.validate();
				jFeatureGUIPanel.repaint();
				jControlScrollPane.revalidate();
				myLastSelectedFeature = selectedFeature;
			} else {
				selectedFeature.updateGUI();
			}

			myFeatureListTableModel.setRebuilding(false);

		} else {
			setEmptyControls();
			jFeatureGUIPanel.validate();
			jFeatureGUIPanel.repaint();
			myLastSelectedFeature = null;
		}
		jObjects3DListTable.repaint();
	}

	private void updateLabel() {
		jInfoLabel.setText(FeatureList.theFeatures.getGUIInfoString());
	}

	private JComponent initFeatureControlGUI() {
		jFeatureGUIPanel = new JPanel();
		jControlScrollPane = new JScrollPane();
		jControlScrollPane.setViewportView(jFeatureGUIPanel);
		jFeatureGUIPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		setEmptyControls();
		return jControlScrollPane;
	}

	/**
	 * The part of the GUI that deals with selection of a individual feature
	 */
	private JComponent initFeatureSelectGUI() {

		jObjectScrollPane = new JScrollPane();
		jInfoLabel = new JLabel("---");
		return jObjectScrollPane;
	}

	private JToolBar initToolBar() {
		jEditToolBar = new javax.swing.JToolBar();
		jEditToolBar.setFloatable(false);
		jNewVSliceButton = new javax.swing.JButton("+Slice");
		jNewStickButton = new javax.swing.JButton("+Stick");
		jNewMapButton = new javax.swing.JButton("+Map");
		jNewPolarGridButton = new javax.swing.JButton("+PolarGrid");
		jEditToolBar.setRollover(true);

		jNewStickButton.setFocusable(false);
		jNewStickButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jNewStickButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		jNewStickButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StickActionPerformed(evt);
			}
		});

		jNewVSliceButton.setFocusable(false);
		jNewVSliceButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jNewVSliceButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		jNewVSliceButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		jEditToolBar.add(jNewStickButton);
		jEditToolBar.add(jNewVSliceButton);

		jNewPolarGridButton.setFocusable(false);
		jNewPolarGridButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jNewPolarGridButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		jNewPolarGridButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jPolarGridButtonActionPerformed(evt);
			}
		});
		jEditToolBar.add(jNewPolarGridButton);

		jNewMapButton.setFocusable(false);
		jNewMapButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jNewMapButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		jNewMapButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		jEditToolBar.add(jNewMapButton);
		return jEditToolBar;
	}

	private void initComponents(boolean dockControls) {

		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		jInfoLabel = new JLabel("---");
		JComponent controls = initFeatureControlGUI();
		JComponent selection = initFeatureSelectGUI();
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
		return jControlScrollPane;
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

	private void StickActionPerformed(java.awt.event.ActionEvent evt) {
		FeatureCreateCommand doit = new FeatureCreateCommand("Stick");
		CommandManager.getInstance().executeCommand(doit, true);
	}

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		URL newMap = MapGUI.doSingleMapOpenDialog();
		if (newMap != null) {
			Feature testOne = new MapFeature(FeatureList.theFeatures, newMap);
			FeatureList.theFeatures.addFeature(testOne);
		}
		updateGUI();
	}

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
		FeatureCreateCommand doit = new FeatureCreateCommand("Slice");
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
