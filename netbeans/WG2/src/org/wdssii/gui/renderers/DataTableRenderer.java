package org.wdssii.gui.renderers;

import java.awt.Point;
import java.util.ArrayList;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.gui.GLWorld;
import org.wdssii.geom.Location;
import org.wdssii.geom.V3;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Renders a DataTable in a opengl  framework
 *
 * Lots of mess here, will need cleanup, redesign, etc....just trying to get it
 * to work at moment...
 *
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private final static Logger LOG = LoggerFactory.getLogger(DataTableRenderer.class);
    
    /**
     * Store world points for a DataTable
     */
    private ArrayList<V3> myWorldPoints = null;
    private ArrayList<V3> myWorldPoints2 = null;

    private PointRenderer myPointRenderer = new PointRenderer();
    
    private static boolean refreshRenderers = true;

    public DataTableRenderer() {
        super(true);
    }

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

        ArrayList<Location> locations = aDataTable.getLocations();
        ArrayList<Location> locations2 = aDataTable.getLocations2();
        
        Symbology s = p.getSymbology();
        String column = s.getCategories().column;        
        Column symbolColumn = aDataTable.getColumnByName(column);
        
        if (symbolColumn != null){
            symbolColumn.createIndex();
        }

        /** Assemble points projected to world view */
        if ((myWorldPoints == null) || refreshRenderers) {
            myWorldPoints = new ArrayList<V3>();
            myWorldPoints2 = new ArrayList<V3>();
            
            int row = 0;
            for (Location l : locations) {
                V3 worldPoint = w.projectLLH(l.getLatitude(), l.getLongitude(), l.getHeightKms()*1000.0);
                myWorldPoints.add(worldPoint);
                if (locations2 != null){
                    Location l2 = locations2.get(row);
                    V3 worldPoint2 = w.projectLLH(l2.getLatitude(), l2.getLongitude(), l2.getHeightKms()*1000.0);
                    myWorldPoints2.add(worldPoint2);
                }
                row++;
            }
            // sync?
            refreshRenderers = false;
        }

        myPointRenderer.draw(w, s, myWorldPoints, myWorldPoints2, symbolColumn);
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(GLWorld w, FeatureMemento m) {
        super.draw(w, m);
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

    @Override
    public void preRender(GLWorld w, FeatureMemento m) {
    }
    
    @Override
    public void pick(GLWorld w, Point p, FeatureMemento m) {
    }
}
