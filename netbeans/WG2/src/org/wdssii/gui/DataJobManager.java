package org.wdssii.gui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Runs a background queue for job generating opengl display objects, for the moment at least.
 * Queues up jobs with an LRU priority (User browses to next RadialSet, we want to make THAT radial
 * set the priority since they are looking at it now, but we still finish the older RadialSet)
 * 
 * Using this wrapper class also allows me to do 'lazy' updating jobs if needed, where
 * we simple check system clock in a single thread. (If I can't get eventually get all the sync
 * stuff done correctly)
 * 
 * @author Robert Toomey
 *
 */
public class DataJobManager implements Singleton {

    private static DataJobManager instance = null;
    private Executor myExecutor = null;

    public static DataJobManager getInstance() {
        if (instance == null) {
            instance = new DataJobManager();
            SingletonManager.registerSingleton(instance);
        }
        return instance;
    }

    private DataJobManager() {
        myExecutor = Executors.newFixedThreadPool(3);
    }

    @Override
    public void singletonManagerCallback() {
        // TODO Auto-generated method stub
    }
}
