package org.wdssii.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHArea.LLHAreaMemento;
import org.wdssii.gui.volumes.LLHAreaBoxFactory;
import org.wdssii.gui.volumes.LLHAreaLayerController;
import org.wdssii.gui.volumes.LLHAreaFactory;
import org.wdssii.gui.volumes.LLHAreaHeightStickFactory;
import org.wdssii.gui.volumes.LLHAreaSliceFactory;
import org.wdssii.gui.worldwind.LLHAreaLayer;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import org.wdssii.gui.views.WorldWindView;

/** This manager holds onto a collection of LLHArea objects in the display.  These are the physical slices, boxes, sticks
 * that are used by the volumeViewer and the chartViewer to draw their stuff.  The objects by themselves are just
 * 3D controls in the window, this maintains a list of these objects.
 * 
 * @author Robert Toomey
 * 
 */
public class LLHAreaManager implements Singleton {

    /** The singleton */
    private static LLHAreaManager instance = null;
    /** The airspace controller in the display */
    private LLHAreaLayerController editorController;
    private ArrayList<LLHAreaFactory> myFactoryList = new ArrayList<LLHAreaFactory>();
    private LLHAreaFactory myCurrentFactory = null;

    /** Storage for each volume object.  Each object has an editor and an airspace that draws something for a volume*/
    public static class VolumeTableData {

        public String visibleName; // Name shown in GUI, renamable
        public String keyName; // Key used internally 
        public LLHArea airspace;
        public boolean checked;
        public boolean onlyMode;
        public String message;
    };
    /** The list of volumes in the display */
    private ArrayList<VolumeTableData> myHandlerList;
    /** The current selected volume */
    private VolumeTableData mySelectedVolume;

    @Override
    public void singletonManagerCallback() {
        myHandlerList = new ArrayList<VolumeTableData>();
    }

    /**
     * Private to prevent creation except by getInstance method
     */
    private LLHAreaManager() {
        // Exists only to defeat instantiation
        // Do nothing here! Do it in singletonManagerCallback, this way
        // the singleton initialization order is controlled.

        // It's ok to create our factories here since they don't do anything until called
        // Not using reflection here since I'm pretty sure there will only be a set number of
        // 3D objects. Can change later if I'm wrong.
        myFactoryList.add(new LLHAreaSliceFactory());
        myFactoryList.add(new LLHAreaBoxFactory());
        myFactoryList.add(new LLHAreaHeightStickFactory());
        myCurrentFactory = myFactoryList.get(0);
    }

    /**
     * @return the singleton for the manager
     */
    public static LLHAreaManager getInstance() {
        if (instance == null) {
            instance = new LLHAreaManager();
            SingletonManager.registerSingleton(instance);
        }
        return instance;
    }

    /** Get the named list of factory objects.  This is for GUI selection */
    public ArrayList<String> getObjectNameList() {
        ArrayList<String> aList = new ArrayList<String>();
        for (LLHAreaFactory f : myFactoryList) {
            aList.add(f.getFactoryNameDisplay());
        }
        return aList;
    }

    /** Get the name of the current selected factory */
    public String getCurrentFactoryName() {
        String name = null;
        if (myCurrentFactory != null) {
            name = myCurrentFactory.getFactoryNameDisplay();
        }
        return name;
    }

    /** Set the name of the current factory to use */
    public void setCurrentFactoryName(String name) {
        if (name != null) {
            for (LLHAreaFactory f : myFactoryList) {
                String factoryKey = f.getFactoryNameDisplay();
                if (factoryKey.compareTo(name) == 0) {
                    myCurrentFactory = f;
                    break;
                }
            }
        }
    }

