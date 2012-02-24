package org.geotools.util.logging;

import java.util.logging.Logger;

/**
 *
 * @author Robert Toomey
 * 
 * A logger that just uses whatever logger SLf4J has already bound too.
 * Geotools currently uses the java.util.Logger class directly, this 
 * emulates it.
 * 
 * Sl4jf already handles swapping out logging systems, I'd recommend geotools
 * switch to sl4fj for logging and include the sl4j-jcl jar instead of
 * commons.  Then none of these classes are necessary, including org.geotools.util.logging
 * 
 */
public class Slf4jLoggerFactory extends LoggerFactory<org.slf4j.Logger> {
    /**
     * The unique instance of this factory.
     */
    private static Slf4jLoggerFactory factory;

    /**
     * Constructs a default factory.
     *
     * @throws NoClassDefFoundError if Slf4j's {@code Log} class was not found on the classpath.
     */
    protected Slf4jLoggerFactory() throws NoClassDefFoundError {
        super(org.slf4j.Logger.class);
    }

    /**
     * Returns the unique instance of this factory.
     *
     * @throws NoClassDefFoundError if Slf4j's {@code Log} class was not found on the classpath.
     */
    public static synchronized Slf4jLoggerFactory getInstance() throws NoClassDefFoundError {
        if (factory == null) {
            factory = new Slf4jLoggerFactory();
        }
        return factory;
    }

    /**
     * Returns the implementation to use for the logger of the specified name,
     * or {@code null} if the logger would delegates to Java logging anyway.
     */
    @Override
    protected org.slf4j.Logger getImplementation(final String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

    /**
     * Wraps the specified {@linkplain #getImplementation implementation} in a Java logger.
     */
    @Override
    protected Logger wrap(String name, org.slf4j.Logger implementation) {
        return new Slf4jLogger(name, implementation);
    }

    /**
     * Returns the {@linkplain #getImplementation implementation} wrapped by the specified logger,
     * or {@code null} if none.
     */
    @Override
    protected org.slf4j.Logger unwrap(final Logger logger) {
        if (logger instanceof org.slf4j.Logger) {
            return ((Slf4jLogger) logger).logger;
        }
        return null;
    }
}