package org.wdssii.gui.views;

/** The Chart view interface lets us wrap around an RCP view or netbean view 
 * without being coupled to those libraries 
 * 
 * @author Robert Toomey
 *
 */
public interface ChartView extends WdssiiView {

    /** Take a snapshot of the current chart */
    public void takeSnapshot(String name);

    public String getCurrentChoice();

    public void setCurrentChoice(String value);
}
