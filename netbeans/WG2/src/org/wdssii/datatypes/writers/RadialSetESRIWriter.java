package org.wdssii.datatypes.writers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialATHeightGateCache;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.RadialUtil;
import org.wdssii.geom.Location;
import org.wdssii.storage.Array1D;

/**
 * Writer to output a RadialSet to ESRI file
 *
 * @author Robert Toomey
 */
public class RadialSetESRIWriter extends ESRIWriter {

    private final int NUM_STATES = 4;
    // Create radials, add features, transaction commit
    private final static Logger LOG = LoggerFactory.getLogger(PPIRadialSet.class);

    @Override
    public WdssiiJobStatus export(DataTypeWriterOptions o) {
        
        final DataType data = o.getData();
        final WdssiiJobMonitor monitor = o.getMonitor();
        final URL aURL = o.getURL();
        if (!(data instanceof RadialSet)) {
            return WdssiiJobStatus.CANCEL_STATUS;
        }
        RadialSet rs = (RadialSet) (data);

        try {
            // Setup for JTS feature
            final SimpleFeatureType TYPE = createFeatureType();
            SimpleFeatureCollection collection = FeatureCollections.newCollection();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

            // RadialSet information
            final Location radarLoc = rs.getRadarLocation();
            final double sinElevAngle = rs.getFixedAngleSin();
            final double cosElevAngle = rs.getFixedAngleCos();
            final float firstGateKms = rs.getRangeToFirstGateKms();
            final int maxGateCount = rs.getNumGates();
            final int numRadials = rs.getNumRadials();
            monitor.beginTask("", NUM_STATES);// For write task

            // Gate and point cache
            Location gate = new Location(0, 0, 0);
            Location gate1 = new Location(0, 0, 0);
            Location gate2 = new Location(0, 0, 0);
            Location gate3 = new Location(0, 0, 0);
            Coordinate c0, c1, c2, c3;

            // --------------------------------------------------------
            // On first radial, create an attenuation cache...
            Radial firstRadial = (numRadials > 0) ? rs.getRadial(0) : null;
            RadialATHeightGateCache c =
                    new RadialATHeightGateCache(rs, firstRadial, maxGateCount, sinElevAngle, cosElevAngle);

            int featureId = 0;
            int currentRadial = 0;
            monitor.subTask("Creating Radial Features");
            monitor.worked(1);
            // Do it first to ensure it's called

            for (int i = 0; i < numRadials; i++) {
                Radial r = rs.getRadial(i);

                // If missing, just continue on
                int numGates = r.getNumGates();
                if (numGates == 0) {
                    continue;
                }

                // The per-radial values
                float startAzimuthRAD = r.getStartRadians();
                float endAzimuthRAD = r.getEndRadians();
                double sinStartAzRAD = Math.sin(startAzimuthRAD);
                double cosStartAzRAD = Math.cos(startAzimuthRAD);
                double sinEndAzRAD = Math.sin(endAzimuthRAD);
                double cosEndAzRAD = Math.cos(endAzimuthRAD);
                float gateWidthKms = r.getGateWidthKms();

                // Reset range to starting gate
                float rangeKms = firstGateKms;
                Array1D<Float> values = r.getValues();
                int lastWrittenIndex = -2;

                for (int j = 0; j < numGates; j++) {
                    float value = values.get(j);

                    if (value == DataType.MissingData) {
                        // This new way we don't have to calculate anything
                        // with missing data.  Much better for long bursts of
                        // missing...
                    } else {

                        // Calculate the two points closest to the radar center
                        // if last written then we have this cached from the 
                        // old two furthest points...
                        //if (j - 1 == lastWrittenIndex) {
                        // Cached from previous top...
                        //    gate2 = gate;
                        //    gate3 = gate1;
                        //    c0 = c2;
                        //    c1 = c3;
                        //} else {

                        // Create 'bottom', part nearest to radar center
                        RadialUtil.getAzRan1(gate, radarLoc, sinStartAzRAD,
                                cosStartAzRAD, rangeKms, sinElevAngle, cosElevAngle,
                                c.heights[j], c.gcdSinCache[j], c.gcdCosCache[j]);
                        RadialUtil.getAzRan1(gate1, radarLoc, sinEndAzRAD, cosEndAzRAD,
                                rangeKms, sinElevAngle, cosElevAngle, c.heights[j],
                                c.gcdSinCache[j], c.gcdCosCache[j]);

                        c0 = new Coordinate(gate.getLatitude(),
                                gate.getLongitude());
                        c1 = new Coordinate(gate1.getLatitude(),
                                gate1.getLongitude());
                        lastWrittenIndex = j;
                        //}

                        // Calculate the furthest two points 'top' of the gate quad
                        // from the radar center.                     
                        float endRangeKms = rangeKms + gateWidthKms;
                        RadialUtil.getAzRan1(gate2, radarLoc, sinEndAzRAD,
                                cosEndAzRAD, endRangeKms, sinElevAngle,
                                cosElevAngle, c.heights[j + 1],
                                c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);
                        RadialUtil.getAzRan1(gate3, radarLoc, sinStartAzRAD,
                                cosStartAzRAD, endRangeKms, sinElevAngle,
                                cosElevAngle, c.heights[j + 1],
                                c.gcdSinCache[j + 1], c.gcdCosCache[j + 1]);

                        c2 = new Coordinate(gate2.getLatitude(),
                                gate2.getLongitude());
                        c3 = new Coordinate(gate3.getLatitude(),
                                gate3.getLongitude());

                        // LOG.debug("OUTPUT " + c0 + c1 + c2 + c3);
                        // Write a polygon for data.  Bleh we have to make unique objects
                        Coordinate[] a = new Coordinate[5];
                        a[0] = new Coordinate(c0);
                        a[1] = new Coordinate(c1);
                        a[2] = new Coordinate(c2);
                        a[3] = new Coordinate(c3);
                        a[4] = new Coordinate(c0);
                        LinearRing shell = geometryFactory.createLinearRing(a);
                        featureBuilder.add(geometryFactory.createPolygon(shell, null));
                        featureBuilder.add(value);  // Float "Value"
                        SimpleFeature feature = featureBuilder.buildFeature(String.valueOf(featureId++));
                        collection.add(feature);
                    }
                    rangeKms += gateWidthKms;
                }
                currentRadial++;
                ///if (currentRadial > 0){
                //    break;
                //}
            }

            // Write output to disk...
            monitor.subTask("Writing output file (wait)");
            writeFeatureToFile(aURL, TYPE, collection, monitor);
        } catch (Exception e) {
            LOG.error("Error during RadialSet to shp generation " + e.toString());
            return WdssiiJobStatus.CANCEL_STATUS;
        }
        monitor.done();
        return WdssiiJobStatus.OK_STATUS;
    }

