package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wdssii.core.WdssiiJob;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.MapFeature.MapMemento;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.Array1D;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.GrowList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 *
 * @author Robert Toomey
 *
 *         Uses geotools to create/render a shapefile map in a GLWorld window.
 *         Currently only looking at JTS MultiPolygon (Polygon) objects and
 *         creating line strips from them.
 * @todo Add other types, clean up code, etc...
 *
 *       FIXME: combine common code from PolarGridRenderer into a background
 *       class that hides most of the job stuff....
 */
public class MapRenderer extends Feature3DRenderer {

	private final static Logger LOG = LoggerFactory.getLogger(MapRenderer.class);
	/**
	 * Lock for changing offsets/polygonData
	 */
	private final Object drawLock = new Object(); // Lock 2 (second)
	/**
	 * Offset array, one per Polygon
	 */
	private GrowList<Integer> myOffsets;
	/**
	 * Verts for the map. Single for now (monster maps may fail)
	 */
	protected Array1D<Float> polygonData;
	/**
	 * Current memento settings
	 */
	private MapMemento current;
	/**
	 * The feature we use from GeoTools
	 */
	private SimpleFeatureSource mySource;
	private boolean tryToLoad = true;

	public MapRenderer() {
	}

	// public MapRenderer(SimpleFeatureSource f) {
	// mySource = f;
	// }
	@Override
	public void initToFeature(Feature m) {
		if (m instanceof MapFeature) {
			MapFeature mf = (MapFeature) (m);
			mySource = mf.getFeatureSource();
		}
	}

	/**
	 * The worker job if we are threading
	 */
	private BackgroundMapMaker myWorker = null;
	private final Object workerLock = new Object(); // Lock 1 (first)
	private boolean myCreated = false;
	private boolean myHack = false;

	/**
	 * Is this map fully created?
	 */
	public boolean isCreated() {
		return myCreated;
	}

	/**
	 * Get if this map if fully created
	 */
	public void setIsCreated(boolean flag) {
		myCreated = flag;
	}

	@Override
	public void pick(GLWorld w, Point p, FeatureMemento m) {
	}

	@Override
	public void preRender(GLWorld w, FeatureMemento m) {
	}

	/**
	 * Job for creating in the background any rendering
	 */
	public static class BackgroundMapMaker extends WdssiiJob {

		public GLWorld w;
		private MapMemento m;
		private MapRenderer myMapRenderer;
		// Humm.. reload map each time we regenerate?
		// Current maps will load once probably...
		public SimpleFeatureSource source;

		public BackgroundMapMaker(String jobName, GLWorld aW, SimpleFeatureSource s, MapRenderer r, MapMemento map) {
			super(jobName);
			w = aW;
			myMapRenderer = r;
			source = s;
			m = map;
		}

		@Override
		public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
			return create(w, m, myMapRenderer, source, monitor);
		}

