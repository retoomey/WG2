package org.wdssii.datatypes.builders;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.index.IndexRecord;

/**
 * @author lakshman
 * 
 * Builders create DataType or DataRequest.  DataRequest is a wrapper
 * for DataType that has flags to tell if the DataType within it is ready
 * and finished loading.
 * 
 * Builder also has utilities for dealing with URL and local files, since
 * 99% of data (other than dynamic or tests) work this way.
 * 
 */
public abstract class Builder {

    /** The subdirectory used in the DataManager.  For example, on startup 
     DataManager makes a 'wg-some-temp/' directory.  This would be
     'wg-some-temp/myTempSubdir' for all files we download
     */
    private String myTempSubdir = "data";
    
    public Builder(String temp){
        myTempSubdir = temp;
    }
    
    /** A background job that builds a DataType in another thread.  This
    lazy fills in the DataType for a DataRequest */
    public class BuilderBackgroundJob extends WdssiiJob {

        IndexRecord myIndexRecord;
        DataRequest myDataRequest;

        public BuilderBackgroundJob(String name, IndexRecord rec, DataRequest dr) {
            super(name);
            myDataRequest = dr;
            myIndexRecord = rec;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            //	monitor.beginTask("DataRequest from "+myIndexRecord.getDataType(), IProgressMonitor.UNKNOWN);
            DataType dt = createDataType(myIndexRecord, monitor);
            myDataRequest.setReady(dt);
            return WdssiiJobStatus.OK_STATUS;
        }
    }

    /** A utility for builders that copies from a ReadableByteChannel to
     * a WritableByteChannel
     * @param in
     * @param out
     * @throws IOException 
     */
    // Read all available bytes from one channel and copy them to the other.
    public static void copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
        // First, we need a buffer to hold blocks of copied bytes.
        // FIXME: advantage to reusing buffer or different size?
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

    /** A utility function to correctly get a File from a URL.  This makes
     * sure to fix spaces, etc. that mess up windows.
     * URL has spaces converted to %20, which won't work as a file name
     * This is THE proper way to convert back
     */
    public static File getFileFromURL(URL aURL) {
        File aFile = null;
        try {
            aFile = new File(aURL.toURI());
        } catch (URISyntaxException e) {
            aFile = new File(aURL.getPath());
        }
        return aFile;
    }

    
    /** Create a DataRequest, which spawns a background job */
    public DataRequest createDataRequest(IndexRecord rec) {
        DataRequest dr = new DataRequest();
        BuilderBackgroundJob b = new BuilderBackgroundJob("Building " + rec.getDataType(), rec, dr);
        b.schedule();
        return dr;
    }

    /** Create a DataType object in this thread and report to given monitor while doing it.
    The monitor is allowed to be null.
     */
    public abstract DataType createDataType(IndexRecord rec, WdssiiJobMonitor w);
    
    /** Subclasses should override to create a valid URL for this record.
     * The URL is used to fetch the actual data file for the record.
     * @param rec
     * @param params
     * @return 
     */
    public URL createURLForRecord(IndexRecord rec, String[] params) {
        return null;  // No idea.
    }
}
