package org.wdssii.gui.products.navigators;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.ProductJumpToCommand;
import org.wdssii.gui.commands.WdssiiCommand;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveLatestBase;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveLatestDown;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveLatestTime;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveLatestUp;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveNextSubType;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveNextTime;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMovePreviousLowestSubType;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMovePreviousSubType;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMovePreviousTime;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveSyncCurrent;

/** The ProductNavigator is a helper class of Product that handles all navigation
 * stuff in the Navigation view.
 * This means, like all helpers if you want different functionality for a Product, such
 * as RadialSet, you create a RadialSetNavigator class that subclasses this.
 * 
 * FIXME: Could have this create have more GUI control for the product, this
 * would allow more complicated controls for a selected product.  Remember though that
 * the subclass of this won't exist until the product is read into memory.
 * 
 * @author Robert Toomey
 *
 */
public class ProductNavigator {

    /** Get the WdssiiCommand for each item in the NavView grid.  Subclasses can
     * override to change the appearance/actions of the grid.  Standard products have
     * a 'cross' of time and subtype with a virtual subtype at the end.
     * Subclasses can override to create their own commands.  The commands also
     * handle the 'status' text/etc of the GUI controls in the grid.
     */
    public WdssiiCommand getGridCommand(int gridNum) {
        WdssiiCommand c = null;
        switch (gridNum) {
            case 0:
                break;
            case 1:
                c = getNextSubtypeCommand();
                break;
            case 2:
                break;

            // Virtual volume at the right side
            case 3:
                c = getLatestUpCommand();
                break;
            case 11:
                c = getLatestDownCommand();
                break;
            case 15:
                c = getLatestBaseCommand();
                break;

            // Time row, left to right
            case 4:
                c = getPreviousTimeCommand();
                break;
            case 5:
                c = getSyncCommand();
                break;
            case 6:
                c = getNextTimeCommand();
                break;
            case 7:
                c = getLatestTimeCommand();
                break;

            case 8:
                c = getJumpToCommand();
                break;
            case 9:
                c = getPreviousSubtypeCommand();
                break;
            case 10:
                break;

            // Subclasses aren't allowed to change the loop button
            case 12:
                c = new AnimateCommand();
                break;
            case 13:
                c = getCurrentBaseCommand();
                break;

            case 14:
                break;
            case 16:
                break;
            default:
                break;
        }
        return c;
    }

    /** Default previous time handler.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getPreviousTimeCommand() {
        return new ProductMovePreviousTime();
    }

    /** Default sync to time handler.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getSyncCommand() {
        return new ProductMoveSyncCurrent();
    }

    /** Default next time handler.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getNextTimeCommand() {
        return new ProductMoveNextTime();
    }

    /** Default move to latest time handler.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getLatestTimeCommand() {
        return new ProductMoveLatestTime();
    }

    // Subtype navigation commands.....
    /** Default move next subtype handler.  Subclasses can override to change behavior/layout
     * FIXME: should modify for mode vs elevation products I think */
    public WdssiiCommand getNextSubtypeCommand() {
        return new ProductMoveNextSubType();
    }

    /** Default move previous subtype handler.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getPreviousSubtypeCommand() {
        return new ProductMovePreviousSubType();
    }

    // Virtual volume commands ....
    /** Default move latest 'up' subtype.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getLatestUpCommand() {
        return new ProductMoveLatestUp();
    }

    /** Default move latest 'down' subtype.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getLatestDownCommand() {
        return new ProductMoveLatestDown();
    }

    /** Default move latest 'base' subtype.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getLatestBaseCommand() {
        return new ProductMoveLatestBase();
    }

    /** Default move 'base' subtype.  Subclasses can override to change behavior/layout */
    public WdssiiCommand getCurrentBaseCommand() {
        return new ProductMovePreviousLowestSubType();
    }

    /** Default jump to command  Subclasses can override to change behavior/layout */
    public WdssiiCommand getJumpToCommand() {
        ProductJumpToCommand p= new ProductJumpToCommand();
        // For now, directly link to the single world view we have.
        // This will change if/when we go multiple groups/earthballs 
        p.setTargetListener( CommandManager.getInstance().getEarthBall());
        return p;
    }
}
