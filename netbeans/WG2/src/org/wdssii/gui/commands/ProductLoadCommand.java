package org.wdssii.gui.commands;

import java.util.Date;
import org.wdssii.gui.ProductManager;

/** Command that loads a product into the display
 * @author Robert Toomey
 */
public class ProductLoadCommand extends ProductCommand {

    private ProductLoadCaller myCaller;
    private String myIndexKey;
    private String myDataType;
    private String mySubType;
    private Date myTime;

    /** The reason for the load product...where it's from. 
     * FIXME: still debating this vs separate subclasses.  Not sure how the reflection will
     * or should work in this case
     */
    public static enum ProductLoadCaller {

        FROM_RECORD_PICKER,
        FROM_TIME_LOOPER
    };

    /** Since we don't call this from rcp, we have a set parameter list in the constructor.
     */
    public ProductLoadCommand(ProductLoadCaller caller, String indexKey,
            String datatype, String subtype, Date time) {
        this.myCaller = caller;
        this.myIndexKey = indexKey;
        this.myDataType = datatype;
        this.mySubType = subtype;
        this.myTime = time;
    }

    @Override
    public boolean execute() {
        switch (myCaller) {  // FIXME: subclass or not?
            case FROM_RECORD_PICKER: {          
                ProductManager.getInstance().recordPickerSelectedProduct(myIndexKey, myDataType, mySubType, myTime);
            }
            break;
            case FROM_TIME_LOOPER: {
                ProductManager.getInstance().recordPickerSelectedProduct(myIndexKey, myDataType, mySubType, myTime);
            }
            break;
            default:
                break;
        }
        return true;
    }
}
