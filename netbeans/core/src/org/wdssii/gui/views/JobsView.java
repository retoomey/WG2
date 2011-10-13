package org.wdssii.gui.views;

import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *
 * @author Robert Toomey
 * 
 * Jobs view will handle displaying updating information on threaded jobs
 * 
 */
public class JobsView extends JThreadPanel implements WdssiiView {

    private JPanel myPanel;
    /** Adding and removing JComponents doesn't seem to be thread-safe,
     *  So we'll make it that way.  add/remove JComponents and access our
     *  Map all in one sync
     */
    private Object addRemoveLock = new Object();
    private Map<String, JobPanel> myJobs = new TreeMap<String, JobPanel>();
   
    int add = 0;
    int add2 = 0;
     int remove = 0;
     int remove2 = 0;
    private JLabel myInfo;

    /** A Panel showing a particular job */
    private static class JobPanel extends JPanel {

        private JProgressBar aBar = new JProgressBar();
        private JLabel aLabel = new JLabel();
        
        public JobPanel(String taskName) {
            setLayout(new MigLayout("fill", "", ""));
            add(new JLabel(taskName), "");
            add(aBar, "growx, wrap");
        }

        public void setValue(int n) {
            aBar.setValue(n);
        }

        public void setLabel(String label){
            aLabel.setText(label);
        }
        
        public void setIndeterminate(boolean f) {
            aBar.setIndeterminate(f);
        }

        public void setMaximum(int m) {
            aBar.setMaximum(m);
        }
    }

    /** A class that exists as a protective wrapper to the JobView.  We 
     * need this because the JobView might not exist but jobs are still
     * running and calling us....we can create the swing job panels without
     * a container...
     * ?>>>
     */
    public static class JobsViewHandler {

        private static JobsView v = null;
        
        public static void setView(JobsView aView){
            v = aView;
            
            // Setup/fix any panels, etc that need to be put here...
        }
        
        public void progress(String s, int u) {
            if (v != null){
                v.progress(s, u);
            }
        }

        public void finish(String s) {
            if (v != null){
                v.finish(s);
            }
        }

        public void addJob(String s, String taskName) {
            if (v != null){
                v.addJob(s, taskName);
            }
        }

        public void switchToIndeterminate(String s) {
            if (v != null){
                v.switchToIndeterminate(s);
            }
        }

        public void switchToDeterminate(String s) {
            if (v != null){
                v.switchToIndeterminate(s);
            }
        }

        public void setMaximum(String s, int u) {
            if (v!= null){
                v.setMaximum(s, u);
            }
        }
        
    }
    
    @Override
    public void updateInSwingThread(Object info) {
        // Update the list of running jobs...
        int total = myJobs.size();
        int children = myPanel.getComponentCount();
        myInfo.setText("Current running " + total + " ch:" + children+ " a,a,r,r "+add+", "+add2+", "+remove+", "+remove2);
        validate();
        doLayout();
        repaint();
    }

    public JobsView() {
        setLayout(new MigLayout("fill", "", ""));
        myPanel = new JPanel();
        add(myPanel, "grow");
        myPanel.setLayout(new MigLayout("fillx", "", ""));
        myInfo = new JLabel();
        myPanel.add(myInfo, "dock north");
        JobsViewHandler.setView(this);
    }

    public void addJob(String aJob, String taskName) {

        try {
            synchronized (addRemoveLock) {
                 add2++;
                JobPanel p = new JobPanel(taskName);
                myPanel.add(p, "growx, wrap");
              
                myJobs.put(aJob, p);
                  add ++;
            }

            updateGUI();
        } catch (Exception e) {
            String a = e.toString();
        }
       
    }

    /** Remove a job from us.  It's possible the job was never added, since
     * we may not have been valid at the time....hummm...how to handle that?
     * 
     * @param aJob 
     */
    public void removeJob(String aJob) {
        // JProgressBar b = myJobs.get(aJob);
        try {
            synchronized (addRemoveLock) {
                
                JobPanel p = myJobs.get(aJob);
                if (p != null) {
                    remove--;
                    myJobs.remove(aJob);
                    myPanel.remove(p);
                    remove2 --;
                }
            }
            updateGUI();
        } catch (Exception e) {
            String a = e.toString();
            int b = 1;
        }
    }

    public void progress(String aJob, int units) {
        try {
            synchronized (addRemoveLock) {
                JobPanel b = myJobs.get(aJob);
                b.setValue(units);
            }
            updateGUI();
        } catch (Exception e) {
        }
    }

    public void switchToIndeterminate(String aJob) {
        try {
            synchronized (addRemoveLock) {
                JobPanel p = myJobs.get(aJob);
                p.setIndeterminate(true);
            }
            updateGUI();
        } catch (Exception e) {
        }
    }

    public void switchToDeterminate(String aJob) {
        try {
            synchronized (addRemoveLock) {
                JobPanel p = myJobs.get(aJob);
                p.setIndeterminate(false);
            }
            updateGUI();
        } catch (Exception e) {
        }
    }

    public void setMaximum(String aJob, int max) {
        try {
            synchronized (addRemoveLock) {
                JobPanel p = myJobs.get(aJob);
                p.setMaximum(max);
            }
            updateGUI();

        } catch (Exception e) {
        }
    }

    public void finish(String aJob) {
        removeJob(aJob);
    }
    
    public void checkValid(){
        int total = myJobs.size();
        int children = myPanel.getComponentCount()-1; // -1 or label
        if (total == children){
            
        }else{
            int a = 1;
        }
    }
}
