package org.geotools.util.logging;

import java.util.logging.Level;

/**
 * An adapter that redirect all Java logging events to the whatever Wdssii
 * is logging to.  Sadly, it 'could' be logging to java.util.logging, lol.
 * Normally, WDSSII logs to sl4fj with the logback plugin.  
 * 
 * @author Robert Toomey
 *
 */
final class WdssiiLoggerAdapter extends LoggerAdapter {

    /**
     * The WDSSII logger to use.
     */
    final org.wdssii.log.Logger logger;
    
    /** Keep a 'state' of logging */
    Level javaUtilLevel = Level.INFO;

    /**
     * Creates a new logger.
     *
     * @param name The logger name.
     * @param logger The result of {@code Logger.getLogger(name)}.
     */
    public WdssiiLoggerAdapter(final String name, final org.wdssii.log.Logger logger) {
        super(name);
        this.logger = logger;
    }

    /**
     * Set the level for this logger.
     */
    @Override
    public void setLevel(final Level level) {
        javaUtilLevel = level;
    }

    /**
     * Returns the level for this logger.
     */
    @Override
    public Level getLevel() {
        return javaUtilLevel;
    }

    /**
     * Returns {@code true} if the specified level is loggable.
     */
    @Override
    public boolean isLoggable(final Level level) {
        // All s4jf levels are loggable...s4jf will filter out messages
        return true;
    }

    /**
     * Logs a record at the specified level.
     */
    @Override
    public void log(final Level level, final String message) {

        final int n = level.intValue();
        switch (n / 100) {
            default:
                logger.error("**FATAL**"+message);
                break;
            case 10:
                logger.error(message);
                break;
            case 9:
                logger.warn(message);
                break;
            case 8:
            case 7:
                logger.info(message);
                break;
            case 6:
            case 5:
                logger.debug(message);
                break;
            case 4:
                logger.trace(message);
                break;
            case 3:                              
            case 2:                                 
            case 1:                                        
            case 0: // ALl?  what to do here, guess info for now.
                logger.info(message);
        }
    }

    /**
     * Logs a record at the specified level.
     */
    @Override
    public void log(final Level level, final String message, final Throwable thrown) {
        log(level, message);
    }

    @Override
    public void severe(String message) {
        logger.error(message);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void config(String message) {
        logger.info(message);
    }

    @Override
    public void fine(String message) {
        logger.debug(message);
    }

    @Override
    public void finer(String message) {
        logger.debug(message);
    }

    @Override
    public void finest(String message) {
        logger.trace(message);
    }
}
