package org.wdssii.gui.commands;

import org.wdssii.gui.sources.Source;

/**
 * A source is 'selected'.  Usually this just updates GUI controls for this
 * source
 * @author Robert Toomey
 */
public class SourceSelectCommand extends SourceCommand {
    Source mySource = null;
    
    /** Select an LLHArea by known keyname */
    public SourceSelectCommand(String key) {
        mySourceKey = key;
    }
    
    /** Select an LLHArea by known contained object */
    public SourceSelectCommand(Source area){
        mySource = area;
    }

    @Override
    public boolean execute() {
        if (mySourceKey != null){
            myList.setSelected(mySourceKey);
        }else{
            myList.setSelected(mySource);
        }
      //  CommandManager.getInstance().getEarthBall().updateOnMinTime();
        return true;
    }
}
