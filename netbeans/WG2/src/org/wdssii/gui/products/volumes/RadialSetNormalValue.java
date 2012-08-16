package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;

/**
 * Our 'normal' value, which just uses a first hit bottom up within beamwidth of
 * RadialSetVolume
 *
 * @author Robert Toomey
 */
public class RadialSetNormalValue extends VolumeValue {

	@Override
	public boolean getValueAt(Object myRadialLock, ArrayList<Product> p, Location loc,
		ColorMap.ColorMapOutput output, DataFilter.DataValueRecord out,
		FilterList list, boolean useFilters) {

		// Maybe this could be a filter in the color map...  You could clip anything by height heh heh..
		// It would make sense for it to be a filter
		if (loc.getHeightKms() < 0) {
			output.setColor(255, 255, 255, 255);
			//output.red = output.green = output.blue = output.alpha = 255;
			output.filteredValue = 0.0f;
			return false;
		}

		float v1 = DataType.MissingData;
		RadialSet.RadialSetQuery q = new RadialSet.RadialSetQuery();
		q.inLocation = loc;
		q.outDataValue = DataType.MissingData;
		RadialSet.SphericalLocation buffer = new RadialSet.SphericalLocation();

		// Poor man's vslice..just grab the first thing NOT missing lol...
		// This is actually slowest when there isn't any data...
		// Notice with 'overlap' the first radial dominates without any smoothing...
		// FIXME: could binary search the radial volume I think...
		int radialSetIndex = 0;
		boolean first = true;

		// Make sure the reading of data values is sync locked with updating in initProduct...
		synchronized (myRadialLock) {

			for (int i = 0; i < p.size(); i++) {
				DataType dt = p.get(i).getRawDataType();
				if (dt != null) {
					RadialSet r = (RadialSet) (dt);
					if (r != null) {
						// First time, get the location in object spherical coordinates.  This doesn't
						// change for any of the radials in the set.
						if (first) {
							r.locationToSphere(loc, buffer);
							q.inSphere = buffer;
							first = false;
						}
						r.queryData(q);
						// Cheapest...first 'hit' gives us value
						v1 = q.outDataValue;
						if (DataType.isRealDataValue(v1)) {
							break;
						}
					}
				}
				radialSetIndex++;
			}
		}
		q.inDataValue = v1;
		q.outDataValue = v1;
		q.outRadialSetNumber = radialSetIndex;
		out.hWeight = q.outDistanceHeight;
		list.fillColor(output, q, useFilters);

		// Find a location value in our radial set collection...
		return true;

	}

	@Override
	public String getName(){
		return "Radial Beamwidth";
	}
}
