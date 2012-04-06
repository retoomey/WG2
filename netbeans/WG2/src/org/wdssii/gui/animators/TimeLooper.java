package org.wdssii.gui.animators;

import java.util.ArrayList;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GUISetting;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.VisualCollection;
import org.wdssii.gui.commands.ProductLoadCommand;
import org.wdssii.gui.commands.ProductLoadCommand.ProductLoadCaller;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.ProductLoopRecord;
import org.wdssii.index.IndexRecord;

/** The animator responsible for looping in time.  This is the standard 'weather loop'
 * 
 * @author Robert Toomey
 *
 */
public class TimeLooper extends Animator {

    // Initial animator settings.  Animators are kept alive in the VisualCollection
    private int myDwellMS = 1000; // Frame dwell
    private int myLastDwellMS = 1000; // Last frame of loop dwel
    private boolean myUseLastDwell = false;
    private int myFrameDelta = 1;
    private final int FORWARD_LOOP = 0;  // FIXME: enum, preference?
    private final int BACKWARD_LOOP = 1;
    private final int ROCK_LOOP = 2;
    private int myType = 0;
    private int myDirection = FORWARD_LOOP;
    private int myCurrentFrame = 0;
    private static final int MIN_DWELL = 100;
    private static final int MAX_DWELL = 5000;

    public void setDwellMS(int dwell) {
        if (dwell < MIN_DWELL) {
            dwell = MIN_DWELL;
        }
        if (dwell > MAX_DWELL) {
            dwell = MAX_DWELL;
        }
        myDwellMS = dwell;
    }

    public int getDwellMS() {
        return myDwellMS;
    }

    public void setLastDwellMS(int dwell) {
        if (dwell < MIN_DWELL) {
            dwell = MIN_DWELL;
        }
        if (dwell > MAX_DWELL) {
            dwell = MAX_DWELL;
        }
        myLastDwellMS = dwell;
    }

    public void setUseLastDwell(boolean flag) {
        myUseLastDwell = flag;
    }

    public TimeLooper() {
        setDisplayedName("Loop");
        setEnabled(true);
    }

