package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;

/**
 * Object that just does the math/grab of data out of a volume. This class is
 * just to make the code more readable.
 *
 * @author Robert Toomey
 */
public abstract class VolumeValue {

	/** Must return a value for a location */
	public abstract boolean getValueAt(Object syncLock, ArrayList<Product> p, Location loc,
		ColorMap.ColorMapOutput output, DataFilter.DataValueRecord out,
		FilterList list, boolean useFilters);

	/** Must return a 'name' of this, used by GUI for menu picking of this object */
	public abstract String getName();
}
