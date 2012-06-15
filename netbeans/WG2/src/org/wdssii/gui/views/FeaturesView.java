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
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.gis.MapFeature;
import org.wdssii.gui.gis.MapGUI;
import org.wdssii.gui.gis.PolarGridFeature;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.*;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.views.WdssiiSDockedViewFactory.SDockView;

public class FeaturesView extends JThreadPanel implements SDockView, CommandListener {

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
	 * Get the items for an individual view
	 */
	@Override
	public void addGlobalCustomTitleBarComponents(List<Object> addTo) {

		// Interpolation button
		Icon test = SwingIconFactory.getIconByName("cart_add.png");
		JPopupMenu menu = new JPopupMenu();

		JMenuItem item;
		item = new JMenuItem("Add Slice");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		menu.add(item);

		item = new JMenuItem("Add Stick");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StickActionPerformed(evt);
			}
		});
		menu.add(item);
		item = new JMenuItem("Add Map from ESRI shapefile...");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		menu.add(item);

		item = new JMenuItem("Add Polargrid");
		item.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jPolarGridButtonActionPerformed(evt);
			}
		});
		menu.add(item);

		JwgDropDownButton b1 = new JwgDropDownButton(test);
		b1.setToolTipText("Menu options");
		b1.setMenu(menu);
		addTo.add(b1);
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
		public String type;
		public String timeStamp;
		public String subType;
		public String message;
		public boolean candelete;
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

	public void updateTable(Object info) {

		// We only want to change selection when the user directly
		// changes one, not from other updates like from looping
		boolean changeSelection = false;
		Feature fromSelect = null;
		if (info instanceof FeatureSelectCommand){
			FeatureSelectCommand c = (FeatureSelectCommand)(info);
			changeSelection = true;
			fromSelect = c.getFeature();
			log.debug("******SELECTCOMMAND "+fromSelect);
			
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
		//log.debug("Top selected in was "+topFeature);
		if (changeSelection){
		if (fromSelect != topFeature){
		//	log.error("**********NOT THE SAME "+fromSelect+", "+topFeature);
		} 
		}

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
			if (myLastSelectedFeature == d){
				oldSelect = currentLine;
			}
			currentLine++;
		}
		myFeatureListTableModel.setDataTypes(newList);
		myFeatureListTableModel.fireTableDataChanged();

		// Keep old selection unless it's gone...
		if (!changeSelection){ 
			// Use old selection if exists...
			if (oldSelect > 0){
			   select = oldSelect; 
			   topFeature = myLastSelectedFeature;
			}
		}else{

			//log.debug("CHANGE SELECTION IS TRUE");
		}

		if (select > -1) {
			select = jObjects3DListTable.convertRowIndexToView(select);

			// This of course fires an event, which calls jProductsListTableValueChanged
			// which would send a command which would do this again in an
			// infinite loop.  So we have a flag.  We don't use isAdjusting
			// because it still fires and event when you set it false
			myFeatureListTableModel.setRebuilding(true);
			jObjects3DListTable.setRowSelectionInterval(select, select);
		//	log.debug("Select row of "+select);

			if (myLastSelectedFeature != topFeature) {
		 //       log.debug("LAST/top "+myLastSelectedFeature+","+topFeature);
		//	log.debug("Change out gui "+changeSelection+", "+oldSelect);
				jFeatureGUIPanel.removeAll();
				topFeature.setupFeatureGUI(jFeatureGUIPanel);
				jFeatureGUIPanel.validate();
				jFeatureGUIPanel.repaint();
				jControlScrollPane.revalidate();
				myLastSelectedFeature = topFeature;
			} else {
				topFeature.updateGUI();
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
		return null;
	}

	private void initComponents(boolean dockControls) {

		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		jInfoLabel = new JLabel("---");
		JComponent controls = initFeatureControlGUI();
		JComponent selection = initFeatureSelectGUI();
	//	JComponent toolbar = initToolBar();
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

	//	add(toolbar, new CC().dockNorth());
		add(jInfoLabel, new CC().dockNorth().growX());
		add(rootComponent, new CC().growX().growY());
		// growX().growY());

		// to size correct, init table last, nope not it
		initTable();
	}

	@Override
	public Component getControlComponent() {
		return jControlScrollPane;
	}

	@Override
	public String getControlTitle(){
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
