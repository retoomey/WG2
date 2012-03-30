package org.wdssii.gui.views;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *
 * @author Robert Toomey
 */
public class SourcesView extends JThreadPanel implements CommandListener {

    public static final String ID = "wdssii.SourcesView";
    private static Logger log = LoggerFactory.getLogger(SourcesView.class);
    
     /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Sources", "brick_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new SourcesView();
        }
    }
    
    @Override
    public void updateInSwingThread(Object command) {
       
    }
    
    public SourcesView() {
        initGUI();
        CommandManager.getInstance().addListener(SourcesView.ID, this);
    }
    
    public void initGUI(){
         setLayout(new MigLayout("fill", "", ""));
        JPanel myPanel = new JPanel();
        add(myPanel, "grow");
        myPanel.setLayout(new MigLayout("fillx", "", ""));
        JLabel myInfo = new JLabel("TESTING");
        myPanel.add(myInfo, "dock north");
    }

}
