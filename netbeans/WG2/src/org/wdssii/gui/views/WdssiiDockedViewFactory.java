package org.wdssii.gui.views;

import java.awt.Component;
import java.util.List;
import javax.swing.Icon;
import org.wdssii.gui.swing.SwingIconFactory;

/**
 * Root factory for all of our docking windows in the display. This returns
 * information that can exist _before_ the actual window, such as the icon for
 * menus and toolbar. See
 * <code>WorldWindView</code> for an example of this.
 *
 * @author Robert Toomey
 */
public abstract class WdssiiDockedViewFactory {

    /**
     * Interface for creating parts of a single dock view
     */
    public static interface DockView {

        /**
         * Get the global title controls for the main dock window
         */
        public void addGlobalCustomTitleBarComponents(List<Object> l);
    }
    /**
     * The title of the docking window
     */
    private String myTitle;
    /**
     * The short name of this factory, used as a key. Same as window title for
     * now..might change with multiple windows
     */
    private String myShortName;
    /**
     * The name of the icon
     */
    private String myIconName;
 
    private Component myComponent = null;
    private WdssiiView myWdssiiView = null;

    public WdssiiDockedViewFactory(String title, String icon) {
        myTitle = title;
        myShortName = title;
        myIconName = icon;
    }

    /**
     * The key for this factory
     */
    public String getShortName() {
        return myShortName;
    }

    /**
     * The icon for this view
     */
    public Icon getWindowIcon() {
        Icon i = null;
        if (!myIconName.isEmpty()) {
            i = SwingIconFactory.getIconByName(myIconName);
        }
        return i;
    }

    /**
     * The window title for this view
     */
    public String getWindowTitle() {
        return myTitle;
    }

    /**
     * The menu for this view. Might eventually be different, or a path
     */
    public String getMenuTitle() {
        return myTitle;
    }

    /**
     * The menu icon for this view.
     */
    public Icon getMenuIcon() {
        return getWindowIcon();
    }

    /**
     * Return the cached component
     */
    public Component getComponent() {
        if (myComponent == null) {
            myComponent = getNewComponent();
        }
        return myComponent;
    }

    public void setWdssiiView(WdssiiView v){
        myWdssiiView = v;
    }
    
    public WdssiiView getWdssiiView() {
        if (myWdssiiView == null) {
            myWdssiiView = createWdssiiView();
        }
        return myWdssiiView;
    }
    
    public WdssiiView createWdssiiView(){
        return new WdssiiView(getWindowTitle(), getWindowIcon(), getNewComponent(), null, "");
    }

    /**
     * The wrapped component for the docking view
     */
    public abstract Component getNewComponent();
}
