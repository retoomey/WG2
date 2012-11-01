package org.wdssii.gui.views;

import java.awt.Component;
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
import org.wdssii.gui.commands.*;
import org.wdssii.gui.commands.ChartCreateCommand.ChartFollowerView;
import org.wdssii.gui.commands.ProductFollowCommand.ProductFollowerView;
import org.wdssii.gui.commands.ProductToggleFilterCommand.ProductFilterFollowerView;
import org.wdssii.gui.commands.VolumeSetTypeCommand.VolumeTypeFollowerView;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;
import org.wdssii.xml.wdssiiConfig.Tag_charts.Tag_chart;
import org.wdssii.xml.wdssiiConfig.Tag_setup;

/**
 * The Chart view interface lets us wrap around an RCP view or netbean view
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

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiMDockedViewFactory implements CommandListener, ChartFollowerView {

        public static ChartView container;
        private String myCurrentChartChoice = ChartSetTypeCommand.getFirstChartChoice();

        public Factory() {
            super("Chart", "chart_bar.png");
        }

        @Override
        public Component getNewComponent() {
            return new ChartView("Chart", myCurrentChartChoice); // Ignored
        }

        @Override
        public Component getNewSubViewComponent(int counter) {
            return new ChartView("Chart-" + counter, myCurrentChartChoice);
        }

        @Override
        public MDockView getTempComponent() {
            return new ChartView();
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
     * The box for the chart. This is reused when chart changes
     */
    private JComponent myChartBox = null;
    /**
     * The box for GUI controls. This is reused when chart changes
     */
    private JComponent myChartGUIBox = null;
    /**
     * The current chart itself, this changes as chart type is selected
     */
    private JComponent myChartPanel = null;
    /**
     * The current chart GUI controls, they change as chart type is selected
     */
    private JComponent myCurrentChartControls = null;
    /**
     * The current choice in the drop down follow product menu
     */
    private String myCurrentProductFollow = ProductManager.TOP_PRODUCT;
    JComponent myParent = null;
    /**
     * The chart we are currently displaying
     */
    ChartViewChart myChart = null;
    /**
     * Do volume charts use virtual or the current volume?
     */
    private boolean myUseVirtualVolume;
    /**
     * Do charts use the current filter settings?
     */
    private boolean myUseProductFilters;
    public final String[] myInterps = new String[]{"None", "Experiment: Binomial I"};
    private String myTitle;

    /**
     * An empty chart used for generating info for the 'top' container in
     * multiview. It's a temporary object
     */
    public ChartView() {
        myTitle = "Top chart object, not a real chart.";
    }

    public ChartView(String title, String chartName) {
        myTitle = title;
        initComponents();
        initCharts(chartName);
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

                    ChartViewChart chart = null;

                    // Try to create chart using XML file....
                    for (Tag_chart c : list) {
                        if ((c.gName != null) && (c.gName.compareTo(factoryChoice) == 0)) {
                            Class<?> aClass = null;
                            try {
                                aClass = Class.forName("org.wdssii.gui.charts." + c.name + "Chart");
                                Method createMethod = aClass.getMethod("create" + c.name + "Chart", new Class[]{});
                                chart = (ChartViewChart) createMethod.invoke(null, new Object[]{});
                                log.debug("Generated chart by factory lookup " + c.gName + " to " + c.name);
                                //setChart(chart);
                                myCurrentChoice = c.gName;
                            } catch (Exception e) {
                                log.error("Couldn't create WdssiiChart by name '"
                                        + c.name + "' because " + e.toString());
                                myCurrentChoice = "";
                                //  setChart(null);
                            }
                        }
                    }

                    // Try to create chart from pure class name...
                    // Not sure I really need an xml file for charts..could create a listing simply
                    // by jar hunting.  Only matters if we have plugins someday.
                    if (chart == null) {
                        try {
                            Class<?> aClass = null;
                            aClass = Class.forName("org.wdssii.gui.charts." + factoryChoice + "Chart");
                            Method createMethod = aClass.getMethod("create" + factoryChoice + "Chart", new Class[]{});
                            chart = (ChartViewChart) createMethod.invoke(null, new Object[]{});
                            log.debug("Generated chart by reflection " + factoryChoice);
                            //setChart(chart);
                            myCurrentChoice = factoryChoice;
                        } catch (Exception e) {
                            log.error("Couldn't create WdssiiChart by name '"
                                    + factoryChoice + "' because " + e.toString());
                            setChart(null);
                        }

                    }
                    setChart(chart);
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
        if (chart != null) {
            if (chart instanceof VSliceChart) {
                VSliceChart v = (VSliceChart) (chart);
            }
            chart.setUseProductFilters(myUseProductFilters);
            chart.setUseVirtualVolume(myUseVirtualVolume);
            chart.setUseProductKey(myCurrentProductFollow);
        }
        myChart = chart;
    }

    @Override
    public void updateInSwingThread(Object info) {
        // if (myParent != null) {
        //if (!myParent.isDisposed()){
        if (myChart != null) {
            boolean force = false;
            // If a feature setting changed, force update of chart
            //if (info != null && info instanceof FeatureCommand){
            //    force = true;
            //}
            myChart.updateChart(force);
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