    private static void writeFeatureToFile(URL aURL, SimpleFeatureType type,
            SimpleFeatureCollection collection, WdssiiJobMonitor monitor) throws MalformedURLException, IOException {
        monitor.subTask("Prepping file");
        monitor.worked(1);   // D
        /*
         * Get an output file name and create the new shapefile
         */
        File newFile = new File(aURL.getFile());
        String name = newFile.getPath();
        if (!name.toLowerCase().endsWith(".shp")) {
            newFile = new File(newFile.getPath() + ".shp");
        }

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(type);

        /*
         * You can comment out this line if you are using the createFeatureType method (at end of
         * class file) rather than DataUtilities.createType
         */
        newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        final WdssiiJobMonitor pp = monitor;
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try {
                monitor.subTask("GeoTools: Adding features (can take a min)");
                monitor.worked(1);   // Do it first to ensure it's called
                featureStore.addFeatureListener(new FeatureListener() {
                    @Override
                    public void changed(FeatureEvent fe) {
                        pp.subTask("Feature changed");
                    }
                });
                featureStore.addFeatures(collection);
                monitor.subTask("GeoTools: Transaction commit (can take a min)");
                monitor.worked(1);   // D
                transaction.commit();

            } catch (Exception problem) {
                // problem.printStackTrace();
                LOG.debug("Exception writing file: " + problem.toString());
                transaction.rollback();

            } finally {
                transaction.close();
            }

        } else {
            LOG.debug(typeName + " does not support read/write access");
        }
    }

    /**
     * Here is how you can use a SimpleFeatureType builder to create the schema
     * for your shapefile dynamically. <p> This method is an improvement on the
     * code used in the main method above (where we used
     * DataUtilities.createFeatureType) because we can set a Coordinate
     * Reference System for the FeatureType and a a maximum field length for the
     * 'name' field dddd
     */
    private static SimpleFeatureType createFeatureType() {
        SimpleFeatureType output = null;
        try {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Location");
            builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

            // add attributes of table in order
            // builder.add("Quad", LinearRing.class);
            builder.add("Goop", Polygon.class);
            builder.add("Value", Float.class);

            // build the type
            final SimpleFeatureType LOCATION = builder.buildFeatureType();
            //return LOCATION;


            // srid 4326 is id for WGS1984
           /* final SimpleFeatureType LOCATION = DataUtilities.createType("Location",
             "lat1:Point:srid=4326," + // <- the geometry attribute: Point type
             "lat2:Point:srid=4326," + // <- the geometry attribute: Point type
             "lat3:Point:srid=4326," + // <- the geometry attribute: Point type
             "lat4:Point:srid=4326," + // <- the geometry attribute: Point type
             "value:Float" // "name:String," + // <- a String attribute
             // "number:Integer" // a number attribute
             );*/
            output = LOCATION;
        } catch (Exception ex) {
            LOG.error("Can't create output type " + ex.toString());
        }
        return output;

    }
}
