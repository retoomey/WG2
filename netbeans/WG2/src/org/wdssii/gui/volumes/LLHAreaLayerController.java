package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.pick.PickedObjectList;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureSelectCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.LLHAreaLayer;

/** This handles the mouse/key events for an LLHAreaLayer.
There is only one of these for all of our LLHAreas

@author Robert Toomey
 */
public class LLHAreaLayerController implements KeyListener, MouseListener, MouseMotionListener {

    private boolean active;
    private String activeAction;
    /** The WorldWindow we have a layer in */
    private final WorldWindow wwd;
    /** The volume drawing layer we are linked to */
    private final LLHAreaLayer volumeLayer;
    // Current selection and device state.
    private Point mousePoint;
    private LLHAreaFeature activeLLHArea;
    private LLHAreaControlPoint activeControlPoint;
    // Action/Cursor pairings.
    private Map<String, Cursor> actionCursorMap = new HashMap<String, Cursor>();
    protected static final String MOVE_AIRSPACE_LATERALLY = "MoveAirspaceLaterally";
    protected static final String MOVE_AIRSPACE_VERTICALLY = "MoveAirspaceVertically";
    protected static final String RESIZE_AIRSPACE = "ResizeAirspace";
    protected static final String ADD_CONTROL_POINT = "AddControlPoint";
    protected static final String REMOVE_CONTROL_POINT = "RemoveControlPoint";
    protected static final String MOVE_CONTROL_POINT = "MoveControlPoint";

    public LLHAreaLayerController(WorldWindow w, LLHAreaLayer volume) {
        this.active = false;
        this.wwd = w;
        this.volumeLayer = volume;
        this.setupActionCursorMap();
        InputHandler h = this.wwd.getInputHandler();
        setupListeners();
    }

    public final void setupListeners() {
        if (this.wwd != null) {
            InputHandler h = this.wwd.getInputHandler();
            h.addKeyListener(this);
            h.addMouseListener(this);
            h.addMouseMotionListener(this);
        }
    }

    public final void removeListeners() {
        if (this.wwd != null) {
            InputHandler h = this.wwd.getInputHandler();
            h.removeKeyListener(this);
            h.removeMouseListener(this);
            h.removeMouseMotionListener(this);
        }
    }

    public boolean isActive() {
        return this.active;
    }

    protected void setActive(boolean active) {
        this.active = active;
    }

    public String getActiveAction() {
        return activeAction;
    }

    protected void setActiveAction(String action) {
        this.activeAction = action;
    }

    public LLHAreaLayer getLLHAreaLayer() {
        return this.volumeLayer;
    }

    public WorldWindow getWorldWindow() {
        return this.wwd;
    }

    protected Point getMousePoint() {
        return this.mousePoint;
    }

    protected void setMousePoint(Point point) {
        this.mousePoint = point;
    }

    protected LLHAreaControlPoint getActiveControlPoint() {
        return this.activeControlPoint;
    }

    protected void setActiveControlPoint(LLHAreaControlPoint controlPoint) {
        this.activeControlPoint = controlPoint;
    }

    protected LLHAreaFeature getActiveAirspace() {
        return activeLLHArea;
    }

    protected void setActiveAirspace(LLHAreaFeature airspace) {
        this.activeLLHArea = airspace;
    }

    /** Picked object as an LLHArea */
    protected LLHAreaFeature asLLHAreaFeature(Object obj) {
        if (!(obj instanceof LLHArea)) {
            return null;
        }
        
        // Bleh! hack this for moment
        List<Feature> fl = FeatureList.theFeatures.getFeatures();
        Iterator<Feature> iter = fl.iterator();
        while(iter.hasNext()){
            Feature f = iter.next();
            if (f instanceof LLHAreaFeature){
                LLHAreaFeature a= (LLHAreaFeature)(f);
                LLHArea area = a.getLLHArea();
                if (area == obj){
                    return a;
                }
            }
        }
        return null;
    }

    /** Picked object as a LLHAreaControlPoint */
    protected LLHAreaControlPoint asLLHAreaControlPoint(Object obj) {
        if (!(obj instanceof LLHAreaControlPoint)) {
            return null;
        }
        return (LLHAreaControlPoint) obj;
    }

