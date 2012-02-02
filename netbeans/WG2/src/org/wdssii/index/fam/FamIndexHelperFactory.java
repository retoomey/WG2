package org.wdssii.index.fam;

import java.io.FilenameFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lakshman
 * 
 */
public class FamIndexHelperFactory {

    private static Logger log = LoggerFactory.getLogger(FamIndexHelperFactory.class);
    private static boolean tryInotify = true;

    public static void setTryInotify(boolean flag) {
        tryInotify = flag;
    }

    public static FamIndexHelper newHelper() {
        if (tryInotify) {
            try {
                return new FamIndexHelperInotifyImpl();
            } catch (Throwable e) {
                log.warn("Can not use inotify, so using 'ls' instead ", e);
            }
        }
        return new FamIndexHelperLsImpl();
    }

    public static FamIndexHelper newHelper(FilenameFilter filter) {
        if (tryInotify) {
            try {
                return new FamIndexHelperInotifyImpl(filter);
            } catch (Throwable e) {
                log.warn("Can not use inotify, so using 'ls' instead ", e);
            }
        }
        return new FamIndexHelperLsImpl(filter);
    }
}