    /** Called from job thread, this asks us to do our stuff... */
    @Override
    public int animate() {

        int currentDwell = myDwellMS;
        ProductLoopRecord recs = null;
        int availableFrames = 0;
        String indexKey = null;
        String name = "None";

        Product prod = ProductManager.getInstance().getTopProduct();
        int myFrames = 0;
        if (prod != null) {
            VisualCollection v = CommandManager.getInstance().getVisualCollection();
            myFrames = v.getLoopFrames();
            indexKey = prod.getIndexKey();
           // name = prod.getIndexDatatypeString();  // Note this name is lagged behind (time is wrong)
            name = "?";
            recs = prod.getLoopRecords(v.getLoopFrames());
            availableFrames = recs.size();
        }

        // 0 = last frame...
        if (availableFrames > 0) {

            // Pin available frames to the max product frames allowed
            if (availableFrames > myFrames) { // pin right
                availableFrames = myFrames;
            }
            // Check in case records changed size...
            if (myCurrentFrame > availableFrames - 1) {  // pin left
                myCurrentFrame = availableFrames - 1;
            }

            // ------------------------------------------------------------
            // Move to the new frame... Product frames are 'backwards',
            // 0 is the latest and 1 is the latest back one frame in time
            // so frames are [available-1 ..... 0]
            switch (myDirection) {
                case FORWARD_LOOP:
                    myCurrentFrame--; // Forward in time
                    if (myCurrentFrame < 0) {
                        myCurrentFrame = availableFrames - 1;
                    }
                    break;
                case BACKWARD_LOOP:
                    myCurrentFrame++;  // Backward in time
                    if (myCurrentFrame > availableFrames - 1) {
                        myCurrentFrame = 0;
                    }
                    break;
                case ROCK_LOOP:
                    System.out.print("Rock frame from " + myCurrentFrame);
                    myCurrentFrame += myFrameDelta;
                    //System.out.println(" to "+myCurrentFrame+ " ("+(availableFrames-1)+")");
                    if (myCurrentFrame > availableFrames - 1) {  // Switch direction
                        myCurrentFrame = availableFrames - 2; // Gotta move right one...
                        if (availableFrames < 2) {
                            myCurrentFrame = 0;
                        }
                        myFrameDelta = -1;
                        System.out.println("Go negative");
                    } else if (myCurrentFrame < 0) {  // Switch direction
                        myCurrentFrame = 1; // Gotta move left one...
                        if (availableFrames < 2) {
                            myCurrentFrame = 0;
                        }
                        myFrameDelta = +1;
                        System.out.println("Go positive");
                    }
                    break;
                default:
                    assert (false) : "Bad loop direction.  FIXME";
                    break;
            }

            // -------------------------------------------------------------

            System.out.println("Looping on " + name + " (" + (myCurrentFrame + 1) + "/" + availableFrames + ")");
            IndexRecord rec = recs.list.get(myCurrentFrame);
            if (rec != null) {

                // Load command sent 
                ProductLoadCommand doIt = new ProductLoadCommand(
                        ProductLoadCaller.FROM_TIME_LOOPER, indexKey,
                        rec.getDataType(), rec.getSubType(), rec.getTime());
                CommandManager.getInstance().executeCommand(doIt, true);
            }

            // Set the 'wait' time for this frame. 
            // On the last frame, use the last dwell if wanted.  Typically longer...
            if (myCurrentFrame == 0) {
                if (myUseLastDwell) {
                    currentDwell = myLastDwellMS;
                }
            } else {
                currentDwell = myDwellMS;
            }
        } else {
            //monitor.subTask("Select a product as a loop frame base...");
            System.out.println("Select a product as a loop frame base...");
        }
        System.out.println("Frame " + myCurrentFrame + ", dwell " + currentDwell);
        return currentDwell;
    }

    @Override
    public ArrayList<GUISetting> getGUISettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getKey() {  // FIXME: used?
        return getDisplayedName() + isEnabled();
    }

    @Override
    public void setEnabled(boolean flag) {
        super.setEnabled(flag);  // Set default to 'true'.  User kinda expects looping by default
        // FIXME: might need to set dwell (since we have different ones)
    }

