package org.wdssii.gui.views;

import gov.nasa.worldwind.geom.LatLon;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.commands.ProductFollowCommand.ProductFollowerView;
import org.wdssii.gui.features.ProductFeature;
import org.wdssii.gui.products.Product2DTable;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.SwingIconFactory;
import javax.swing.filechooser.FileFilter;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeatureFilter;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaHeightStick;

public class TableProductView extends JThreadPanel implements MDockView, CommandListener, ProductFollowerView {

	private static Logger log = LoggerFactory.getLogger(TableProductView.class);
	// ----------------------------------------------------------------
	// Reflection called updates from CommandManager.
	// See CommandManager execute and gui updating for how this works
	// When sources or products change, update the navigation controls
	private String myCurrentFollow = ProductManager.TOP_PRODUCT;

	public void ProductCommandUpdate(ProductCommand command) {
		updateGUI(command);
	}

	public void FeatureCommandUpdate(FeatureCommand command) {
		updateGUI(command);
	}

	public void SourceCommandUpdate(SourceCommand command) {
		updateGUI(command);
	}

	public void AnimateCommandUpdate(AnimateCommand command) {
		updateGUI(command);
	}
	public static final String ID = "wj.TableProductView";
	private Product2DTable myTable;
	private JLabel selectionLabel;
	// Global mouse mode for all tables....
	private int myMouseMode = 0;

	// ProductFollower methods...
	@Override
	public void setCurrentProductFollow(String changeTo) {
		myCurrentFollow = changeTo;
	}

	@Override
	public String getCurrentProductFollow() {
		return myCurrentFollow;
	}

	public void scrollLocationToVisible(Location loc) {
		myTable.centerToLocation(loc);
	}

	/** Our factory, called by reflection to populate menus, etc...*/
	public static class Factory extends WdssiiMDockedViewFactory {

		public Factory() {
			super("DataTable", "color_swatch.png");
		}

		@Override
		public Component getNewComponent() {
			return new TableProductView("DataTable");
		}

		@Override
		public Component getNewSubViewComponent(int counter) {
			return new TableProductView("DataTable-" + counter);
		}

		@Override
		public MDockView getTempComponent() {
			return new TableProductView();
		}
	}

	private String myTitle;

	public TableProductView() {
		myTitle="Top datatable object, not a real datatable";
	}

	public TableProductView(String title) {
		myTitle = title;
		initComponents();
		CommandManager.getInstance().addListener(myTitle, this);
	}

	private void initComponents() {

		setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		jDataTableScrollPane = new javax.swing.JScrollPane();
		add(jDataTableScrollPane, new CC().growX().growY());

		JToolBar bar = initToolBar();
	        add(bar, new CC().dockNorth());
		initTable();
		updateDataTable();
	}

	/** Get the items for the view group */
	@Override
	public void addGlobalCustomTitleBarComponents(List addTo) {
	}

	/** Get the items for an individual view */
	@Override
	public void addCustomTitleBarComponents(List addTo) {
	}

	private javax.swing.JScrollPane jDataTableScrollPane;

	@Override
	public void updateInSwingThread(Object command) {
		updateDataTable();
	}

	private class JMToggleButton extends JToggleButton {

		private int myMode;

		public JMToggleButton(int mode) {
			super();
			myMode = mode;
		}

		public int getMode() {
			return myMode;
		}
	}

