package org.wdssii.gui.commands;

/** Called by name from WdssiiDynamic
 * @author Robert Toomey
 * 
 */
public class SourceDisconnectCommand extends SourceClearCommand {

    /** Called from rcp button click, this means disconnect the selected source */
    public SourceDisconnectCommand() {
    }

    /** Called by programming */
    public SourceDisconnectCommand(String key) {
        setIndexName(key);
    }

    @Override
    public boolean execute() {

        // Clear cache items...
        super.execute();

        // Delete the index
        String key = getIndexName();
        if (key != null) {
            disconnect(getIndexName());
        }

        return true;
    }
}
