package org.wdssii.gui.sources;

import java.net.URL;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A 'source' is a loadable source of data for the display The hierarchy here is
 * similar to features, we have a list of stuff and then a GUI for each type.
 * This might be generalized even more later, for now keeping the code separate.
 * Most likely will merge common stuff to a common superclass/interface
 *
 * @author rtoomey
 */
public class Source {

    /**
     * Each Source gets a short key of the form 'i'+number, this shrinks memory
     * and allows us to change the visible name of source
     */
    private static int indexCounter = 1;
    
    /**
     * What is our key?
     */
    private String myKey = "";
 
    /** What is our visible name?
     * 
     */
    private String myVisibleName = "";
    
    /** What is the URL we use to refer to our source?  I'm assuming only 1
     * here, if a source had multiple this is the one it wants shown in the
     * table GUI.
     */
    private URL myURL;
    
    private synchronized String getNewKeyName() {
        String newKey = String.format("s%d", indexCounter++);
        return (newKey);
    }
    
    public Source(String niceName, URL aURL) {
        myKey = getNewKeyName();
        myURL = aURL;
        myVisibleName = niceName; 
    }

    /**
     * Get the key for this source
     */
    public String getKey() {
        return myKey;
    }

    /**
     * Set the key for this source
     */
    public void setKey(String n) {
        myKey = n;
    }
    
    /**
     * Get the visible name for this source
     */
    public String getVisibleName() {
        return myVisibleName;
    }
    
    /** Get the shown type name for this source
     * The GUI uses this to show what type we think we are
     */
    public String getShownTypeName(){
        return "SOURCE";
    }
    
    /**
     * Set the key for this featuremyTopSelectedSource
     */
    public void setVisibleName(String n) {
        myVisibleName = n;
    }

        
    public String getURLString(){
        if (myURL != null){
            return myURL.toString();
        }else{
            return "";
        }
    }
    
    public URL getURL(){
        return myURL;
    }
    
    public void setURL(URL u){
        myURL = u;
    }
    
    public void setupSourceGUI(JComponent source) {

        // Set the layout and add our controls
        source.setLayout(new java.awt.BorderLayout());
        JTextField t = new JTextField();
        t.setText("No controls for this source");
        t.setEditable(false);
        source.add(t, java.awt.BorderLayout.CENTER);
        source.doLayout();

        updateGUI();
    }

    public void updateGUI() {
    }

    /** Called by GUI right before background connection job */
    public boolean aboutToConnect(boolean start){
        return true;
    }
    
    /** Connect to a source if needed.  Default is just true */
    public boolean connect(){
        return true;
    }
    
    /** Disconnect to a source if needed. */
    public void disconnect(){
        
    }
    
    public boolean isConnected(){
        return true;
    }
    
    public boolean isConnecting(){
        return false;
    }
    
    public boolean isRealtime(){
        return false;
    }

    /** Called by GUI when this source is selected, to place in the
     * info bar at top of sources view 
     */
    public String getSourceDescription() {
        return getVisibleName();
    }
}
