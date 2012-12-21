package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.pick.PickedObjectList;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureSelectCommand;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.LLHAreaLayer;

/**
 * This handles the mouse/key events for an LLHAreaLayer. There is only one of
 * these for all of our LLHAreas
 *
 * @author Robert Toomey
 */
public class LLHAreaLayerController implements KeyListener, MouseListener, MouseMotionListener {

	private static Logger log = LoggerFactory.getLogger(LLHAreaLayerController.class);
	private ActionMode activeAction = ActionMode.NO_ACTION;
	/**
	 * The WorldWindow we have a layer in
	 */
	private final WorldWindow wwd;
	/**
	 * The volume drawing layer we are linked to
	 */
	private final LLHAreaLayer volumeLayer;
	// Current selection and device state.
	private Point mousePoint;
	private LLHAreaFeature activeLLHArea;
	private LLHAreaControlPoint activeControlPoint;
	private DFAMode myDFAMode = DFAMode.NO_ACTION;

	public enum DFAMode {

		NO_ACTION, // The 'ready' state, or stopped
		DRAGGING_CONTROL_POINT, // Dragging a control point
	}

	// Current action we are doing with mouse
	public enum ActionMode {

		NO_ACTION(),
		MOVE_AIRSPACE_LATERALLY(Cursor.MOVE_CURSOR),
		MOVE_AIRSPACE_VERTICALLY(Cursor.N_RESIZE_CURSOR),
		RESIZE_AIRSPACE(Cursor.N_RESIZE_CURSOR),
		ADD_CONTROL_POINT(Cursor.CROSSHAIR_CURSOR),
		REMOVE_CONTROL_POINT(Cursor.CROSSHAIR_CURSOR),
		MOVE_CONTROL_POINT(Cursor.HAND_CURSOR);
		private Cursor actionCursor;

		private ActionMode() {
			actionCursor = null;
		}

		private ActionMode(int c) {
			actionCursor = Cursor.getPredefinedCursor(c);
		}
	}

	// Really this should belong to the earth ball itself I think... FIXME
	public enum MouseMode {

		MOUSE_INACTIVE, // Ignore mouse events completely
		MOUSE_NORMAL, // Selection and drag when over control points
		MOUSE_ADD_REMOVE, // Add with click, remove with shift-click
		MOUSE_REMOVE       // Remove with click (explicit remove tool)
	}

	public enum MouseAction {

		MOUSE_DOWN,
		MOUSE_UP,
		MOUSE_DRAGGED,
		MOUSE_MOVED
	}
	private MouseMode myMouseMode = MouseMode.MOUSE_NORMAL;

	public LLHAreaLayerController(WorldWindow w, LLHAreaLayer volume) {
		this.wwd = w;
		this.volumeLayer = volume;
		//this.setupActionCursorMap();
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

	/**
	 * Get the action we are currently doing...
	 */
	public ActionMode getActiveAction() {
		return activeAction;
	}

	/**
	 * Set the action we are currently doing...
	 */
	protected void setActiveAction(ActionMode action) {
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

	/**
	 * Picked object as an LLHArea
	 */
	protected LLHAreaFeature asLLHAreaFeature(Object obj) {
		if (!(obj instanceof LLHArea)) {
			return null;
		}

		// Bleh! hack this for moment
		List<Feature> fl = FeatureList.theFeatures.getFeatures();
		Iterator<Feature> iter = fl.iterator();
		while (iter.hasNext()) {
			Feature f = iter.next();
			if (f instanceof LLHAreaFeature) {
				LLHAreaFeature a = (LLHAreaFeature) (f);
				LLHArea area = a.getLLHArea();
				if (area == obj) {
					return a;
				}
			}
		}
		return null;
	}

	/**
	 * Picked object as a LLHAreaControlPoint
	 */
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

	//**************************************************************//
	//********************  Key Events  ****************************//
	//**************************************************************//
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		log.debug("LLH key pressed");
		if (e == null) {
			return;
		}

//		this.updateCursor(e);

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			//noinspection UnnecessaryReturnStatement
			return;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		log.debug("LLH key released");
		if (e == null) {
			return;
		}

//		this.updateCursor(e);

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			//noinspection UnnecessaryReturnStatement
			return;
		}
	}

