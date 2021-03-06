package org.wdssii.gui;

import org.wdssii.core.CommandManager;
import org.wdssii.core.SingletonManager;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataRequest.DataRequestGlobalListener;
import org.wdssii.gui.commands.ProductChangeCommand;

/**
 * The SingletonManager for the display
 *
 * @author Robert Toomey
 */
public class GUISingletonManager extends SingletonManager implements DataRequestGlobalListener  {

    private GUISingletonManager() {
    }

    public static void setup() {
        SingletonManager.setInstance(new GUISingletonManager());
    }

    /**
     * Control the order and creation of Singletons
     */
    @Override
    public void setupSingletons() {
        add(PreferencesManager.create());
        PreferencesManager.introduce(new XMLPrefHandler());
        add(ProductManager.create());
        add(CommandManager.create());
        add(SourceManager.create());
        add(GLCacheManager.create());
    
        DataRequest.setDataRequestGlobalListener(this);
        notifyAllCreated();
    }

    @Override
    public void notifyDataRequestDone() {
        CommandManager.getInstance().fireUpdate(new ProductChangeCommand());
    }
}
