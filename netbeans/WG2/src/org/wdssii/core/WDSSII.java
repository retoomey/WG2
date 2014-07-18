package org.wdssii.core;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.DataManager;

import ucar.nc2.util.DiskCache;

/**
 * 
 * The initialize() method of this class needs to be invoked by every algorithm or process.
 * 
 * @author lakshman
 *
 */
public class WDSSII {

    private final static Logger LOG = LoggerFactory.getLogger(WDSSII.class);
    private static WDSSII singleton = new WDSSII();

    public static WDSSII getInstance() {
        return singleton;
    }
    private TimeZone initialTimeZone;
    private File temporaryDir = null;

    private WDSSII() {
        //LOG.info("WDSS-II (c) University of Oklahoma and NOAA National Severe Storms Laboratory");
        // any other inits go here
        this.initialTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        if (LOG.isInfoEnabled()) {
            LOG.info("Set default timezone to: " + TimeZone.getDefault());
        }
        makeTemporaryDirectory();
    }

    private void makeTemporaryDirectory() {
        // make sure to use a temporary directory for netcdf temporary files
        try {
            temporaryDir = DataManager.getInstance().getTempDir("netcdf");
            LOG.info("Using " + temporaryDir + " to store temporary netcdf files");
            DiskCache.setRootDirectory(temporaryDir.getAbsolutePath());
            DiskCache.setCachePolicy(true); // always in cache
        } catch (Exception e) {
            LOG.error("Unable to create temporary directory for netcdf files; using default DiskCache strategy");
            temporaryDir = null;
        }
    }

    /** The actual TimeZone before it was changed to UTC */
    public TimeZone getInitialTimeZone() {
        return initialTimeZone;
    }

    public void cleanupTemporaryFiles() {
        if (temporaryDir != null) {
            for (File f : temporaryDir.listFiles()) {
                f.delete();
            }
        }
    }

    public Date getCurrentLocalTime() {
        Date now = new Date();
        return new Date(now.getTime() + initialTimeZone.getRawOffset());
    }

    public Calendar getCurrentLocalCalendar() {
        return new GregorianCalendar(initialTimeZone);
    }
}
