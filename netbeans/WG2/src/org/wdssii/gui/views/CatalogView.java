package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;
import java.awt.Component;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *
 * CatalogView contains different catalogs, or lists of sources/features
 * that can be added to the display
 *
 * @author Robert Toomey
 *
 */
public class CatalogView extends JThreadPanel implements CommandListener {

    private final static Logger LOG = LoggerFactory.getLogger(CatalogView.class);

    private javax.swing.JTabbedPane jRootTab;
    
    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Catalog", "cart_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new CatalogView();
        }
    }
  
    @Override
    public void updateInSwingThread(Object info) {
        // We don't update externally..only from clicked buttons, etc..
        // which are of course already in the swing thread.
    }

    public CatalogView() {

        initComponents();    

        CommandManager.getInstance().addListener("Catalog", this);
    }

    private void initComponents() {

        // Create a tabbed layout for our contents
        // setLayout(new MigLayout("fill", "", ""));
        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        jRootTab = new javax.swing.JTabbedPane();
        add(jRootTab, new CC().growX().growY());     
        jRootTab.addTab("WDSS2", new WdssiiCatalog());  
        jRootTab.addTab("WMS", new WMSCatalog());
    }
}
