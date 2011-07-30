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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
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
public class NetcdfBuilder extends Builder {

    private static Log log = LogFactory.getLog(NetcdfBuilder.class);

    /** Info snagged from a netcdf file.  Used by GUI to prefetch
     * Product, Choice and Time (selections) from our format netcdf files
     */
    public static class NetcdfFileInfo {

        /** The TypeName such as Reflectivity.  Attribute 'TypName' */
        public String TypeName;
        /** The DataType gotten from the file.   Attribute 'DataType' */
        public String DataType;
        /** Choice from file.  Elevation, height, etc... */
        public String Choice;
        /** Was the DataType 'sparse'? */
        boolean sparse;
        /** Any error during netcdf reading. GUI can show a dialog */
        public String error;
        /** Field set to true only if everything read correctly */
        public boolean success = false;
        /** The time for this product */
        public Date Time;
    }

    public NetcdfBuilder() {
        super("netcdf");
    }

    /** Here's where we do the work of building a DataType */
    @Override
    public DataType createDataType(IndexRecord rec, WdssiiJobMonitor m) {
        URL url = rec.getDataLocationURL(this);
        if (url == null) {
            return null;
        }

        return createDataTypeFromURL(url, m);
    }

    /** pass in the file name and obtain an object back. */
    public DataType createDataTypeFromURL(URL aURL, WdssiiJobMonitor m) {

        if (m != null) {
            m.beginTask(aURL.toString(), WdssiiJobMonitor.UNKNOWN);
        }

        File theFile;
        DataType dt = null;
        //  boolean isTemp = false;
        if (!aURL.toString().startsWith("file:")) {
            // Download to a local temp file
            theFile = downloadURLToTempFile(aURL, m);
            // isTemp = true;
        } else {
            // Already a local file, read directly...
            theFile = getFileFromURL(aURL);
        }
        if (theFile != null) {
            String absolutePath = theFile.getAbsolutePath();
            dt = fromNetcdfFile(absolutePath, m);
        }
        // if (isTemp) ---> cleanup...
        return dt;

    }

    /** Download/Copy a URL to a local temp File object */
    private File downloadURLToTempFile(URL aURL, WdssiiJobMonitor m) {
        File localFile = null;
        String path = "";
        try {
            // read from remote file and store it as uncompressed file,
            // so that netcdf doesn't need to uncompress a copy (saves IO)
            // because we don't copy it.
            path = aURL.toString();
            if (m != null) {
                m.subTask("Downloading " + path);
            }

            File dir = DataManager.getInstance().getTempDir("netcdf");
            InputStream theStream = aURL.openStream();
            if (path.endsWith(".gz")) {
                InputStream gzip = new GZIPInputStream(theStream);
                theStream = gzip;
            }
            ReadableByteChannel urlC = Channels.newChannel(theStream);
            localFile = File.createTempFile("ncdf", ".nc", dir);
            FileOutputStream fos2 = new FileOutputStream(localFile);
            WritableByteChannel fc = fos2.getChannel();

            copy(urlC, fc);
        } catch (Exception e) {
            throw new FormatException("Can not read remote file: " + path);
        }
        return localFile;
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
            String createByName = "org.wdssii.datatypes.builders.netcdf." + dataType + "Netcdf";
            Class<?> aClass = null;
            try {
                m.subTask("Creating " + dataType);
                aClass = Class.forName(createByName);
                Class<?>[] argTypes = new Class[]{NetcdfFile.class, boolean.class};
                Object[] args = new Object[]{ncfile, sparse}; // Actual args

                //DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse)
                //Constructor<?> c = aClass.getConstructor(argTypes);
                Object classInstance = aClass.newInstance();
                Method aMethod = aClass.getMethod("createFromNetcdf", argTypes);
                obj = (DataType) aMethod.invoke(classInstance, args);
            } catch (Exception e) {
                System.out.println("ERROR " + createByName + ", " + e.toString());
                log.warn("Couldn't create object by name '"
                        + createByName + "' because " + e.toString());
            } finally {
                // FIXME: Probably will create a "NetcdfDataType" object for the display
                // so it can still display unknown netcdf data for debugging purposes
            }
        } catch (Exception e) {
            log.warn("Couldn't open netcdf local file " + e.toString());
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

    /** Use to get the attributes that describe the product, choice
     * and time of one of our wdssii format files...
     * 
     * @param path 
     */
    public static NetcdfFileInfo getNetcdfFileInfo(String path) {
        NetcdfFileInfo info = new NetcdfFileInfo();
        NetcdfFile ncfile = null;

        try {
            ncfile = NetcdfFile.open(path);

            // First type to get the 'DataType' field from the netcdf, we use this
            // to look for a constructor to call
            try {
                info.DataType = ncfile.findGlobalAttribute("DataType").getStringValue();
            } catch (Exception e) {
                info.DataType = "Missing";
            }

            // Any DataType with 'Sparse' in it gets sent to the class without the sparse:
            // "SparseRadialSet" --> "RadialSet"
            // "SparseWindField" --> "WindField"
            boolean sparse = false;
            if (info.DataType.contains("Sparse")) {
                info.DataType = info.DataType.replaceFirst("Sparse", "");
                sparse = true;
            }
            info.sparse = sparse;

            // Create class from reflection.  Note this will create a 'RadialSetNetcdf',
            // 'LatLonGridNetcdf', 'WindfieldNetcdf', etc. based on DataType from netcdf file
            String createByName = "org.wdssii.datatypes.builders.netcdf." + info.DataType + "Netcdf";
            Class<?> aClass = null;
            try {
                aClass = Class.forName(createByName);
                Class<?>[] argTypes = new Class[]{NetcdfFile.class, NetcdfFileInfo.class};
                Object[] args = new Object[]{ncfile, info}; // Actual args
                Object classInstance = aClass.newInstance();
                Method aMethod = aClass.getMethod("fillNetcdfFileInfo", argTypes);
                aMethod.invoke(classInstance, args);
            } catch (Exception e) {
                System.out.println("ERROR " + createByName + ", " + e.toString());
                log.warn("Couldn't create object by name '"
                        + createByName + "' because " + e.toString());
            } finally {
                // FIXME: Probably will create a "NetcdfDataType" object for the display
                // so it can still display unknown netcdf data for debugging purposes
            }

            // Do this last....
            info.error = "";
            info.success = true;
        } catch (Exception e) {
            // It's ok, returning null lets us know the failure...
            info.error = e.toString();
        } finally {
            try {
                if (ncfile != null) {
                    ncfile.close();
                }
            } catch (Exception e) {
            }
        }
        return info;
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

    /** Handle params to URL for a netcdf index record.
     * @param rec
     * @param params
     * @return 
     */
    @Override
    public URL createURLForRecord(IndexRecord rec, String[] params) {
        // Params 0 are of this form for a regular index:
        // 0 - builder name such as 'netcdf'
        // 1 - indexLocation such as C:/KTLX/
        // 2 - Product such as 'Reflectivity'
        // 3 - Choice such as '05.25'
        // 4 - short file such as '1999_ktlx.netcdf.gz'
        StringBuilder path = new StringBuilder(params[1]);
        for (int i = 2; i < params.length; ++i) {
            path.append('/').append(params[i]);
        }

        URL url = null;
        try {
            url = new URL(path.toString());
        } catch (MalformedURLException e) {
        }
        return url;
    }
}
