package org.wdssii.gui.products.filters;

import java.util.ArrayList;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.datatypes.RadialSet.RadialSetQuery;
import org.wdssii.gui.GUISetting;
import org.wdssii.gui.products.volumes.RadialSetVolume;
import org.wdssii.gui.products.volumes.ProductVolume;

/** A filter that applys Storm Relative Motion to a RadialSet or RadialSetVolume 
 * @author Robert Toomey
 */
public class StormRMFilter extends DataFilter {

    public float mySpeedMS = 0.0f;
    public float myDegrees = 0.0f;
    // We can store the SRM deltas for a single RadialSet.  This is fine, since
    // each product handler has a unique filter instance...
    public RadialSetVolume myLastRadialVolume = null;
    ArrayList<ArrayList<Float>> myDeltaList = null;
    public boolean myDirtySRM = true;

    /** Prep filter for a volume...we need to make sure the delta for SRM is calculated for all... */
    @Override
    public void prepFilterForVolume(ProductVolume v) {
        if (v != null) {

            if (v instanceof RadialSetVolume) {
                RadialSetVolume r = (RadialSetVolume) (v);

                // Only recalculate if the volume changed or settings changed...
                if ((myLastRadialVolume != r) || (myDirtySRM)) {

                    myLastRadialVolume = r;
                    myDeltaList = r.getSRMDeltas(mySpeedMS, myDegrees);
                    myDirtySRM = false;
                }
            }

        } else {
            // Null volume?  need to CLEAR FIXME:
        }
    }

    ;

	public StormRMFilter() {
        setDisplayedName("StormRM");
        //myGUISettings.add(new GUISetting.ToggleBoolean());
    }

    /** Return filtered thing for a value */
    @Override
    public void f(DataTypeQuery q) {
        if (isEnabled()) {
            if (q instanceof RadialSetQuery) {
                try {
                    if (DataType.isRealDataValue(q.outDataValue)) {
                        if (myDeltaList != null) {  // FIXME: check ranges
                            RadialSetQuery r = (RadialSetQuery) (q);
                            ArrayList<Float> radial = myDeltaList.get(r.outRadialSetNumber);
                            if (radial != null) {
                                float delta = radial.get(r.outHitRadialNumber);
                                q.outDataValue += delta;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Got exception during filter " + e.toString());
                }
            }
        }
    }

    @Override
    public String getKey() {
        return getDisplayedName() + isEnabled() + mySpeedMS + ":" + myDegrees;
    }

    @Override
    public ArrayList<GUISetting> getGUISettings() {
        return myGUISettings;
    }

    /** Can be called more than once */
    @Override
    public Object getNewGUIBox(Object parent) {
        /*
        final Composite box = new Composite((Composite)parent, SWT.NONE);
        box.setLayout(new RowLayout());
        
        // SRV 
        Text srv = new Text(box, SWT.LEFT);
        srv.setText("Meters/S:");
        srv.setEditable(false);
        final Spinner speedSpin = new Spinner(box, 0);
        speedSpin.setMaximum(200);
        speedSpin.setMinimum(-200);
        speedSpin.setSelection((int)mySpeedMS);
        
        // speedSpin.setDigits(2);
        
        Text label2 = new Text(box, SWT.LEFT);
        label2.setText("Degrees:");
        label2.setEditable(false);
        final Spinner degreeSpin = new Spinner(box, 0);
        degreeSpin.setMaximum(365);
        degreeSpin.setMinimum(-365);
        degreeSpin.setSelection((int)myDegrees);
        
        
        // Add listeners for GUI elements
        speedSpin.addSelectionListener(new SelectionListener(){
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {					
        }
        
        @Override
        public void widgetSelected(SelectionEvent arg0) {
        mySpeedMS = speedSpin.getSelection();
        System.out.println("Set to "+mySpeedMS);
        myDirtySRM = true;
        fireFilterChangedEvent();
        }
        
        }
        );
        
        degreeSpin.addSelectionListener(new SelectionListener(){
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {					
        }
        
        @Override
        public void widgetSelected(SelectionEvent arg0) {
        myDegrees = degreeSpin.getSelection();
        System.out.println("Set to "+myDegrees);
        myDirtySRM = true;
        fireFilterChangedEvent();
        }
        
        }
        );
        return box;
         * 
         */
        return null;
    }
}
