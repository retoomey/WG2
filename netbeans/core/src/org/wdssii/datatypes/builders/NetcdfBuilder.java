package org.wdssii.datatypes.builders;

/**
 * @author lakshman
 * 
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMetric;
import org.wdssii.index.IndexRecord;
import org.wdssii.storage.Array2Dfloat;
import org.wdssii.storage.Array2DfloatAsTiles;
import org.wdssii.storage.DataManager;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * 
 * Reads a local/remote netcdf file (gzipped or not) and builds a DataType
 * 
 * @author Lakshman
 */
public class NetcdfBuilder implements Builder {

    private static Log log = LogFactory.getLog(NetcdfBuilder.class);

    public NetcdfBuilder() {
    }

    /** Experimental background job reading in a DataType.
     * FIXME: wrap with our own job class */
    public class backgroundCreate extends WdssiiJob {

        IndexRecord myIndexRecord;
        DataRequest myDataRequest;

        public backgroundCreate(String name, IndexRecord rec, DataRequest dr) {
            super(name);
            myDataRequest = dr;
            myIndexRecord = rec;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {

            // FIXME: really the 'task' should be higher up...the thing that 'started' us loading...
            //	monitor.beginTask("DataRequest from "+myIndexRecord.getDataType(), IProgressMonitor.UNKNOWN);
            DataType dt = createBackgroundObject(myIndexRecord, monitor);
            if (dt == null) {
                log.info(">>>>>>>>>>>>>>>>>NULL DATATYPE CREATED?");
            } else {
                log.info(">>>>>>>>>>>>>>>DATATYPE CREATED ");
            }
            myDataRequest.setReady(dt);
            monitor.done();
            return WdssiiJobStatus.OK_STATUS;
        }
    }

    /** create object in background thread.
     * FIXME: how to handle duplicates or does it matter?
     */
    @Override
    public DataRequest createObjectBackground(IndexRecord rec) {
        DataRequest dr = new DataRequest();
        //backgroundCreate b = new backgroundCreate("NetcdfBuilder ("+counter+++")"+rec.getDataType(), rec, dr);
        backgroundCreate b = new backgroundCreate("NetcdfBuilder " + rec.getDataType(), rec, dr);
        b.schedule();
        return dr;
    }

    /** create object if the record params are netcdf ones. */
    @Override
    public DataType createObject(IndexRecord rec) {
        String[] params = rec.getParams();

        for (String s : params) {
            log.info("Record perams: " + s);
        }
        // Append params to get URL.  FIXME: the Index should do this so that
        // different types can do it differently if needed
        StringBuilder path = new StringBuilder(params[1]);
        for (int i = 2; i < params.length; ++i) {
            path.append('/').append(params[i]);
        }

        URL url = null;
        try {
            url = new URL(path.toString());
            log.info("Create datatype from URL:" + url);
        } catch (MalformedURLException e) {
            log.warn("Malformed URL for IndexRecord, DataType cannot be created");
            return null;
        }
        log.info("Path for this URL is " + url.getPath());

        /*
        // Open the netcdf stream, where it may be...
        // We'll stream it through the builder and create our own RAM/file tiles on the fly
        // which saves I/O
        InputStream is2;
        try {
        is2 = url.openStream();
        InputStream gzip = new GZIPInputStream(is2); // Assume it is zipped (FIXME: check URL path for ".gz"?)
        ReadableByteChannel urlC = Channels.newChannel(gzip);
        //	NetcdfFile.open(url);
        
        } catch (IOException e) {
        log.info("I/O exception reading URL: "+url);
        }*/
        //return createObject(path.toString());
        return createObject(url, null);
    }

    public DataType createBackgroundObject(IndexRecord rec, WdssiiJobMonitor m) {
        String[] params = rec.getParams();

        for (String s : params) {
            log.info("Record perams: " + s);
        }
        // Append params to get URL.  FIXME: the Index should do this so that
        // different types can do it differently if needed
        StringBuilder path = new StringBuilder(params[1]);
        for (int i = 2; i < params.length; ++i) {
            path.append('/').append(params[i]);
        }

        URL url = null;
        try {
            url = new URL(path.toString());
            //	log.info("Create datatype from URL:"+url);
        } catch (MalformedURLException e) {
            //	log.warn("Malformed URL for IndexRecord, DataType cannot be created");
            return null;
        }
        //log.info("Path for this URL is "+url.getPath());

        return createObject(url, m);
    }

