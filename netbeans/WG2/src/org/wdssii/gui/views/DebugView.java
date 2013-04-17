package org.wdssii.gui.views;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.infonode.docking.DockingWindow;
import net.infonode.docking.RootWindow;
import net.infonode.docking.TabWindow;
import net.infonode.docking.View;
import net.infonode.docking.properties.WindowTabProperties;
import net.infonode.properties.propertymap.PropertyMap;
import org.wdssii.gui.DockWindow;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *  The debug view will contain all the views that I've made just for
 *  debugging purpose.  Views that a regular user probably doesn't need to 
 *  concern themselves with.
 * 
 * @author Robert Toomey
 */
public class DebugView extends JThreadPanel 
{

    @Override
    public void updateInSwingThread(Object info) {
       
    }
    
    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {
        private static int counter = 1;
        public Factory() {
            super("Debug", "chart_bar.png");
        }

        @Override
        public DockingWindow getNewDockingWindow() {

            JPanel holder = new JPanel();
            holder.setLayout(new BorderLayout());
            
            JTextField t = new JTextField();
            t.setText("All debugging views are contained within this window");
            holder.add(t, BorderLayout.NORTH);
            
            // -------------------------------------------------------------
            // Create a root window (just a square that's hold views), it's
            // not a view itself...all charts will dock to this
            RootWindow root = DockWindow.createARootWindow();

            // Jobs view
            JobsView.Factory f1 = new JobsView.Factory();
            Icon i = f1.getWindowIcon();
            String title = f1.getWindowTitle();
            Component p = f1.getNewComponent();        
            View v = new View(title, i, p);
            
            // Cache view
            CacheView.Factory f2 = new CacheView.Factory();
            Icon i2 = f2.getWindowIcon();
            String title2 = f2.getWindowTitle();
            Component p2 = f2.getNewComponent();           
            View v2= new View(title2, i2, p2);
       
            // ColorKey view
            ColorKeyView.Factory f3 = new ColorKeyView.Factory();
            Icon i3 = f3.getWindowIcon();
            String title3 = f3.getWindowTitle();
            Component p3 = f3.getNewComponent();           
            View v3= new View(title3, i3, p3);
            
            TabWindow stuff = new TabWindow(new DockingWindow[]{v, v2, v3});
            root.setWindow(stuff);
            stuff.setSelectedTab(0);
           
            // ------------------------------------------------------------
            
            holder.add(root, BorderLayout.CENTER); 
            // Add it to a view for 'Debugging'
            View topWindow = new View("Debug", i, holder);
           
            return topWindow;
        }

        @Override
        public Component getNewComponent() {
            return new DebugView();
        }
    }
 
}
