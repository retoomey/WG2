package org.wdssii.gui.products;

import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;
import javax.swing.JScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.geom.Location;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.ProductFeature;
import org.wdssii.gui.products.renderers.ProductRenderer;
import org.wdssii.gui.swing.ProductTableModel;
import org.wdssii.gui.swing.SimpleTable;

/** Helper class for containing information on how to display this product
 * in the TableProductView.
 * 
 * @author Robert Toomey
 * 
 */
public class Product2DTable implements Feature3DRenderer {

	private static Logger log = LoggerFactory.getLogger(Product2DTable.class);
	// The default 2D table for a product uses the virtual table for
	// really large datasets.  This is default for RadialSets, etc.
	// There's not much to this table other than it is purely virtual and
	// has no O(n) column/row stuff.
	private ProductTableModel myTableModel;
	private SimpleTable jProductDataTable;
	private ProductFeature myProductFeature;

	/* Create a brand new table in given scrollpane.  Humm, we might
	 * want to save some information...or maybe not.
	 */
	public void createInScrollPane(JScrollPane scrollPane, ProductFeature p,
		int mouseMode) {

		// Default is to make a virtual table..
		myProductFeature = p;
		myTableModel = new ProductTableModel();
		jProductDataTable = new SimpleTable(scrollPane, 100, 100);
		jProductDataTable.setMode(mouseMode);
		jProductDataTable.setupScrollPane(scrollPane);
		jProductDataTable.setModel(myTableModel);
		myTableModel.setProductFeature(p);
		scrollPane.revalidate();
		scrollPane.repaint();
		//jProductDataTable.revalidate();
		// jProductDataTable.repaint();

	}

	public void updateTable() {
		myTableModel.checkDataAvailability();
		jProductDataTable.repaint();
	}

	public void centerToLocation(Location loc) {
		if (myProductFeature != null) {
			DataType dt = myProductFeature.getLoadedDatatype();
			if (dt instanceof Table2DView) {
				Table2DView t = (Table2DView) (dt);
				Table2DView.Cell aCell = new Table2DView.Cell();
				t.getCell(loc, aCell);
				jProductDataTable.scrollToCenter(aCell.row, aCell.col);
			}
		}
	}

	/** Return a visible grid.  This is used to draw the outline of the
	 * displayed table within the 3D world ball view 
	 */
	public GridVisibleArea getCurrentVisibleGrid() {
		GridVisibleArea a = null;
		if (myTableModel != null) {
			return myTableModel.getCurrentVisibleGrid();
		}
		return null;
	}

	/** Return the selected grid.
	 */
	public GridVisibleArea getSelectedGrid() {
		GridVisibleArea a = null;
		if (myTableModel != null) {
			return myTableModel.getSelectionGrid();
		}
		return null;
	}

	/** Set the mouse 'mode', controlled by the TableView */
	public void setMode(int myMouseMode) {
		jProductDataTable.setMode(myMouseMode);
	}

	/** Beginning of export ability */
	public void exportToURL(URL aURL) {
		if (myProductFeature != null) {
			DataType dt = myProductFeature.getLoadedDatatype();
			if (dt instanceof Table2DView) {
				Table2DView t = (Table2DView) (dt);
				GridVisibleArea g = getSelectedGrid();
				if (g != null) {
					t.exportToURL(aURL, g);
				}
			}
		}
	}

	 /* Not sure if the Table should draw this or the ProductRenderer...
	 * We actually have the selected grid which is based on our
	 * table layout, since we have will have multiple tables, might be good to at
	 * least draw through us.  This would allow tables to share
	 * the product renderer at least (n--> 1).  So if we had 5 tables
	 * looking at same product, we could draw 5 outlines representing each
	 * table..
	 * @param dc
	 * @param m 
	 */
	@Override
	public void draw(DrawContext dc, FeatureMemento m) {
		// Delegate to the product feature product renderer....
		if (myProductFeature != null) {
			Product p = myProductFeature.getProduct();
			if (p != null) {
				ProductRenderer r = p.getRenderer();
				if (r != null) {
					r.drawProductOutline(dc);
				}
			}
		}
	}
}
