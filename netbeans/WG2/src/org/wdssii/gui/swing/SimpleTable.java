package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import org.wdssii.gui.GridVisibleArea;

/**
 * My version of a JSwing Table that avoids all of the O(n) data structures
 * in order to be true virtual.
 * 
 * A virtual table has to have a set width and height per cell, otherwise
 * you end up doing a lot of O(n) updating.
 * A virtual table shouldn't create N objects such as TableColumn for each
 * of its billion columns.
 * 
 * We subclass from JLabel?
 * 
 * @author Robert Toomey
 */
public class SimpleTable extends JLabel
        implements Scrollable,
        MouseMotionListener {

    /** Simple table model, unlike the swing counter part, doesn't
     * create billions of TableColumn objects.  Every cell in the table
     * is the same type, having a set width/height.  We also combine the model
     * with the cell renderer.
     * 
     * This default creates a checkered grid for testing.
     * FIXME: make a TestSimpleTableModel instead...
     * 
     * This model will power the headers, corner and contents of our Table
     */
    public static class SimpleTableModel {

        public SimpleTable myTable;
        
        // We shouldn't be reentrant, sync should be not needed.
        public SimpleTableRenderInfo buffer = new SimpleTableRenderInfo();

        public void handleScrollAdjust(AdjustmentEvent e) {
        }

        /** Subclasses can create subclasses of this to add information
         * to render.  ???
         */
        public static class SimpleTableRenderInfo {

            Color background;
            Color foreground;
            String text;
        }
        private int cellWidth = 50;
        private int cellHeight = 16;
        private int headerWidth = 70;
        private int headerHeight = 16;
        private int numRows = 10000;
        private int numCols = 10000;
        private GridVisibleArea myCurrentGrid;

        /** Get the total width of the cells in the table */
        public int getWidth() {
            return getNumCols() * getCellWidth();
        }

        public int getNumCols() {
            return numCols;
        }

        public int getCellWidth() {
            return cellWidth;
        }

        /** Get the total height of the cells in the table */
        public int getHeight() {
            return (getNumRows() * getCellHeight());
        }

        public int getNumRows() {
            return numRows;
        }

        public int getCellHeight() {
            return cellHeight;
        }

        /** Return width of the header, or -1 if off */
        public int getRowHeaderWidth() {
            return headerWidth;
        }

        public int getColHeaderHeight() {
            return headerHeight;
        }

        /** Get the row label for given row number ??? */
        public String getRowHeader(int row) {
            return String.format("%d,R", row);

        }

        /** Get the col label for given col number */
        public String getColHeader(int col) {
            return String.format("H,%d", col);
        }

        // Humm depends upon wdssii..should it? The GridVisibleArea is used
        // by the display for rendering the outline of table in 3D window
        public GridVisibleArea getVisibleGrid(Rectangle clipBounds) {
            GridVisibleArea a = new GridVisibleArea();

            a.startCol = clipBounds.x / getCellWidth();
            a.startRow = clipBounds.y / getCellHeight();
            a.lastPartialColumn = (clipBounds.x + clipBounds.width) / getCellWidth();
            a.lastPartialRow = (clipBounds.y + clipBounds.height) / getCellHeight();

            int rows = getNumRows();
            int cols = getNumCols();
            a.numRows = rows;
            a.numCols = cols;

            if (a.lastPartialColumn > cols - 1) {
                a.lastPartialColumn = cols - 1;
            }
            if (a.lastPartialRow > rows - 1) {
                a.lastPartialRow = rows - 1;
            }
            a.lastFullRow = a.lastPartialRow - 1;
            a.lastFullColumn = a.lastPartialColumn - 1;

            myCurrentGrid = a;
            return a;

        }

        public GridVisibleArea getCurrentVisibleGrid() {
            return myCurrentGrid;
        }

        public SimpleTableRenderInfo getCellInfo(int row, int col,
                int x, int y, int w, int h) {
            SimpleTableRenderInfo info = new SimpleTableRenderInfo();
            if (row == -1) {
                info.background = Color.YELLOW;
                info.text = String.format("H,%d", col);
            } else if (col == -1) {
                info.background = Color.GREEN;
                info.text = String.format("%d,R", row);
            } else {
                if ((row % 2 == 0)) {
                    if ((col % 2 == 0)) {
                        info.background = Color.RED;
                    } else {
                        info.background = Color.BLUE;
                    }
                } else {
                    if ((col % 2 == 0)) {
                        info.background = Color.BLUE;
                    } else {
                        info.background = Color.RED;
                    }
                }
            }
            info.foreground = Color.BLACK;
            return info;
        }

        /** Draw a cell box, we're using our model as a renderer as well,
        a -1 for row means it's the rowheader, -1 for col that it's the col
        header.
         * 
         * We have a default drawCellBox that just draws a text string
         * with a background/foreground.
         */
        public void drawCellBox(Graphics g, int row, int col,
                int x, int y, int w, int h) {

            SimpleTableRenderInfo info = getCellInfo(row, col, x, y, w, h);
            g.setColor(info.background);
            g.fillRect(x, y, w, h);

            g.setColor(info.foreground);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(info.text, x, y + 10);

            // Cheap border.  FIXME: draw lines instead of boxes for each?
            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
        }
        
        public void setTable(SimpleTable t){
            myTable = t;
        }
        public void handleDataChanged(){
            if (myTable != null){
                myTable.handleDataChanged();
            }
        }
    }

    private static class Rule extends JComponent {

        public static final int HORIZONTAL = 0;
        public static final int VERTICAL = 1;
        public int orientation;
        private SimpleTableModel myModel;

        public Rule(int o, boolean m) {
            orientation = o;
        }

        public void setModel(SimpleTableModel model, boolean update) {
            myModel = model;
            if (update) {
                handleDataChanged();
            }
        }

        public void handleDataChanged() {
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = 0, h = 0;
            if (myModel != null) {
                if (orientation == HORIZONTAL) {
                    w = myModel.getWidth();
                    h = myModel.getColHeaderHeight();
                } else {
                    w = myModel.getRowHeaderWidth();
                    h = myModel.getHeight();
                }
            }
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (myModel != null) {
                Rectangle drawHere = g.getClipBounds();

                // Fill clipping area with dirty brown/orange.
                g.setColor(new Color(230, 163, 4));
                g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

                GridVisibleArea a = myModel.getVisibleGrid(drawHere);
                if (orientation == HORIZONTAL) {
                    // The header columns....
                    int w = myModel.getCellWidth();
                    int h = myModel.getColHeaderHeight();
                    for (int col = a.startCol; col <= a.lastPartialColumn; col++) {

                        int x = myModel.getCellWidth() * col;
                        //  int y = drawHere.y;
                        int y = 0;

                        myModel.drawCellBox(g, -1, col,
                                x, y, w, h);
                    }
                } else {
                    // The header columns....
                    int w = myModel.getRowHeaderWidth();
                    int h = myModel.getCellHeight();
                    for (int row = a.startRow; row <= a.lastPartialRow; row++) {
                        //Rectangle cell;

                        //   int x = drawHere.x;
                        int x = 0;
                        int y = myModel.getCellHeight() * row;

                        myModel.drawCellBox(g, row, -1,
                                x, y, w, h);

                    }
                }

            }


        }
    }
    private SimpleTableModel myModel;
    private Rule myRowHeader;
    private Rule myColHeader;

    public SimpleTable(JScrollPane pane, int row, int col) {
        super();
        myColHeader = new Rule(Rule.HORIZONTAL, true);
        myRowHeader = new Rule(Rule.VERTICAL, true);
        setOpaque(true);
        setBackground(Color.white);

        //Let the user scroll by dragging to outside the window.
        setAutoscrolls(true); //enable synthetic drag events
        // addMouseMotionListener(this); //handle mouse drags
    }

    public void setupScrollPane(JScrollPane pane) {
        pane.setViewportView(this);
        pane.setColumnHeaderView(myColHeader);
        pane.setRowHeaderView(myRowHeader);
        
        // Listen for value changes in the scroll pane's scrollbars
        AdjustmentListener listener = new AdjustmentListener(){

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (myModel != null){
                    myModel.handleScrollAdjust(e);
                }
                     /*  Adjustable source = e.getAdjustable();

                       /*
        // getValueIsAdjusting() returns true if the user is currently
        // dragging the scrollbar's knob and has not picked a final value
        if (evt.getValueIsAdjusting()) {
            // The user is dragging the knob
            return;
        }

        // Determine which scrollbar fired the event
        int orient = source.getOrientation();
        if (orient == Adjustable.HORIZONTAL) {
            // Event from horizontal scrollbar
        } else {
            // Event from vertical scrollbar
        }

        // Determine the type of event
        int type = evt.getAdjustmentType();
        switch (type) {
          case AdjustmentEvent.UNIT_INCREMENT:
              // Scrollbar was increased by one unit
              break;
          case AdjustmentEvent.UNIT_DECREMENT:
              // Scrollbar was decreased by one unit
              break;
          case AdjustmentEvent.BLOCK_INCREMENT:
              // Scrollbar was increased by one block
              break;
          case AdjustmentEvent.BLOCK_DECREMENT:
              // Scrollbar was decreased by one block
              break;
          case AdjustmentEvent.TRACK:
              // The knob on the scrollbar was dragged
              break;
        }

        // Get current value
        int value = evt.getValue();*/
            }
            
        };
        pane.getHorizontalScrollBar().addAdjustmentListener(listener);
        pane.getVerticalScrollBar().addAdjustmentListener(listener);
    }

    public void setModel(SimpleTableModel aModel) {
        myModel = aModel;
        myModel.setTable(this);
        myColHeader.setModel(myModel, false);
        myRowHeader.setModel(myModel, false);
        handleDataChanged();
    }

    public void handleDataChanged() {
        myColHeader.handleDataChanged();
        myRowHeader.handleDataChanged();
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) (g);

        if (myModel != null) {
            Rectangle drawHere = g.getClipBounds();
            GridVisibleArea a = myModel.getVisibleGrid(drawHere);
            // g2.setColor(Color.GREEN);
            // g.fillRect(drawHere.x + 10, drawHere.y + 10, drawHere.width - 20, drawHere.height - 20);

            for (int row = a.startRow; row <= a.lastPartialRow; row++) {
                for (int col = a.startCol; col <= a.lastPartialColumn; col++) {

                    int x = myModel.getCellWidth() * col;
                    int y = myModel.getCellHeight() * row;
                    int w = myModel.getCellWidth();
                    int h = myModel.getCellHeight();
                    myModel.drawCellBox(g, row, col,
                            x, y, w, h);
                }
            }
        }
    }

    //Methods required by the MouseMotionListener interface:
    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    @Override
    public Dimension getPreferredSize() {
        int w = myModel.getWidth();
        int h = myModel.getHeight();
        return new Dimension(w, h);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation,
            int direction) {
        //Get the current position.
        int currentPosition = 0;
        int cellSize;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
            cellSize = myModel.getCellWidth();
        } else {
            currentPosition = visibleRect.y;
            cellSize = myModel.getCellHeight();
        }

        //Return the number of pixels between currentPosition
        //and the nearest tick mark in the indicated direction.
        if (direction < 0) {
            int newPosition = currentPosition
                    - (currentPosition / cellSize)
                    * cellSize;
            return (newPosition == 0) ? cellSize : newPosition;
        } else {
            return ((currentPosition / cellSize) + 1)
                    * cellSize
                    - currentPosition;
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation,
            int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            int cellSize = myModel.getCellWidth();
            return visibleRect.width - cellSize;
        } else {
            int cellSize = myModel.getCellWidth();
            return visibleRect.height - cellSize;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}