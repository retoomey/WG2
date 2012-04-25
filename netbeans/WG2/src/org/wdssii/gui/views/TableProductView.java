package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Cursor;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.commands.ProductChangeCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.features.ProductFeature;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product2DTable;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.SwingIconFactory;

public class TableProductView extends JThreadPanel implements CommandListener {

	private static Logger log = LoggerFactory.getLogger(TableProductView.class);
	// ----------------------------------------------------------------
	// Reflection called updates from CommandManager.
	// See CommandManager execute and gui updating for how this works
	// When sources or products change, update the navigation controls

	public void ProductCommandUpdate(ProductCommand command) {
		if (command instanceof ProductChangeCommand) {
			int a = 1;
		}
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
	// Global mouse mode for all tables....
	private int myMouseMode = 0;

	/** Our factory, called by reflection to populate menus, etc...*/
	public static class Factory extends WdssiiDockedViewFactory {

		public Factory() {
			super("DataTable", "color_swatch.png");
		}

		@Override
		public Component getNewComponent() {
			return new TableProductView();
		}
	}

	public TableProductView() {
		initComponents();
		CommandManager.getInstance().addListener(TableProductView.ID, this);

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
	private javax.swing.JScrollPane jDataTableScrollPane;

	@Override
	public void updateInSwingThread(Object command) {
		updateDataTable();
	}

	public GridVisibleArea getVisibleGrid() {
		GridVisibleArea a = null;
		if (myTable != null) {
			return myTable.getCurrentVisibleGrid();
		}
		return null;
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

		return bar;
	}

	private void jMouseModeActionPerformed(java.awt.event.ActionEvent evt) {
		JMToggleButton abstractButton = (JMToggleButton) evt.getSource();
		boolean selected = abstractButton.getModel().isSelected();
		if (selected) {
			myMouseMode = abstractButton.getMode();
			if (myTable != null){
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

	private void updateDataTable() {
		Product2DTable t = null;

		ProductFeature f = ProductManager.getInstance().getTopProductFeature();
		if (f != null) {
			t = f.get2DTable();
		}

		// Always check and replace table.  There may be no ProductFeature,
		// there may be no table...
		if (myTable != t) {

			// Remove any old stuff completely
			remove(jDataTableScrollPane);
			jDataTableScrollPane = new javax.swing.JScrollPane();
			add(jDataTableScrollPane, new CC().growX().growY());

			// Add new stuff if there
			if (t != null) {
				t.createInScrollPane(jDataTableScrollPane, f, myMouseMode);
				log.debug("Installed 2D table "+t);
			}
			myTable = t;
			this.doLayout();
			this.revalidate();
		}
	}
}
