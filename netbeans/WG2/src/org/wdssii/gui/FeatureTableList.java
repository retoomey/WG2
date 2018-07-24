package org.wdssii.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.wdssii.core.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.commands.FeatureDeleteCommand;
import org.wdssii.gui.commands.FeatureSelectCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/** Generic handling of feature lists by group name.  For example the nav list of products, or tabs in features.*/
@SuppressWarnings("serial")
public class FeatureTableList extends  RowEntryTable {
	private final static Logger LOG = LoggerFactory.getLogger(FeatureTableList.class);

	// Commands...subclasses add to
	private final static int DELETECOMMAND = 0;
	
	private FeatureTableListModel myModel;
	private FeatureTableListCellRenderer myRenderer;
	private IconHeaderRenderer myIconHeaderRenderer = null;

	private String myGroupName;

	/**
	 * Storage for displaying the current feature list.
	 */
	public static class FeatureTableListData {

		public String visibleName; // Name shown in list
		public String group; // The feature group such as 'maps'
		public String keyName; // The key used to select this handler
		public boolean checked;
		public boolean onlyMode;
		// Used by the navigator right...
		public String type;
		public String timeStamp;
		public String subType;
		public String message;
		public boolean candelete;
	}

	public static class FeatureTableListModel extends RowEntryTableModel<FeatureTableListData> {

		public static final int OBJ_VISIBLE = 0;
		public static final int OBJ_ONLY = 1;
		public static final int OBJ_NAME = 2;
		public static final int OBJ_GROUP = 3;
		public static final int OBJ_MESSAGE = 4;
		private boolean isRebuilding = false;

