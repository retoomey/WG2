package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Container;
import java.net.URL;
import java.util.List;

import javax.swing.Icon;

import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;
import org.wdssii.xml.views.RootWindow;

/**
 * This class will wrap generic window managing of all WG2 views This will hide
 * the details of how views are managed, allowing plugin of GUI views into
 * different frameworks
 *
 * @author Robert Toomey
 */
public class ViewManager {

    public final static String DATA_VIEW_NAME = "DataFeatureView";
    
    /**
     * Holder the class doing the work here
     */
    public static ViewMaker myWorker = null;
    private static DataFeatureView myDataFeatureView;

    public static void setConfigPath(URL aURL) {
        if (myWorker != null) {
            myWorker.setConfigPath(aURL);
        }
    }

 public static interface RootContainer {

        public void addWdssiiView(WdssiiView w, MDockView md);

        public void addControls(List<Object> list);

        public void createSingleSubWindow(String controlTitle, Icon i, Component f, Component c);
    }

    /**
     * interface to doing real window work
     */
    public static interface ViewMaker {

        public void init();

        /**
         * Create root component for holding stuff
         */
        public RootContainer createRootContainer();

        /**
         * Wrap a WdssiiView with an actual GUI container
         */
        public void wrapWdssiiViewWithGUI(WdssiiView wv);

        /**
         *
         * @return layout xml JAXB node
         */
        public RootWindow getLayoutXML();

        /**
         * Set layout to that from XML, if possible
         */
        public void setLayoutXML(RootWindow r);

        /**
         * Set a new layout state
         */
        public void setNewLayout();

        /**
         * Set the URL path of this document
         */
        public void setConfigPath(URL aURL);

        public void collapseLayout(RootContainer rootW);

        /**
         * Add a WdssiiView to a given root container
         */
        // public void addWdssiiViewToRootContainer(RootContainer c, WdssiiView wv, MDockView m);
        public DataFeatureView getDataFeatureView();
    }

    public void setViewMaker(ViewMaker worker) {
        myWorker = worker;
    }

    public static void init(ViewMaker w) {
        myWorker = w;
        startWindows();
    }

    public static void startWindows() {
        if (myWorker != null) {
            myWorker.init();
        }
    }

    public static RootContainer createRootContainer() {
        RootContainer holder = null;
        if (myWorker != null) {
            holder = myWorker.createRootContainer();
        }
        return holder;
    }

    /**
     * Wrap a given WdssiiView with a GUI container
     */
    public static void wrapWdssiiViewWithGUI(WdssiiView wv) {
        Container holder = null;
        if (myWorker != null) {
            myWorker.wrapWdssiiViewWithGUI(wv);
        }
    }

    public static RootWindow getLayoutXML() {
        RootWindow xml = null;
        if (myWorker != null) {
            xml = myWorker.getLayoutXML();
        }
        return xml;
    }

    public static void setLayoutXML(RootWindow r) {
        if (myWorker != null) {
            myWorker.setLayoutXML(r);
        }
    }

    public static void setNewLayout() {
        if (myWorker != null) {
            myWorker.setNewLayout();
        }
    }

    public static void collapseLayout(RootContainer rootW) {
        if (myWorker != null) {
            myWorker.collapseLayout(rootW);
        }
    }
    
    public static  DataFeatureView getDataView() {
        if (myDataFeatureView == null){
        if (myWorker != null){
            myDataFeatureView = myWorker.getDataFeatureView();
        }
        }
        return myDataFeatureView;
    }

   
}