    /** Create a new volume in the display.
    Called from an LLHAreaCreateCommand
     */
    public void createNewVolume() {
        // FIXME: make a factory for each type of volume object
        LLHAreaLayer layer = getVolumeLayer();
        if (layer != null) {

            boolean success = false;
            WorldWindow world = getWorld();
            VolumeTableData theData = new VolumeTableData();

            if (myCurrentFactory != null) {
                success = myCurrentFactory.create(world, theData);
                if (success) {
                    //  editor.addEditListener(this); not sure I need this
                    layer.addLLHArea(theData.airspace);
                    theData.checked = theData.airspace.isVisible();
                    theData.onlyMode = theData.airspace.isOnly();
                    myHandlerList.add(theData);
                }
            }

            // Create an editor controller if none exists (only do once)
            // FIXME: probably should be done on startup somewhere
            if (editorController == null) {
                this.editorController = new LLHAreaLayerController(world, layer);
            }

            // Since theData is a NEW volume, select will do the GUI update as well
            if (success) {
                selectVolume(theData);
            }
        }

    }

    private void deleteVolume(VolumeTableData theVolume) {

        // If it's a different volume than selected...
        if (theVolume != null) {

            int index = myHandlerList.indexOf(theVolume);

            LLHAreaLayer layer = getVolumeLayer();
            layer.removeLLHArea(theVolume.airspace);
            myHandlerList.remove(theVolume);       
            
            // Select previous in list if any, or null
            VolumeTableData toSelect = null;
            if (myHandlerList.size() > 0) {
                
                // Select the previous item in list, if any...
                index = index - 1;
                if (index < 0) {
                    index = 0;
                }
                toSelect = myHandlerList.get(index);
            }
            selectVolume(toSelect); // can be null
        }
    }

    public void deleteLLHArea(String myLLHAreaKey) {

        for (VolumeTableData v : myHandlerList) {
            if (v.keyName.equals(myLLHAreaKey)) {
                deleteVolume(v);
                break;
            }
        }
    }

    /** Select the given volume in the list and in the gl window.  This means turning on the
     * editing controls
     * @param theVolume the volume to turn on.  This can be NULL
     */
    private void selectVolume(VolumeTableData theVolume) {

        // If it's a different volume than selected...
        if (theVolume != mySelectedVolume) {
            mySelectedVolume = theVolume;
            updateEarthBall();
        }
    }

    /** Called by the LLHAreaLayerController to select */
    public void selectLLHArea(LLHArea area) {
        for (VolumeTableData v : myHandlerList) {
            if (v.airspace == area) {
                selectVolume(v);
                break;
            }
        }
    }

    /** Select LLHArea given a key */
    public void selectLLHArea(String myLLHAreaKey) {
        for (VolumeTableData v : myHandlerList) {
            if (v.keyName.equals(myLLHAreaKey)) {
                selectVolume(v);
                break;
            }
        }
    }

    public String getVisibleName(LLHArea area) {
        String key = "?";
        for (VolumeTableData v : myHandlerList) {
            if (area == v.airspace) {
                key = v.visibleName;
                break;
            }
        }
        return key;
    }

    public String getKey(LLHArea area) {
        String key = "?";
        for (VolumeTableData v : myHandlerList) {
            if (area == v.airspace) {
                key = v.keyName;
                break;
            }
        }
        return key;
    }

    /** Get the volume layer we use.  Currently only one earthball */
    private LLHAreaLayer getVolumeLayer() {
        LLHAreaLayer world = null;

        WorldWindView v = CommandManager.getInstance().getEarthBall();
        if (v != null){
            world = v.getVolumeLayer();
        }
  
        return world;
    }

    /** Get the world we use.  Currently only one earthball */
    private WorldWindow getWorld() {
        WorldWindow world = null;

        WorldWindView v = CommandManager.getInstance().getEarthBall();
        if (v != null){
            world = v.getWwd();
        }
 
        return world;
    }

    // Current only one earth ball...
    public void updateEarthBall() {
        CommandManager.getInstance().getEarthBall().updateOnMinTime();
    }

    /** Get default attributes (How the volume draws) */
    public static AirspaceAttributes getDefaultAttributes() {
        AirspaceAttributes attributes = new BasicAirspaceAttributes();
        attributes.setMaterial(new Material(Color.BLACK, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK, 0.0f));
        attributes.setOutlineMaterial(Material.DARK_GRAY);
        attributes.setDrawOutline(true);
        attributes.setOpacity(0.95);
        attributes.setOutlineOpacity(.95);
        attributes.setOutlineWidth(2);
        return attributes;
    }