    /** pass in the file name and obtain an object back. */
    private DataType createObject(URL path, WdssiiJobMonitor m) {

        if (m != null) {
            m.beginTask(path.toString(), WdssiiJobMonitor.UNKNOWN);
        }
        if (path.toString().startsWith("file:")) {

            return fromNetcdfFile(path.toString(), m);
        }
        return readRemoteNetcdfFile(path.toString(), m);

        // Read the URL data in....
        //int colon = path.indexOf(':');
        //	if (colon < 0 || colon == 1) { // colon will be 1 in Windows in the case of drive letters
        //		return fromNetcdfFile(path);
        //	}
        //	return readRemoteNetcdfFile(path);

    }

      // Read all available bytes from one channel and copy them to the other.
  public static void copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
    // First, we need a buffer to hold blocks of copied bytes.
    ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 1024);

    // Now loop until no more bytes to read and the buffer is empty
    while (in.read(buffer) != -1 || buffer.position() > 0) {
      // The read() call leaves the buffer in "fill mode". To prepare
      // to write bytes from the bufferwe have to put it in "drain mode"
      // by flipping it: setting limit to position and position to zero
      buffer.flip();

      // Now write some or all of the bytes out to the output channel
      out.write(buffer);

      // Compact the buffer by discarding bytes that were written,
      // and shifting any remaining bytes. This method also
      // prepares the buffer for the next call to read() by setting the
      // position to the limit and the limit to the buffer capacity.
      buffer.compact();
    }
  }
  
    private DataType readRemoteNetcdfFile(String path, WdssiiJobMonitor m) {
        File localFile = null;
        try {
            // read from remote file and store it as uncompressed file,
            // so that netcdf doesn't need to uncompress a copy (saves IO)
            // because we don't copy it.
            m.subTask("Reading "+path);
            URL url = new URL(path);

            File dir = DataManager.getInstance().getTempDir("netcdf");
            InputStream theStream = url.openStream();
            if (path.endsWith(".gz")){
                  InputStream gzip = new GZIPInputStream(theStream);
                  theStream = gzip;
            }
            ReadableByteChannel urlC = Channels.newChannel(theStream);
            localFile = File.createTempFile("ncdf", ".nc", dir);
            FileOutputStream fos2 = new FileOutputStream(localFile);
            WritableByteChannel fc = fos2.getChannel();
            
            copy(urlC, fc);

            if (localFile.exists()) {
                log.info("File exists " + localFile.getAbsolutePath());
            } else {
                log.info("File doesn't exist");
            }

            DataType dt = fromNetcdfFile(localFile.getAbsolutePath(), m);
            return dt;
        } catch (Exception e) {
            throw new FormatException("Can not read remote file: " + path);
        } finally {
            //if ( localFile != null ){
            //	localFile.delete();
            //}
        }
    }

    @SuppressWarnings("serial")
    private static class FormatException extends DataUnavailableException {

        FormatException(String msg) {
            super(msg);
        }
    }

    private DataType fromNetcdfFile(String path, WdssiiJobMonitor m) {
        NetcdfFile ncfile = null;
        DataType obj = null;

        try {
            m.subTask("Opening " + path);
            log.info("Opening " + path + " for reading");
            ncfile = NetcdfFile.open(path);

            // First type to get the 'DataType' field from the netcdf, we use this
            // to look for a constructor to call
            String dataType = "DataType";
            try {
                dataType = ncfile.findGlobalAttribute("DataType").getStringValue();
            } catch (Exception e) {
                // Exception ok..we'll just create a DataType with nothing in
                // it for the GUI??
                // FIXME: not 100% sure on this yet
            }

            // Any DataType with 'Sparse' in it gets sent to the class without the sparse:
            // "SparseRadialSet" --> "RadialSet"
            // "SparseWindField" --> "WindField"
            boolean sparse = false;
            if (dataType.contains("Sparse")) {
                String oldName = dataType;
                dataType = dataType.replaceFirst("Sparse", ""); // "SparseRadialSet" handled by "RadialSet"
                sparse = true;
                log.info("Replaced class name " + oldName + " with class name " + dataType);
            }

            // Create class from reflection.  Note this will create a 'RadialSetNetcdf',
            // 'LatLonGridNetcdf', 'WindfieldNetcdf', etc. based on DataType from netcdf file
            String createByName = "org.wdssii.datatypes.builders.netcdf." + dataType+"Netcdf";
            Class<?> aClass = null;
            try {
                m.subTask("Creating "+dataType);
                aClass = Class.forName(createByName);
                Class<?>[] argTypes = new Class[]{NetcdfFile.class, boolean.class};
                Object[] args = new Object[]{ncfile, sparse}; // Actual args
                
                //DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse)
                //Constructor<?> c = aClass.getConstructor(argTypes);
                Object classInstance = aClass.newInstance();
                Method aMethod = aClass.getMethod("createFromNetcdf", argTypes);
                obj = (DataType) aMethod.invoke(classInstance, args);
            } catch (Exception e) {
                System.out.println("ERROR "+createByName+", "+e.toString());
                log.warn("Couldn't create object by name '"
                        + createByName + "' because " + e.toString());
            } finally {
                // FIXME: Probably will create a "NetcdfDataType" object for the display
                // so it can still display unknown netcdf data for debugging purposes
            }
        } catch (Exception e) {
            log.warn(e);
        } finally {
            try {
                if (ncfile != null) {
                    ncfile.close();
                }
            } catch (Exception e) {
            }
        }
        return obj;
    }

    public static Variable getVariable(NetcdfFile ncfile, String name)
            throws FormatException {
        Variable data = ncfile.findVariable(name);
        if (data == null) {
            throw new FormatException("missing variable " + name);
        }
        return data;
    }

    public static Array2Dfloat readArray2Dfloat(NetcdfFile ncfile, String typeName,
            DataTypeMetric optional)
            throws IOException {
        Variable data = getVariable(ncfile, typeName);
        Array gate_values = data.read();
        int num_radials = data.getDimension(0).getLength();
        int num_gates = data.getDimension(1).getLength();

        Array2Dfloat values = new Array2DfloatAsTiles(num_radials, num_gates, 0.0f);
        Index gate_index = gate_values.getIndex();
        if (optional != null) {
            optional.beginArray2D();
        }
        for (int i = 0; i < num_radials; ++i) {
            for (int j = 0; j < num_gates; ++j) {
                gate_index.set(i, j);
                float value = gate_values.getFloat(gate_index);
                values.set(i, j, value);
                if (optional != null) {
                    optional.updateArray2D(i, j, value);
                }
            }
        }
        return values;
    }

    public static Array2Dfloat readSparseArray2Dfloat(NetcdfFile ncfile, String typeName,
            DataTypeMetric optional)
            throws IOException {
        int num_radials = (ncfile.getDimensions().get(0)).getLength();
        int num_gates = (ncfile.getDimensions().get(1)).getLength();

        Variable data = getVariable(ncfile, typeName);
        float backgroundValue = data.findAttribute("BackgroundValue").getNumericValue().floatValue();
        Variable pixelxVariable = getVariable(ncfile, "pixel_x");
        Variable pixelyVariable = getVariable(ncfile, "pixel_y");
        Variable countVariable = null;
        try {
            countVariable = getVariable(ncfile, "pixel_count");
        } catch (Exception e) {
            // ok
        }
        Array data_values = data.read();
        Array pixelx_values = pixelxVariable.read();
        Array pixely_values = pixelyVariable.read();
        Array count_values = (countVariable == null) ? null : countVariable.read();
        int num_pixels = pixelxVariable.getDimension(0).getLength();
        Index pixel_index = data_values.getIndex();
        Array2Dfloat values = new Array2DfloatAsTiles(num_radials, num_gates, backgroundValue);

        log.info("Array from sparse data " + num_radials + "," + num_gates + " " + backgroundValue);

        int actualData = 0;
        if (optional != null) {
            optional.beginArray2D();
        }
        for (int i = 0; i < num_pixels; ++i) {
            pixel_index.set(i);
            float value = data_values.getFloat(pixel_index);
            short x = pixelx_values.getShort(pixel_index);
            short y = pixely_values.getShort(pixel_index);
            int count = (count_values == null) ? 1 : count_values.getInt(pixel_index);
            for (int j = 0; j < count; ++j, ++y) {
                if (y == num_gates) {
                    ++x;
                    y = 0;
                }
                values.set(x, y, value);
                if (optional != null) {
                    optional.updateArray2D(x, y, value);
                }
                actualData++;
            }
        }
        log.warn("Actual data count was " + actualData);

        return values;
    }
}
