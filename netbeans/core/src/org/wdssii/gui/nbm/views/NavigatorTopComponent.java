package org.wdssii.gui.nbm.views;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.commands.WdssiiCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.ProductNavigator;
import org.wdssii.gui.views.NavView;

@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Navigator//EN",
autostore = false)
@TopComponent.Description(preferredID = "NavigatorTopComponent",
iconBase = "org/wdssii/gui/nbm/views/eye.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.NavigatorTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_NavigatorAction",
preferredID = "NavigatorTopComponent")
/** Navigator allows us to move forward/back in time and up/down elevation
 * for a particular product.
 * 
 * @author Robert Toomey
 */
public final class NavigatorTopComponent extends TopComponent implements NavView {

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void ProductCommandUpdate(ProductCommand command) {
        update();
    }

    public void SourceCommandUpdate(SourceCommand command) {
        update();
    }

    public void AnimateCommandUpdate(AnimateCommand command) {
        update(); // Catch animation command to update loop button
    }
    private static final int myGridRows = 4;
    private static final int myGridCols = 4;
    private static final int myGridCount = myGridRows * myGridCols;
    private ArrayList<NavButton> myNavControls = new ArrayList<NavButton>();

    @Override
    public void update() {
        updateNavButtons();
    }

    /** Our special class for drawing the grid controls */
    public static class NavButton extends JButton {

        private int myGridIndex;
        private WdssiiCommand myCommand = null;

        public NavButton(String title, int index) {
            super(title);
            myGridIndex = index;
            this.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    handleActionPerformed(e);
                }
            });
        }

        public void handleActionPerformed(ActionEvent e) {
            CommandManager m = CommandManager.getInstance();
            m.executeCommand(myCommand, true);
        }

        public int getGridIndex() {
            return myGridIndex;
        }

        public void setCommand(WdssiiCommand c) {
            myCommand = c;

            ProductButtonStatus p = null;
            if (myCommand == null) {
                setVisible(false);
            } else {
                setVisible(true);
                p = c.getButtonStatus();
            }
            updateNavButton(p);

        }

        public void updateNavButton(ProductButtonStatus p) {
            if (p == null) {
                setVisible(false);
            } else {
                setVisible(true);
                setText(p.getButtonText());
                setToolTipText(p.getToolTip());
                setEnabled(p.getValidRecord());
                Color c;
                if (p.getUseColor()) {
                    c = new Color(p.getRed(), p.getGreen(), p.getBlue());
                } else {
                    c = new Color(255, 0, 0);
                }

                // Everytime we update status we recreate the icon, which is kinda
                // messy..but then
                // again we could do things like change icon colors/animate? Lots of
                // possibilities.
                // Need to read SWT well make sure we aren't leaking here.
                //setImage(p.getIcon(myDisplay));
                setBackground(c);
                setEnabled(p.getEnabled());

            }
        }
    }

    public NavigatorTopComponent() {
        initComponents();

        GridLayout l = new GridLayout(myGridRows, myGridCols);
        jNavPanel.setLayout(l);
        l.setHgap(2);
        for (int i = 0; i < myGridCount; i++) {
            NavButton b = new NavButton("Test" + i, i);
            b.setBackground(Color.red);
            if (i == 0) {
                b.setVisible(false);
            }
            jNavPanel.add(b);
            myNavControls.add(b);
        }
        updateNavButtons();
        CommandManager.getInstance().registerView(NavView.ID, this);
        setName(NbBundle.getMessage(NavigatorTopComponent.class, "CTL_NavigatorTopComponent"));
        setToolTipText(NbBundle.getMessage(NavigatorTopComponent.class, "HINT_NavigatorTopComponent"));

    }

    private Product updateNavButtons() {
        // Update the navigation button array
        CommandManager m = CommandManager.getInstance();
        ProductHandlerList l = m.getProductOrderedSet();
        ProductHandler h = l.getTopProductHandler();
        Product d = null;
        if (h != null) {
            d = h.getProduct();
        }

        ProductNavigator n = null;
        if (d != null) {
            n = d.getNavigator();
        }

        // Update the button grid to the current ProductNavigator
        // product.  A product sets the commands/output of these
        // buttons depending on the product type
        for (NavButton b : myNavControls) {
            WdssiiCommand w = null;
            if (n != null) {
                w = n.getGridCommand(b.getGridIndex());
            }
            b.setCommand(w);
        }
        return d;

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jNavPanel = new javax.swing.JPanel();
        jLoopPanel = new javax.swing.JPanel();

        jNavPanel.setBackground(new java.awt.Color(0, 102, 51));
        jNavPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 0)));
        jNavPanel.setLayout(null);

        jLoopPanel.setBackground(new java.awt.Color(153, 0, 153));
        jLoopPanel.setLayout(null);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLoopPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
            .addComponent(jNavPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jNavPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLoopPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jLoopPanel;
    private javax.swing.JPanel jNavPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
