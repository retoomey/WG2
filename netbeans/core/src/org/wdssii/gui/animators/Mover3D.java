package org.wdssii.gui.animators;

import java.util.ArrayList;

import org.wdssii.gui.GUISetting;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.volumes.LLHAreaSlice;

public class Mover3D extends Animator {

    public Mover3D() {
        setDisplayedName("3D-Mover");
    }

    /** The time loop job.  Eventually this job will be outside of this particular animator,
     * it has to be for us to animate other things like vslice or camera */
    @Override
    public ArrayList<GUISetting> getGUISettings() {
        return null;
    }

    @Override
    public String getKey() {
        return getDisplayedName() + isEnabled();
    }

    @Override
    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
    }

    @Override
    public int animate() {

        // Get top slice....obviously we need GUI choices here
        LLHAreaSlice slice = null;
        ArrayList<VolumeTableData> test = LLHAreaManager.getInstance().getVolumes();
        if (test != null) {
            if (test.size() > 0) {
                VolumeTableData data = test.get(0);
                if (data.airspace instanceof LLHAreaSlice) {
                    slice = (LLHAreaSlice) data.airspace;
                }
            }
        }
        if (slice != null) {

            // Do a proof of concept animation of vslice
            // FIXME: thread safety..uggggh user might be dragging as well...
            double[] a = slice.getAltitudes();
            a[1] = a[1] - 500;
            if (a[1] < 20) {
                a[1] = 80000;
            }
            slice.setAltitudes(a[0], a[1]);

        }
        return 100; // new min dwell in MS
    }

    @Override
    public Object getNewGUIBox(Object parent) {

        // @todo GUI manager to create a gui, hopefully form based
            /*
        final ScrolledComposite sc = new ScrolledComposite((Composite)parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        final Composite box = new Composite(sc, SWT.NONE);
        sc.setContent(box);
        //box.setBackground(box.getDisplay().getSystemColor(
        //       SWT.COLOR_DARK_BLUE));
        
        // The 'full' layout of the entire thing
        RowLayout topLayout = new RowLayout();
        topLayout.type = SWT.VERTICAL;
        topLayout.wrap = false;
        box.setLayout(topLayout);		
        
        Text t = new Text(box, SWT.NONE);
        t.setText("FIXME:GUI for 3d moving");
        // For scrolled composites, need to call setSize for the scroll
        box.setSize(box.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return sc;
         *
         */
        return null;
    }
}
