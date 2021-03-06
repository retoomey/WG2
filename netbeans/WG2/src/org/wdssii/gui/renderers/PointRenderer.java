package org.wdssii.gui.renderers;

import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.datatypes.AttributeTable.AttributeColumn;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.symbology.SymbolFactory;
import org.wdssii.gui.renderers.SymbolRenderer;
import org.wdssii.gui.renderers.SymbolRenderer.SymbolRectangle;
import org.wdssii.storage.Interval;
import org.wdssii.storage.IntervalTree;
import org.wdssii.xml.iconSetConfig.Category;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Renders points in lat/lon using our symbology and attribute table
 *
 * For now this will do: SingleSymbol Category:UniqueValues Might break up more
 * later
 *
 * FIXME: Need to clean this up...all experimental code right now so it's a mess
 *
 * @author Robert Toomey
 */
public class PointRenderer {

    private final static Logger LOG = LoggerFactory.getLogger(PointRenderer.class);
    public final static String NO_CATEGORY = "no_category";
    private List<SymbolRenderer> myRenderers = null;
    private SymbolRenderer myFailSafe = null;
    private SymbolRenderer mySingle = null;
    private AttributeColumn myColumn = null; // Column if category, null if single
    private final Object myRefreshLock = new Object();
    private boolean myRefresh = false;
    private Symbology mySymbology = null;
    private TreeMap<String, SymbolRenderer> myCategoryLookup;
    private List<String> myCategoryKeys;
    private TreeMap<String, List<CatLookup>> myGroups;
    /**
     * Toggle from single for all to categories...
     */
    private boolean tryCategories = false;

    public void refreshRenderers() {
        synchronized (myRefreshLock) {
            myRefresh = true;
        }
    }

    public static class CatLookup {

        public CatLookup(int r, V3 a3d) {
            row = r;
            the3D = a3d;
        }
        final int row;
        final V3 the3D;
    }

    public static class Node {

        // FIXME: Use a row 'list' for all merged objects..
        // ArrayList<Integer> theRows;
        public Node(V2 p, int row, int count) {
            thePoint = p;
            theRow = row;
            myCount = count;
        }

        public Node(V2 p, int row) {
            thePoint = p;
            theRow = row;
        }
        public V2 thePoint;
        public int theRow;
        /**
         * How many merged nodes make this rectangle?
         */
        public int myCount = 1;
        public SymbolRectangle myCoverage;
    }

    public void checkSymbology(Symbology s) {

        // This is set with a copy from GUI on change, so sync shouldn't be an issue...
        boolean changed = (s != mySymbology);
        // Create a fall back default if symbology fails...
        if (myFailSafe == null) {
            Symbol p;
            PolygonSymbol ps = new PolygonSymbol();
            ps.pointsize = 12;
            ps.toSquare();
            p = ps;
            SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(p);
            renderer.setSymbol(p);
            myFailSafe = renderer;
            mySingle = myFailSafe;
        }

        // Check if change and update
        if (changed) {

            mySymbology = s;

            if (s != null) {
                myCategoryKeys = new ArrayList<String>();
                myCategoryKeys.add(NO_CATEGORY);
                // Read single from Symbology
                List<Symbol> aList = mySymbology.getSingle().symbols;
                if ((aList != null) && (!aList.isEmpty())) {
                    Symbol sym = aList.get(0);
                    SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(sym);
                    renderer.setSymbol(sym);
                    mySingle = renderer;
                }
            } else {
                mySingle = myFailSafe;
            }

            // Create a lookup table from key to renderer
            if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {
                // Create a new lookup when symbology changes...
                myCategoryLookup = new TreeMap<String, SymbolRenderer>();
                myCategoryKeys = new ArrayList<String>();
                myCategoryKeys.add(NO_CATEGORY);
                // Creating every single time....bleh....
                // Should create a map lookup String -> renderer when
                // symbology changes.....
                List<Category> cats = s.getCategories().getCategoryList();
                String key;
                for (Category c : cats) {
                    key = c.value;
                    myCategoryKeys.add(key);
                    SymbolRenderer r = null;
                    Symbol aSymbol = c.symbols.get(0);  // bleh
                    if (aSymbol != null) {
                        r = SymbolFactory.getSymbolRenderer(aSymbol);
                        r.setSymbol(aSymbol);
                    }
                    myCategoryLookup.put(key, r);
                }

            }
        }
    }