		/**
		 * Create the render geomerty. If countPass, count the points needed
		 * without creating
		 */
		protected int createGeometry(Boolean c, SimpleFeatureCollection stuff, MapRenderer r,
				Array1D<Float> workPolygons, GrowList<Integer> workOffsets, MathTransform t) {

			int count = 0;
			int idx = 0;

			SimpleFeatureIterator i = stuff.features();

			if (!c) {
				workPolygons.begin();
				workOffsets.add(0);
			}
			int handle = 0;
	
			while (i.hasNext()) {
				SimpleFeature f = i.next();
				Object geo = f.getDefaultGeometry();

				// --------------------------------------------------------------------
				// Actually transform this point to our coordinate system
				if (!c) {

					try {
						Geometry geoorg = (Geometry) f.getDefaultGeometry();
						if (t != null) {
							geo = JTS.transform(geoorg, t);
						}
					} catch (MismatchedDimensionException ex) {
						LOG.error("Couldn't tranform map, mismatched dimensions");
						continue; // humm would probably fail again..should
									// abort
					} catch (TransformException ex) {
						LOG.error("Transform exception with map");
						continue;
					}
				}
				// End transform
				// --------------------------------------------------------------------

				if (geo instanceof GeometryCollection){
					GeometryCollection gc = (GeometryCollection)(geo);
				}
				
				// JTS polygon format
				if (geo instanceof MultiPolygon) { // Not seeing this anymore?
					if (r != null) {r.hackSetType(false);}
					MultiPolygon mp = (MultiPolygon) (geo);
					int subGeometryCount = mp.getNumGeometries();
					for (int sg = 0; sg < subGeometryCount; sg++) {
						Object o1 = mp.getGeometryN(sg);

						// Render each polygon of a multipoly as its
						// own line strip or polygon in opengl.
						if (o1 instanceof Polygon) {
							Polygon poly = (Polygon) (o1);

							if (c) {
								// Add number of points
								poly.getNumPoints();
								count += poly.getNumPoints();
							
							} else {

								// Actual create....
								Coordinate[] coorArray = poly.getCoordinates();
								for (int p = 0; p < coorArray.length; p++) {
									Coordinate C = coorArray[p];

									double lat = C.y;
									double lon = C.x;
									V3 point = w.projectLLH(lat, lon, w.getElevation(lat, lon) + 100.0);
									workPolygons.set(idx++, (float) point.x);
									workPolygons.set(idx++, (float) point.y);
									workPolygons.set(idx++, (float) point.z);
									count += 1;
									// actualCount++;
								}
								workOffsets.add(idx);
								// Update our renderer once a ring...abort if
								// we're stale
								if (!r.updateData(this, workOffsets, workPolygons, false)) {
									return count;
								}
							}

						}
					}

				} else if (geo instanceof MultiLineString) { // Seeing this with
																// polyline
																// maps...
					
					// Code the same..but it's a line strip not a line loop.
					if (r != null) {r.hackSetType(true);}
					
					MultiLineString mp = (MultiLineString) (geo);
					int subGeometryCount = mp.getNumGeometries();
					for (int sg = 0; sg < subGeometryCount; sg++) {
						Object o1 = mp.getGeometryN(sg);
						if (o1 instanceof LineString) {
							LineString line = (LineString) (o1);
							if (c) {
								// Add number of points
								count += line.getNumPoints();
							
							}  else {

								// Actual create....
								Coordinate[] coorArray = line.getCoordinates();
								for (int p = 0; p < coorArray.length; p++) {
									Coordinate C = coorArray[p];

									double lat = C.y;
									double lon = C.x;
									V3 point = w.projectLLH(lat, lon, w.getElevation(lat, lon) + 100.0);
									workPolygons.set(idx++, (float) point.x);
									workPolygons.set(idx++, (float) point.y);
									workPolygons.set(idx++, (float) point.z);
									count += 1;
									// actualCount++;
								}
								workOffsets.add(idx);
								// Update our renderer once a ring...abort if
								// we're stale
								if (!r.updateData(this, workOffsets, workPolygons, false)) {
									return count;
								}
							}

						}
					}
				}
				handle++;
			}
			if (!c) {
				workPolygons.end();
			}
			;
			LOG.debug("NUMBER BACK IS "+handle);
			return count;

		}

