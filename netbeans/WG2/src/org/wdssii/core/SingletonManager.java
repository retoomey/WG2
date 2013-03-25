package org.wdssii.core;

import java.net.URL;
import java.util.ArrayList;
import org.wdssii.xml.wdssiiConfig.Tag_setup;

/**
 * SingletonManager handles creation order of all Singleton classes Override to
 * create your singletons in the setupSingleton method
 *
 * @author Robert Toomey
 *
 */
public class SingletonManager {

    private static SingletonManager instance = null;
    // Don't let it be read while it's reading...
    private final Object wdssiiReadLock = new Object();
    private Tag_setup theWdssiiXML;
    private ArrayList<Singleton> mySingletons = new ArrayList<Singleton>();
    
    protected SingletonManager() {
        try {
            synchronized (wdssiiReadLock) {
                URL u = W2Config.getURL("wdssii.xml");
                theWdssiiXML = new Tag_setup();
                theWdssiiXML.processAsRoot(u);
            }

        } catch (Exception e) {
            //System.out.println("*********Exception reading setup configuration file:"+e.toString());
        }
    }

    public Tag_setup getSetupXML() {
        synchronized (wdssiiReadLock) {
            return theWdssiiXML;
        }
    }

    /**
     * Control the order and creation of Singletons
     */
    public void setupSingletons() {
    }
    
    public void add(Singleton s){
        mySingletons.add(s);
    }
    
    public void notifyAllCreated(){
        for(Singleton s:mySingletons){
            s.singletonManagerCallback();
        }
    }

    public static void setInstance(SingletonManager m) {
        instance = m;
        instance.setupSingletons();
    }

    public static SingletonManager getInstance() {
        return instance;
    }
    
    /** Still kind of a hack. Plug for low level to notify the higher levels */
    public void notifyDataRequestDone(){
        
    }
}