    /**
     * Given DrawContext and collection of points and symbology, draw these
     * points
     */
    public void draw(GLWorld w, Symbology s, List<V3> points, List<V3> points2, AttributeColumn theColumn) {

        myColumn = theColumn;
        checkSymbology(s);

        // Pre non-opengl first...
        TextRenderer aText = null;
        Font font = new Font("Arial", Font.PLAIN, 14);
        if (aText == null) {
            aText = new TextRenderer(font, true, true);
        }

        // Draw using the symbols...
        GLUtil.pushOrtho2D(w);
        w.gl.glDisable(GL.GL_DEPTH_TEST);
        if (mySymbology.merge == Symbology.MERGE_CATEGORIES) {
            renderMergedCategories(w, aText, points, s);
        } else {
            renderNormal(w, aText, points, points2, s);
        }
        GLUtil.popOrtho2D(w.gl);
    }

    /**
     * Standard render of all points, looking up renderer for each
     */
    public void renderNormal(GLWorld w, TextRenderer text, List<V3> points, List<V3> points2, Symbology s) {

        /**
         * Draw extra line for now between two points
         */
        final boolean drawLine = ((points2 != null)
                && (points2.size() == points.size())
                && (s.use2ndlatlon == Symbology.USE_2ND_LAT_LON));

        // Just get symbology once for single mode...
        boolean updateRenderer = true;
        SymbolRenderer item = null;
        if ((s.use == Symbology.SINGLE) && (points.size() > 0)) {
            item = getRenderer(0, s);
            updateRenderer = false;
        }

        int row = 0;
        final GL glold = w.gl;
    	final GL2 g = glold.getGL().getGL2();

        for (V3 at3D : points) {
            V2 at2D = w.project(at3D);
            if (updateRenderer) {
                item = getRenderer(row, s);
            }

            /**
             * Draw line from first lat,lon to second lat, lon. Special feature
             * request by Lak
             */
            if (drawLine) {
                V3 at2nd3D = points2.get(row);
                V2 at2 = w.project(at2nd3D);
                g.glLineWidth(3);

                g.glColor4f(1, 1, 1, 1);
                g.glBegin(GL.GL_LINES);
                g.glVertex2d(at2D.x, at2D.y);
                g.glVertex2d(at2.x, at2.y);
                g.glEnd();
            }

            /**
             * Draw point symbol
             */
            g.glTranslated(at2D.x, at2D.y, 0);
            item.render(w.gl);
            g.glTranslated(-at2D.x, -at2D.y, 0);

            //text.begin3DRendering();
            //GLUtil.cheezyOutline(text, Integer.toString(row), Color.WHITE, Color.BLACK, (int) at2D.x + 20, (int) at2D.y - 20);
            //text.end3DRendering();
            row++;
        }
    }

