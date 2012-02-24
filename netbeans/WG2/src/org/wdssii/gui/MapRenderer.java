package org.wdssii.gui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import javax.media.opengl.GL;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array1DfloatAsNodes;

/**
 *
 * @author Robert Toomey
 * 
 * Uses geotools to create/render a shapefile map in a worldwind window.
 * Currently only looking at JTS MultiPolygon (Polygon) objects and creating
 * line strips from them.
 * @todo Add other types, clean up code, etc...
 * 
 */
public class MapRenderer {

    private static Logger log = LoggerFactory.getLogger(MapRenderer.class);
    
    /** Offset array, one per Polygon */
    private ArrayList<Integer> myOffsets;
    /** Verts for the map.  Single for now (monster maps may fail) */
    protected Array1Dfloat polygonData;
    
    /** The feature we use from GeoTools */
    private SimpleFeatureSource mySource;
    
    private boolean tryToLoad = true;

    public MapRenderer(SimpleFeatureSource f) {
        mySource = f;
    }
    /** Do we render inside a background job? */
   // private boolean myAsBackgroundJob = false;
    /** The worker job if we are threading */
    private backgroundRender myWorker = null;
    /** Volatile because renderer job creates the data in one thread, but drawn in another.
     * myCreated set to true by worker thread after the buffers are created (draw allowed)
     */
    private volatile boolean myCreated = false;

    /** Is this map fully created? */
    public synchronized boolean isCreated() {
        return myCreated;
    }

    /** Get if this map if fully created */
    public synchronized void setIsCreated() {
        myCreated = true;
    }

    /** Job for creating in the background any rendering */
    public class backgroundRender extends WdssiiJob {

        public DrawContext dc;
        public SimpleFeatureSource source;

        public backgroundRender(String jobName, DrawContext aDc, SimpleFeatureSource s) {
            super(jobName);
            dc = aDc;
            source = s;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return createForMap(dc, source, monitor);
        }
    }

    public void setupIfNeeded(DrawContext dc) {
        if (tryToLoad) {
            initToMap(dc, mySource);
            tryToLoad = false;
        }
    }

    public void initToMap(DrawContext dc, SimpleFeatureSource s) {

        // FIXME: handle background flag?
        if (myWorker == null) {
            myWorker = new backgroundRender("Job", dc, s);
            myWorker.schedule();
        }
    }

    /** Draw the product in the current dc */
    public void draw(DrawContext dc) {
        if (isCreated() && (polygonData != null)) {
            GL gl = dc.getGL();

            boolean attribsPushed = false;
            try {
                Object lock1 = polygonData.getBufferLock();
                synchronized (lock1) {

                    gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                            | GL.GL_COLOR_BUFFER_BIT
                            | GL.GL_ENABLE_BIT
                            | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                            | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT);

                    gl.glDisable(GL.GL_LIGHTING);
                    gl.glDisable(GL.GL_TEXTURE_2D);
                    gl.glDisable(GL.GL_DEPTH_TEST);
                    gl.glShadeModel(GL.GL_FLAT);
                    gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                            | GL.GL_CLIENT_PIXEL_STORE_BIT);
                    gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                    // gl.glEnableClientState(GL.GL_COLOR_ARRAY);
                    attribsPushed = true;
                    FloatBuffer z = polygonData.getRawBuffer();

                    // Only render if there is data to render
                    if ((z != null) && (z.capacity() > 0)) {
                        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
                        gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());
                        int size = myOffsets.size();
                        if (size > 1) {
                            for (int i = 0; i < size - 1; i++) {
                                int start_index = myOffsets.get(i);
                                int end_index = myOffsets.get(i + 1);
                                int run_indices = end_index - start_index;
                                int start_vertex = start_index / 3;
                                int run_vertices = run_indices / 3;
                                gl.glDrawArrays(GL.GL_LINE_LOOP, start_vertex,
                                        run_vertices);
                            }
                        }

                    }

                }
            } finally {
                if (attribsPushed) {
                    gl.glPopClientAttrib();
                    gl.glPopAttrib();
                }
            }
        }
    }

    /** Pick an object in the current dc at point */
    public void doPick(DrawContext dc, java.awt.Point pickPoint) {
    }

    /** Do the work of generating the OpenGL stuff */
    public WdssiiJobStatus createForMap(DrawContext dc, SimpleFeatureSource s, WdssiiJobMonitor monitor) {

        Globe myGlobe = dc.getGlobe();
        myOffsets = new ArrayList<Integer>();
        myOffsets.add(0);  // Just one for now
        int idx = 0;
        MultiPolygon m = null;
        try {
            SimpleFeatureCollection stuff = s.getFeatures();

            // Counter pass.  See how much memory we need....
            int multiPolyPointCount = 0;
            SimpleFeatureIterator i = stuff.features();
            while (i.hasNext()) {
                SimpleFeature f = i.next();
                Object geo = f.getDefaultGeometry();

                // JTS polygon format
                if (geo instanceof MultiPolygon) {
                    m = (MultiPolygon) (geo);
                    int subGeometryCount = m.getNumGeometries();
                    for (int sg = 0; sg < subGeometryCount; sg++) {
                        Object o1 = m.getGeometryN(sg);

                        // Render each polygon of a multipoly as its
                        // own line strip or polygon in opengl.
                        if (o1 instanceof Polygon) {
                            Polygon poly = (Polygon) (o1);
                            poly.getNumPoints();
                            multiPolyPointCount += m.getNumPoints();
                        }
                    }
                }
            }

            // Allocate memory...
            polygonData = new Array1DfloatAsNodes(multiPolyPointCount * 3, 0.0f);

            // Creation pass
            i = stuff.features();
            int actualCount = 0;
            while (i.hasNext()) {
                SimpleFeature f = i.next();
                Object geo = f.getDefaultGeometry();
                if (geo instanceof MultiPolygon) {
                    m = (MultiPolygon) (geo);

                    int subGeometryCount = m.getNumGeometries();
                    for (int sg = 0; sg < subGeometryCount; sg++) {
                        Object o1 = m.getGeometryN(sg);

                        // Render each polygon of a multipoly as its
                        // own line strip or polygon in opengl.
                        if (o1 instanceof Polygon) {
                            Polygon poly = (Polygon) (o1);


                            Coordinate[] coorArray = poly.getCoordinates();
                            for (int p = 0; p < coorArray.length; p++) {
                                Coordinate C = coorArray[p];
                                double lat = C.y;
                                double lon = C.x;
                                Vec4 point = myGlobe.computePointFromPosition(
                                        Angle.fromDegrees(lat),
                                        Angle.fromDegrees(lon),
                                        0 * 1000); // Fix me... have line maps follow terrain height?
                                polygonData.set(idx++, (float) point.x);
                                polygonData.set(idx++, (float) point.y);
                                polygonData.set(idx++, (float) point.z);
                                actualCount++;
                            }
                            myOffsets.add(idx);

                        }
                    }

                }
                myCreated = true;
            }
            log.debug("Map points allocated/actual" + multiPolyPointCount + ", " + actualCount);

        } catch (Exception e) {
            log.error("Exception creating map first pass " + e.toString());
        }

        return WdssiiJobStatus.OK_STATUS;
    }
}
