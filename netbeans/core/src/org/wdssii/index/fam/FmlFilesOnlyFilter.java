package org.wdssii.index.fam;

import java.io.File;
import java.io.FilenameFilter;

public class FmlFilesOnlyFilter implements FilenameFilter {

    @Override
    public boolean accept(File dirParameterIgnored, String baseName) {
        if (baseName.length() < 2 || baseName.charAt(0) == '.') {
            return false; // hidden file
        }
        return baseName.endsWith(".fml");
    }
}
