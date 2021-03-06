package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;
import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.miginfocom.swing.MigLayout;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *
 * @author Robert Toomey
 * 
 * Jobs view will handle displaying updating information on threaded jobs
 * 
 */
public class JobsView extends JThreadPanel implements CommandListener {

    private JPanel myPanel;
    /** Adding and removing JComponents doesn't seem to be thread-safe,
     *  So we'll make it that way.  add/remove JComponents and access our
     *  Map all in one sync
     */
    private final Object addRemoveLock = new Object();
    private Map<String, JobPanel> myJobs = new TreeMap<String, JobPanel>();
   
    int add = 0;
    int add2 = 0;
     int remove = 0;
     int remove2 = 0;
     int addEx = 0;
     int removeEx = 0;
    private JLabel myInfo;
    
    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {
        public Factory() {
             super("Jobs", "computer_go.png");   
        }
        public Component getNewComponent(){
            return new JobsView();
        }
    }
    
    /** A Panel showing a particular job */
    private static class JobPanel extends JPanel {

        /** The progress bar showing a job running */
        private JProgressBar myBar = new JProgressBar();
        
        /** Label showing the main 'task' */
        private JLabel myTask = new JLabel();
        
        /** Label showing the subtask of a job currently running */
        private JLabel mySubTask = new JLabel();
        
        /** Total units of work iff the job is determinate */
        private int myTotalUnits = -1;
        
        public JobPanel() {
            setLayout(new MigLayout("fill", "", ""));
            add(myTask, "growx, wrap");
            add(myBar, "growx, wrap");
            add(mySubTask, "growx, wrap");
            
            // Additional setup
            myBar.setStringPainted(true);
            myTask.setFont(new Font(getFont().getName(), Font.PLAIN, 10));
            mySubTask.setText("none");
            mySubTask.setFont(new Font(getFont().getName(), Font.PLAIN, 9));
        }

        public void setValue(int n) {
            String f = String.format("%d/%d", n, myTotalUnits);
            myBar.setString(f);
            myBar.setValue(n);
        }

        public void setLabel(String label){
            myTask.setText(label);
        }
        
        public void setSubTask(String label){
            mySubTask.setText(label);
        }
        
        public void setIndeterminate(boolean f) {
            myBar.setIndeterminate(f);
        }

        public void setMaximum(int m) {
            myTotalUnits = m;
            myBar.setMaximum(m);
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

        public void addJob(String s) {
            if (v != null){
                v.addJob(s);
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

        public void setSubTask(String s, String subTaskName) {
             if (v!= null){
                v.setSubTask(s, subTaskName);
            }
        }

        public void setLabel(String s, String taskName) {
            if (v!= null){
                v.setLabel(s, taskName);
            }
        }
        
    }
    
    @Override
    public void updateInSwingThread(Object info) {
        // Update the list of running jobs...
        //int total = myJobs.size();
        //int children = myPanel.getComponentCount();
        
        //myInfo.setText("Current running " + total + " ch:" + children+ " a,a,r,r "+add+", "+add2+", "+remove+", "+remove2);
          myInfo.setText("Start/Finished "+WdssiiJob.getStartCount()+", " +WdssiiJob.getEndCount()+", "+add+", " +remove+", "
                  +add2+", "+remove2+", "+addEx+", " +removeEx);
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

    public void addJob(String aJob) {

        try {
            synchronized (addRemoveLock) {
                   add ++;
                JobPanel p = new JobPanel();
                myPanel.add(p, "growx, wrap");
              
                myJobs.put(aJob, p);
                add2++;
            }

            updateGUI();
        } catch (Exception e) {
            addEx++;
        }
       
    }

    /** Remove a job from us.  It's possible the job was never added, since
     * we may not have been valid at the time....hummm...how to handle that?
     * 
     * @param aJob 
     */
    public void removeJob(String aJob) {
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
            removeEx++;
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

    public void setSubTask(String aJob, String subTaskName) {
         try {
            synchronized (addRemoveLock) {
                JobPanel p = myJobs.get(aJob);
                p.setSubTask(subTaskName);
            }
            updateGUI();

        } catch (Exception e) {
        }
    }
    
    public void setLabel(String aJob, String s) {
         try {
            synchronized (addRemoveLock) {
                JobPanel p = myJobs.get(aJob);
                p.setLabel(s);
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
