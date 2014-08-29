package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import org.wdssii.gui.views.ViewManager.RootContainer;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * This created a docking view window with a 'main' set of contents and
 * controls, with a single child sub-docked window that has controls that are
 * linked to main.
 *
 * @author Robert Toomey
 */
public abstract class WdssiiSDockedViewFactory extends WdssiiDockedViewFactory {

    private final static Logger LOG = LoggerFactory.getLogger(WdssiiSDockedViewFactory.class);

    /**
     * Interface for creating parts of a sub dock view
     */
    public static interface SDockView extends DockView {

        /**
         * Get the component for the single control sub-view
         */
        public Component getControlComponent();

        /**
         * Get the window title for the single control sub-view
         */
        public String getControlTitle();
    }

    public WdssiiSDockedViewFactory(String title, String icon) {
        super(title, icon);
    }
    /**
     * Create a sub-bock for gui controls, or a split pane
     */
    public static final boolean myDockControls = true;

    @Override
    public WdssiiView createWdssiiView() {

        if (!myDockControls) {
            // Get a single non-docked FeatureView component
            return super.createWdssiiView();
        } else {
            // Get the main window parts
            Component f = getNewComponent();
            String title = getWindowTitle();
            Icon i = getWindowIcon();

            RootContainer rootW = ViewManager.createRootContainer();
            List<Object> l = new ArrayList<Object>();
            
            // Create a special child view that cannot be destroyed, only redocked
            if (f instanceof SDockView) {
                SDockView s = (SDockView) (f);

                // Add global controls
                s.addGlobalCustomTitleBarComponents(l);

                // Create the control component
                Component c = s.getControlComponent();
                String ct = s.getControlTitle();
                rootW.createSingleSubWindow(ct, i, f, c);
            }
            Component rc = (Container) (rootW);

            return new WdssiiView(title, i, rc, l, "classkey");

        }
    }
}
