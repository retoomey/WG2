package org.wdssii.index.fam;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FamIndexHelperInotifyImpl implements FamIndexHelper,
        JNotifyListener {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<File> files = new ArrayList<File>();
    private int wd;
    private final FilenameFilter filenamePattern;

    /** By default look only at files that end with .fml */
    public FamIndexHelperInotifyImpl() {
        this(new FmlFilesOnlyFilter());
    }

    public FamIndexHelperInotifyImpl(FilenameFilter pattern) {
        // this will cause a class-load exception if JNotify is not in classpath
        try {
            JNotify.removeWatch(-1);
        } catch (JNotifyException e) {
            // ok
        }
        filenamePattern = pattern;
    }

    @Override
    public File[] getInitialFiles(String indexDir) {
        try {
            // JNotify maps IN_CREATE and IN_MOVED_TO to these constants
            final int mask = JNotify.FILE_CREATED | JNotify.FILE_RENAMED;
            this.wd = JNotify.addWatch(indexDir, mask, false, this);
            if (log.isInfoEnabled()) {
                log.info("Successfully created inotify watch for " + indexDir
                        + " wd=" + wd);
            }
        } catch (JNotifyException e) {
            log.error("Make sure that the jnotify jar file and .so are in Tomcat's shared/lib");
            log.error(
                    "Otherwise, you could try using WebIndexDirectoryListingDAO instead",
                    e);
            throw new UnsupportedOperationException(e);
        }
        // existing files
        File[] files = new File(indexDir).listFiles(filenamePattern);
        Arrays.sort(files);
        return files;
    }

    @Override
    public synchronized void fileCreated(int watch, String indexDir,
            String filename) {
        if (filename != null && filenamePattern.accept(null, filename)) {
            File f = new File(indexDir, filename);
            files.add(f);
        }
    }

    @Override
    public synchronized void fileDeleted(int arg0, String arg1, String arg2) {
    }

    @Override
    public synchronized void fileModified(int arg0, String arg1, String arg2) {
    }

    @Override
    public synchronized void fileRenamed(int wd, String indexDir,
            String oldName, String newName) {
        if (newName != null && filenamePattern.accept(null, newName)) {
            File f = new File(indexDir, newName);
            files.add(f);
        }
    }

    @Override
    /**
     * returns new files in watched directory since previous call to this
     * method.
     */
    public synchronized File[] getNewFiles() {
        File[] newfiles = files.toArray(new File[0]);
        files.clear(); // start accumulating again
        return newfiles;
    }

    /**
     * De-registers from the watch so that the servlet container does not keep
     * receiving events.
     */
    @Override
    public void close() {
        if (wd >= 0) {
            try {
                JNotify.removeWatch(wd);
                if (log.isInfoEnabled()) {
                    log.info("Removed inotify watch wd=" + wd);
                }
            } catch (JNotifyException e) {
                // ok
            }
        }
    }

    @Override
    public void pruneToMaxSize(long m, long n) {
        // nothing to do
    }
}
