package org.wdssii.gui.products;

import javax.swing.JScrollPane;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.gui.features.ProductFeature;
import org.wdssii.gui.swing.ProductTableModel;
import org.wdssii.gui.swing.SimpleTable;

/** Helper class for containing information on how to display this product
 * in the TableProductView.
 * 
 * @author Robert Toomey
 * 
 */
public class Product2DTable {
    
    // The default 2D table for a product uses the virtual table for
    // really large datasets.  This is default for RadialSets, etc.
    // There's not much to this table other than it is purely virtual and
    // has no O(n) column/row stuff.
    private ProductTableModel myTableModel;
    private SimpleTable jProductDataTable;
    
    /* Create a brand new table in given scrollpane.  Humm, we might
     * want to save some information...or maybe not.
     */
    public void createInScrollPane(JScrollPane scrollPane, ProductFeature p,
	    int mouseMode){
        
       // Default is to make a virtual table..
       myTableModel = new ProductTableModel();
       jProductDataTable = new SimpleTable(scrollPane, 100,100);
       jProductDataTable.setMode(mouseMode);
       jProductDataTable.setupScrollPane(scrollPane);
       jProductDataTable.setModel(myTableModel);
       myTableModel.setProductFeature(p);
       scrollPane.revalidate();
       scrollPane.repaint();
       //jProductDataTable.revalidate();
      // jProductDataTable.repaint();
       
    }
    
    /** Return a visible grid.  This is used to draw the outline of the
     * displayed table within the 3D world ball view 
     */
    public GridVisibleArea getCurrentVisibleGrid() {
        GridVisibleArea a= null;
        if (myTableModel != null){
            return myTableModel.getCurrentVisibleGrid();
        }
        return null;
    }

    /** Set the mouse 'mode', controlled by the TableView */
    public void setMode(int myMouseMode) {
        jProductDataTable.setMode(myMouseMode);
    }
}
