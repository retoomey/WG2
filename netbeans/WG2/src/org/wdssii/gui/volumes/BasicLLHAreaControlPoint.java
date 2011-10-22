package org.wdssii.gui.volumes;

import org.wdssii.gui.worldwind.LLHAreaLayer;

import gov.nasa.worldwind.geom.Vec4;

public class BasicLLHAreaControlPoint implements LLHAreaControlPoint {

    public static class BasicControlPointKey {

        private int locationIndex;
        private int altitudeIndex;

        public BasicControlPointKey(int locationIndex, int altitudeIndex) {
            this.locationIndex = locationIndex;
            this.altitudeIndex = altitudeIndex;
        }

        public int getLocationIndex() {
            return this.locationIndex;
        }

        public int getAltitudeIndex() {
            return this.altitudeIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }

            BasicControlPointKey that = (BasicControlPointKey) o;
            return (this.locationIndex == that.locationIndex) && (this.altitudeIndex == that.altitudeIndex);
        }

        @Override
        public int hashCode() {
            int result = this.locationIndex;
            result = 31 * result + this.altitudeIndex;
            return result;
        }
    }
    private LLHArea airspace;
    private int locationIndex;
    private int altitudeIndex;
    private Vec4 point;

    public BasicLLHAreaControlPoint(LLHAreaLayer editor, LLHArea airspace, int locationIndex, int altitudeIndex,
            Vec4 point) {
        //this.editor = null;
        this.airspace = airspace;
        this.locationIndex = locationIndex;
        this.altitudeIndex = altitudeIndex;
        this.point = point;
    }

    public BasicLLHAreaControlPoint(LLHAreaLayer editor, LLHArea airspace, Vec4 point) {
        this(editor, airspace, -1, -1, point);
    }

    @Override
    public LLHArea getAirspace() {
        return this.airspace;
    }

    @Override
    public int getLocationIndex() {
        return this.locationIndex;
    }

    @Override
    public int getAltitudeIndex() {
        return this.altitudeIndex;
    }

    @Override
    public Vec4 getPoint() {
        return this.point;
    }

    @Override
    public Object getKey() {
        return keyFor(this.locationIndex, this.altitudeIndex);
    }

    public static Object keyFor(int locationIndex, int altitudeIndex) {
        return new BasicControlPointKey(locationIndex, altitudeIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        BasicLLHAreaControlPoint that = (BasicLLHAreaControlPoint) o;

        if (this.airspace != that.airspace) {
            return false;
        }
        if (this.altitudeIndex != that.altitudeIndex) {
            return false;
        }
        if (this.locationIndex != that.locationIndex) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (this.point != null ? !this.point.equals(that.point) : that.point != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        //int result = this.editor != null ? this.editor.hashCode() : 0;
        int result = 0;
        result = 31 * result + (this.airspace != null ? this.airspace.hashCode() : 0);
        result = 31 * result + this.locationIndex;
        result = 31 * result + this.altitudeIndex;
        result = 31 * result + (this.point != null ? this.point.hashCode() : 0);
        return result;
    }
}
