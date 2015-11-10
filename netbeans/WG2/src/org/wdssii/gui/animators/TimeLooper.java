package org.wdssii.gui.animators;

import java.util.ArrayList;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.AnimateManager;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.GUISetting;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.ProductLoadCommand;
import org.wdssii.gui.commands.ProductLoadCommand.ProductLoadCaller;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LoopFeature;
import org.wdssii.gui.features.LoopFeature.LoopMemento;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.ProductLoopRecord;
import org.wdssii.index.IndexRecord;

/**
 * The animator responsible for looping in time. This is the standard 'weather
 * loop'
 *
 * @author Robert Toomey
 *
 */
public class TimeLooper extends Animator {

	private final static Logger LOG = LoggerFactory.getLogger(TimeLooper.class);
	// Initial animator settings.  Animators are kept alive in the VisualCollection
	private int myDwellMS = 100; // Frame dwell
	private int myLastDwellMS = myDwellMS; // Last frame of loop dwel
	private int myFirstDwellMS = myDwellMS; // Last frame of loop dwel
	private int myFrameDelta = 1;
	private final int FORWARD_LOOP = 0;  // FIXME: enum, preference?
	private final int BACKWARD_LOOP = 1;
	private final int ROCK_LOOP = 2;
	private int myMaxFrames = 10;
	private int myType = 0;
	private int myDirection = FORWARD_LOOP;
	private int myCurrentFrame = 0;
	private static final int MIN_DWELL = 100;
	private static final int MAX_DWELL = 5000;

	public TimeLooper() {
		setDisplayedName("Loop");
		setEnabled(true);
	}

	/** Synchronize current LoopFeature settings to ours,
	 * Could just hold a memento probably.
	 * FIXME: does this mess with loop logic changing on the fly?
	 */
	public void syncSettings() {
		try {
			// Set to defaults in case of exception...
			myDwellMS = 1000;
	                myLastDwellMS = myDwellMS; // Last frame of loop dwel
	                myFirstDwellMS = myDwellMS; // Last frame of loop dwel

			LoopFeature f = (LoopFeature) FeatureList.theFeatures.getFirstFeature(LoopFeature.class);
			LoopMemento m = (LoopMemento) f.getMemento();

			// First dwell, Last dwell, dwell..
			myDwellMS = m.get(LoopMemento.FRAME_TIME_MS, myDwellMS);
			
			Integer fd = m.get(LoopMemento.FIRST_DWELL_SECS, 1);
			myFirstDwellMS = myDwellMS+(fd*1000);
			
			Integer ld = m.get(LoopMemento.LAST_DWELL_SECS, 0);
			myLastDwellMS = myDwellMS+(ld*1000);

			// Num frames...
			myMaxFrames = m.get(LoopMemento.NUM_OF_FRAMES, myMaxFrames);

			// ROCK Setting
			Boolean r = m.get(LoopMemento.ROCK_LOOP, false);
			if (r) {
				myDirection = ROCK_LOOP;
			} else {
				myDirection = FORWARD_LOOP;
			}

		} catch (Exception e) {
			// Bleh...don't update then...might warn
			LOG.warn("Couldn't update loop settings " + e.toString());
		}
	}

	/**
	 * Called from job thread, this asks us to do our stuff...
	 */
	@Override
	public int animate() {

		syncSettings();

		int currentDwell = myDwellMS;
		ProductLoopRecord recs = null;
		int availableFrames = 0;
		String indexKey = null;
		String name = "None";

		Product prod = ProductManager.getInstance().getTopProduct();
		int myFrames = 0;
		if (prod != null) {
			AnimateManager v = AnimateManager.getVisualCollection();
			myFrames = v.getLoopFrames();
			indexKey = prod.getIndexKey();
			// name = prod.getIndexDatatypeString();  // Note this name is lagged behind (time is wrong)
			name = "?";
			recs = prod.getLoopRecords(v.getLoopFrames());
			availableFrames = recs.size();
			if (availableFrames > myMaxFrames){
				availableFrames = myMaxFrames;
			}
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
					//LOG.debug("Rock frame from " + myCurrentFrame);
					myCurrentFrame += myFrameDelta;
					//LOG.debug(" to "+myCurrentFrame+ " ("+(availableFrames-1)+")");
					if (myCurrentFrame > availableFrames - 1) {  // Switch direction
						myCurrentFrame = availableFrames - 2; // Gotta move right one...
						if (availableFrames < 2) {
							myCurrentFrame = 0;
						}
						myFrameDelta = -1;
						//LOG.debug("Go negative");
					} else if (myCurrentFrame < 0) {  // Switch direction
						myCurrentFrame = 1; // Gotta move left one...
						if (availableFrames < 2) {
							myCurrentFrame = 0;
						}
						myFrameDelta = +1;
						//LOG.debug("Go positive");
					}
					break;
				default:
					assert (false) : "Bad loop direction.  FIXME";
					break;
			}

			// -------------------------------------------------------------

			//LOG.debug("Looping on " + name + " (" + (myCurrentFrame + 1) + "/" + availableFrames + ")");
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
			        currentDwell = myLastDwellMS;
			}else if (myCurrentFrame == (availableFrames-1)){
				currentDwell = myFirstDwellMS;
			}else{
				currentDwell = myDwellMS;
			}

		} else {
			//monitor.subTask("Select a product as a loop frame base...");
			//LOG.debug("Select a product as a loop frame base...");
		}
		//LOG.debug("Frame " + myCurrentFrame + ", dwell " + currentDwell);
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
}
