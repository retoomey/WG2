package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import javax.swing.Icon;
import org.wdssii.gui.views.WdssiiMDockedViewFactory.MDockView;

/**
 * This class will wrap generic window managing of all WG2 views This will hide
 * the details of how views are managed, allowing plugin of GUI views into
 * different frameworks
 *
 * @author Robert Toomey
 */
public class ViewManager {

    /**
     * Holder the class doing the work here
     */
    public static ViewMaker myWorker = null;

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
         * Add a WdssiiView to a given root container
         */
        // public void addWdssiiViewToRootContainer(RootContainer c, WdssiiView wv, MDockView m);
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
    
    
}
