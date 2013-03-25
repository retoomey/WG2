package org.wdssii.core;

/** Button status used to update the product navigation buttons.
 * It is up to the navigation GUI to use the IconString and color info
 * to set the display accordingly.
 * 
 * @author Robert Toomey
 *
 */
public class ProductButtonStatus {

    private String myButtonText = "None";
    private String myToolTip = "None";
    private boolean myValid = false;
    private boolean myUseColor = false;
    private int myRed = 0;
    private int myGreen = 0;
    private int myBlue = 0;
    private String myIconString = "";
    private WdssiiCommand myCommand = null;
    private boolean myEnabled = true;

    public ProductButtonStatus() {
    }

    public boolean getUseColor() {
        return myUseColor;
    }

    public void setNoColor() {
        myUseColor = false;
    }

    public void setButtonText(String t) {
        myButtonText = t;
    }

    public String getButtonText() {
        return myButtonText;
    }

    public WdssiiCommand getCommand() {
        return myCommand;
    }

    public void setCommand(WdssiiCommand c) {
        myCommand = c;
    }

    public void setToolTip(String t) {
        myToolTip = t;
    }

    public String getToolTip() {
        return myToolTip;
    }

    public void setColor(int red, int green, int blue) {
        myRed = red;
        myGreen = green;
        myBlue = blue;
        myUseColor = true;
    }

    public int getRed() {
        return myRed;
    }

    public int getGreen() {
        return myGreen;
    }

    public int getBlue() {
        return myBlue;
    }

    public void setIconString(String s) {
        myIconString = s;
    }

    public String getIconString() {
        return myIconString;
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    public boolean getEnabled() {
        return myEnabled;
    }
}
