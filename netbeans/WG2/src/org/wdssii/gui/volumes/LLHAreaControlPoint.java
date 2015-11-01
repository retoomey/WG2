package org.wdssii.gui.volumes;

import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V3;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

// FIXME: Think we can merge into LLD_X class pretty much...
// Though...separate control points lets us have multiple controls for a given location,
// so this extra layer could be handy.  For example, worldwind used this idea to have separate
// controls for bottom and top of an object....  But really just need the 'pick' object to have
// the information needed.  Humm..  We don't need to create these, just return them on a click right?

public class LLHAreaControlPoint  {

	private final static Logger LOG = LoggerFactory.getLogger(LLHAreaControlPoint.class);
	
	private int locationIndex;
	//private int altitudeIndex;
	private V3 point;
	
	private LLD_X location;

	public LLHAreaControlPoint(int locationIndex,
			V3 point, LLD_X location) {
		this.locationIndex = locationIndex;
		//this.altitudeIndex = altitudeIndex;
		this.point = point;
		this.location = location;
	}

	public int getLocationIndex() {
		return this.locationIndex;
	}

	/*public int getAltitudeIndex() {
		return this.altitudeIndex;
	}
*/
	public V3 getPoint(){
		return this.point;
	}

	public LLD_X getLocation(){
		return location;
	}
}
