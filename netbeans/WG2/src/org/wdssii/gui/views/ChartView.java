package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SingletonManager;
import org.wdssii.gui.charts.ChartViewChart;
import org.wdssii.gui.charts.VSliceChart;
import org.wdssii.gui.commands.ProductFollowCommand.ProductFollowerView;
import org.wdssii.gui.commands.ProductToggleFilterCommand.ProductFilterFollowerView;
import org.wdssii.gui.commands.VolumeSetTypeCommand.VolumeTypeFollowerView;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.products.volumes.RadialSetVolume;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.JwgDropDownButton;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.xml.wdssiiConfig.Tag_charts.Tag_chart;
import org.wdssii.xml.wdssiiConfig.Tag_setup;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;

/** The Chart view interface lets us wrap around an RCP view or netbean view 
 * without being coupled to those libraries 
 * 
 * @author Robert Toomey
 *
 */
public class ChartView extends JThreadPanel implements MDockView, CommandListener, ProductFilterFollowerView, ProductFollowerView, VolumeTypeFollowerView {

	private static Logger log = LoggerFactory.getLogger(ChartView.class);
        // ----------------------------------------------------------------
	// Reflection called updates from CommandManager.
	// See CommandManager execute and gui updating for how this works
	// Default for any product commands....
	// FIXME: probably should update on ANY data command...

	public void ProductCommandUpdate(ProductCommand command) {
		updateGUI(); // load, delete, etc..
	}

	public void FeatureCommandUpdate(FeatureCommand command) {
		updateGUI();
	}
	// Update when we toggle virtual/regular volume button

	public void VolumeSetTypeCommandUpdate(VolumeSetTypeCommand command) {
		updateGUI();
	}

	/** Our factory, called by reflection to populate menus, etc...*/
	public static class Factory extends WdssiiMDockedViewFactory {

		public Factory() {
			super("Chart", "chart_bar.png");
		}
		
		@Override
		public Component getNewComponent() {
			return new ChartView("Chart");
		}

		@Override
		public Component getNewSubViewComponent(int counter) {
			return new ChartView("Chart-" + counter);
		}

		@Override
		public MDockView getTempComponent() {
			return new ChartView();
		}
	}

	private JToggleButton jVirtualToggleButton;
	private String myCurrentChoice = null;
	/** The box for the chart.  This is reused when chart changes */
	private JComponent myChartBox = null;
	/** The box for GUI controls. This is reused when chart changes */
	private JComponent myChartGUIBox = null;
	/** The current chart itself, this changes as chart type is selected */
	private JComponent myChartPanel = null;
	/** The current chart GUI controls, they change as chart type is selected */
	private JComponent myCurrentChartControls = null;
	/** The current choice in the drop down follow product menu */
	private String myCurrentProductFollow = ProductManager.TOP_PRODUCT;
	JComponent myParent = null;
	/** The chart we are currently displaying */
	ChartViewChart myChart = null;
	/** Do volume charts use virtual or the current volume? */
	private boolean myUseVirtualVolume;
	/** Do charts use the current filter settings? */
	private boolean myUseProductFilters;
	public final String[] myInterps = new String[]{"None", "Experiment: Binomial I"};
	private String myTitle;

	/** An empty chart used for generating info for the 'top' container
	 * in multiview.  It's a temporary object
	 */
	public ChartView(){
		myTitle="Top chart object, not a real chart.";
	}

	public ChartView(String title) {
		myTitle = title;
		initComponents();
		initCharts();
	}

	private void initComponents() {

		myChartBox = new javax.swing.JPanel();
		myChartGUIBox = new javax.swing.JPanel();

		setLayout(new java.awt.BorderLayout());

		//add(jToolBar1, java.awt.BorderLayout.NORTH);

		myChartBox.setLayout(new java.awt.BorderLayout());
		add(myChartBox, java.awt.BorderLayout.CENTER);

		myChartGUIBox.setLayout(new java.awt.BorderLayout());
		add(myChartGUIBox, java.awt.BorderLayout.SOUTH);

	}