	//**************************************************************//
	//********************  Mouse Events  **************************//
	//**************************************************************//
	/**
	 * Linux window managers steal cntl and alt keys now. 3D window manager
	 * takes the alt click to move entire window...so we switch to a mouse
	 * mode
	 */
	public void setMouseMode(MouseMode aMode) {
		myMouseMode = aMode;
	}

	public MouseMode getMouseMode() {
		return myMouseMode;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		log.debug("Mouse clicked LLH");
		/*
		 * if (e == null) { return; }
		 *
		 * this.updateCursor(e);
		 *
		 * // Include this test to ensure any derived implementation
		 * performs it. if (this.getLLHAreaLayer() == null ||
		 * !this.getLLHAreaLayer().isArmed()) { return; } Object obj =
		 * this.getTopPickedObject(); LLHAreaControlPoint
		 * topControlPoint = this.asLLHAreaControlPoint(obj);
		 *
		 * if (e.getButton() == MouseEvent.BUTTON1) { if
		 * (e.isControlDown()) { if (topControlPoint != null) {
		 * this.handleControlPointRemoved(topControlPoint, e); }
		 * e.consume(); } //else if (e.isAltDown()) //{ // // Actual
		 * logic is handled in mousePressed, but we consume the event
		 * here to keep the any other // // system from receiving it. //
		 * e.consume(); //} }
		 */
	}

	@Override
	public void mousePressed(MouseEvent e) {
		log.debug("Mouse pressed LLH");
		if (e == null) {
			return;
		}
		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			return;
		}
		processDFA(e, MouseAction.MOUSE_DOWN);

		/*
		 * // copy to insulate us from changes by the caller
		 * this.setMousePoint(new Point(e.getPoint())); ActionMode a =
		 * this.updateCursor(e, MouseAction.MOUSE_DOWN); if (myMouseMode
		 * == MouseMode.MOUSE_INACTIVE) { return; }
		 *
		 * // If mouse pressed over a LLHArea, we own them all so
		 * select it... Object obj = this.getTopPickedObject();
		 *
		 * LLHAreaFeature topAirspace = this.asLLHAreaFeature(obj); if
		 * (topAirspace != null) { FeatureSelectCommand c = new
		 * FeatureSelectCommand(topAirspace);
		 * CommandManager.getInstance().executeCommand(c, true); }
		 * LLHAreaControlPoint topControlPoint =
		 * this.asLLHAreaControlPoint(obj);
		 *
		 * // If clicked make potential into active action... if
		 * (e.getButton() == MouseEvent.BUTTON1) { activeAction = a; }
		 * else { activeAction = ActionMode.NO_ACTION; }
		 *
		 * switch (activeAction) { case NO_ACTION: default: break; case
		 * MOVE_AIRSPACE_LATERALLY: break; case
		 * MOVE_AIRSPACE_VERTICALLY: break; case RESIZE_AIRSPACE: break;
		 * case ADD_CONTROL_POINT: // Don't add on top of an existing
		 * point... if (topControlPoint == null) {
		 *
		 * // Add point on mouse down and make active...continuing to
		 * // drag will move the new point LLHAreaControlPoint p =
		 * this.handleControlPointAdded(this.getLLHAreaLayer().getAirspace(),
		 * e); if (p != null) { this.setActiveControlPoint(p); } }
		 * break; case REMOVE_CONTROL_POINT: break; case
		 * MOVE_CONTROL_POINT: break; }
		 *
		 * if (activeAction != ActionMode.NO_ACTION) { e.consume(); }
		 */

		// Linux window managers can steal cntl and alt keys now it seems...
		// so we go with a 'mode' 
		/*
		 * if (e.getButton() == MouseEvent.BUTTON1) {
		 *
		 * // What we do depends on mouse mode switch (myMouseMode) {
		 * case MOUSE_INACTIVE: break; // already handled case
		 * MOUSE_ADD_REMOVE:
		 * this.setActiveAction(ActionMode.REMOVE_CONTROL_POINT);
		 * e.consume(); break; case MOUSE_REMOVE: break; }
		 *
		 * if (e.isControlDown()) { // Actual logic is handled //j	in
		 * mouseClicked, but we consume the event here to // keep the
		 * any other // system from receiving it. this.setActive(true);
		 * this.setActiveAction(REMOVE_CONTROL_POINT); e.consume(); }
		 * else if (e.isAltDown()) { this.setActive(true);
		 * this.setActiveAction(ADD_CONTROL_POINT); if (topControlPoint
		 * == null) { LLHAreaControlPoint p =
		 * this.handleControlPointAdded(this.getLLHAreaLayer().getAirspace(),
		 * e); if (p != null) { this.setActiveControlPoint(p); } }
		 * e.consume(); } else { if (topControlPoint != null) {
		 * this.setActive(true); this.setActiveAction(null); // Don't
		 * know what action we'll perform until mouseDragged().
		 *
		 * this.setActiveControlPoint(topControlPoint); e.consume(); }
		 * else if (topAirspace != null) { this.setActive(true);
		 * this.setActiveAction(null); // Don
		 *
		 *
		 * 't know what action we'll perform until mouseDragged ().
		 * this.setActiveAirspace(topAirspace); e.consume(); } }
		 *
		 * }
		 *
		 */
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		log.debug("Mouse released LLH");
		if (e == null) {
			return;
		}

