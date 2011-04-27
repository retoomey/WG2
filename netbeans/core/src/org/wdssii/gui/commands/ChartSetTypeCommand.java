package org.wdssii.gui.commands;

import java.util.ArrayList;
import org.wdssii.gui.SingletonManager;
import org.wdssii.gui.WdssiiXMLAttributeList;
import org.wdssii.gui.WdssiiXMLAttributeList.WdssiiXMLAttribute;
import org.wdssii.gui.WdssiiXMLCollection;
import org.wdssii.gui.WdssiiXMLDocument;
import org.wdssii.gui.views.ChartView;
import org.wdssii.gui.commands.WdssiiCommand.WdssiiMenuList;

/** Command to set the chart type for the current chart window
 * 
 * @author Robert Toomey
 *
 */
public class ChartSetTypeCommand extends WdssiiCommand implements WdssiiMenuList {

    /** Get the list of suboptions for command.  Sort them in drop-down or dialog order */
    @Override
    public ArrayList<MenuListItem> getSuboptions() {

        // Fill in drop down from the XML
        ArrayList<MenuListItem> options = new ArrayList<MenuListItem>();
        WdssiiXMLDocument doc = SingletonManager.getInstance().getSetupXML();
        if (doc != null) {
            WdssiiXMLCollection c = doc.get("charts");
            if (c != null) {
                // bet I'm gonna get sync errors here....maybe not, we shouldn't
                // be still reading in the xml by this time.  Could be though.
                ArrayList<String> nameCopy = c.getNames();
                for (String s : nameCopy) {
                    WdssiiXMLAttributeList a = c.get(s);
                    WdssiiXMLAttribute show = a.get("show");
                    WdssiiXMLAttribute gui = a.get("guiString");
                    if (show.getBoolean()) {
                        //options.add(gui.getString());	
                        options.add(new MenuListItem(gui.getString(), gui.getString()));
                    } else {
                        //options.add("DONT SHOW"+gui.getString());
                    }
                }
            }
        }
        return options;
    }

    /** During RCP updateElements, each element of the list needs updating. */
    @Override
    public String getCurrentOptionInfo() {

        String choice = null;
        if (myWdssiiView != null) {
            if (myWdssiiView instanceof ChartView) {
                choice = ((ChartView) myWdssiiView).getCurrentChoice();
            }
        }
        if (choice == null) {
            choice = getFirstChartChoice();
        }
        return choice;
    }

    /** Get the start-up chart choice */
    public static String getFirstChartChoice() {

        String defaultName = null;

        WdssiiXMLDocument doc = SingletonManager.getInstance().getSetupXML();
        /*	if (doc != null){
        WdssiiXMLCollection c = doc.get("charts");
        if (c != null){
        WdssiiXMLAttributeList a = c.getDefaultList();
        if (a != null){
        WdssiiXMLAttribute gui = a.get("guiString");
        if (gui != null){
        defaultName = gui.getString();
        }
        }
        
        }
        }*/

        try {
            defaultName = doc.get("charts").getDefaultList().get("guiString").getString();
        } catch (java.lang.NullPointerException e) {
            // it's ok, just means missing xml in chain...	
        }
        return defaultName;
    }

    /** Get the checked suboption...passing in active view (For example, each chart view has a drop
     * down that is view dependent */
    @Override
    public boolean execute() {
        // Get the parameter out of us.  Should be "wdssii.ChartSetTypeParameter"
        if (myParameters != null) {
            String value = myParameters.get("wdssii.ChartSetTypeParameter");

            // Null choice currently means button was picked..should bring up dialog..
            if (value != null) {
                // Need the view in order to send the command...
                if (myWdssiiView != null) {
                    if (myWdssiiView instanceof ChartView) {
                        ((ChartView) myWdssiiView).setCurrentChoice(value);
                    }
                }
            }
        } else {
            System.out.println("EXECUTE ChartSetType without any params (FIXME: Dialog for user)");

            // Ok this is the 'top' button..not the drop menu...so bring up a dialog or do nothing?
            // Need the view though...
        }
        return false;  // The chart setCurrentChoice updates the GUI of the chart.
    }
}
