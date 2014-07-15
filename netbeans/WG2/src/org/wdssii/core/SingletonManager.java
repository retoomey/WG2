package org.wdssii.core;

import java.util.ArrayList;

/**
 * SingletonManager handles creation order of all Singleton classes Override to
 * create your singletons in the setupSingleton method
 *
 * @author Robert Toomey
 *
 */
public class SingletonManager {

    private static SingletonManager instance = null;
   
    private ArrayList<Singleton> mySingletons = new ArrayList<Singleton>();
    
    protected SingletonManager() {
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