		public WdssiiJobStatus create(GLWorld w, MapMemento m, MapRenderer r, SimpleFeatureSource source,
				WdssiiJobMonitor monitor) {

			// Try to load the feature collection and transform
			// -------------------------------------------
			boolean success = true;
			SimpleFeatureCollection stuff;
			MathTransform transform = null;
			try {
				stuff = source.getFeatures();

				// Look at CRS. Our original wdssi shapefiles
				// were all simple lat/lon
				SimpleFeatureType schema = source.getSchema();
				CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
				if (crs == null) {
					// Should warn that we have no .prj and 'guess' or
					// allow user to set it...
					LOG.error(
							"shapefile is missing projection information (.prj), assuming it's lat/lon data...map could be wrong.");
					success = false;
				} else {

					// If we have .prj, we want it in a projection system with
					// a latitude/longitude...
					CoordinateReferenceSystem world = DefaultGeographicCRS.WGS84;
					boolean lenient = true;
					transform = CRS.findMathTransform(crs, world, lenient);
				}
			} catch (Throwable ex) { // We want GUI to not freak on errors, so
										// we justify catching everything here
				LOG.error("Can't create map visual due to loading or transform error: " + ex.toString());
				return WdssiiJobStatus.OK_STATUS;
			}
			if (!success) {
				return WdssiiJobStatus.OK_STATUS;
			}
			// END locate feature collection and transform
			// ---------------------------------------------------

			// Count only pass...preallocation of memory
			int multiPolyPointCount = createGeometry(true, stuff, null, null, null, null);

			Array1D<Float> workPolygons = new Array1DOpenGL(multiPolyPointCount * 3, 0.0f);
			GrowList<Integer> workOffsets = new GrowList<Integer>();
			int actualCount = createGeometry(false, stuff, r, workPolygons, workOffsets, transform);

			LOG.debug("Map points allocated/actual: " + multiPolyPointCount + ", " + actualCount);

			return WdssiiJobStatus.OK_STATUS;
		}
	}
	/*
	 * Tell if this changes requires a new background job. Some changes, like
	 * line thickness are done by the renderer on the fly
	 */

	public boolean changeNeedsUpdate(MapMemento new1, MapMemento old) {
		boolean needsUpdate = false;
		if (current == null) { // No settings yet...definitely update
			return true;
		}
		// Never change again for the moment
		return false;
	}

	public void hackSetType(boolean b) {
		// if true, we are rendering lines instead of loops.  Of course if it's
		// mixed we got problems.  FIXME: Need to allow map to have lines, loops and points as separate
		// renderable arrays
		myHack  = b;
	
	}

	/**
	 * Draw the product in the current dc
	 */
	@Override
	public void draw(GLWorld w, FeatureMemento mf) {

		MapMemento m = (MapMemento) (mf);
		// Regenerate if memento is different...
		if (changeNeedsUpdate(m, current)) {
			// Have to make copy of information for background job
			current = new MapMemento(m); // Keep new settings...
			MapMemento aCopy = new MapMemento(m);

			// Change out workers only in workerLock
			synchronized (workerLock) {
				if (myWorker != null) {
					myWorker.cancel(); // doesn't matter really
				}
				myWorker = new BackgroundMapMaker("Job", w, mySource, this, aCopy);
				myWorker.schedule();
			}

		}

		synchronized (drawLock) {

			if (isCreated() && (polygonData != null)) {
				final GL gl = w.gl;
				Color line = m.get(MapMemento.LINE_COLOR, Color.WHITE);
				final float r = line.getRed() / 255.0f;
				final float g = line.getGreen() / 255.0f;
				final float b = line.getBlue() / 255.0f;
				final float a = line.getAlpha() / 255.0f;
				boolean attribsPushed = false;
				try {
					Object lock1 = polygonData.getBufferLock();
					synchronized (lock1) {

						gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT | GL.GL_COLOR_BUFFER_BIT
								| GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
								| GL.GL_CURRENT_BIT | GL.GL_LINE_BIT);

						gl.glDisable(GL.GL_LIGHTING);
						gl.glDisable(GL.GL_TEXTURE_2D);

						// We WANT depth test, we use the hidden stipple for
						// things behind...
						gl.glEnable(GL.GL_DEPTH_TEST);

						gl.glShadeModel(GL.GL_FLAT);
						gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT | GL.GL_CLIENT_PIXEL_STORE_BIT);
						gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
						// gl.glEnableClientState(GL.GL_COLOR_ARRAY);
						attribsPushed = true;
						FloatBuffer z = polygonData.getRawBuffer();
						gl.glColor4f(r, g, b, a);
						Integer t = m.get(MapMemento.LINE_THICKNESS, 1);
						gl.glLineWidth(t);
						int type = GL.GL_LINE_LOOP;
						if (myHack){
							type = GL.GL_LINE_STRIP;
						}
						GLUtil.renderArrays(w.gl, z, myOffsets, type);

						// Try the hidden stipple
						GLUtil.pushHiddenStipple(w.gl);
						GLUtil.renderArrays(w.gl, z, myOffsets,type);
						GLUtil.popHiddenStipple(w.gl);

					}
				} finally {
					if (attribsPushed) {
						gl.glPopClientAttrib();
						gl.glPopAttrib();
					}
				}
			}
		}
	}

	/**
	 * Update our data to the data of a worker. Note because of threads for
	 * brief time periods more than one worker might be going. (Fast changing of
	 * settings). The worker will stop on false
	 */
	public boolean updateData(BackgroundMapMaker worker, GrowList<Integer> off, Array1D<Float> poly, boolean done) {

		// WorkerLock --> drawLock. Never switch order
		boolean keepWorking;
		synchronized (workerLock) {
			// See if worker changed..if so
			if (worker == myWorker) {

				synchronized (drawLock) {
					myOffsets = off;
					polygonData = poly;
					setIsCreated(true);
				}
				keepWorking = true;
			} else {
				// Old worker, stop...
				keepWorking = false;
			}
		}
		FeatureList.theFeatures.updateOnMinTime();
		return keepWorking;
	}
}
