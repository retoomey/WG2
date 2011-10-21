package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.wdssii.gui.nbm.views.ThreadedTopComponent;

/**
 * A factory that returns an icon based on a passed name.  We can have
 * generated icons and/or disk based icons.
 * 
 * @author Robert Toomey
 */
public class SwingIconFactory {

    public static Icon getIconByName(String name) {

        Icon image = null;
        Class<?> c = null;

        // Try looking for dynamic icon first....
        try {
            c = Class.forName("org.wdssii.gui.swing.icon." + name);
            image = (Icon) c.newInstance();
            // It's ok...we just return null...
        } catch (Exception e) {
           // It's ok, we'll try by file....
        }
        if (image == null) {
           image = getImageIconByName(name, "");
           if (image == null){
               image = new MissingIcon();
           }
        }
        return image;
    }

    /** Load an image icon from the nbm.viems directory.
     * Netbeans puts view icons there, probably will make a general 'icon'
     * folder later on.
     */
    public static ImageIcon getImageIconByName(String path,
            String description) {
        java.net.URL imgURL = ThreadedTopComponent.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            return null;
        }
    }

    /** Load an image icon from the nbm.viems directory.
     * Netbeans puts view icons there, probably will make a general 'icon'
     * folder later on.
     */
    public static ImageIcon getImageIconByName(String path) {
        java.net.URL imgURL = ThreadedTopComponent.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            return null;
        }
    }

    /** Return an image of the icon.  This locks the drawing
     * of it of course.
     */
    public static Image getImageByName(String name) {

        Icon icon = getIconByName(name);
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(w, h);
        Graphics2D g = image.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }

    public static class MissingIcon implements Icon {

        private int width = 16;
        private int height = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, getIconWidth(), getIconHeight());
        g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
    //static public Image rightIcon(PaletteData colors, Display display)
    //static public Image rightEndIcon(PaletteData colors, Display display)
    //static public Image leftIcon(PaletteData colors, Display display) {
    //static public Image prevNotVirtualIcon(PaletteData colors, Display display) 
}