    public static List<LatLon> createSliceInViewport(WorldWindow wwd, Position position, Angle heading,
            double sizeInMeters) {
        Globe globe = wwd.getModel().getGlobe();
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
        transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));

        double widthOver2 = sizeInMeters / 2.0;
        double heightOver2 = sizeInMeters / 2.0;
        Vec4[] points = new Vec4[]{
            new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
            // new Vec4(widthOver2,  -heightOver2, 0.0).transformBy4(transform), // lower right
            new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
        //new Vec4(-widthOver2,  heightOver2, 0.0).transformBy4(transform)  // upper left
        };


        LatLon[] locations = new LatLon[points.length];
        for (int i = 0; i < locations.length; i++) {
            locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
        }

        return Arrays.asList(locations);
    }

    /*
    public void deleteSelectedVolume() {
    System.out.println("Deleting selected volume");
    if (mySelectedVolume != null) {
    
    // Selected volume's editor, etc..will be in the world....
    //	WorldWindowGLCanvas world = getWorld(); // What if world is different now???
    
    // Remove the current selected volume editor and layer...
    //	mySelectedVolume.editor.setArmed(false);
    //	removeEditorFromLayers(world, mySelectedVolume.editor);
    //	editorController.setEditor(null);
    
    int index = myHandlerList.indexOf(mySelectedVolume);
    
    LLHAreaLayer layer = getVolumeLayer();
    layer.removeLLHArea(mySelectedVolume.airspace);
    
    myHandlerList.remove(mySelectedVolume);
    mySelectedVolume = null;
    
    if (myHandlerList.size() > 0) {
    
    // Select the previous item in list, if any...
    index = index - 1;
    if (index < 0) {
    index = 0;
    }
    selectVolume(myHandlerList.get(index));
    } else {
    updateEarthBall();
    }
    
    }
    }*/
    /** Get the volume that is currently selected (Has the controls) */
    public VolumeTableData getSelection() {
        return mySelectedVolume;
    }

    /** Get the number of volume objects in the display */
    public int getVolumeListSize() {
        return myHandlerList.size();
    }

    /** Get the list of volumes in the display */
    public ArrayList<VolumeTableData> getVolumes() {
        return myHandlerList;
    }

    /** Get a list of the currently to be drawn objects */
    public Iterable<LLHArea> getDrawnVolumes() {
        Collection<LLHArea> theStuff = new ArrayList<LLHArea>();

        // In only mode the selected volume is the only possible visible one
        if ((mySelectedVolume != null) && (mySelectedVolume.onlyMode)) {
            if (mySelectedVolume.checked) {
                theStuff.add(mySelectedVolume.airspace);
            }
        } else {
            for (VolumeTableData v : myHandlerList) {
                if (v.checked) {
                    theStuff.add(v.airspace);
                }
            }
        }
        return theStuff;
        //   return this.airspaces;
    }

    /** Set if a LLHArea is visible or not */
    @Deprecated
    public void setVisibleVolume(String aName, boolean flag) {
        for (VolumeTableData v : myHandlerList) {
            if (v.keyName.equals(aName)) {
                v.checked = flag;
                v.airspace.setVisible(flag);
                CommandManager.getInstance().getEarthBall().updateOnMinTime();
                break;
            }
        }
    }

    /** Set if a LLHArea is in only mode or not */
    @Deprecated
    public void setOnlyMode(String aName, boolean flag) {
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                v.onlyMode = flag;
                v.airspace.setOnly(flag);
                CommandManager.getInstance().getEarthBall().updateOnMinTime();
                break;
            }
        }
    }

    /** Set a collection of changes to a LLHArea */
    public void setLLHAreaChange(String aName, LLHAreaMemento myChange) {
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                // hack, really should use state in the LLHArea
                if (myChange.useVisible) {
                    v.checked = myChange.visible;
                }
                if (myChange.useOnly) {
                    v.onlyMode = myChange.only;
                }
                v.airspace.setFromMemento(myChange);
                CommandManager.getInstance().getEarthBall().updateOnMinTime();
                break;
            }
        }
    }

    public LLHArea getLLHArea(String aName) {
        LLHArea a = null;
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                a = v.airspace;
                break;
            }
        }
        return a;
    }
}
