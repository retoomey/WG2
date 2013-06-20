package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.pick.PickSupport;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.geom.GLWorld;
import org.wdssii.geom.Location;
import org.wdssii.geom.V3;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.features.Feature;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Renders a DataTable in a worldwind window
 *
 * Lots of mess here, will need cleanup, redesign, etc....just trying to get it
 * to work at moment...
 *
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

  //  private ArrayList<BaseIconAnnotation> myIcons = new ArrayList<BaseIconAnnotation>();
    private final static Logger LOG = LoggerFactory.getLogger(DataTableRenderer.class);
    //public static BasicAnnotationRenderer myRenderer = new BasicAnnotationRenderer();
    private ColorMap myTextColorMap = null;
    private ColorMap myPolygonColorMap = null;
    // Not sure this should be here....
    private PickSupport pickSupport = new PickSupport();

    /**
     * Store world points for a DataTable
     */
    private ArrayList<V3> myWorldPoints = null;
    private PointRenderer myPointRenderer = new PointRenderer();
    ;
    private static boolean refreshRenderers = true;
    //private static Symbol hackme = new StarSymbol();

    public DataTableRenderer() {
        super(true);
    }

    //public static Symbol getHackMe() {
    //    return hackme;
    //}
    //public static void setHackMe(Symbol s) {
    //    refreshRenderers = true;
    //    hackme = s;
    // }
    @Override
    public WdssiiJobStatus createForDatatype(GLWorld w, Product aProduct, WdssiiJobMonitor monitor) {

        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        AnimateManager.updateDuringRender();
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(GLWorld w, boolean pickMode, java.awt.Point pickPoint) {
        if (pickMode) {
            return;
        }
        Product p = getProduct();
        if (p == null) {
            return;
        }
        DataTable aDataTable = (DataTable) p.getRawDataType();

        //Globe g = dc.getGlobe();

        ArrayList<Location> locations = aDataTable.getLocations();

        Symbology s = p.getSymbology();
        String column = s.getCategories().column;        
        Column symbolColumn = aDataTable.getColumnByName(column);
        //  TreeMap<String, SymbolRenderer> myCategoryRenderer = new TreeMap<String, SymbolRenderer>();
        // We'd set up from xml, creating a symbol renderer per class
        // "hail" --> StarSymbolRenderer...

        // Project each row in table to a world point
        // Warning: If you change project (round to flat, etc.. need to 
        // recalculate)


        if ((myWorldPoints == null) || refreshRenderers) {
            myWorldPoints = new ArrayList<V3>();
            // Only needed if renderer can change per point (such as category)
            //  myRenderers = new ArrayList<SymbolRenderer>();
            int row = 0;
            for (Location l : locations) {
                //Vec4 worldPoint = g.computePointFromPosition(
                //        Angle.fromDegrees(l.getLatitude()),
                //        Angle.fromDegrees(l.getLongitude()),
                //        l.getHeightKms() * 1000.0);
                V3 worldPoint = w.projectLLH(l.getLatitude(), l.getLongitude(), l.getHeightKms()*1000.0);
                myWorldPoints.add(worldPoint);
                row++;
            }
            // sync?
            refreshRenderers = false;
        }

        myPointRenderer.draw(w, s, myWorldPoints, symbolColumn);
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(GLWorld w) {
        drawData(w, false, null);
    }

    /**
     * Pick an object in the current dc at point
     */
    @Override
    public void doPick(GLWorld w, java.awt.Point pickPoint) {
        drawData(w, true, pickPoint);
    }

    @Override
    public void highlightObject(Object o) {
       
    }

    @Override
    public boolean canOverlayOtherData() {
        return true;
    }

    @Override
    public int getFeatureRank() {
        return Feature.POINT;  // Draw over others
    }
}
