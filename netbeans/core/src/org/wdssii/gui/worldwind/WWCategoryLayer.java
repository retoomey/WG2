/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wdssii.gui.worldwind;

/**
 * A WorldWind view can implement this if it has a particular
 * category for our LayerView.
 * 
 * @author Robert Toomey
 */
public interface WWCategoryLayer {
    public final String WDSSII_CATEGORY = "WDSSII GUI Layer";
    /** Return the category */
    public String getCategory();
}
