package org.wdssii.index.fam;

import java.io.File;

public interface FamIndexHelper {

    public File[] getInitialFiles(String indexDir);

    public File[] getNewFiles();

    public void close();

    public void pruneToMaxSize(long maxSize, long targetSize);
}