    @Override
    public Object getNewGUIBox(Object parent) {
        return null;
    }
    /*
    public Object getHorizontalBox(Object parent, String label)
    {
    Composite frameBox = new Composite((Composite)parent, SWT.NONE);
    RowLayout layout = new RowLayout();
    layout.center = true;
    layout.type = SWT.HORIZONTAL;
    layout.wrap = false;
    frameBox.setLayout(layout);
    
    addLabel(frameBox, label);
    
    return frameBox;
    }
    
    public void addLabel(Composite box, String label)
    {
    Label l = new Label(box, SWT.LEFT);
    l.setText(label);
    }
    
    @Override
    public Object getNewGUIBox(Object parent) {
    
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
    
    // Type of loop combo
    Composite typeBox = (Composite) this.getHorizontalBox(box, "Type:");
    final Combo typeCombo = new Combo(typeBox, SWT.READ_ONLY);
    String[] items = new String[]{"By Product Frame", "By Time Frame"};
    typeCombo.setItems(items);
    typeCombo.select(myType);
    typeCombo.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetDefaultSelected(SelectionEvent arg0){
    System.out.println("Default selected "+typeCombo.getSelectionIndex());
    }
    
    @Override
    public void widgetSelected(SelectionEvent arg0){
    myType = typeCombo.getSelectionIndex();
    System.out.println("Combo selection "+typeCombo.getSelectionIndex());
    
    }
    });
    
    // -----------------------------------------------------
    // Max frames
    Composite frameBox = (Composite)getHorizontalBox(box, "Max:");
    frameBox.setBackground(null);
    //frameBox.setBackground(frameBox.getDisplay().getSystemColor(
    //       SWT.COLOR_DARK_BLUE));
    final Spinner speedSpin = new Spinner(frameBox, 0);
    addLabel(frameBox, "steps");
    
    speedSpin.setMaximum(20);
    speedSpin.setMinimum(1);
    VisualCollection v = CommandManager.getInstance().getVisualCollection();
    int myFrames = v.getLoopFrames();
    speedSpin.setSelection(myFrames);		
    speedSpin.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {					
    }
    
    @Override
    public void widgetSelected(SelectionEvent arg0) {
    int newFrames = speedSpin.getSelection();
    VisualCollection v = CommandManager.getInstance().getVisualCollection();
    v.setLoopFrames(newFrames);
    
    // FIXME: Probably a command here for notification....
    }
    }
    );
    
    // -----------------------------------------------------
    // Dwell gui setting.
    Composite dwellBox = (Composite) getHorizontalBox(box, "Dwell:");
    final Spinner dwellSpin = new Spinner(dwellBox, 0);
    addLabel(dwellBox, "ms");
    //dwellBox.setBackground(dwellBox.getDisplay().getSystemColor(
    //       SWT.COLOR_CYAN));
    
    dwellSpin.setMinimum(MIN_DWELL);
    dwellSpin.setMaximum(MAX_DWELL);
    dwellSpin.setSelection(myDwellMS);
    dwellSpin.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {					
    }
    
    @Override
    public void widgetSelected(SelectionEvent arg0) {
    int value = dwellSpin.getSelection();
    if (value != myDwellMS){
    setDwellMS(value);
    //myDwellMS = value;
    //synchronized(myLoopJobSync){
    //	myLoopJob.setJobDwell(myDwellMS);
    //}
    }
    }
    
    }
    );
    
    // -----------------------------------------------------
    // Last dwell gui setting.
    Composite ldwellBox = (Composite) getHorizontalBox(box, "Last Dwell:");
    final Button l = new Button(ldwellBox, SWT.CHECK);
    l.setToolTipText("Turn on/off a different dwell for last frame of loop");
    l.setSelection(myUseLastDwell);
    l.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetSelected(SelectionEvent e) {
    myUseLastDwell = l.getSelection();
    }
    
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {				
    }
    
    });
    
    final Spinner ldwellSpin = new Spinner(ldwellBox, 0);
    addLabel(ldwellBox, "ms");
    //dwellBox.setBackground(dwellBox.getDisplay().getSystemColor(
    //       SWT.COLOR_CYAN));
    
    ldwellSpin.setMinimum(MIN_DWELL);
    ldwellSpin.setMaximum(MAX_DWELL);
    ldwellSpin.setSelection(myLastDwellMS);
    ldwellSpin.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {					
    }
    
    @Override
    public void widgetSelected(SelectionEvent arg0) {
    int value = ldwellSpin.getSelection();
    if (value != myLastDwellMS){
    setLastDwellMS(value);
    }
    }
    
    }
    );
    
    // Direction of loop combo
    Composite dirBox = (Composite) this.getHorizontalBox(box, "Direction:");
    final Combo dirCombo = new Combo(dirBox, SWT.READ_ONLY);
    String[] dirItems = new String[]{"Forward", "Backward", "Rock"};
    dirCombo.setItems(dirItems);
    dirCombo.select(myDirection);
    dirCombo.addSelectionListener(new SelectionListener(){
    
    @Override
    public void widgetDefaultSelected(SelectionEvent arg0){
    }
    
    @Override
    public void widgetSelected(SelectionEvent arg0){
    myDirection = dirCombo.getSelectionIndex();				
    }
    });
    
    // For scrolled composites, need to call setSize for the scroll
    box.setSize(box.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    return sc;
    
    }
     * */
}
