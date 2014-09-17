package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.gui.views.ViewManager.RootContainer;

/**
 * Multiple windows contained within a single window
 *
 * Windows are all children of a single control window. Dynamic windows can be
 * created that are listed/owned by this main docking window.
 *
 * @author Robert Toomey
 */
public abstract class WdssiiMDockedViewFactory extends WdssiiDockedViewFactory {

    private final static Logger LOG = LoggerFactory.getLogger(WdssiiMDockedViewFactory.class);

    /**
     * Interface for creating parts of a multi dock view
     */
    public static interface MDockView extends DockView {

        /**
         * Get the controls for an individual dock window instance
         */
        public void addCustomTitleBarComponents(List<Object> l);

        /**
         * Sent when subwindow added
         */
        public void windowAdded();

        /**
         * Sent when subwindow closing (being removed)
         */
        public void windowClosing();
    }

    public WdssiiMDockedViewFactory(String title, String icon) {
        super(title, icon);
    }
    /**
     * The root component we add new subviews to
     */
    public RootContainer rootW;
    /**
     * Each subview gets a unique counter in its title
     */
    private int counter = 1;

    @Override
    public WdssiiView createWdssiiView() {

        // -------------------------------------------------------------
        // Create a root window (just a square that's hold views), it's
        // not a view itself...all charts will go into this
        rootW = ViewManager.createRootContainer();
        Component c = (Container)(rootW);
       // c.setVisible(false);
        addNewSubView();  // The 'first' data view...

        // The main view that contains all sub-docked views
        String title = getWindowTitle();
        Icon i = getWindowIcon();

        // Add the top level window title items
        List<Object> l = new ArrayList<Object>();

        MDockView m = getTempComponent();
        m.addGlobalCustomTitleBarComponents(l);
        addCreationButton(l);
        rootW.addControls(l);
        
        return new WdssiiView(title + "s", i, c, l, "classkey");
    }

    public void collapseLayout(){
        ViewManager.collapseLayout(rootW);
    }
    
    /**
     * Return a MDockView that can give us global title components
     */
    public abstract MDockView getTempComponent();

    /**
     * Return a new numbered sub view
     */
    public abstract Component getNewSubViewComponent(int counter);

    /**
     * Get available counter for a new view
     */
    protected int getNewViewCounter() {
        return (counter++);
    }

    /**
     * Add view already created
     */
    protected void addNewSubView(WdssiiView v) {
       // ViewManager.addWdssiiViewToRootContainer(rootW, v, null);
    }

    /**
     * Create a new subview, add to management
     */
    public void addNewSubView() {

        Icon i = getWindowIcon();
        String title = getWindowTitle();
        int c = getNewViewCounter();
        Component p = getNewSubViewComponent(c);

        // Gather menu items for this sub view
        List<Object> l = new ArrayList<Object>();
        MDockView m = null;
        if (p instanceof MDockView) {
            m = (MDockView) (p);
            m.addCustomTitleBarComponents(l);
        }
        WdssiiView v = new WdssiiView(title + "-" + c, i, p, l, "classkey2");
        
        rootW.addWdssiiView(v, m);
    }

    /**
     * Add the standard button on main container that allows creating a new
     * sub-view
     */
    public void addCreationButton(List<Object> addTo) {

        JButton test = new JButton();
        Icon i = SwingIconFactory.getIconByName("brick_add.png");
        test.setIcon(i);
        test.setToolTipText("Add new subwindow");
        test.setFocusable(false);
        test.setOpaque(false);
        test.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewSubView();
            }
        });
        addTo.add(test);
    }
}
