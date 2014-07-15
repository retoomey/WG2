package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.JPanel;
import org.wdssii.gui.RadarInfo;
import org.wdssii.gui.RadarInfo.ARadarInfo;
import org.wdssii.xml.SourceBookmarks.BookmarkURLSource;

/**
 * CONUS JPanel shows the CONUS image from the web, with vcp information.
 *
 * @author Robert Toomey
 */
public class CONUSJPanel extends JPanel {

    /**
     * The image we are using in the background
     */
    private BufferedImage image;
    /**
     * The collection of radar information we use
     */
    private RadarInfo myRadarInfo;
    private String myDragging;
    String hitName;
    private ArrayList<BookmarkURLSource> mySourceList;
    private CONUSJPanelListener myCONUSJPanelListener;

    /**
     * interface for listening to conus events
     */
    public static interface CONUSJPanelListener {

        public void radarClicked(String name);
        public void radarDoubleClicked(String name);
    }

    public void addCONUSJPanelListener(CONUSJPanelListener l) {
        // Just one for now..
        myCONUSJPanelListener = l;
    }

    public void setBookmarkList(ArrayList<BookmarkURLSource> list) {
        mySourceList = list;
    }

    public CONUSJPanel() {
        mouser m = new mouser();
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setImage(BufferedImage aImage) {
        image = aImage;
    }

    public void setRadarInfo(RadarInfo r) {
        myRadarInfo = r;
    }

    @Override
    public void paintComponent(Graphics g) {
        // Call super so background gets cleared where image isn't
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, null);

            if (myRadarInfo != null) {

                // Gather font information
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                FontRenderContext frc = g2.getFontRenderContext();
                Font f = Font.decode("Arial-PLAIN-12");

                // Draw all the boxes first before mouse hilite..
                Map<String, ARadarInfo> divs = myRadarInfo.getRadarInfos();

                // Update the rectangles for the curent text size
                int m = 5;
                for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                    ARadarInfo d = entry.getValue();
                    // Use the current VCP rectangle as the new center
                    TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                    Rectangle2D r = t1.getBounds();
                    d.width = (int) r.getWidth() + m + m;
                    d.height = (int) r.getHeight() + m + m;
                }

                // Draw the non-hit radar entries in 'background'
                for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                    ARadarInfo d = entry.getValue();
                    Color c = d.getColor();
                    String key = entry.getKey();
                    Rectangle2D r = d.getRect();
                    TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                    if (!key.equals(hitName)) {
                        g.setColor(c);
                        g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
                        g.setColor(Color.BLACK);
                        t1.draw(g2, (float) r.getX() + m, (float) (r.getY() + r.getHeight() - m));
                    }
                }

                // Mouse hit pass....
                for (Map.Entry<String, ARadarInfo> entry : divs.entrySet()) {
                    //Gonna have to draw em...
                    ARadarInfo d = entry.getValue();
                    Color c = d.getColor();
                    String key = entry.getKey();
                    int left = d.getLeft();
                    int top = d.getTop();

                    if (key.equals(hitName)) {

                        // Cross reference to the current source list...
                        if (mySourceList != null) {
                            StringBuilder b = new StringBuilder(key);
                            for (BookmarkURLSource s : mySourceList) {
                                if (s.name.equals(key)) {
                                    b.append(": ");
                                    b.append(d.id);   
                                }
                            }
                            key = b.toString();
                        }

                        // Hilight the actual radar color rectangle..
                        TextLayout t1 = new TextLayout(d.getVCPString(), f, frc);
                        Rectangle2D r = d.getRect();

                        // Fill     
                        g.setColor(c.brighter());
                        g.fillRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

                        // Outline
                        g.setColor(Color.WHITE);
                        g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());

                        // Text
                        g.setColor(Color.BLACK);
                        t1.draw(g2, (float) r.getX() + m, (float) (r.getY() + r.getHeight() - m));

                        // Draw the floating text for hovering ----------------------------------
                        TextLayout t2 = new TextLayout(key, f, frc);
                        Rectangle2D b = t2.getBounds();
                        int hLeft = left;
                        int hTop = (int) (top + r.getHeight() + m + b.getHeight());
                        b.setRect(b.getX() + hLeft - 2, b.getY() + hTop - 2, b.getWidth() + 4, b.getHeight() + 4);

                        // Try to move text to left if past right edge....
                        int over = (int) ((b.getX() + b.getWidth()) - image.getWidth());
                        if (over > 0) {
                            b.setRect(b.getX() - over - 5, b.getY(), b.getWidth(), b.getHeight());
                            hLeft -= over + 5;
                        }

                        g2.setClip(0, 0, image.getWidth(), image.getHeight());
                        g2.setColor(Color.WHITE);
                        g2.fillRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
                        g2.setColor(Color.black);
                        t2.draw(g2, hLeft, hTop);
                        g2.drawRect((int) b.getX(), (int) b.getY(), (int) b.getWidth(), (int) b.getHeight());
                        // End draw hover text -------------------------------------
                    }
                }

            }
        }
    }

    /**
     * A cheezy outline behind the text that doesn't require an outline font to
     * render. It shadows by shifting the text 1 pixel in every direction. Not
     * very fast, but color keys are more about looks.
     */
    public void cheezyOutline(Graphics2D g, int x, int y, TextLayout t) {

        // Draw a 'grid' of background to shadow the character....
        // We can get away with this because there aren't that many labels
        // in a color key really. Draw 8 labels shifted to get outline.
        g.setColor(Color.black);
        t.draw(g, x + 1, y + 1);
        t.draw(g, x, y + 1);
        t.draw(g, x - 1, y + 1);
        t.draw(g, x - 1, y);
        t.draw(g, x - 1, y - 1);
        t.draw(g, x, y - 1);
        t.draw(g, x + 1, y - 1);
        t.draw(g, x + 1, y);

        g.setColor(Color.white);
        t.draw(g, x, y);
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        return new Dimension(100, 100);
    }

    private class mouser extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (myRadarInfo != null) {
                int x = e.getX();
                int y = e.getY();
                String newHit = "";
                newHit = myRadarInfo.findRadarAt(x, y);
                if (newHit != null) {
                    myDragging = newHit;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            myDragging = "";
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // On release..find it in the current source list and 
            // select the line in the bookmark table....
            if (myRadarInfo != null) {
                int x = e.getX();
                int y = e.getY();
                String newHit = "";
                newHit = myRadarInfo.findRadarAt(x, y);
                if (newHit != null) {
                    if (myCONUSJPanelListener != null) {
                        myCONUSJPanelListener.radarClicked(newHit);
                    }
                    /**
                     * On double click
                     */
                    if (e.getClickCount() == 2) {
                        myCONUSJPanelListener.radarDoubleClicked(newHit);
                    }
                }
            }

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (myRadarInfo != null) {
                int x = e.getX();
                int y = e.getY();
                String newHit = "";
                newHit = myRadarInfo.findRadarAt(x, y);
                if (!newHit.equals(hitName)) {
                    hitName = newHit;
                    repaint();
                }
            }
        }
    }
}