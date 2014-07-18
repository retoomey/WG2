package org.geotools.util.logging;

import java.util.logging.Logger;

/**
 *
 * @author Robert Toomey
 * 
 * A java.util logger that just uses whatever logger WDSSII has already bound too.
 * Some libraries use java.util, this allows us to set them up to redirect to us
 * for consistent logging.
 * 
 */
public class WdssiiLoggerFactory extends LoggerFactory<org.wdssii.log.Logger> {
    /**
     * The unique instance of this factory.
     */
    private static WdssiiLoggerFactory factory;

    /**
     * Constructs a default factory.
     *
     * @throws NoClassDefFoundError if WDSSII's {@code Log} class was not found on the classpath.
     */
    protected WdssiiLoggerFactory() throws NoClassDefFoundError {
        super(org.wdssii.log.Logger.class);
    }

    /**
     * Returns the unique instance of this factory.
     *
     * @throws NoClassDefFoundError if WDSSII's {@code Log} class was not found on the classpath.
     */
    public static synchronized WdssiiLoggerFactory getInstance() throws NoClassDefFoundError {
        if (factory == null) {
            factory = new WdssiiLoggerFactory();
        }
        return factory;
    }

    /**
     * Returns the implementation to use for the logger of the specified name,
     * or {@code null} if the logger would delegates to Java logging anyway.
     */
    @Override
    protected org.wdssii.log.Logger getImplementation(final String name) {
        return org.wdssii.log.LoggerFactory.getLogger(name);
    }

    /**
     * Wraps the specified {@linkplain #getImplementation implementation} in a Java logger.
     */
    @Override
    protected Logger wrap(String name, org.wdssii.log.Logger implementation) {
        return new WdssiiLoggerAdapter(name, implementation);
    }

    /**
     * Returns the {@linkplain #getImplementation implementation} wrapped by the specified logger,
     * or {@code null} if none.
     */
    @Override
    protected org.wdssii.log.Logger unwrap(final Logger logger) {
        if (logger instanceof org.wdssii.log.Logger) {
            return ((WdssiiLoggerAdapter) logger).logger;
        }
        return null;
    }
}