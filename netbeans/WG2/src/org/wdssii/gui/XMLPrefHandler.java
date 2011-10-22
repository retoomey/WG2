package org.wdssii.gui;

import org.wdssii.gui.PreferencesManager.PreferenceHandler;

/**
 * This preference handler will store preferences in an XML file
 * FIXME: make it work
 * 
 * @author Robert Toomey
 */
public class XMLPrefHandler implements PreferenceHandler 
{

    @Override
    public void setDefault(String name, boolean value) {
        setBoolean(name, value);
    }

  //  @Override
  //  public void setDefault(String name, String value) {
  //     setString(name, value);
  //  }

    @Override
    public void setDefault(String name, int value) {
        setInteger(name, value);
    }

    @Override
    public boolean getBoolean(String key) {
         return false;
    }

    @Override
    public void setBoolean(String key, boolean value){
         
    }
    
    @Override
    public int getInteger(String key) {
         return 0;
    }

    @Override
    public void setInteger(String key, int i) {
        
    }

    @Override
    public void setValue(String key, boolean value) {
         
    }

    @Override
    public void setValue(String key, int value) {
         
    }
    
}
