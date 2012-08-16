package org.wdssii.gui.products.volumes;

import java.util.ArrayList;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.filters.DataFilter;

/**
 * Binomial interpretation of RadialSet Volume...
 *
 * @author Robert Toomey
 */
public class RadialSetBIValue extends VolumeValue {

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

		// output.location.init(lat, lon, heightM / 1000.0);

		// Smooth in the vertical direction....?? how
		// We would need a weight based on range
		float v1 = DataType.MissingData;
		float w1;
		float w1r;

		float D = .01f;
		PPIRadialSet.PPIRadialSetQuery q = new PPIRadialSet.PPIRadialSetQuery();
		PPIRadialSet.PPIRadialSetQuery q2 = new PPIRadialSet.PPIRadialSetQuery();
		Location lminus = new Location(loc.getLatitude(), loc.getLongitude() - D, loc.getHeightKms());
		Location lplus = new Location(loc.getLatitude(), loc.getLongitude() + D, loc.getHeightKms());
		q.inLocation = lminus;
		q2.inLocation = lplus;
		q.inNeedInterpolationWeight = true;
		q2.inNeedInterpolationWeight = true;
		q.outDataValue = DataType.MissingData;
		q2.outDataValue = DataType.MissingData;

		PPIRadialSet.SphericalLocation buffer = new PPIRadialSet.SphericalLocation();
		PPIRadialSet.SphericalLocation buffer2 = new PPIRadialSet.SphericalLocation();

		// Poor man's vslice..just grab the first thing NOT missing lol...
		// This is actually slowest when there isn't any data...
		// Notice with 'overlap' the first radial dominates without any smoothing...
		// FIXME: could binary search the radial volume I think...
		int radialSetIndex = 0;
		boolean first = true;
		float oldWeight = 1.0f, oldValue = 0.0f;
		float oldWeightR = 1.0f, oldValueR = 0.0f;

		// Make sure the reading of data values is sync locked with updating in initProduct...
		synchronized (myRadialLock) {

			for (int i = 0; i < p.size(); i++) {
				DataType dt = p.get(i).getRawDataType();
				if (dt != null) {
					PPIRadialSet r = (PPIRadialSet) (dt);
					if (r != null) {
						// First time, get the location in object spherical coordinates.  This doesn't
						// change for any of the radials in the set.
						if (first) {
							r.locationToSphere(lminus, buffer);
							q.inSphere = buffer;
							r.locationToSphere(lplus, buffer2);
							q2.inSphere = buffer2;
							first = false;
						}
						r.queryData(q);
						r.queryData(q2);
						w1 = q.outDistanceHeight;
						w1r = q2.outDistanceHeight;

						// Binomial interpolation....

						// Interpolate in true height...
						// Since we go bottom up, we want the first beam we are UNDER
						// The part above 0 is handled by i+1...
						if (w1 <= 0) {
							// If there's a radial under us....
							// then use the weight/value of it....
							if (i > 0) {

								DataType dt2 = p.get(i - 1).getRawDataType();
								if (dt2 != null) {
									PPIRadialSet r2 = (PPIRadialSet) (dt2);
									if (r2 != null) {
										float v2 = oldValue;
										float v2r = oldValueR;
										if (DataType.isRealDataValue(v2)) {
											if (DataType.isRealDataValue(v2r)) {
												v1 = q.outDataValue;
												float w2 = oldWeight;

												// Interpolation....
												// Ok now we have 2 weights...
												// and 2 values...
												// This is R2 and R1 of the Binomail interpolation
												//r1 = value,  r2 = v2;  
												float totalWeight = Math.abs(w2) + Math.abs(w1);
												float i1 = Math.abs(w2 / totalWeight) * v1;
												float i2 = Math.abs(w1 / totalWeight) * v2;

												//  float vInterp = (Math.abs(w2/totalWeight)*q.outDataValue)+(Math.abs(weightAtValue/totalWeight)*v2); 
												float vInterp = i1 + i2;

												float v1R = q2.outDataValue;
												float w2R = oldWeightR;
												float totalWeightR = Math.abs(w2R) + Math.abs(w1r);
												float i3 = Math.abs(w2R / totalWeightR) * v1R;
												float i4 = Math.abs(w1r / totalWeightR) * v2r;

												float vInterpR = i3 + i4;

												float totalWeightMid = Math.abs(D) + Math.abs(D);
												float i5 = Math.abs(D / totalWeightMid) * vInterp;
												float i6 = Math.abs(D / totalWeightMid) * vInterpR;
												v1 = i5 + i6;
												//v1 = vInterp+vInterpR;
												//v1 = vInterpR;
											}
										}
									}
								}
							}

							break;

						}
						oldValue = q.outDataValue;
						oldWeight = q.outDistanceHeight;
						oldValueR = q2.outDataValue;
						oldWeightR = q2.outDistanceHeight;
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
		return "Radial Binomial Interpolation";
	}
}
