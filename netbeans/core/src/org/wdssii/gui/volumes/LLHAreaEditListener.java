package org.wdssii.gui.volumes;

import java.util.EventListener;

public interface LLHAreaEditListener extends EventListener {

    void airspaceMoved(LLHAreaEditEvent e);

    void airspaceResized(LLHAreaEditEvent e);

    void controlPointAdded(LLHAreaEditEvent e);

    void controlPointRemoved(LLHAreaEditEvent e);

    void controlPointChanged(LLHAreaEditEvent e);
}
