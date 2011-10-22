package org.wdssii.core;

/**
 * @author lakshman
 * 
 */
@SuppressWarnings("serial")
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(Exception e) {
        super(e);
    }

    public ConfigurationException(String error) {
        super(error);
    }
}
