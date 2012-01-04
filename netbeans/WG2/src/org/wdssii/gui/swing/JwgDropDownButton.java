package org.wdssii.gui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * J 'wg' DropDownButton makes a JButton with a drop down menu.
 * We can add a little arrow to the side of the button to show that
 * a menu will come down.  Clean engineered off the netbeans drop down
 * menu button concept.
 * 
 * @author Robert Toomey
 */
public class JwgDropDownButton extends JButton {

    protected int dashedRectGapX;
    protected int dashedRectGapY;
    protected int dashedRectGapWidth;
    protected int dashedRectGapHeight;
    private Color focusColor;
    private boolean isShowingPopup = false;
    private boolean showPopup = false;
    private JPopupMenu myMenu = null;
    private Icon myDropDownIcon = new DropDownArrowIcon();

    public class DropDownArrowIcon implements Icon {

        private int width = 8;
        private int height = 16;
        protected BasicStroke stroke = new BasicStroke(1);

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            Polygon p = new Polygon();
            p.addPoint(4, 11);   // point
            p.addPoint(1, 8);   // left side
            p.addPoint(7, 8);  // right side
            p.translate(x, y);
            g2d.setColor(getFillColor());
            g2d.fillPolygon(p);
            g2d.setColor(getLineColor());
            g2d.setStroke(stroke);
            g2d.drawPolygon(p);
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

        public Color getLineColor() {
            return Color.BLACK;
        }

        public Color getFillColor() {
            return Color.BLACK;
        }
    }

    /** User interface class for custom drawing */
    private class JwgDropDownButtonUI extends BasicButtonUI {

        private final int X_OVERLAP = 5;

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
            super.paintFocus(g, b, r1, r2, r);
            // int width = b.getWidth();
            // int height = b.getHeight();
            // g.setColor(focusColor);
            //BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX, dashedRectGapY,
            //         width - dashedRectGapWidth, height - dashedRectGapHeight);
        }

        @Override
        public void paintText(Graphics g, JComponent c, Rectangle r, String text) {
            super.paintText(g, c, r, text);
        }

        public void orgPaint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();

            //    String text = layout(b, SwingUtilities2.getFontMetrics(b, g),
            //          b.getWidth(), b.getHeight());

            clearTextShiftOffset();

            // perform UI specific press action, e.g. Windows L&F shifts text
            // if (model.isArmed() && model.isPressed()) {
            //   paintButtonPressed(g,b); 
            // }

            // Paint the Icon
            // if(b.getIcon() != null) { 
            ////      paintIcon(g,c,iconRect);
            // }

            // if (text != null && !text.equals("")){
            //    View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            ///    if (v != null) {
            //	//v.paint(g, textRect);
            //    } else {
            //	//paintText(g, b, textRect, text);
            //    }
            // }

            if (b.isFocusPainted() && b.hasFocus()) {
                // paint UI specific focus
                //  paintFocus(g,b,viewRect,textRect,iconRect);
            }
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            JwgDropDownButton b = (JwgDropDownButton) (c);
            boolean orgArmed = model.isArmed();
            boolean orgPressed = model.isPressed();
            boolean orgRoll = model.isRollover();
            boolean orgSel = model.isSelected();

            if (b.isShowingPopup) {
                model.setArmed(true);  // if side effects we could get into trouble here
                model.setPressed(true);
                model.setRollover(true);
                model.setSelected(true);
            }

            super.paint(g, c);
            // orgPaint(g, c);

            //   JwgDropDownButton b = (JwgDropDownButton) (c);
            Icon i = b.getDropDownIcon();
            int w = i.getIconWidth();
            i.paintIcon(c, g, c.getWidth() - w - X_OVERLAP, 0);

            Border t = c.getBorder();
            t.paintBorder(c, g, 0, 0, c.getWidth(), c.getHeight());
            model.setArmed(orgArmed);
            model.setPressed(orgPressed);
            model.setSelected(orgSel);
            model.setRollover(orgRoll);

        }

        @Override
        public void update(Graphics g, JComponent c) {
            super.update(g, c);
            /* AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            
            // Our simple hilite when armed/pressed
            if (model.isArmed() && model.isPressed()) {
            g.setColor(Color.YELLOW);
            } else {
            g.setColor(c.getBackground());
            }
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
            paint(g, c);
             * 
             */
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);

            // Add the stuff for the little drop down arrow...
            JwgDropDownButton b = (JwgDropDownButton) (c);
            Icon i = b.getDropDownIcon();
            int w = i.getIconWidth();
            d.width += w;
            return d;
        }
    }

    public JwgDropDownButton(String title) {
        super(title);
        setUpListeners();
    }

    public JwgDropDownButton(Icon icon) {
        super(icon);
        setUpListeners();
    }

    /** The icon for drop down purposes */
    private void setDropDownIcon(Icon icon) {
        myDropDownIcon = icon;
    }

    private Icon getDropDownIcon() {
        return myDropDownIcon;
    }

    public void setMenu(JPopupMenu menu) {
        if (myMenu != menu) {
            myMenu = menu;

            menu.addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent e) {
                    isShowingPopup = false;
                }

                @Override
                public void focusGained(FocusEvent e) {
                    isShowingPopup = true;
                }
            });
        }
    }

    /** Generate a dynamic menu if needed */
    public void generateMenu(){
        
    }
    
    private void setUpListeners() {

        addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPopup) {
                    Component c = (Component) e.getSource();
                    generateMenu();
                    if (myMenu != null) {
                        myMenu.show(c, -1, c.getHeight());
                        myMenu.requestFocus();
                    }
                } else {
                    showPopup = true;
                }
            }
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (isShowingPopup) {
                    showPopup = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup = true;
            }
        });

    }

    @Override
    protected void paintComponent(Graphics g) {

        // We ignore the OS rendering because we have color cues
        // on our buttons..don't want any look and feel, etc...
        // FIXME: do this globally some how with UIManager?
        // Only want it set for this class though.
        if (!(ui instanceof JwgDropDownButtonUI)) {
            setUI(new JwgDropDownButtonUI());
        }
        super.paintComponent(g);
    }
}