    /**
     * Render point data using my merging level of detail algorithm
     *
     * @author Robert Toomey
     * @param v
     * @param gl
     * @param text
     * @param points
     * @param s
     */
    public void renderMergedCategories(GLWorld w, TextRenderer text, List<V3> points, Symbology s) {

        // Merge tree algorithm.
        int row = 0;

        // Create render groups...we will group by common symbol renderer
        // FIXME: This should be done before render pass probably....
        myGroups = new TreeMap<String, List<CatLookup>>();
        for (V3 at3D : points) {
            String catKey = NO_CATEGORY;
            catKey = getCategory(row, mySymbology);

            // Add this cat key to the collection...
            List<CatLookup> list = myGroups.get(catKey);
            if (list == null) {
                list = new ArrayList<CatLookup>();
                myGroups.put(catKey, list);
            }
            CatLookup c = new CatLookup(row, at3D);
            list.add(c);

            row++;
        }

        //for (String category : myGroups.keySet()) {
        try {
            for (String category : myCategoryKeys) {
                List<CatLookup> theLookup = myGroups.get(category);
                // There could be no symbols in this category...
                if (theLookup != null) {
                    // This monster is for a 2 dimension interval tree..
                    IntervalTree<IntervalTree<Node>> theMergeTree = new IntervalTree<IntervalTree<Node>>();
                    // row = 0;
                    for (CatLookup cat : theLookup) {
                        //for (Vec4 at3D : thePoints) {

                        // Project into GL to get the rectangle of the actual drawn symbol
                        //Vec4 at2D = v.project(cat.the3D);
                        V2 at2D = w.project(cat.the3D);
                        SymbolRenderer item = getRenderer(cat.row, s);
                        SymbolRectangle r = item.getSymbolRectangle((int) at2D.x, (int) at2D.y);

                        // Initial node information
                        Interval xRange = new Interval(r.x, r.x2);
                        Interval yRange = new Interval(r.y, r.y2);
                        Node new1 = new Node(at2D, cat.row, 1);
                        new1.myCoverage = r;

                        boolean hittingRectangles = true;
                        while (hittingRectangles) {  // Gobbling up rectangles
                            hittingRectangles = false;

                            List<Interval> xOverlapSet = theMergeTree.getOverlappingIntervals(xRange);
                            if (xOverlapSet.size() > 0) {
                                // Eat the old ranges into the new rectangle...
                                for (Interval x : xOverlapSet) {

                                    IntervalTree<Node> ySet = theMergeTree.get(x);
                                    List<Interval> yOverlapSet = ySet.getOverlappingIntervals(yRange);

                                    // Hit x range AND y range
                                    if (yOverlapSet.size() > 0) {
                                        hittingRectangles = true;

                                        // Expand rectangle to cover this x range plus ourselves...
                                        r.x = Math.min(r.x, x.getStart());
                                        r.x2 = Math.max(r.x2, x.getEnd());

                                        // For each hit rectangle, increase the hit count...
                                        // Mark the old rectangle for removal.
                                        List<Interval> toDelete = new ArrayList<Interval>();
                                        for (Interval y : yOverlapSet) {
                                            Node data = ySet.get(y);
                                            // New rectangle represents all the data that came before...
                                            new1.myCount += data.myCount;
                                            r.y = Math.min(r.y, y.getStart());
                                            r.y2 = Math.max(r.y2, y.getEnd());
                                            toDelete.add(y);
                                        }
                                        // and then remove old rectangles...
                                        for (Interval i : toDelete) {
                                            ySet.delete(i);
                                        }
                                    }
                                }
                            }

                            if (hittingRectangles) {
                                xRange = new Interval(r.x, r.x2);
                                yRange = new Interval(r.y, r.y2);
                                at2D = new V2((r.x2 + r.x) / 2, (r.y2 + r.y) / 2);

                                new1.thePoint = at2D;
                                new1.theRow = cat.row;
                                r.centerx = (int) at2D.x;
                                r.centery = (int) at2D.y;
                            }
                        }


                        // Insert the final rectangle....
                        IntervalTree<Node> existing = theMergeTree.get(xRange);
                        if (existing == null) {
                            existing = new IntervalTree<Node>();
                            theMergeTree.insert(xRange, existing);
                        }
                        existing.insert(yRange, new1);
                        //  LOG.debug("while done");
                        // row++;
                        // if (row > MAX) {
                        //    break;
                        // }
                    }

                    // Render pass with interval tree...
                    Iterable<IntervalTree<Node>> XSets = theMergeTree.getValues();
                    int xcount = 0;
                    int ycount = 0;
                    final GL glold = w.gl;
                	final GL2 g = glold.getGL().getGL2();
                    for (IntervalTree<Node> x : XSets) {
                        Iterable<Node> YSet = x.getValues();
                        for (Node y : YSet) {
                            ycount++;
                            g.glTranslated(y.thePoint.x, y.thePoint.y, 0);
                            SymbolRenderer sr = getRenderer(y.theRow, s);
                            sr.render(w.gl);

                            g.glTranslated(-y.thePoint.x, -y.thePoint.y, 0);
                            if (y.myCount > 1) {
                                sr.renderSymbolRectangle(w.gl, y.myCoverage);
                            }
                        }
                        xcount++;
                    }

                    // Separate label pass if wanted....
                    text.begin3DRendering();
                    for (IntervalTree<Node> x : XSets) {
                        Iterable<Node> YSet = x.getValues();
                        for (Node y : YSet) {
                            if (y.myCount > 1) {
                                int tx = y.myCoverage.x2;
                                int ty = y.myCoverage.y;
                                GLUtil.cheezyOutline(text, Integer.toString(y.myCount), Color.WHITE, Color.BLACK, tx, ty);

                            }
                        }
                    }
                    text.end3DRendering();
                }
            }
        } catch (Exception e) {
            LOG.debug("Render error during interval tree point merger..." + e.toString());
        }
    }

    public String getCategory(int row, Symbology s) {
        String category = NO_CATEGORY;

        if (s != null) {

            // Simple single mode...
            if (s.use == Symbology.SINGLE) {
                category = NO_CATEGORY;

                // Category 
            } else if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {

                if (myColumn != null) {
                    category = myColumn.getValue(row);
                    SymbolRenderer rr = myCategoryLookup.get(category);  // Could be null, it's ok
                    if (rr == null) {
                        category = NO_CATEGORY;
                    }
                }
            }
        }
        return category;

    }

    public SymbolRenderer getRenderer(int row, Symbology s) {
        SymbolRenderer rr = null;

        if (s != null) {

            // Simple single mode...
            if (s.use == Symbology.SINGLE) {
                rr = mySingle;

                // Category 
            } else if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {

                if (myColumn != null) {
                    String text = myColumn.getValue(row);
                    rr = myCategoryLookup.get(text);  // Could be null, it's ok
                    if (rr == null) { // no category found...
                        rr = mySingle; // then try the single one...
                    }
                }
            }
        }
        if (rr == null) {
            rr = myFailSafe; // Fall back renderer
        }
        return rr;
    }
}
