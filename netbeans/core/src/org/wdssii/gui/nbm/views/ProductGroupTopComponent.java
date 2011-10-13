package org.wdssii.gui.nbm.views;

import net.miginfocom.swing.MigLayout;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.views.ProductGroupView;

@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//ProductGroup//EN",
autostore = false)
@TopComponent.Description(preferredID = "ProductGroupTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.ProductGroupTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_ProductGroupAction",
preferredID = "ProductGroupTopComponent")
public final class ProductGroupTopComponent extends TopComponent {

    private ProductGroupView myPanel;
        
    public ProductGroupTopComponent() {
        initComponents();
        
        setLayout(new MigLayout("fill, inset 0", "", ""));
        myPanel = new ProductGroupView();
        add(myPanel, "grow");
        
        setName(NbBundle.getMessage(ProductGroupTopComponent.class, "CTL_ProductGroupTopComponent"));
        setToolTipText(NbBundle.getMessage(ProductGroupTopComponent.class, "HINT_ProductGroupTopComponent"));

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
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
