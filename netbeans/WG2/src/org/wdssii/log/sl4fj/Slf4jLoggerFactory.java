package org.wdssii.log.sl4fj;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import org.slf4j.ILoggerFactory;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Our link to Slf4j logging and our logback extension
 *
 * @author Robert Toomey
 */
public class Slf4jLoggerFactory extends LoggerFactory {

    public String firstMessage;
    
    public Slf4jLoggerFactory(){
        firstMessage = checkForLogback();
    }
    
    public Logger getLoggerImpl(Class<?> aClass) {
        return new Slf4jLogger(aClass);
    }
    
    public Logger getLoggerImpl(String string) {
        return new Slf4jLogger(string);
    }

    /**
     * Initialize the logback system. If we're bound to it.
     *
     * If SLF4J is bound to logback in the current environment, then we manually
     * assign the logback.xml file. If deployed as a jar, we put the logback.xml
     * in the same directory. I don't want it inside the jar so that it can
     * easily be modified for debugging without having to know how to get it
     * in/out of the jar. Jars assume the classpath is only the jar typically by
     * default.
     *
     * So basically: 1. For deployment there is a user.dir such as
     * "WG2-timestamp" and the logback.xml file will be in this folder with the
     * deployed jar. 2. For development in the IDE the user.dir will be the root
     * IDE folder where I have a debug logback.xml by default.
     */
    public final String checkForLogback() {
        String message = null;

        ILoggerFactory ilog = org.slf4j.LoggerFactory.getILoggerFactory();
        if (ilog instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) ilog;

            try {

                // Find the logback.xml file.  Otherwise we're stuck with
                // the default logback output.
                // For jar deployment this will be where the windows.bat, WG2.jar
                // is at.  For IDE running this will be the 'root' folder
                // of the IDE.  I have two logback.xml files, one for deployment
                // in util/run and another for debugging in IDE.
                String dir = System.getProperty("user.dir") + "/logback.xml";
                boolean exists = (new File(dir)).exists();

                // Problem with this is that it causes logging to happen,
                // and we aren't ready yet...
                // URL aURL = W2Config.getURL("logback.xml");
                if (exists) {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(context);
                    // Call context.reset() to clear any previous configuration, e.g. default 
                    // configuration. For multi-step configuration, omit calling context.reset().
                    context.reset();
                    configurator.doConfigure(dir);
                    message = "Logback configuration file " + dir;
                } else {
                    message = "Couldn't find logback configuration file, default logging is on";
                }
            } catch (JoranException je) {
                // StatusPrinter will handle this
            }
            // StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
        return message;
    }
}