		public FeatureTableListModel() {
			super(FeatureTableListData.class, new String[]{
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
	public static class FeatureTableListCellRenderer extends WG2TableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean cellHasFocus, int row, int col) {

			// Let super set all the defaults...
			super.getTableCellRendererComponent(table, "",
					isSelected, cellHasFocus, row, col);

			String info;
			int trueCol = table.convertColumnIndexToModel(col);

			// Each row uses a single LayerTableEntry...
			if (value instanceof FeatureTableListData) {
				FeatureTableListData e = (FeatureTableListData) value;

				switch (trueCol) {

				case FeatureTableListModel.OBJ_VISIBLE:
					return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);
				case FeatureTableListModel.OBJ_ONLY:
					return getJCheckBoxIcon(table, e.onlyMode, "picture.png", "pictures.png", isSelected, cellHasFocus, row, col);
				case FeatureTableListModel.OBJ_NAME:
					info = e.visibleName;
					break;
				case FeatureTableListModel.OBJ_MESSAGE:
					info = e.message;
					break;
				case FeatureTableListModel.OBJ_GROUP:
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

	/** Create and execute a command from the right click pop up menu.  Up
	 * to subclasses to do what they want
	 * @param row
	 * @param column
	 * @param commandnumber
	 */
	public void executeMenuCommand(int row, int column, int commandnumber)
	{
		FeatureTableListData data = myModel.getDataForRow(row);
		if (data != null) {
			if (commandnumber == FeatureTableList.DELETECOMMAND) {
				
				FeatureDeleteCommand del = new FeatureDeleteCommand(data.keyName);
				CommandManager.getInstance().executeCommand(del, true);
				
				// Navigator does a ProductDeleteCommand.  What's different does it matter?
				// It should be consistent action even if done in different windows I think.
				//ProductDeleteCommand del = new ProductDeleteCommand();
                //del.ProductDeleteByKey(i.getData().keyName);
                //CommandManager.getInstance().executeCommand(del, true);
			}
    	}
	}
	
	public static class FeatureTableActionListener implements ActionListener
	{
		/** The list we refer to */
		protected FeatureTableList myFeatureTableList;
		
		/** Row number of action event */
		protected int myRowNumber;
		
		/** Col number of action event */
		protected int myColNumber;
		
		/** Command number of action event, what to do */
		protected int myCommandNumber;
		
		/** Store information so we can send back to table to handle */
		public FeatureTableActionListener(FeatureTableList list, int row, int col, int commandNumber) {
			super();
			myRowNumber = row;
			myColNumber = col;
			myCommandNumber = commandNumber;
			myFeatureTableList = list;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			myFeatureTableList.executeMenuCommand(myRowNumber, myColNumber, myCommandNumber);
		}
	
	}
	
	/** Set header of given column number to an icon */
	public void setIconColumn(int columnNum, String iconname) {

		// Set the 'show' and 'visible' columns to icons...
		TableColumnModel cm = getColumnModel();
		JCheckBox aBox = new JCheckBox();
		Dimension d = aBox.getMinimumSize();
		if (myIconHeaderRenderer == null) { // Cache it.
			myIconHeaderRenderer = new IconHeaderRenderer();
		}
		IconHeaderRenderer r = myIconHeaderRenderer;

		int count = getColumnCount();
		for (int i = 0; i < count; i++) {
			final TableColumn col = cm.getColumn(i);
			// Make all headers draw the same to be consistent.
			col.setHeaderRenderer(r);
			if (i == columnNum) {
				IconHeaderInfo info = new IconHeaderInfo(iconname);
				col.setHeaderValue(info);
				// FIXME: this isn't right, how to do it with look + feel
				col.setWidth(2 * d.width);
				col.setMaxWidth(2 * d.width);
				col.setResizable(false);
				break;
			}
		}
	}

	/** Callback for table value changed (change in selection) */
	public void featureTableValueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) {
            return;
		}
		
		// We're in the updateTable and have set the selection to the old
        // value, we don't want to loop infinitely
        if (myModel.rebuilding()) {
            return;
        }
        
        int row = getSelectedRow();
        if (row > -1) {
        	int dataRow = convertRowIndexToModel(row);
        	FeatureTableListData d = myModel.getDataForRow(dataRow);
        	if (d != null) {
                FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
		/*  Navigator
		 *         if (evt.getValueIsAdjusting()) {
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
            ProductsTableData d = myProductsListTableModel.getDataForRow(dataRow);
            if (d != null) {
                FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
		 * 
		 * Feature view....
		 *   if (evt.getValueIsAdjusting()) {
            return;
        }
        // We're in the updateTable and have set the selection to the old
        // value, we don't want to loop infinitely
        if (myProductListTableModel.rebuilding()) {
            return;
        }
        int row = myProductListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = myProductListTable.convertRowIndexToModel(row);
            FeatureListTableData d = (FeatureListTableData) (myProductListTableModel.getDataForRow(dataRow));
            if (d != null) {
                FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                CommandManager.getInstance().executeCommand(c, true);
            }
        }
		 * 
		 * 
		 */
	}

	/** Callback for the right click pop up menu */
	public JPopupMenu featureTablePopUp(Object line, int row, int column)
	{
		JPopupMenu popupmenu = new JPopupMenu();
		
		// Instead of copying..use the model directly
		// Check is rebuilding?
		FeatureTableListData data = myModel.getDataForRow(row);
		if (data == null) {
			
			// No data.  Could just not have a popup....
			JMenuItem i = new JMenuItem("No data available");
	        popupmenu.add(i);
		}else {

			// ------------------------------------------
			// Basic delete feature ability.  Probably will
			// always have this...
			String name;
			boolean canDelete = data.candelete;
			if (canDelete) {
				name = "Delete '" + data.visibleName+"'";
			}else {
				name = "This feature cannot be deleted";
			}
			FeatureTableActionListener al = 
				new FeatureTableActionListener(this, row, column, FeatureTableList.DELETECOMMAND) {
			};
			JMenuItem i = new JMenuItem(name);
			i.addActionListener(al);
			popupmenu.add(i);
			
		}
        return popupmenu;
	}
	
    public void featureTableHandleClick(Object stuff, int orgRow, int orgColumn)
    {
		FeatureTableListData data = myModel.getDataForRow(orgRow);
		if (data != null) {
    	switch(orgColumn) {
    	case 0:  {// How to map the columns generically? (VISIBLE COLUMN)
             Feature f = FeatureList.theFeatures.getFeature(data.keyName);
             if (f != null) {
                 FeatureMemento m = f.getNewMemento();
                 m.setProperty(FeatureMemento.VISIBLE, !data.checked);
                 FeatureChangeCommand c = new FeatureChangeCommand(data.keyName, m);
                 CommandManager.getInstance().executeCommand(c, true);
             }
    	} 
    		break;
    	case 1: {// How to map the columns generically? (ONLY COLUMN)
            Feature f = FeatureList.theFeatures.getFeature(data.keyName);
            if (f != null) {
                FeatureMemento m = f.getNewMemento();
                m.setProperty(FeatureMemento.ONLY, !data.onlyMode);
                FeatureChangeCommand c = new FeatureChangeCommand(data.keyName, m);
                CommandManager.getInstance().executeCommand(c, true);
            }
    	}
            break;
    		default:
    			// The selection change will handle clicking another column...
    			break;
    	}
    	}	
    }
    
	public FeatureTableList(String groupName)
	{
		myGroupName = groupName;
		
		// Set the model which describes our columns
		myModel = new FeatureTableListModel();
		setModel(myModel);

		// Set the renderer/appearance for table
		myRenderer = new FeatureTableListCellRenderer();
		setDefaultRenderer(FeatureTableListData.class, myRenderer);
		setIconColumn(FeatureTableListModel.OBJ_VISIBLE, "layervisible.png");
		setIconColumn(FeatureTableListModel.OBJ_ONLY, "picture.png");

		setFillsViewportHeight(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Hook up line change listener to our class.
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                featureTableValueChanged(e);
            }
        });
        
        // Hook up mouse listener to our class
        addMouseListener(new RowEntryTableMouseAdapter(this, myModel)
        {
        	
        	@Override
            public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {
        		return featureTablePopUp(line, row, column);	
        	}
        	
        	@Override
            public void handleClick(Object stuff, int orgRow, int orgColumn)
        	{
        		featureTableHandleClick(stuff, orgRow, orgColumn);
        	}
        	
        });
            
	}

	/** Update list to feature command, note..we might not the feature in us
	 * that is being sent 
	 * @param info
	 */
	public void updateList(Object info) {

		// We only want to change selection when the user directly
		// changes one, not from other updates like from looping
		boolean changeSelection = false;
		Feature fromSelect = null;
		if (info instanceof FeatureSelectCommand) {
			FeatureSelectCommand c = (FeatureSelectCommand) (info);
			changeSelection = true;
			fromSelect = c.getFeature();
			//LOG.debug("******SELECTCOMMAND " + fromSelect);
		}

		// -----------------------------------------------------------
		// FEATURE SORT
		// Note nav view calls productmanager to do this..

		// Get the full list of features and sort it.
		// Sort this list....might be better to keep a sorted list within
		// the FeatureList...we'll see how much this gets 'hit'
		// This is done a lot...every feature table list will do it over and over...
		// FIXME: make FeatureList maintain a sorted list?  No reason it couldn't be.
		final List<Feature> porg = FeatureList.theFeatures.getFeatureGroup(myGroupName);
		List<Feature> p = new ArrayList<Feature>(porg);
		Feature top = FeatureList.theFeatures.getSelected(myGroupName);
		
        Collections.sort(p,
                new Comparator<Feature>() {
            @Override
            public int compare(Feature o1, Feature o2) {
                String k1 = o1.getFeatureGroup();
                String k2 = o2.getFeatureGroup();
                int c = k1.compareTo(k2);
                if (c == 0) { // same group, sort by key name...
                    c = o1.getKey().compareTo(o2.getKey());
                }
                return c;
            }
        });
		 
		if (p != null) {
			int currentLine = 0;
			int select = -1;
			ArrayList<FeatureTableListData> newList = new ArrayList<FeatureTableListData>();
			Iterator<Feature> iter = p.iterator();  // Question is do we need it to be subclasses, such as MapFeature

			for (Feature d : p) {
				FeatureTableListData d2 = new FeatureTableListData();
				d2.visibleName = d.getName();
				d2.group = d.getFeatureGroup(); // The feature group such as 'maps'
				d2.keyName = d.getKey(); // The key used to select this handler
				d2.checked = d.getVisible();
				d2.onlyMode = d.getOnlyMode();
				// Used by the navigator right...
				d2.type = "type";
				d2.timeStamp = "time";
				d2.subType = "sub";
				d2.message = d.getMessage();
				d2.candelete = d.getDeletable();
				newList.add(d2);

				if (d == top) {
					select = currentLine;
				}
				//if (myLastSelectedFeature == d) {
				//     oldSelect = currentLine;
				// }
				currentLine++;

			}
			myModel.setDataTypes(newList);
			myModel.fireTableDataChanged();

			if (select > -1) {
				select = convertRowIndexToView(select);

				// This of course fires an event, which calls jProductsListTableValueChanged
				// which would send a command which would do this again in an
				// infinite loop.  So we have a flag.  We don't use isAdjusting
				// because it still fires and event when you set it false
				myModel.setRebuilding(true);
				setRowSelectionInterval(select, select);

				// Here do the old control to new control swap...

				myModel.setRebuilding(false);

			}else {
				// FeaturesView does this but nav doesn't because it destroy controls ..
				// setEmptyControls();
				// jFeatureGUIPanel.validate();
				// jFeatureGUIPanel.repaint();
				// myLastSelectedFeature = null;
			}

			repaint();
			// Used by features view ... Hack updates...need a better system for this
			// if (myLastSelectedFeature != null){
			//     myLastSelectedFeature.getControls().updateGUI();
			// }

		}
	}

}

