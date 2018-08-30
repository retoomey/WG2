package org.wdssii.gui.features;

import java.awt.Point;
import java.util.ArrayList;
import java.util.TreeMap;

import org.wdssii.core.CommandManager;
import org.wdssii.geom.D3;
import org.wdssii.geom.LLD_X;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.commands.PointSelectCommand;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.gui.renderers.CompassRenderer;
import org.wdssii.gui.renderers.LLHPolygonRenderer;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaControlPoint;
import org.wdssii.gui.volumes.LLHAreaFactory;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.gui.volumes.LLHAreaSetFactory;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;

/**
 * A LLHArea feature draws a 3D shape in the window that can be used by charts
 * or display for rendering stuff.
 *
 * @author Robert Toomey
 */
public class LLHAreaFeature extends Feature {

    private final static Logger LOG = LoggerFactory.getLogger(LLHAreaFeature.class);
    public static final String LLHAreaGroup = "3D";
    // Factories shared by all LLHAreaFeatures anywhere
    // Probably should make this more configurable...
    private static final TreeMap<String, LLHAreaFactory> myFactoryList = new TreeMap<String, LLHAreaFactory>();
    /**
     * The factory we were created with
     */
    private LLHAreaFactory myFactory;

    static {
        myFactoryList.put("Set", new LLHAreaSetFactory());
    }
    /**
     * The LLHArea object we render
     */
    private LLHArea myLLHArea;
	private LLHPolygonRenderer myLLHPolygonRenderer;
	private LLHAreaControlPoint myControlPoint;

    public LLHAreaFeature(FeatureList f) {
        super(f, LLHAreaGroup);
    }

    public boolean createLLHArea(String factory, Object info) {
        boolean success = false;
        if (myFactory == null) {
            myFactory = myFactoryList.get(factory);

            if (myFactory != null) {
                FeatureTableInfo theData = new FeatureTableInfo();
                
                // LLHArea shouldn't need a worldwind world to work....
                // at moment it does..bleh...uses this to figure out
                // where to place the initial points in lat/lon...
                // How do we location lat/lon if no world available..or if
                // there are two worlds?
                
                //FIXME: MULTIVIEW
                //success = myFactory.create(null, this, theData, info);
                success = myFactory.create(this, theData, info);
                if (success) {
                    myLLHArea = (LLHArea) theData.created;
                    setVisible(true);
                    setOnlyMode(false);
                    setName(theData.visibleName);
                    setKey(theData.keyName);
                    setMessage(theData.message);
                }

            } else {
                setName("Error");
                setMessage("Not created");
            }
        }
        return success;
    }

    	@Override
	public FeatureMemento getNewMemento() {
            if (myLLHArea != null){
                return myLLHArea.getMemento();
            }else{
                return super.getNewMemento();
            }
	}
   @Override //superclass should handle this...  This breaks points completely if removed.  Wow.
    public void updateMemento(Memento m) {
        super.updateMemento(m);
        if (myLLHArea != null) {
            myLLHArea.setFromMemento(m);
        }
    }
 
    @Override
    public void addNewRendererItem(ArrayList<FeatureRenderer> list, String id, String packageName, String className) {
        FeatureRenderer r = createRenderer(id, packageName, className);
        if (r != null){
            r.initToFeature(this);   // Why not the default????
            list.add(r);
        }
    }
    
    @Override
    public ArrayList<FeatureRenderer> getNewRendererList(String type, String packageName) {
    	ArrayList<FeatureRenderer> list = new ArrayList<FeatureRenderer>();
        //addNewRendererItem(list, type, packageName, "LLHPolygonRenderer");    
        // Kinda violates the whole create by name thing...  Need a generic way of getting
        // pick ids from renderers if we want to avoid this...
    	myLLHPolygonRenderer = new LLHPolygonRenderer();
    	myLLHPolygonRenderer.initToFeature(this);
        list.add(myLLHPolygonRenderer);
        
        return list;
    }
    
