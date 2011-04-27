package org.wdssii.gui.volumes;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

public class LLHAreaScreenSizeDetailLevel extends AVListImpl implements LLHAreaDetailLevel {

    private static final double DEFAULT_MIN_SIZE = 40.0;
    private static final double DEFAULT_MAX_SIZE = 700.0;
    private final double screenSize;
    private final String key;

    public LLHAreaScreenSizeDetailLevel(double minimumScreenSize, String key) {
        this.screenSize = minimumScreenSize;
        this.key = key;
    }

    public double getScreenSize() {
        return this.screenSize;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        LLHAreaScreenSizeDetailLevel that = (LLHAreaScreenSizeDetailLevel) o;
        return Double.compare(this.screenSize, that.screenSize) == 0;
    }

    @Override
    public int compareTo(LLHAreaDetailLevel level) {
        if (this == level) {
            return 0;
        }
        if (level == null || this.getClass() != level.getClass()) {
            return -1;
        }

        LLHAreaScreenSizeDetailLevel that = (LLHAreaScreenSizeDetailLevel) level;
        return Double.compare(this.screenSize, that.screenSize);
    }

    @Override
    public int hashCode() {
        long temp = this.screenSize != +0.0d ? Double.doubleToLongBits(this.screenSize) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return this.key;
    }

    @Override
    public boolean meetsCriteria(DrawContext dc, LLHArea airspace) {
        if (dc == null) {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (dc.getView() == null) {
            String message = "nullValue.DrawingContextViewIsNull";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Extent extent = airspace.getExtent(dc);
        if (extent == null) {
            return false;
        }

        double d = dc.getView().getEyePoint().distanceTo3(extent.getCenter());
        double pixelSize = dc.getView().computePixelSizeAtDistance(d);
        double shapeScreenSize = extent.getDiameter() / pixelSize;
        return shapeScreenSize < this.screenSize;
    }

    public static double[] computeDefaultScreenSizeRamp(int levels) {
        return computeLinearScreenSizeRamp(levels, DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE);
    }

    public static double[] computeLinearScreenSizeRamp(int levels, double min, double max) {
        double[] ramp = new double[levels];
        double a;

        for (int i = 0; i < levels; i++) {
            a = (double) i / (double) (levels - 1);
            ramp[levels - i - 1] = min + a * (max - min);
        }

        return ramp;
    }
}
