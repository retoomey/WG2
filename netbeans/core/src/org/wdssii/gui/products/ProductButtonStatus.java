package org.wdssii.gui.products;

//import org.eclipse.swt.graphics.Image;
//import org.eclipse.swt.graphics.PaletteData;
//import org.eclipse.swt.graphics.RGB;
//import org.eclipse.swt.widgets.Display;
import org.wdssii.gui.commands.WdssiiCommand;
//import org.wdssii.gui.swt.widgets.BuiltInIcons;
import org.wdssii.gui.util.FileUtil;
import org.wdssii.index.IndexRecord;

/** Button status used to update WJButtons.
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
    private IndexRecord myRecord = null;
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

    public void setValidRecord(boolean flag) {
        myValid = flag;
    }

    public boolean getValidRecord() {
        return myValid;
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

    /*public Image getIcon(Display display) {
    Image image = null;
    PaletteData paletteData = new PaletteData(
    new RGB[] { new RGB(255, 255, 255), new RGB(0, 255, 0),
    new RGB(0, 0, 255) });
    if (myIconString == "") {
    image = BuiltInIcons.noIcon(paletteData, display);
    } else {
    image = BuiltInIcons
    .imageByName(myIconString, paletteData, display);
    if (image == null){
    image =  FileUtil.imageFromFile(display,myIconString);
    }
    }
    return (image);
    }*/
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

    public void setIndexRecord(IndexRecord record) {
        myRecord = record;
    }

    public IndexRecord getIndexRecord() {
        return myRecord;
    }
}
