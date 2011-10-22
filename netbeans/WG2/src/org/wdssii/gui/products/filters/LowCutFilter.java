package org.wdssii.gui.products.filters;

import java.util.ArrayList;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.gui.GUISetting;
import org.wdssii.gui.products.volumes.ProductVolume;

/** A filter that removes/changes data values below a certain value.
 * This is one of the prove of concept filters (for now at least)
 * @author Robert Toomey
 *
 */
public class LowCutFilter extends DataFilter {

    private float myCutOff = 0.0f;

    public LowCutFilter() {
        setDisplayedName("LowCut");
    }

    @Override
    public void f(DataTypeQuery q) {
        // Anything lower than the data value is turned into missing
        if (isEnabled()) {
            if (q.outDataValue < myCutOff) {
                q.outDataValue = DataType.MissingData;
            }
        }
    }

    @Override
    public ArrayList<GUISetting> getGUISettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKey() {
        return getDisplayedName() + isEnabled() + myCutOff;
    }

    @Override
    public Object getNewGUIBox(Object parent) {
        /*
        final Composite box = new Composite((Composite)parent, SWT.NONE);
        box.setLayout(new RowLayout());
        
        // SRV 
        Text srv = new Text(box, SWT.LEFT);
        srv.setText("LowCutOff:");
        srv.setEditable(false);
        final Spinner speedSpin = new Spinner(box, 0);
        speedSpin.setMaximum(200);
        speedSpin.setMinimum(-200);
        speedSpin.setSelection((int)myCutOff);
        
        // Add listeners for GUI elements
        speedSpin.addSelectionListener(new SelectionListener(){
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {					
        }
        
        @Override
        public void widgetSelected(SelectionEvent arg0) {
        myCutOff = speedSpin.getSelection();
        //myDirtySRM = true;
        fireFilterChangedEvent();
        }
        
        }
        );
        
        return box;
         *
         */
        return null;
    }

    @Override
    public void prepFilterForVolume(ProductVolume v) {
        // We don't care about volume for this simple filter
    }
}
