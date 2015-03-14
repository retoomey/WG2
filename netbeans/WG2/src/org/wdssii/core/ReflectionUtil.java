package org.wdssii.core;

import java.lang.reflect.Constructor;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Since we will use reflection a lot to reduce coupling, we will migrate it all
 * to here to making code tracking easier
 *
 * @author Robert Toomey
 */
public class ReflectionUtil {

    private final static Logger LOG = LoggerFactory.getLogger(ReflectionUtil.class);

    /**
     * Create a simple class by name with default constructor, allow failure and
     * return success
     */
    public static <T> T optionalCreate(String fullname, Class<?> clazz) {
        Object thing = null;
        Class<?> aClass = null;
        try {
            aClass = Class.forName(fullname);
            Constructor<?> c = aClass.getConstructor();
            thing = aClass.newInstance();
            if (!clazz.isInstance(thing)){
                LOG.info("Reflected class "+fullname+", but it wasn't expected class type..");
                thing = null;
            }
            LOG.info("Reflected class "+fullname);
        } catch (Exception e) {
            // We're optional class, just don't use it..or maybe warn here
            LOG.warn("Couldn't create optional class "+fullname);
        }
        return (T) thing;

        /*aClass = Class.forName(createByName);
         Class<?>[] argTypes = new Class[]{NetcdfFile.class, boolean.class};
         Object[] args = new Object[]{ncfile, sparse}; // Actual args

         //DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse)
         //Constructor<?> c = aClass.getConstructor(argTypes);
         Object classInstance = aClass.newInstance();
         Method aMethod = aClass.getMethod("createFromNetcdf", argTypes);
         obj = (DataType) aMethod.invoke(classInstance, args);
         */
    }
}
