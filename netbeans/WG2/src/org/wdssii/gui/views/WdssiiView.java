package org.wdssii.gui.views;

import java.awt.Component;
import java.util.List;
import javax.swing.Icon;

/**
 * Used to return all the parts of a view to the window manager
 *
 * @author Robert Toomey
 */
public class WdssiiView {

    private Icon icon;
    private String title;
    private Component component;
    private List<Object> windowTitleItems = null;
    private String key;
    
    public Object container;  // ? need this
    
    public WdssiiView(String t, Icon i, Component c, List<Object> w, String k){
        title = t;
        icon = i;
        component = c;
        windowTitleItems = w;
        key = k;
    }
    
    public Icon getIcon(){
        return icon;
    }
    
    public String getTitle(){
        return title;
    }
    
    public Component getComponent(){
        return component;
    }
    
    public List<Object> getWindowTitleItems(){
        return windowTitleItems;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String shortName) {
        key = shortName;
    }
}