	private JMToggleButton initMouseButton(int mode, ButtonGroup g, String icon, String tip) {
		JMToggleButton b = new JMToggleButton(mode);
		Icon i = SwingIconFactory.getIconByName(icon);
		b.setIcon(i);
		b.setToolTipText(tip);
		g.add(b);
		b.setFocusable(false);
		b.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		b.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		b.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMouseModeActionPerformed(evt);
			}
		});
		return b;
	}

	private JToolBar initToolBar() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);

		ButtonGroup group = new ButtonGroup();
		JMToggleButton first = initMouseButton(0, group, "stock-tool-move-16.png", "Move table with mouse");
		bar.add(first);
		bar.add(initMouseButton(1, group, "stock-tool-rect-select-16.png", "Select table with mouse"));
		myMouseMode = 0;
		group.setSelected(first.getModel(), true);
		updateMouseCursor();

		JButton export = new JButton("Export...");
		export.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				jExportActionPerformed(e);
			}
		});
		bar.add(export);
		selectionLabel = new JLabel("");
		bar.add(selectionLabel);
		return bar;
	}

	/** Handle export. For the moment will only handle Bim's file format */
	private void jExportActionPerformed(java.awt.event.ActionEvent evt) {
		JFileChooser fileopen = new JFileChooser();
		fileopen.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				String t = f.getName().toLowerCase();
				// FIXME: need to get these from the Builders
				return (f.isDirectory() || t.endsWith(".inp"));
			}

			@Override
			public String getDescription() {
				return "INP Bim Wood format";
			}
		});
		fileopen.setDialogTitle("Export Table Selection");
		int ret = fileopen.showSaveDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = fileopen.getSelectedFile();
			try {
				// Bim's format....
				URL aURL = file.toURI().toURL();
				log.debug("Would try to write to " + aURL.toString());
				if (myTable != null) {
					myTable.exportToURL(aURL);
				}
			} catch (MalformedURLException ex) {
			}
		}
	}

	private void jMouseModeActionPerformed(java.awt.event.ActionEvent evt) {
		JMToggleButton abstractButton = (JMToggleButton) evt.getSource();
		boolean selected = abstractButton.getModel().isSelected();
		if (selected) {
			myMouseMode = abstractButton.getMode();
			if (myTable != null) {
				myTable.setMode(myMouseMode);
			}
			updateMouseCursor();
		}

	}

	private void updateMouseCursor() {
		switch (myMouseMode) {
			case 0: // Mode mode
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			default:
				break;
			case 1:  // Hand mode
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				break;
		}

	}

	private void initTable() {
		jDataTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jDataTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	/** Return an LLHAreaFeature that contains a LLHAreaHeightStick */
	private static class StickFilter implements FeatureFilter {

		@Override
		public boolean matches(Feature f) {
			if (f instanceof LLHAreaFeature) {
				LLHAreaFeature a = (LLHAreaFeature) f;
				LLHArea area = a.getLLHArea();
				if (area instanceof LLHAreaHeightStick) {
					return true;
				}
			}
			return false;
		}
	}

	/** Get the feature we are tracking */
	public LLHAreaFeature getTrackFeature() {
		// -------------------------------------------------------------------------
		// Snag the top stick for the moment
		LLHAreaFeature f = FeatureList.theFeatures.getTopMatch(new StickFilter());
		if (f != null) {
			LLHArea area = f.getLLHArea();
			if (area instanceof LLHAreaHeightStick) {
				return f;
			}
		}
		return null;
	}

	/** Get the stick we are tracking */
	public LLHAreaHeightStick getTrackStick() {

		// -------------------------------------------------------------------------
		// Snag the top stick for the moment
		LLHAreaHeightStick stick = null;
		LLHAreaFeature f = getTrackFeature();
		if (f != null) {
			stick = (LLHAreaHeightStick) f.getLLHArea();
		}
		return stick;
	}

	/**
	 * Return the Location we are currently tracking
	 */
	public Location getTrackLocation() {
		Location L = null;
		LLHAreaHeightStick stick = getTrackStick();
		if (stick != null) {
			// FIXME: would be nice to have a Position ability
			List<LatLon> list = stick.getLocations();
			double[] alts = stick.getAltitudes();
			if (list.size() > 0) {
				LatLon l = list.get(0);
				double a = alts[0]; // assuming correct
				L = new Location(l.latitude.degrees, l.longitude.degrees, a);
			}
		}
		return L;
	}

	private void updateDataTable() {
		Product2DTable newTable = null;
		Product2DTable oldTable = myTable;

		ProductFeature f = ProductManager.getInstance().getProductFeature(myCurrentFollow);
		if (f != null) {
			newTable = f.get2DTable();
		}

		// Always check and replace table.  There may be no ProductFeature,
		// there may be no table...
		if (myTable != newTable) {

			// Remove any old stuff completely
			remove(jDataTableScrollPane);
			jDataTableScrollPane = new javax.swing.JScrollPane();
			add(jDataTableScrollPane, new CC().growX().growY());

			// Add new stuff if there
			if (newTable != null) {
				newTable.createInScrollPane(jDataTableScrollPane, f, myMouseMode);
				log.debug("Installed 2D table " + newTable);
			}
			myTable = newTable;

			// Link 3DRenderer (usually outline of product) to current stick...
			// Revalidate is delayed, we need it NOW because the GridVisibleArea
			// calculation needs a valid ViewRect
			//this.revalidate();
			this.validate(); // update now
		}

		// Always register..bleh..this is because you can add a stick without changing table..
		// bleh...guess it's cheap enough to do for now
		FeatureList.theFeatures.remove3DRenderer(oldTable);
		LLHAreaFeature s = getTrackFeature();
		if (s != null) {
			log.debug("TABLE VIEW ADD for "+this+"-->"+newTable);
			s.addRenderer(newTable);
		}

		// Feature update, move table to stick experiment...
		if (myTable != null) {
			Location l = getTrackLocation();
			if (l != null) {
				myTable.centerToLocation(l);
			}
			// Product update, redraw current table...
			myTable.updateTable();
		}
	}
}
