package org.wdssii.gui.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.infonode.docking.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.DockWindow;
import org.wdssii.gui.swing.SwingIconFactory;

/**
 * A docked factory handling adding of new sub-docked Views
 *
 * Docked windows are all children of a single control window. Dynamic windows
 * can be created that are listed/owned by this main docking window.
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
     * The root window we add new subviews to
     */
    private RootWindow rootW;
    /**
     * Each subview gets a unique counter in its title
     */
    private int counter = 1;
    /**
     * Our factory
     */
    private WdssiiDockedViewFactory myAddFactory;

    @Override
    public DockingWindow getNewDockingWindow() {

        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());

        //info = new JTextField();
        //info.setText("Testing Nested Docking Charts (in progress)");
        //holder.add(info, BorderLayout.NORTH);

        // -------------------------------------------------------------
        // Create a root window (just a square that's hold views), it's
        // not a view itself...all charts will dock to this
        RootWindow root = DockWindow.createARootWindow();
        rootW = root;
        addNewSubView();
        holder.add(root, BorderLayout.CENTER);
        // ------------------------------------------------------------

        // The main view that contains all sub-docked views
        String title = getWindowTitle();
        Icon i = getWindowIcon();
        View topWindow = new View(title + "s", i, holder);
        MDockView m = getTempComponent();

        List<Object> l = getCustomTitleBarComponents(topWindow);
        m.addGlobalCustomTitleBarComponents(l);
        addCreationButton(l);

        return topWindow;
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
    protected void addNewSubView(View v) {
        //Icon i = getWindowIcon();
        DockingWindow base = rootW.getWindow();

        // Depending on how user has moved stuff around, should be one of three
        // possibilities:
        // 1.  All closed, so null --> create a new tabwindow
        // 2.  Dragged into split window...
        // 3.  TabWindow already...
        if (base == null) {
            TabWindow theTab = new TabWindow(v);
            rootW.setWindow(theTab);
        } else {
            if (base instanceof TabWindow) {
                TabWindow theTab = (TabWindow) (base);
                theTab.addTab(v);
            } else if (base instanceof SplitWindow) {
                TabWindow theTab = new TabWindow(new DockingWindow[]{v, base});
                rootW.setWindow(theTab);
            } else {
                LOG.error("Unknown window type...We should handle this type (FIXME)");
            }
        }
    }

    /**
     * Create a new subview, add to management
     */
    public View addNewSubView() {
        Icon i = getWindowIcon();
        String title = getWindowTitle();

        int c = getNewViewCounter();
        Component p = getNewSubViewComponent(c);
        View v = new View(title + "-" + c, i, p);
        MDockView m = null;
        if (p instanceof MDockView) {
            m = (MDockView) (p);
        }
        final MDockView link = m;
        if (m != null) {
            m.addCustomTitleBarComponents(getCustomTitleBarComponents(v));
        }
        // Add a 'close' listener so we can remove from the global list when
        // deleted
        if (link != null) {

            link.windowAdded();
            v.addListener(new DockingWindowAdapter() {
                @Override
                public void windowClosing(DockingWindow window)
                        throws OperationAbortedException {
                    if (link != null) {
                        link.windowClosing();
                    }
                }
            });
        }
        addNewSubView(v);
        return v;
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
