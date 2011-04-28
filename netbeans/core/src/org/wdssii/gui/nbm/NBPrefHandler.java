package org.wdssii.gui.nbm;

import org.wdssii.gui.PreferencesManager.PreferenceHandler;
import org.openide.util.NbPreferences;

/**
 * This preference handler uses the Netbeans preference system to store
 * preferences for the application.
 * 
 * @author Robert Toomey
 */
public class NBPrefHandler implements PreferenceHandler 
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
         return NbPreferences.forModule(NBPrefHandler.class).getBoolean(key, false);
    }

    @Override
    public void setBoolean(String key, boolean value){
         NbPreferences.forModule(NBPrefHandler.class).putBoolean(key, value);
    }
    
    @Override
    public int getInteger(String key) {
         return NbPreferences.forModule(NBPrefHandler.class).getInt(key, 0);
    }

    @Override
    public void setInteger(String key, int i) {
        NbPreferences.forModule(NBPrefHandler.class).putInt(key, i);
    }

    @Override
    public void setValue(String key, boolean value) {
         NbPreferences.forModule(NBPrefHandler.class).putBoolean(key, value);
    }

    @Override
    public void setValue(String key, int value) {
         NbPreferences.forModule(NBPrefHandler.class).putInt(key, value);
    }
    
}
