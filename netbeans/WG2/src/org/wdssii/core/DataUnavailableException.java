package org.wdssii.core;

/**
 * @author lakshman
 * 
 */
@SuppressWarnings("serial")
public class DataUnavailableException extends RuntimeException {

    public DataUnavailableException(Exception e) {
        super(e);
    }

    public DataUnavailableException(String e) {
        super(e);
    }
}
