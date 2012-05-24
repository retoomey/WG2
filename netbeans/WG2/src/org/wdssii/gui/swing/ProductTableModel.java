package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.event.AdjustmentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.datatypes.Table2DView.CellQuery;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.swing.SimpleTable.SimpleTableModel;
import org.wdssii.gui.views.WorldWindView;

/**
 * Links a Table2DView product to a SimpleTableModel
 * Currently one of these for all possible datatypes...we might
 * eventually create a factory by reflection so that we can draw
 * different stuff for different DataTypes.
 * 
 * @author Robert Toomey
 */
public class ProductTableModel extends SimpleTableModel {

	private static Logger log = LoggerFactory.getLogger(ProductTableModel.class);
	/** The Product feature we were created for */
	ProductFeature myProductFeature = null;
	/** Raw datatype we are viewing...which is lazy loading */
	Table2DView myView = null;

	/** Typically called once...FIXME: could put in constructor make final? */
	public void setProductFeature(ProductFeature p) {
		myView = null;
		myProductFeature = p;
	}

	/** Check for change in our displayed data...  */
	public void checkDataAvailability() {
		// Check for a new datatype
		Table2DView newOne = null;
		if (myProductFeature != null) {
			DataType dt = myProductFeature.getLoadedDatatype();
			if (dt != null) {
				if (dt instanceof Table2DView) {
					newOne = (Table2DView) (dt);
				}
			}
		}
		// Update table visually 
		if (newOne != myView) {
			myView = newOne;
			handleDataChanged();
		}
	}

	@Override
	public int getNumRows() {
		checkDataAvailability();
		if (myView != null) {
			return myView.getNumRows();
		} else {
			return 0;
		}
	}

	@Override
	public int getNumCols() {
		checkDataAvailability();
		if (myView != null) {
			return myView.getNumCols();
		} else {
			return 0;
		}
	}

	@Override
	public String getRowHeader(int row) {
		checkDataAvailability();
		if (myView != null) {
			return myView.getRowHeader(row);
		}
		return "";
	}

	@Override
	public SimpleTableRenderInfo getCellInfo(int row, int col,
		int x, int y, int w, int h) {

		// Note: we could subclass this to add info, then override
		// drawCell to use it
		checkDataAvailability();
		SimpleTableRenderInfo info = new SimpleTableRenderInfo();
		if (row == -1) {
			info.background = Color.DARK_GRAY;
			info.foreground = Color.WHITE;
			if (myView != null) {
				info.text = myView.getColHeader(col);
			} else {
				info.text = "?";
			}
		} else if (col == -1) {
			info.background = Color.DARK_GRAY;
			info.foreground = Color.WHITE;
			if (myView != null) {
				info.text = myView.getRowHeader(row);
			} else {
				info.text = "?";
			}
		} else {
			info.background = Color.WHITE;
			info.foreground = Color.BLACK;
			if (myView != null) {

				// All this should encapsulate somehow...
				DataTypeQuery dq = new DataTypeQuery();
				CellQuery cq = new CellQuery();
				// float value = myView.getCellValue(row, col);

				myView.getCellValue(row, col, cq);
				Color fore, back;
				if (cq.text == null) {
					FilterList list = myProductFeature.getFList();
					ColorMapOutput out = new ColorMapOutput();
					dq.inDataValue = dq.outDataValue = cq.value;
					if (out == null) {
						log.debug("out is null");
					}
					if (dq == null) {
						log.debug("dq is null");
					}
					if (list == null) {
						log.debug("list is null");
					}
					list.fillColor(out, dq, true);
					back = new Color(out.redI(), out.greenI(), out.blueI());
					fore = java.awt.Color.white;
					fore = ColorMap.getW3cContrast(back, fore);
					info.text = Product.valueToString(cq.value);
				} else {
					fore = java.awt.Color.BLACK;
					back = java.awt.Color.WHITE;
					info.text = cq.text;
				}
				info.background = back;
				info.foreground = fore;

			} else {
				info.text = "?";
			}
		}
		return info;
	}

	/** Get the col label for given col number */
	@Override
	public String getColHeader(int col) {
		checkDataAvailability();
		if (myView != null) {
			return myView.getColHeader(col);
		}
		return "";
	}

	@Override
	public void handleScrollAdjust(AdjustmentEvent e) {
		WorldWindView v = CommandManager.getInstance().getEarthBall();
		if (v != null) {
			v.updateOnMinTime();
		}
	}
}