    protected Object getTopPickedObject() {
        if (this.getLLHAreaLayer() == null) {
            return null;
        }
        if (this.getWorldWindow() == null) {
            return null;
        }

        PickedObjectList pickedObjects = this.getWorldWindow().getObjectsAtCurrentPosition();
        if (pickedObjects == null || pickedObjects.getTopPickedObject() == null
                || pickedObjects.getTopPickedObject().isTerrain()) {
            return null;
        }

        return pickedObjects.getTopPickedObject().getObject();
    }

    protected Map<String, Cursor> getActionCursorMap() {
        return this.actionCursorMap;
    }

    //**************************************************************//
    //********************  Key Events  ****************************//
    //**************************************************************//
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e == null) {
            return;
        }

        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e == null) {
            return;
        }

        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    //**************************************************************//
    //********************  Mouse Events  **************************//
    //**************************************************************//
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e == null) {
            return;
        }

        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }
        Object obj = this.getTopPickedObject();
        LLHAreaControlPoint topControlPoint = this.asLLHAreaControlPoint(obj);

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.isControlDown()) {
                if (topControlPoint != null) {
                    this.handleControlPointRemoved(topControlPoint, e);
                }
                e.consume();
            }
            //else if (e.isAltDown())
            //{
            //    // Actual logic is handled in mousePressed, but we consume the event here to keep the any other
            //    // system from receiving it.
            //    e.consume();
            //}
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e == null) {
            return;
        }

        this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }

        // If mouse pressed over a LLHArea, we own them all so select it...
        Object obj = this.getTopPickedObject();
                        
        LLHAreaFeature topAirspace = this.asLLHAreaFeature(obj);
        if (topAirspace != null) {
            FeatureSelectCommand c = new FeatureSelectCommand(topAirspace);
            CommandManager.getInstance().executeCommand(c, true);
        }

        LLHAreaControlPoint topControlPoint = this.asLLHAreaControlPoint(obj);

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.isControlDown()) {
                // Actual logic is handled in mouseClicked, but we consume the event here to keep the any other
                // system from receiving it.
                this.setActive(true);
                this.setActiveAction(REMOVE_CONTROL_POINT);
                e.consume();
            } else if (e.isAltDown()) {
                this.setActive(true);
                this.setActiveAction(ADD_CONTROL_POINT);
                if (topControlPoint == null) {
                    LLHAreaControlPoint p = this.handleControlPointAdded(this.getLLHAreaLayer().getAirspace(), e);
                    if (p != null) {
                        this.setActiveControlPoint(p);
                    }
                }
                e.consume();
            } else {
                if (topControlPoint != null) {
                    this.setActive(true);
                    this.setActiveAction(null); // Don't know what action we'll perform until mouseDragged().
                    this.setActiveControlPoint(topControlPoint);
                    e.consume();
                } else if (topAirspace != null) {
                    this.setActive(true);
                    this.setActiveAction(null); // Don't know what action we'll perform until mouseDragged().
                    this.setActiveAirspace(topAirspace);
                    e.consume();
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e == null) {
            return;
        }

        this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (this.isActive()) {
                this.setActive(false);
                this.setActiveAction(null);
                this.setActiveAirspace(null);
                this.setActiveControlPoint(null);
                e.consume();
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (e == null) {
            return;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (e == null) {
            return;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
        }
    }

    protected LLHAreaControlPoint handleControlPointAdded(LLHArea airspace, MouseEvent mouseEvent) {
        LLHAreaControlPoint controlPoint = this.getLLHAreaLayer().addControlPoint(this.getWorldWindow(), airspace,
                mouseEvent.getPoint());
        this.getWorldWindow().redraw();

        return controlPoint;
    }

    protected void handleControlPointRemoved(LLHAreaControlPoint controlPoint, MouseEvent mouseEvent) {
        this.getLLHAreaLayer().removeControlPoint(this.getWorldWindow(), controlPoint);
    }

    //**************************************************************//
    //********************  Mouse Motion Events  *******************//
    //**************************************************************//
    @Override
    public void mouseDragged(MouseEvent e) {
        if (e == null) {
            return;
        }

        Point lastMousePoint = this.getMousePoint();
        this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return;
        }

        if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            if (this.isActive()) {
                if (this.getActiveControlPoint() != null) {
                    this.handleControlPointDragged(this.getActiveControlPoint(), e, lastMousePoint);
                } else if (this.getActiveAirspace() != null) {
                    this.handleLLHAreaDragged(this.getActiveAirspace(), e, lastMousePoint);
                }
                e.consume();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (e == null) {
            return;
        }

        this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
        this.updateCursor(e);

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    protected void handleControlPointDragged(LLHAreaControlPoint controlPoint, MouseEvent e, Point lastMousePoint) {
        if (e.isShiftDown()) {
            this.setActiveAction(RESIZE_AIRSPACE);
            this.getLLHAreaLayer().resizeAtControlPoint(this.getWorldWindow(), controlPoint, e.getPoint(), lastMousePoint);
        } else {
            this.setActiveAction(MOVE_CONTROL_POINT);
            this.getLLHAreaLayer().moveControlPoint(this.getWorldWindow(), controlPoint, e.getPoint(), lastMousePoint);
        }
    }

    protected void handleLLHAreaDragged(LLHAreaFeature f, MouseEvent e, Point lastMousePoint) {
        LLHArea area = f.getLLHArea();
        if (e.isShiftDown()) {
            this.setActiveAction(MOVE_AIRSPACE_VERTICALLY);
            this.getLLHAreaLayer().moveAirspaceVertically(this.getWorldWindow(), area, e.getPoint(), lastMousePoint);
        } else {
            this.setActiveAction(MOVE_AIRSPACE_LATERALLY);
            this.getLLHAreaLayer().moveAirspaceLaterally(this.getWorldWindow(), area, e.getPoint(), lastMousePoint);
        }
    }

    //**************************************************************//
    //********************  Action/Cursor Pairing  *****************//
    //**************************************************************//
    private void setupActionCursorMap() {
        // TODO: find more suitable cursors for the remove control point action, and the move vertically action.
        this.getActionCursorMap().put(MOVE_AIRSPACE_LATERALLY, Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        this.getActionCursorMap().put(MOVE_AIRSPACE_VERTICALLY, Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        this.getActionCursorMap().put(RESIZE_AIRSPACE, Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        this.getActionCursorMap().put(ADD_CONTROL_POINT, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        this.getActionCursorMap().put(REMOVE_CONTROL_POINT, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        this.getActionCursorMap().put(MOVE_CONTROL_POINT, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    protected void updateCursor(InputEvent e) {
        // Include this test to ensure any derived implementation performs it.
        if (e == null || e.getComponent() == null) {
            return;
        }

        Cursor cursor = this.getCursorFor(e);
        e.getComponent().setCursor(cursor);
        e.getComponent().repaint();
    }

    protected Cursor getCursorFor(InputEvent e) {
        // If we're actively engaged in some action, then return the cursor associated with that action. Otherwise
        // return the cursor representing the action that would be invoked (if the user pressed the mouse) given the
        // curent modifiers and pick list.

        if (e == null) {
            return null;
        }

        // Include this test to ensure any derived implementation performs it.
        if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
            return null;
        }

        String action = this.isActive() ? this.getActiveAction() : this.getPotentialActionFor(e);
        return this.getActionCursorMap().get(action);
    }

    protected String getPotentialActionFor(InputEvent e) {
        Object obj = this.getTopPickedObject();
        LLHAreaFeature area = this.asLLHAreaFeature(obj);
        LLHAreaControlPoint topControlPoint = this.asLLHAreaControlPoint(obj);

        if (e.isAltDown()) {
            if (topControlPoint == null) {
                return ADD_CONTROL_POINT;
            }
        } else if (e.isControlDown()) {
            if (topControlPoint != null) {
                return REMOVE_CONTROL_POINT;
            }
        } else if (e.isShiftDown()) {
            if (topControlPoint != null) {
                return RESIZE_AIRSPACE;
            } else if (area != null) {
                return MOVE_AIRSPACE_VERTICALLY;
            }
        } else {
            if (topControlPoint != null) {
                return MOVE_CONTROL_POINT;
            } else if (area != null) {
                return MOVE_AIRSPACE_LATERALLY;
            }
        }

        return null;
    }
}
