package org.wdssii.log.sl4fj;

import org.slf4j.LoggerFactory;

/**
 * Our link to Slf4j logging
 *
 * @author Robert Toomey
 */
public class Slf4jLogger implements org.wdssii.log.Logger {

    /**
     * the 'real' logger
     */
    private org.slf4j.Logger log;

    public Slf4jLogger(Class<?> aClass) {
        log = LoggerFactory.getLogger(aClass);
    }

    public Slf4jLogger(String string) {
        log = LoggerFactory.getLogger(string);
    }

    @Override
    public void debug(String string) {
        log.debug(string);
    }

    @Override
    public void error(String string) {
        log.error(string);
    }

    @Override
    public void info(String string) {
        log.info(string);
    }

    @Override
    public void info(String string, Object o) {
        log.info(string, o);
    }

    @Override
    public void info(String string, Object o, Object o1) {
        log.info(string, o, o1);
    }

    @Override
    public void info(String string, Object[] os) {
        log.info(string, os);
    }

    @Override
    public void warn(String string) {
        log.warn(string);
    }

    @Override
    public void trace(String string) {
        log.trace(string);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }
}
