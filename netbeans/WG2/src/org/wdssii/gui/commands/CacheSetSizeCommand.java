package org.wdssii.gui.commands;

//import org.eclipse.jface.dialogs.IInputValidator;
//import org.eclipse.jface.dialogs.InputDialog;
//import org.eclipse.jface.window.Window;
//import org.eclipse.swt.widgets.Display;
import org.wdssii.gui.ProductManager;

/** Called by name from WdssiiDynamic */
public class CacheSetSizeCommand extends WdssiiCommand {

    /**
     * This class validates a String. It makes sure that the String is between 5 and 8
     * characters
     */
    /*private class LengthValidator implements IInputValidator {
    
     * Validates the String. Returns null for no error, or an error message
     * 
     * @param newText the String to validate
     * @return String
     *
    @Override
    public String isValid(String newText) {
    //	int len = newText.length();
    int minCacheSize = ProductManager.MIN_CACHE_SIZE;  // FIXME: get from cache, and/or make a class that takes range
    int maxCacheSize = ProductManager.MAX_CACHE_SIZE;
    
    int value = 0;
    try{
    value = Integer.valueOf(newText);
    }catch(NumberFormatException e){
    return "Enter a number";
    }
    if (value < minCacheSize){
    return "Too small a number";
    }
    if (value > maxCacheSize){
    return "Too large a number";
    }
    
    // Input must be OK
    return null;
    }
    }*/
    @Override
    public boolean execute() {

        // FIXME: Make a parameter that allows us to bypass dialog...?

        // Bring up a model dialog to get cache size.  We will pin the size
        // between the min and max.
        int currentSize = ProductManager.getInstance().getCacheSize();

        /*InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
        "", "Enter cache size between "+ProductManager.MIN_CACHE_SIZE+" and "
        +ProductManager.MAX_CACHE_SIZE, Integer.toString(currentSize), new LengthValidator());
        if (dlg.open() == Window.OK) {
        // User clicked OK; update the label with the input
        // label.setText(dlg.getValue());
        ProductManager.getInstance().setCacheSize(Integer.valueOf(dlg.getValue()));
        //System.out.println("Value is "+dlg.getValue());
        }*/

        return true;
    }
}