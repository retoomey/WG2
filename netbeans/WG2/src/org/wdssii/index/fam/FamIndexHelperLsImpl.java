package org.wdssii.index.fam;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lakshman
 * 
 */
public class FamIndexHelperLsImpl implements FamIndexHelper {

    private String indexDir;
    private Set<String> knownFiles;
    private long timeOfLastUpdate;
    private final FilenameFilter filenamePattern;
    private NewFilesOnly newFilesOnly;
    private long updateIntervalInMilliSeconds = 60 * 1000; // milliseconds
    private final File[] emptyList = new File[0];
    private final Logger log = LoggerFactory.getLogger(FamIndexHelperLsImpl.class);

    public FamIndexHelperLsImpl(FilenameFilter pattern) {
        this.filenamePattern = pattern;
    }

    /** By default look only at files that end with .fml */
    public FamIndexHelperLsImpl() {
        this(new FmlFilesOnlyFilter());
    }

    @Override
    public File[] getInitialFiles(String indexDir) {
        this.close(); // resets
        this.indexDir = indexDir;
        File[] files = new File(indexDir).listFiles(filenamePattern);
        return processFileListAndReturn(files);
    }

    @Override
    public void close() {
        knownFiles = new HashSet<String>();
        timeOfLastUpdate = 0;
        newFilesOnly = new NewFilesOnly();
    }

    private File[] processFileListAndReturn(File[] files) {
        if (files == null) {
            log.warn("Problem listing files in " + indexDir);
            return emptyList;
        }

        if (log.isDebugEnabled()) {
            log.debug("Found " + files.length + " files in " + indexDir);
        }

        Arrays.sort(files); // alphabetical
        for (File f : files) {
            knownFiles.add(f.getAbsolutePath());
        }
        return files;
    }

    /** Accepts only new FML files */
    public class NewFilesOnly implements FilenameFilter {

        @Override
        public boolean accept(File dir, String baseName) {
            boolean isFml = filenamePattern.accept(dir, baseName);
            if (isFml) {
                File f = new File(dir, baseName);
                if (isNew(f)) {
                    return true;
                }
            }
            return false;
        }

        protected boolean isNew(File f) {
            String absPath = f.getAbsolutePath();

            // Has this file been aged off in memory?
            if (!knownFiles.isEmpty()) {
                String firstFile = knownFiles.iterator().next();
                if (absPath.compareTo(firstFile) < 0) {
                    // older than oldest file in map
                    return false;
                }
            }
            return (!knownFiles.contains(absPath));
        }
    }

    @Override
    public File[] getNewFiles() {
        // don't update if we just did
        long now = new Date().getTime();
        if ((now - timeOfLastUpdate) < updateIntervalInMilliSeconds) {
            return emptyList;
        }
        timeOfLastUpdate = now;

        // get all files
        File[] files = new File(indexDir).listFiles(newFilesOnly);
        return processFileListAndReturn(files);
    }

    @Override
    public void pruneToMaxSize(long maxNumRecords, long targetNumRecords) {
        // prune
        if (knownFiles.size() > maxNumRecords) {
            String[] temp = knownFiles.toArray(new String[0]);
            knownFiles.clear();
            for (int i = 1; i <= targetNumRecords; ++i) {
                knownFiles.add(temp[temp.length - i]);
            }
        }
    }
}
