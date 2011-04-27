package org.wdssii.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.wdssii.gui.views.EarthBallView;
import org.wdssii.gui.views.LLHAreaView;
import org.wdssii.gui.views.WdssiiView;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaBoxFactory;
import org.wdssii.gui.volumes.LLHAreaLayerController;
import org.wdssii.gui.volumes.LLHAreaFactory;
import org.wdssii.gui.volumes.LLHAreaHeightStickFactory;
import org.wdssii.gui.volumes.LLHAreaSliceFactory;
import org.wdssii.gui.worldwind.LLHAreaLayer;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;

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
        // public LLHAreaEditor editor;
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

    /** Create a new volume in the display */
    public void createNewVolume() {
        // FIXME: make a factory for each type of volume object
        LLHAreaLayer layer = getVolumeLayer();
        if (layer != null) {

            boolean success = false;
            WorldWindowGLCanvas world = getWorld();
            VolumeTableData theData = new VolumeTableData();

            if (myCurrentFactory != null) {
                success = myCurrentFactory.create(world, theData);
                if (success) {
                    //  editor.addEditListener(this); not sure I need this
                    layer.addLLHArea(theData.airspace);
                    myHandlerList.add(theData);
                }
            }

            // Create an editor controller if none exists (only do once)
            // FIXME: probably should be done on startup somewhere
            if (editorController == null) {
                this.editorController = new LLHAreaLayerController();

                // The ordering is important here; we want first pass at mouse events.
                this.editorController.setWorldWindow(world);

                this.editorController.setEditor(layer);
            }

            // Since theData is a NEW volume, select will do the GUI update as well
            if (success) {
                selectVolume(theData);
            }
        }

    }

    /** Select the given volume in the list and in the gl window.  This means turning on the
     * editing controls
     * @param theVolume the volume to turn on
     */
    public void selectVolume(VolumeTableData theVolume) {

        // If it's a different volume than selected...
        if (theVolume != mySelectedVolume) {
            //System.out.println("Select volume called "+theVolume);

            WorldWindowGLCanvas world = getWorld(); // What if world is different now???

            /*
             * Old 'different' layer editor...we merged it into one layer...
            // Remove any current selected volume from the editor layer
            if (mySelectedVolume != null){
            mySelectedVolume.editor.setArmed(false);
            removeEditorFromLayers(world, mySelectedVolume.editor);
            }
            
            // Update the current selected in the editor layer....
            mySelectedVolume = theVolume;
            editorController.setEditor(theVolume.editor);
            theVolume.editor.setArmed(true);
            addEditorToLayers(world, theVolume.editor);
             */
            mySelectedVolume = theVolume;
            // Show it.
            world.redraw();

            // Whenever selection changes, make sure the GUI list is synced
            updateLLHAreaView();
        }
    }

    /** Remove volume editor to the worldwind layers.  This turns off its drawing controls */
    /*public void removeEditorFromLayers(WorldWindowGLCanvas world, LLHAreaEditor editor)
    {
    // Place it before compass layer for the moment
    LayerList layers = world.getModel().getLayers();
    layers.remove(editor);
    }*/
    /** Add volume editor to the worldwind layers.  This allows it to draw its controls in the window */
    /*public void addEditorToLayers(WorldWindowGLCanvas world, LLHAreaEditor editor)
    {
    // Place it before compass layer for the moment
    LayerList layers = world.getModel().getLayers();
    int compassPosition = 0;
    
    for (Layer l : layers)
    {
    if (l instanceof PlaceNameLayer)
    compassPosition = layers.indexOf(l);
    }
    layers.add(compassPosition, editor);
    }*/
    /** Get the volume layer we use.  Currently only one earthball */
    private LLHAreaLayer getVolumeLayer() {
        LLHAreaLayer world = null;

        WdssiiView view = CommandManager.getInstance().getNamedViewed(EarthBallView.ID);
        if (view instanceof EarthBallView) {
            EarthBallView earth = (EarthBallView) (view);
            world = earth.getVolumeLayer();
        }
        return world;
    }

    /** Get the world we use.  Currently only one earthball */
    private WorldWindowGLCanvas getWorld() {
        WorldWindowGLCanvas world = null;

        WdssiiView view = CommandManager.getInstance().getNamedViewed(EarthBallView.ID);
        if (view instanceof EarthBallView) {
            EarthBallView earth = (EarthBallView) (view);
            world = earth.getWwd();
        }
        return world;
    }

    /** Get the world we use.  Currently only one earthball */
    private void updateLLHAreaView() {
        LLHAreaView v = null;

        WdssiiView view = CommandManager.getInstance().getNamedViewed(LLHAreaView.ID);
        if (view instanceof LLHAreaView) {
            v = (LLHAreaView) (view);
        }
        if (v != null) {
            v.update();
        }
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
                updateLLHAreaView();
            }

        }
    }

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

        // In only mode the selected volume is the only visible one
        if ((mySelectedVolume != null) && (mySelectedVolume.onlyMode)) {
            theStuff.add(mySelectedVolume.airspace);
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

    public void toggleVisibleVolume(String aName) {
        System.out.println("Looking to toggle " + aName);
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                //System.out.println("Found it, old value is "+v.checked+" now "+v);
                v.checked = !v.checked;
                break;
            }
        }
    }

    public void setVisibleVolume(String aName, boolean flag) {
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                v.checked = flag;
                break;
            }
        }
    }

    public void setOnlyMode(String aName, boolean flag) {
        for (VolumeTableData v : myHandlerList) {
            if (v.visibleName.equals(aName)) {
                v.onlyMode = flag;
                break;
            }
        }
    }
}
