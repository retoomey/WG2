package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/**
 * SimplerJButton is our custom swing button that ignores the
 * themes/etc of the OS.  We use this for our navigation buttons because
 * I like having meaningful color feedback on the data.  Would be
 * nice to have custom settings for all the colors though for possible
 * colorblind usage.
 * I'm sure there's a simpler way to do all this, however this hides the details
 * from the rest of GUI.
 * 
 * @author Robert Toomey
 */
public class SimplerJButton extends JButton {

    protected int dashedRectGapX;
    protected int dashedRectGapY;
    protected int dashedRectGapWidth;
    protected int dashedRectGapHeight;
    private Color focusColor;

    /** We don't want the windows theme stuff, etc..but we still
     * want basic text, etc.
     */
    private class SimpleButtonUI extends BasicButtonUI {

        @Override
        protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);

            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setRolloverEnabled(true);

            dashedRectGapX = UIManager.getInt("ButtonUI.dashedRectGapX");
            dashedRectGapY = UIManager.getInt("ButtonUI.dashedRectGapY");
            dashedRectGapWidth = UIManager.getInt("ButtonUI.dashedRectGapWidth");
            dashedRectGapHeight = UIManager.getInt("ButtonUI.dashedRectGapHeight");
            focusColor = UIManager.getColor("ButtonUI.focus");

            b.setHorizontalAlignment(AbstractButton.LEFT);
        }

        @Override
        public void paintFocus(Graphics g, AbstractButton b, Rectangle r1, Rectangle r2, Rectangle r) {
          //  int width = b.getWidth();
          //  int height = b.getHeight();
          //  g.setColor(focusColor);
          //  BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX, dashedRectGapY,
          //          width - dashedRectGapWidth, height - dashedRectGapHeight);
        }

        @Override
        public void paintText(Graphics g, JComponent c, Rectangle r, String text) {

            super.paintText(g, c, r, text);

            // Draw a simple black border
            int w = getWidth();
            int h = getHeight();
            if (w > 0) {
                w -= 1;
            }
            if (h > 0) {
                h -= 1;
            }
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, w, h);
        }

        @Override
        public void update(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();

            // Our simple hilite when armed/pressed
            if (model.isArmed() && model.isPressed()) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(c.getBackground());
            }
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            paint(g, c);
        }
    }

    public SimplerJButton(String title) {
        super(title);
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {

        // We ignore the OS rendering because we have color cues
        // on our buttons..don't want any look and feel, etc...
        // FIXME: do this globally some how with UIManager?
        // Only want it set for this class though.
        if (!(ui instanceof SimpleButtonUI)) {
            setUI(new SimpleButtonUI());
        }
        super.paintComponent(g);
    }
}