		processDFA(e, MouseAction.MOUSE_UP);
		/*
		 * this.setMousePoint(new Point(e.getPoint())); // copy to
		 * insulate us from changes by the caller this.updateCursor(e,
		 * MouseAction.MOUSE_UP);
		 *
		 * // Include this test to ensure any derived implementation
		 * performs it. if (this.getLLHAreaLayer() == null ||
		 * !this.getLLHAreaLayer().isArmed()) { return; } if
		 * (myMouseMode == MouseMode.MOUSE_INACTIVE) { return; }
		 *
		 * // Consume any mouse release if (e.getButton() ==
		 * MouseEvent.BUTTON1) { if (this.getActiveAction() !=
		 * ActionMode.NO_ACTION) {
		 * this.setActiveAction(ActionMode.NO_ACTION);
		 * this.setActiveAirspace(null);
		 * this.setActiveControlPoint(null); e.consume(); } }
		 *
		 */
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		log.debug("MOUSE ENTERED LLH");
		if (e == null) {
			return;
		}

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		log.debug("MOUSE EXITED LLH");
		if (e == null) {
			return;
		}

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
		}
	}

	protected void handleControlPointRemoved(LLHAreaControlPoint controlPoint, MouseEvent mouseEvent) {
		this.getLLHAreaLayer().removeControlPoint(this.getWorldWindow(), controlPoint);
	}

	//**************************************************************//
	//********************  Mouse Motion Events  *******************//
	//**************************************************************//
	@Override
	public void mouseDragged(MouseEvent e) {
		log.debug("LLH AREA Mouse DRAGGED ");
		if (e == null) {
			return;
		}

		//Point lastMousePoint = this.getMousePoint();
		//this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
		this.updateCursor(e, MouseAction.MOUSE_DRAGGED);

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			return;
		}

		processDFA(e, MouseAction.MOUSE_DRAGGED);

		/*
		 * if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
		 * { if (this.getActiveAction() != ActionMode.NO_ACTION) { if
		 * (this.getActiveControlPoint() != null) {
		 * this.handleControlPointDragged(this.getActiveControlPoint(),
		 * e, lastMousePoint); } else if (this.getActiveAirspace() !=
		 * null) { this.handleLLHAreaDragged(this.getActiveAirspace(),
		 * e, lastMousePoint); } e.consume(); } }
		 *
		 */
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		log.debug("LLH AREA Mouse moved ");
		if (e == null) {
			return;
		}

		this.setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller
		this.updateCursor(e, MouseAction.MOUSE_MOVED);

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			//noinspection UnnecessaryReturnStatement
			return;
		}
	}

	protected void handleLLHAreaDragged(LLHAreaFeature f, MouseEvent e, Point lastMousePoint) {
		LLHArea area = f.getLLHArea();
		if (e.isShiftDown()) {
			this.setActiveAction(ActionMode.MOVE_AIRSPACE_VERTICALLY);
			this.getLLHAreaLayer().moveAirspaceVertically(this.getWorldWindow(), area, e.getPoint(), lastMousePoint);
		} else {
			this.setActiveAction(ActionMode.MOVE_AIRSPACE_LATERALLY);
			this.getLLHAreaLayer().moveAirspaceLaterally(this.getWorldWindow(), area, e.getPoint(), lastMousePoint);
		}
	}

	protected ActionMode updateCursor(InputEvent e, MouseAction m) {
		// Include this test to ensure any derived implementation performs it.
		if (e == null || e.getComponent() == null) {
			return ActionMode.NO_ACTION;
		}

		// Include this test to ensure any derived implementation performs it.
		if (this.getLLHAreaLayer() == null || !this.getLLHAreaLayer().isArmed()) {
			return ActionMode.NO_ACTION;
		}

		Cursor cursor;
		ActionMode action = getPotentialActionFor(e, m);

		// The cursor part
		cursor = action.actionCursor;
		e.getComponent().setCursor(cursor);
		e.getComponent().repaint();

		return action;
	}

	/**
	 * Get the action we would do for a given event...
	 */
	protected ActionMode getPotentialActionFor(InputEvent e, MouseAction m) {
		Object obj = this.getTopPickedObject();
		LLHAreaFeature area = this.asLLHAreaFeature(obj);
		LLHAreaControlPoint topControlPoint = this.asLLHAreaControlPoint(obj);

		switch (myMouseMode) {
			case MOUSE_INACTIVE:
			default:
				return null;

			case MOUSE_ADD_REMOVE:
				// If no point under us, we'll add a point
				if (topControlPoint == null) {
					return ActionMode.ADD_CONTROL_POINT;
				} else {
					// Point under us with shift, delete a point...
					if (e.isShiftDown()) {
						return ActionMode.REMOVE_CONTROL_POINT;
					} else {
						// Move this point....
						return ActionMode.MOVE_CONTROL_POINT;
					}
				}
			// Do nothing on mouse up or drag 

			case MOUSE_NORMAL:
				if (e.isShiftDown()) {
					if (topControlPoint != null) {
						return ActionMode.RESIZE_AIRSPACE;
					} else if (area != null) {
						return ActionMode.MOVE_AIRSPACE_VERTICALLY;
					}
				} else { // shift not down...
					if (topControlPoint != null) {
						return ActionMode.MOVE_CONTROL_POINT;
					} else if (area != null) {
						return ActionMode.MOVE_AIRSPACE_LATERALLY;
					}

				}
		}
		return ActionMode.NO_ACTION;
	}

	/**
	 * Handle all mouse action with a DFA state machine
	 */
	protected void processDFA(MouseEvent e, MouseAction m) {

		// For input we get the area and or point we are over...
		Object obj = getTopPickedObject();
		LLHAreaFeature area = asLLHAreaFeature(obj);
		LLHAreaControlPoint newPoint = asLLHAreaControlPoint(obj);

		// Keep last mouse point and next always...hummm
		Point lastMousePoint = getMousePoint();
		setMousePoint(new Point(e.getPoint())); // copy to insulate us from changes by the caller

		log.debug("DFA STATE IS " + myDFAMode);
		LLHAreaLayer l = getLLHAreaLayer();
		WorldWindow w = getWorldWindow();
		switch (myDFAMode) {
			case NO_ACTION:
				// Handle mouse button 1 press...
				if ((m == MouseAction.MOUSE_DOWN) && (e.getButton() == MouseEvent.BUTTON1)) {

					// Don't add on top of an existing point...
					if (newPoint == null) {

						// Add point on mouse down and make active...continuing to
						// drag will move the new point
						newPoint = l.addControlPoint(w, l.getAirspace(), e.getPoint());
						w.redraw();

						if (newPoint != null) {
							setActiveControlPoint(newPoint);
						}
					} else {
						setActiveControlPoint(newPoint);

					}
					// Go into drag mode on new or current point...
					myDFAMode = DFAMode.DRAGGING_CONTROL_POINT;

//					e.consume();  allow dragging still...
				}
				break;
			case DRAGGING_CONTROL_POINT:

				if (m == MouseAction.MOUSE_DRAGGED) {
					if (getActiveControlPoint() != null) {
						if (e.isShiftDown()) {
					//		setActiveAction(ActionMode.RESIZE_AIRSPACE);
							l.resizeAtControlPoint(w, getActiveControlPoint(), e.getPoint(), lastMousePoint);
						} else {
						//	setActiveAction(ActionMode.MOVE_CONTROL_POINT);
						l.doMoveControlPoint(w, getActiveControlPoint(), e.getPoint(), lastMousePoint);
						}
					
						e.consume();// keep window from dragging....
					}
				} else if ((m == MouseAction.MOUSE_UP) && (e.getButton() == MouseEvent.BUTTON1)) {
					setActiveControlPoint(null);
					myDFAMode = DFAMode.NO_ACTION;
				}
//				e.consume();
				break;
		}
	}
}