    public LLHArea getLLHArea() {
        return myLLHArea;
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public FeatureGUI createNewControls() {
        //LOG.error("IN create new controls");

        if (myFactory != null) {
            return myFactory.createGUI(this, myLLHArea);
        } else {
            return super.createNewControls();
        }
    }
    
    @Override
	public boolean handleMousePressed(int pickID, GLWorld w, FeatureMouseEvent e) {	
		int id = myLLHPolygonRenderer.myFirstPick;
		int count = myLLHPolygonRenderer.myPickCount;		
		LOG.error("INFO: "+pickID +", " +id+", " +(id+count));
		boolean found = ((pickID >= id) && (pickID < (id+count)));
		if (found) {
			LOG.error("FOUND ID OF "+pickID);
			int pointNumber = pickID-id;
			LOG.error("PICK INDEX OF "+pointNumber);
			// Auto select.  FIXME: redesign/streamline
			// From the LLHAreaLayer stuff.  Bleh this needs to integrate...
			// Wow needs a GLWorld to create new control points each time...
			// for moment hack into renderer...clean me clean me clean me..
			// (got into this mess because of worldwind)
			ArrayList<LLHAreaControlPoint> c = myLLHPolygonRenderer.myLastControls;
			//LLHAreaControlPoint controlPoint = c.get(pointNumber);
			myControlPoint = c.get(pointNumber);
			
			if (myLLHArea instanceof LLHAreaSet) {  // BLEH!
				LLHAreaSet set = (LLHAreaSet) (myLLHArea);
				int index = myControlPoint.getLocationIndex();
				PointSelectCommand command = new PointSelectCommand(set, index);
				CommandManager.getInstance().executeCommand(command, true);
				return true;
			}			
		}
		return false;	
	} 
    
    @Override
    public boolean handleMouseMoved(int pickID, GLWorld w, FeatureMouseEvent e) {
    	/*
    	// FIXME: Shouldn't have to search..ranges should be in order...
    	//LOG.error("Got pick "+pickID);
		//ArrayList<Integer> picks = myLLHPolygonRenderer.myPicks;
		int id = myLLHPolygonRenderer.myFirstPick;
		int count = myLLHPolygonRenderer.myPickCount;		
		LOG.error("INFO: "+pickID +", " +id+", " +(id+count));
		boolean found = ((pickID >= id) && (pickID < (id+count)));
		if (found) {
			LOG.error("FOUND ID OF "+pickID);
			int pointNumber = pickID-id;
			LOG.error("PICK INDEX OF "+pointNumber);
			// Auto select.  FIXME: redesign/streamline
			// From the LLHAreaLayer stuff.  Bleh this needs to integrate...
			// Wow needs a GLWorld to create new control points each time...
			// for moment hack into renderer...clean me clean me clean me..
			// (got into this mess because of worldwind)
			ArrayList<LLHAreaControlPoint> c = myLLHPolygonRenderer.myLastControls;
			LLHAreaControlPoint controlPoint = c.get(pointNumber);
			
			if (myLLHArea instanceof LLHAreaSet) {  // BLEH!
				LLHAreaSet set = (LLHAreaSet) (myLLHArea);
				int index = controlPoint.getLocationIndex();
				PointSelectCommand command = new PointSelectCommand(set, index);
				CommandManager.getInstance().executeCommand(command, true);
				return true;
			}			
		}
		*/
		//LOG.error("ID BACK HANDLE PICK IS "+id);

		//boolean value = (leftDown && (pickID == id) && (pickID > -1));
		//getMemento().setProperty(LegendMemento.INCOMPASS, value);
    	return false;
	}
    
    @Override
	public boolean handleMouseReleased(int pickID, GLWorld w, FeatureMouseEvent e) {
    	if (myControlPoint != null) {
    		myControlPoint = null;
    		return true;
    	}
		return false;
	}
    
    @Override
	public boolean handleMouseDragged(int pickID, GLWorld w, FeatureMouseEvent e) {
    	V3 aPoint = w.project2DToEarthSurface(e.x, e.y, D3.EARTH_RADIUS_KMS);
    	V3 zPosition = w.projectV3ToLLH(aPoint); // xyz --> LLH
    	V3 backPoint = w.projectLLH(zPosition.x, zPosition.y, zPosition.z); // 
    	
    	//LOG.error("EARTH : "+aPoint.x+", "+aPoint.y+", "+aPoint.z);

    //	LOG.error("NEW : "+zPosition.x+", "+zPosition.y+", "+zPosition.z);
    	
    //	LOG.error("BACK : "+backPoint.x+", "+backPoint.y+", "+backPoint.z);

    	w.gl.getContext().makeCurrent();
    	
    	if (myControlPoint != null) {
    		
    		// Bleh we need GLWorld to project the point.  So do we just
    		// pass world in?  I think GLWorld should be even more generic then
    		
    		// Get current 3d point in lat, lon, height....this seems to be to get the current elevation
    		V3 pp = myControlPoint.getPoint();
    		//LOG.error("PP is "+pp.x+", "+pp.y+", "+pp.z);
    		//V3 controlPointL = w.projectV3ToLLH(myControlPoint.getPoint());
    		//LOG.error("OLD POINT: "+controlPointL.x+", "+controlPointL.y+", "+controlPointL.z);
    		
    		// Find new 3D point on earth surface using this elevation...
    		//V3 newPoint = w.project2DToEarthSurface(e.x, e.y, controlPointL.z);
    		V3 newPoint = w.project2DToEarthSurface(e.x, e.y, 0);  // this should be correct...
    		
    		if (newPoint == null) {
    			return false;
    		}else {
    			//LOG.error("PROJECTED to "+newPoint.x+", "+newPoint.y+", "+newPoint.z);
    			V3 newPosition = w.projectV3ToLLH(newPoint);
    			LOG.error("X/Y  "+e.x+","+e.y+" NEW: "+newPosition.x+", "+newPosition.y+", "+newPosition.z);
    			// And make it lat, lon, height...
    			int index = myControlPoint.getLocationIndex();
    			LOG.error("NEW: "+newPosition.x+", "+newPosition.y+", "+newPosition.z);
    			if (myLLHArea instanceof LLHAreaSet) {
    				LLHAreaSet set = (LLHAreaSet) (myLLHArea);
    				FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
    														// control...hummm
    				// currently copying all points into 'points'
    				@SuppressWarnings("unchecked")
    				ArrayList<LLD_X> list = ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
    				if (list != null) {

    					// Copy the location info at least...
    					LLD_X oldOne = list.get(index);
    					LLD_X newOne = new LLD_X(newPosition.x, newPosition.y, oldOne);

    					list.set(index, newOne);

    					FeatureMemento fm = (FeatureMemento) (m); // Check it
    					FeatureChangeCommand c = new FeatureChangeCommand(myLLHArea.getFeature(), fm);
    					CommandManager.getInstance().executeCommand(c, true);
    				}
    			}

    		}
    		return true;
    	}
    	return false;
    	
	}
    
   /* public void doMoveControlPoint(GLWorld w, LLHAreaControlPoint controlPoint,
    		Point mousePoint) {

		// Include this test to ensure any derived implementation performs it.
		if (this.getAirspace() == null) {
			return;
		}

		// Get current 3d point in lat, lon, height....this seems to be to get the current elevation
		V3 controlPointL = w.projectV3ToLLH(controlPoint.getPoint());
		
		// Find new 3D point on earth surface using this elevation...
		V3 newPoint = w.project2DToEarthSurface(mousePoint.getX(), mousePoint.getY(), controlPointL.z);
		if (newPoint == null) {
			return;
		}

		// And make it lat, lon, height...
		V3 newPosition = w.projectV3ToLLH(newPoint);
		
		int index = controlPoint.getLocationIndex();
		LLHArea area = this.getAirspace();

		if (area instanceof LLHAreaSet) {
			LLHAreaSet set = (LLHAreaSet) (area);
			FeatureMemento m = set.getMemento(); // vs getNewMemento as in gui
													// control...hummm
			// currently copying all points into 'points'
			@SuppressWarnings("unchecked")
			ArrayList<LLD_X> list = 
			  ((ArrayList<LLD_X>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
			if (list != null) {

				// Copy the location info at least...
				LLD_X oldOne = list.get(index);
				LLD_X newOne = new LLD_X(newPosition.x, newPosition.y, oldOne);

				list.set(index, newOne);

				FeatureMemento fm = (FeatureMemento) (m); // Check it
				FeatureChangeCommand c = new FeatureChangeCommand(area.getFeature(), fm);
				CommandManager.getInstance().executeCommand(c, true);
			}
		}
	}*/
    
}
