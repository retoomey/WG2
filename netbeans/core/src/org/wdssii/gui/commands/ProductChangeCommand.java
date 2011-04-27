package org.wdssii.gui.commands;

/**
 * A commmand that represents a change in a product, a change due to updating/moving,
 * toggling only mode, etc.  Usually this means a vslice must change or a table, etc...anything looking at
 * a product.
 * 
 * There isn't a 'direct' action for this command, it's more of an event.
 * @author Robert Toomey
 *
 */
public class ProductChangeCommand extends ProductCommand {

    /** A filter has changed due to direct GUI interaction.  The filter view needs to know if just a filter changed so it doesn't regenerate
     * its GUI */
    public static class ProductFilterCommand extends ProductChangeCommand {
    }

    @Override
    public boolean execute() {
        return true;
    }
}
