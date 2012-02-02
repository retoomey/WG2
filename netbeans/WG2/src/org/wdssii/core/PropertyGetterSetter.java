package org.wdssii.core;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.util.StringUtil;

/**
 * A generic property setter that uses a Properties file to set properties on an
 * object. Built on top of java.beans
 * 
 * @author lakshman
 * 
 */
public class PropertyGetterSetter {

    private static Logger log = LoggerFactory.getLogger(PropertyGetterSetter.class);

    /**
     * Invoke any setter method on object if the corresponding property is found
     * in props. If any properties are not set, explanation of why is sent to
     * the debug log.
     * 
     * @param object
     *            whose setter methods should be invoked
     * @param props
     *            contains name=value pairs with the corresponding properties
     */
    @SuppressWarnings("rawtypes")
    public static void setProperties(Object object, Properties props) {
        try {
            BeanInfo p = Introspector.getBeanInfo(object.getClass(),
                    Object.class);

            // Get the list of all keys, including those in defaults backing
            // props
            Enumeration keyIter = props.propertyNames();
            Set<String> keys = new HashSet<String>();
            while (keyIter.hasMoreElements()) {
                keys.add((String) keyIter.nextElement());
            }

            PropertyDescriptor[] propDescriptors = p.getPropertyDescriptors();
            for (PropertyDescriptor propDescriptor : propDescriptors) {
                // get name of property
                String name = propDescriptor.getName();

                if (keys.contains(name) == false) {
                    if (log.isDebugEnabled()) {
                        log.debug("Property "
                                + name
                                + " not set as the key was not found in properties passed in");
                    }
                    continue;
                }
                // find setter method and expected param type
                Method setter = propDescriptor.getWriteMethod();

                // set the property
                String value = props.getProperty(name);
                setProperty(object, setter, name, value);
            }
        } catch (Exception e) {
            log.warn("Properties not set: ", e);
        }
    }

    /**
     * Invoke all getter methods on object
     * 
     * @param object
     *            whose getter methods should be invoked
     * @return properties object that contains name=value pairs with the
     *         corresponding properties
     */
    public static Properties getProperties(Object object) {
        Properties result = new Properties();
        try {
            BeanInfo p = Introspector.getBeanInfo(object.getClass(),
                    Object.class);

            PropertyDescriptor[] propDescriptors = p.getPropertyDescriptors();
            for (PropertyDescriptor propDescriptor : propDescriptors) {
                try {
                    String name = propDescriptor.getName();
                    Method getter = propDescriptor.getReadMethod();
                    String value = getter.invoke(object, (Object[]) null).toString();
                    result.setProperty(name, value);
                    if (log.isDebugEnabled()) {
                        log.debug(name + "=" + value);
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Skipping " + propDescriptor + " for " + propDescriptor.getName() + ": " + e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not get properties: ", e);
        }
        return result;
    }

    /**
     * Invoke setter on object with the provided value.
     * 
     * @param object
     * @param setter
     * @param name
     *            name of property (used only for logging)
     * @param value
     *            value to call setter method with
     * @return whether the setter method succeeded. On failure, see debug log.
     */
    @SuppressWarnings("rawtypes")
    public static boolean setProperty(Object object, Method setter,
            String name, String value) {
        Object settableValue = null;
        try {
            // Find paramType
            Class[] paramTypes = setter.getParameterTypes();
            if (paramTypes.length != 1) {
                if (log.isDebugEnabled()) {
                    log.debug("Not setting "
                            + name
                            + " since the number of parameters in setter method is not 1");
                }
                return false;
            }
            Class paramType = paramTypes[0];

            // convert the value to the appropriate type
            if (value != null) {
                if (String.class.isAssignableFrom(paramType)) {
                    settableValue = value;
                } else if (Boolean.TYPE.isAssignableFrom(paramType)) {
                    if (value.equals("true")) {
                        settableValue = Boolean.TRUE;
                    } else {
                        settableValue = Boolean.FALSE;
                    }
                } else if (Short.TYPE.isAssignableFrom(paramType)) {
                    settableValue = Short.parseShort(value);
                } else if (Integer.TYPE.isAssignableFrom(paramType)) {
                    settableValue = Integer.parseInt(value);
                } else if (Long.TYPE.isAssignableFrom(paramType)) {
                    settableValue = Long.parseLong(value);
                } else if (Float.TYPE.isAssignableFrom(paramType)) {
                    settableValue = Float.parseFloat(value);
                } else if (Double.TYPE.isAssignableFrom(paramType)) {
                    settableValue = Double.parseDouble(value);
                } else if (double[].class.isAssignableFrom(paramType)) {
                    List<String> pieces = StringUtil.split(value);
                    double[] values = new double[pieces.size()];
                    for (int i = 0; i < values.length; ++i) {
                        values[i] = Double.parseDouble(pieces.get(i));
                    }
                    settableValue = values;
                } else if (float[].class.isAssignableFrom(paramType)) {
                    List<String> pieces = StringUtil.split(value);
                    float[] values = new float[pieces.size()];
                    for (int i = 0; i < values.length; ++i) {
                        values[i] = Float.parseFloat(pieces.get(i));
                    }
                    settableValue = values;
                } else if (int[].class.isAssignableFrom(paramType)) {
                    List<String> pieces = StringUtil.split(value);
                    int[] values = new int[pieces.size()];
                    for (int i = 0; i < values.length; ++i) {
                        values[i] = Integer.parseInt(pieces.get(i));
                    }
                    settableValue = values;
                } else {
                    log.debug("Unsupported type: " + paramType);
                    return false;
                }
            }

            // invoke method
            setter.invoke(object, new Object[]{settableValue});
            if (log.isInfoEnabled()) {
                log.info("Set " + name + "=" + settableValue);
            }
            return true;
        } catch (Exception e) {
            log.debug(
                    "Unable to set name= " + name + " value=" + settableValue,
                    e);
            return false;
        }
    }
}
