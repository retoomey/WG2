package org.wdssii.gui.charts;

import org.jfree.data.DomainOrder;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;

/**
 * DynamicXYZDataset is a JFreeChart dataset where we recreate a fixed 'grid' of
 * values for a set zoom level. For example, in JFreeChart if you zoom in
 * normally the resolution remains the same. This resamples as you zoom in or
 * out.
 *
 * TerrainXYZDataset in the VSlice chart uses this to increase the terrain
 * detail as you zoom in. DataRangeValueChart uses this to increase readout
 * samples as you zoom in.
 */
public class DynamicXYZDataset implements XYZDataset {

    /**
     * Fixed grid sample size PER ZOOM LEVEL. For any zoom level, this is the
     * resolution of samples we will have.
     */
    public int mySampleSize;
    /* A way to turn off visibility by pretending we have zero samples */
    private boolean myShowSamples;
    /**
     * Storage for sample values
     */
    private double[] mySamples;
    /**
     * The 'delta' range per sample point of axis
     */
    private double myXDelta;
    /**
     * The starting range location (left side) of range line
     */
    private double myXLower;
    /**
     * The key for this series of data
     */
    private String mySeriesKey;

    public DynamicXYZDataset(String key, int size) {
        mySamples = new double[size]; // Fixme: what if fails?
        mySampleSize = size;
        mySeriesKey = key;
    }

    public void setShowSamples(boolean flag) {
        myShowSamples = flag;
    }

    public void clearRange() {
        setShowSamples(false);
    }

    public int getSampleSize() {
        return mySampleSize;
    }

    public void setSample(int index, double value) {
        mySamples[index] = value;
    }

    public void setRange(Range r) {
        myXLower = r.getLowerBound();
        double distance = r.getUpperBound() - myXLower;
        myXDelta = distance / (mySampleSize - 1);
        setShowSamples(true);
    }

    @Override
    public Number getZ(int series, int item) {
        return new Double(getZValue(series, item));
    }

    @Override
    public double getZValue(int series, int item) {
        // This number is meaningless since the 'value' is
        // the same as the height, we might find a use for this later
        return 0;
    }

    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    @Override
    public int getItemCount(int i) {
        if (myShowSamples) {
            return mySampleSize;
        } else {
            return 0;
        }
    }

    @Override
    public Number getX(int series, int item) {
        return new Double(getXValue(series, item));
    }

    @Override
    public double getXValue(int series, int item) {
        double x = myXLower + (myXDelta * item);
        return x;
    }

    @Override
    public Number getY(int series, int item) {
        return new Double(getYValue(series, item));
    }

    @Override
    public double getYValue(int series, int item) {
        return mySamples[item];
    }

    @Override
    public int getSeriesCount() {
        return 1; // just 1 terrain height per range
    }

    @Override
    public Comparable getSeriesKey(int i) {
        return mySeriesKey;
    }

    @Override
    public int indexOf(Comparable cmprbl) {
        return 0;
    }

    @Override
    public void addChangeListener(DatasetChangeListener dl) {
        // ignore
    }

    @Override
    public void removeChangeListener(DatasetChangeListener dl) {
        // ignore
    }

    @Override
    public DatasetGroup getGroup() {
        return null;
    }

    @Override
    public void setGroup(DatasetGroup dg) {
        // ignore
    }
}