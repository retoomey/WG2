package org.wdssii.gui.views;

import java.awt.Component;
import java.util.List;
import javax.swing.Icon;
import net.infonode.docking.*;
import net.infonode.docking.properties.DockingWindowProperties;
import net.infonode.docking.properties.ViewProperties;
import net.infonode.docking.properties.ViewTitleBarProperties;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.DockWindow;

/**
 * This created a docking view window with a 'main' set of contents and
 * controls, with a single child sub-docked window that has controls that are
 * linked to main.
 *
 * @author Robert Toomey
 */
public abstract class WdssiiSDockedViewFactory extends WdssiiDockedViewFactory {

	private final static Logger LOG = LoggerFactory.getLogger(WdssiiSDockedViewFactory.class);

	/**
	 * Interface for creating parts of a sub dock view
	 */
	public static interface SDockView extends DockView {

		/** Get the component for the single control sub-view */
		public Component getControlComponent();

		/** Get the window title for the single control sub-view */
		public String getControlTitle();
	}

	public WdssiiSDockedViewFactory(String title, String icon) {
		super(title, icon);
	}
	/**
	 * Create a sub-bock for gui controls, or a split pane
	 */
	public static final boolean myDockControls = true;

	@Override
	public DockingWindow getNewDockingWindow() {
		if (!myDockControls) {
			// Get a single non-docked FeatureView component
			return super.getNewDockingWindow();
		} else {
			Icon i = getWindowIcon();
			String title = getWindowTitle();

			Component f = getNewComponent();

			// Create a RootWindow in the view.  Anything added to this
			// will be movable.
			RootWindow root = DockWindow.createARootWindow();
			View topWindow = new View(title, i, root);

			// Create a special child view that cannot be destroyed, only redocked
			if (f instanceof SDockView) {
				SDockView s = (SDockView) (f);

				// Add global controls
				s.addGlobalCustomTitleBarComponents(getCustomTitleBarComponents(topWindow));

				// Inside the root window we'll add two views...
				String ct = s.getControlTitle();
				View controls = new View(ct, i, s.getControlComponent());
				View select = new View(ct+" selection", i, f);

				// The select is our 'root', so make it non-dragable.  Basically
				// the main view will be the actual holder for this.  By making it a view,
				// we allow the other view to be docked above it, etc..
				ViewProperties vp = select.getViewProperties();
				ViewTitleBarProperties tp = vp.getViewTitleBarProperties();
				tp.setVisible(false);

				// Since menu allows changing, make a new window properties
				DockingWindowProperties org = controls.getWindowProperties();
				org.setCloseEnabled(false);
				org.setMaximizeEnabled(false);
				org.setMinimizeEnabled(false);

				SplitWindow w = new SplitWindow(false, select, controls);
				root.setWindow(w);
				// Add a 'close' listener to our internal root so that if the
				// control window is closed we redock instead.. (we could close
				// it but we'll need some control to get it back then)
				// Add a listener which shows dialogs when a window is closing or closed.
				root.addListener(new DockingWindowAdapter() {

					@Override
					public void windowClosing(DockingWindow window)
						throws OperationAbortedException {
						window.dock();
					}
				});
			}

			return topWindow;
		}
	}
}