	/** Get the items for the view group */
	@Override
	public void addGlobalCustomTitleBarComponents(List addTo) {
	}

	/** Get the items for an individual view */
	@Override
	public void addCustomTitleBarComponents(List addTo) {

		// Virtual Volume toggle
		jVirtualToggleButton = new JToggleButton();
		Icon i = SwingIconFactory.getIconByName("brick_add.png");
		jVirtualToggleButton.setIcon(i);
		jVirtualToggleButton.setToolTipText("Toggle virtual/nonvirtual volume");
		jVirtualToggleButton.setFocusable(false);
		jVirtualToggleButton.setBorderPainted(false);
		jVirtualToggleButton.setOpaque(false);
		jVirtualToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jVirtualToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
		jVirtualToggleButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jVirtualToggleButtonActionPerformed(evt);
			}
		});
		addTo.add(jVirtualToggleButton);

		// Interpolation button
		Icon test = SwingIconFactory.getIconByName("layers.png");
		JPopupMenu menu = new JPopupMenu();
		ActionListener menuAction = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				jPopupMenuActionPerformed(e);
			}
		};

		ButtonGroup group = new ButtonGroup();
		// Should use Radio buttons but they usually look nasty in menus
		JCheckBoxMenuItem item;
		boolean selectedOne = false;
		for (String s : myInterps) {
			item = new JCheckBoxMenuItem(s);
			if (!selectedOne) {
				item.setSelected(true);
				selectedOne = true;
			}
			item.addActionListener(menuAction);
			group.add(item);
			menu.add(item);
		}

		JwgDropDownButton b1 = new JwgDropDownButton(test);
		b1.setToolTipText("Choose the type of interpolation");
		b1.setMenu(menu);
		addTo.add(b1);

		// ---------------------------------------------------------
		// The product follow menu
		addTo.add(ProductFollowCommand.getDropButton(this));

		// ---------------------------------------------------------
		// The 3D object follow menu
	/*
		test = SwingIconFactory.getIconByName("application_cascade.png");
		JwgDropDownButton b3 = new JwgDropDownButton(test) {
		
		@Override
		public void generateMenu() {
		// Because the list dynamically changes
		//ProductFollowCommand f = new ProductFollowCommand();
		//f.setTargetListener(ChartView.this);
		//JPopupMenu menu = WdssiiCommand.getSwingMenuFor(f);
		//setMenu(menu);
		}
		};
		b3.setToolTipText("Choose 3D object to follow");
		addTo.add(b3);
		 * 
		 */

		//b2.setFocusable(false);
		//b2.setMargin(new Insets(0,0,0,0));

		//JButton b = ButtonFactory.createFlatHighlightButton(test, "HELLO", 0, null);
		//b.setFocusable(false);
		//addTo.add(b);

	}

	private void jPopupMenuActionPerformed(java.awt.event.ActionEvent evt) {
		String item = evt.getActionCommand();
		// Hack in my experiment I guess...until it works don't bother
		// doing all the fancy GUI work...will have to manually refresh
		// by moving.
		RadialSetVolume.myExperiment = (myInterps[1].equals(item));
		if (myChart != null) {
			myChart.updateChart();
		}
	}

	private void jVirtualToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
		AbstractButton abstractButton = (AbstractButton) evt.getSource();
		boolean selected = abstractButton.getModel().isSelected();
		VolumeSetTypeCommand vToggle = new VolumeSetTypeCommand(this, selected);

		vToggle.setToggleState(selected);
		CommandManager.getInstance().executeCommand(vToggle, true);
	}

	public final void initCharts() {

		CommandManager.getInstance().addListener(myTitle, this);

		// The command class handles the chart list for us.
		createChart(ChartSetTypeCommand.getFirstChartChoice());

		updateGUI();
	}
	// ProductFollowerView methods -----------------------------------

	@Override
	public void setCurrentProductFollow(String changeTo) {
		myCurrentProductFollow = changeTo;
		if (myChart != null) {
			myChart.setUseProductKey(myCurrentProductFollow);
		}
	}

	@Override
	public String getCurrentProductFollow() {
		return myCurrentProductFollow;
	}

	public void takeSnapshot(String name) {
		if (myChart != null) {
			myChart.takeSnapshot(name);
		}
	}

	public String getCurrentChoice() {
		return myCurrentChoice;
	}

	public void setCurrentChoice(String newChoice) {
		createChart(newChoice);
	}

	public void createChart(String factoryChoice) {
		// If a different choice is picked...
		if ((myCurrentChoice == null) || (factoryChoice.compareTo(myCurrentChoice) != 0)) {

			// Create object by name from XML..if possible
			Tag_setup doc = SingletonManager.getInstance().getSetupXML();
			if (doc != null) {
				ArrayList<Tag_chart> list = doc.charts.charts;
				if (list != null) {

					for (Tag_chart c : list) {
						if ((c.gName != null) && (c.gName.compareTo(factoryChoice) == 0)) {
							Class<?> aClass = null;
							try {
								//System.out.println("NAME TO CREATE IS "+name);
								aClass = Class.forName("org.wdssii.gui.charts." + c.name + "Chart");
								Method createMethod = aClass.getMethod("create" + c.name + "Chart", new Class[]{});
								ChartViewChart chart = (ChartViewChart) createMethod.invoke(null, new Object[]{});
								log.debug("Generated chart by reflection " + c.name);

								setChart(chart);
								//chart.setUseVirtualVolume(myUseVirtualVolume);
								//myChart = chart;

								myCurrentChoice = c.gName;
							} catch (Exception e) {
								log.error("Couldn't create WdssiiChart by name '"
									+ c.name + "' because " + e.toString());
								myChart = null;
							}
						}
					}
					// bet I'm gonna get sync errors here....maybe not, we shouldn't
					// be still reading in the xml by this time.  Could be though.

				}
			}

			// Dispose old chart and GUI
			if (myChartPanel != null) {
				myChartBox.remove(myChartPanel);
				myChartPanel = null;
			}
			if (myCurrentChartControls != null) {
				myChartGUIBox.remove(myChartPanel);
				myCurrentChartControls = null;
			}

			if (myChart != null) {
				myChartPanel = (JComponent) myChart.getNewGUIForChart(myChartBox);
				if (myChartPanel != null) {
					myChartBox.add(myChartPanel);
				}
				myCurrentChartControls = (JComponent) myChart.getNewGUIBox(myChartGUIBox);
				if (myCurrentChartControls != null) {
					myChartGUIBox.add(myCurrentChartControls);
				}
			}

			updateGUI();
		}

	}

	private void setChart(ChartViewChart chart) {
		if (chart instanceof VSliceChart) {
			VSliceChart v = (VSliceChart) (chart);
		}
		chart.setUseProductFilters(myUseProductFilters);
		chart.setUseVirtualVolume(myUseVirtualVolume);
		chart.setUseProductKey(myCurrentProductFollow);
		myChart = chart;
	}

	@Override
	public void updateInSwingThread(Object info) {
		// if (myParent != null) {
		//if (!myParent.isDisposed()){
		if (myChart != null) {
			myChart.updateChart();
		}
		//}
		// }
		if (myUseVirtualVolume) {
			setContentDescription("Virtual volume");
		} else {
			setContentDescription("Regular volume");
		}
	}

	public void setContentDescription(String test) {
	}

	@Override
	public void setUseFilter(boolean useFilter) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean getUseFilter() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	// VolumeTypeFollowerView methods -------------------------------
	@Override
	public void setUseVirtualVolume(boolean useVirtual) {
		myUseVirtualVolume = useVirtual;
		if (myChart != null) {
			myChart.setUseVirtualVolume(myUseVirtualVolume);
		}
	}

	@Override
	public boolean getUseVirtualVolume() {
		boolean use = false;
		if (myChart != null) {
			use = myChart.getUseVirtualVolume();
		}
		return use;
	}
}
