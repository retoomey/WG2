package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;
import java.awt.Component;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.charts.DataView;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.commands.ChartCreateCommand.ChartFollowerView;
import org.wdssii.gui.commands.ProductFollowCommand.ProductFollowerView;
import org.wdssii.gui.commands.ProductToggleFilterCommand.ProductFilterFollowerView;
import org.wdssii.gui.commands.VolumeSetTypeCommand.VolumeTypeFollowerView;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;

/**
 * The Chart view interface lets us wrap around an RCP view or netbean view
 * without being coupled to those libraries
 *
 * @author Robert Toomey
 *
 */
public class DataFeatureView extends JThreadPanel implements MDockView, CommandListener, ProductFilterFollowerView, ProductFollowerView, VolumeTypeFollowerView {

    private final static Logger LOG = LoggerFactory.getLogger(DataFeatureView.class);
    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // Default for any product commands....
    // FIXME: probably should update on ANY data command...

    public void ProductCommandUpdate(ProductCommand command) {
        updateGUI(command); // load, delete, etc..
    }

    public void FeatureCommandUpdate(FeatureCommand command) {
        updateGUI(command);
    }
    // Update when we toggle virtual/regular volume button

    public void VolumeSetTypeCommandUpdate(VolumeSetTypeCommand command) {
        updateGUI(command);
    }

    public void SourceCommandUpdate(SourceCommand command) {
        updateGUI(command);
    }

    public void AnimateCommandUpdate(AnimateCommand command) {
        updateGUI(command);
    }

    public void DataCommandUpdate(DataCommand command) {
        updateGUI(command); // load, delete, etc..	
    }
    // Keep a global list of sub views...
    // FIXME: could be generalized to any mdockview.
    // FIXME: Only called from GUI?  If so, no sync needed
    // We are assuming one and only one chart view
    private static ArrayList<DataFeatureView> theSubViews = new ArrayList<DataFeatureView>();

    public static ArrayList<DataFeatureView> getList() {
        ArrayList<DataFeatureView> list = new ArrayList<DataFeatureView>();
        for (DataFeatureView c : theSubViews) {
            list.add(c);
        }
        return list;
    }

    public static void addSubView(DataFeatureView v) {
        theSubViews.add(v);
        LOG.debug("ADD subview list is :");
        for (DataFeatureView c : theSubViews) {
            LOG.debug("Chart is " + c.getTitle());
        }
    }

    public static void removeSubView(DataFeatureView v) {
        theSubViews.remove(v);
    }

    @Override
    public void windowAdded() {
        addSubView(this);
    }

    @Override
    public void windowClosing() {
        //throw new UnsupportedOperationException("Not supported yet.");
        removeSubView(this);
    }

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiMDockedViewFactory implements CommandListener, ChartFollowerView {

        public static DataFeatureView container;
        private String myCurrentChartChoice = ChartSetTypeCommand.getFirstChartChoice();

        public Factory() {
            super("DataView", "chart_bar.png");
        }

        @Override
        public Component getNewComponent() {
            return new DataFeatureView("Chart", myCurrentChartChoice); // Ignored
        }

        @Override
        public Component getNewSubViewComponent(int counter) {
            return new DataFeatureView("Chart-" + counter, myCurrentChartChoice);
        }

        @Override
        public MDockView getTempComponent() {
            return new DataFeatureView();
        }

        /**
         * Add the standard button on main container that allows creating a new
         * sub-view
         */
        @Override
        public void addCreationButton(List<Object> addTo) {
            addTo.add(ChartCreateCommand.getDropButton(this));
        }

        @Override
        public void addChart(String name) {
            myCurrentChartChoice = name;
            addNewSubView();
        }
    }
    private JToggleButton jVirtualToggleButton;
    private String myCurrentChoice = null;
    /**
     * The box for the chart.
     */
    private JComponent myChartBox = null;
    /**
     * The current chart itself, this changes as chart type is selected
     */
    private Component myChartPanel = null;
    /**
     * The current choice in the drop down follow product menu
     */
    private String myCurrentProductFollow = ProductManager.TOP_PRODUCT;
    JComponent myParent = null;
    /**
     * The chart we are currently displaying
     */
    DataView myChart = null;
    /**
     * Do volume charts use virtual or the current volume?
     */
    private boolean myUseVirtualVolume;
    /**
     * Do charts use the current filter settings?
     */
    private boolean myUseProductFilters;
    public final String[] myInterps = new String[]{"None", "Experiment: Binomial I"};
    /**
     * The visible title used for GUI stuff
     */
    private String myTitle;
    /**
     * The neverchanging key for this chart
     */
    private final String myKey;

    /**
     * An empty chart used for generating info for the 'top' container in
     * multiview. It's a temporary object
     */
    public DataFeatureView() {
        myTitle = "Top chart object, not a real chart.";
        myKey = "";
    }

    public DataFeatureView(String title, String chartName) {
        myTitle = title;
        myKey = title;
        initComponents();
        initCharts(chartName);
    }

    public String getTitle() {
        return myTitle;
    }

    public String getKey() {
        return myKey;
    }

    private void initComponents() {

        // Really should let subcomponent choose layout right?
        setLayout(new java.awt.BorderLayout());
    }

    /**
     * Get the items for the view group
     */
    @Override
    public void addGlobalCustomTitleBarComponents(List<Object> addTo) {
    }

    /**
     * Get the items for an individual view
     */
    @Override
    public void addCustomTitleBarComponents(List<Object> addTo) {
        if (myChart != null) {
            myChart.addCustomTitleBarComponents(addTo);
        }

        // ---------------------------------------------------------
        // The product follow menu
        addTo.add(ProductFollowCommand.getDropButton(this));
    }

    public final void initCharts(String chartName) {

        CommandManager.getInstance().addListener(myTitle, this);
        createChart(chartName);
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
        myCurrentChoice = newChoice;
        //createChart(newChoice);
    }

    public void createChart(String factoryChoice) {
        // If a different choice is picked...
        if ((myCurrentChoice == null) || (factoryChoice.compareTo(myCurrentChoice) != 0)) {

            // Try to create chart from pure class name...
            // Not sure I really need an xml file for charts..could create a listing simply
            // by jar hunting.  Only matters if we have plugins someday.
            DataView chart = null;
            if (chart == null) {
                try {
                    Class<?> aClass = null;
                    aClass = Class.forName(factoryChoice);
                    Method createMethod = aClass.getMethod("create", new Class[]{});
                    chart = (DataView) createMethod.invoke(null, new Object[]{});
                    LOG.debug("Generated chart by reflection " + factoryChoice);
                    //setChart(chart);
                    myCurrentChoice = factoryChoice;
                } catch (Exception e) {
                    LOG.error("Couldn't create WdssiiChart by name '"
                            + factoryChoice + "' because " + e.toString());
                    setChart(null);
                }

            }
            setChart(chart);

            if (myChart != null) {
                Component p = (Component) myChart.getNewGUIForChart(this);
                add(p, java.awt.BorderLayout.CENTER);
            }

            updateGUI();
        }

    }

    private void setChart(DataView chart) {
        if (chart != null) {
            chart.setUseProductFilters(myUseProductFilters);
            chart.setUseVirtualVolume(myUseVirtualVolume);
            chart.setUseProductKey(myCurrentProductFollow);
        }
        myChart = chart;
    }

    /**
     * Get the current chart we have
     */
    public DataView getChart() {
        return myChart;
    }

    @Override
    public void updateInSwingThread(Object info) {
        if (myChart != null) {
            boolean force = false;     
            myChart.updateChart(force);
        }
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
