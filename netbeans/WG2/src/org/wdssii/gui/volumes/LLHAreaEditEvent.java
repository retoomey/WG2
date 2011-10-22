package org.wdssii.gui.volumes;

import java.util.EventObject;

import org.wdssii.gui.worldwind.LLHAreaLayer;

public class LLHAreaEditEvent extends EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 6137494129018063334L;
    private LLHArea airspace;
    private LLHAreaControlPoint controlPoint;

    public LLHAreaEditEvent(Object source, LLHArea airspace, LLHAreaLayer airspace2, LLHAreaControlPoint controlPoint) {
        super(source);
        this.airspace = airspace;
        this.controlPoint = controlPoint;
    }

    public LLHAreaEditEvent(Object source, LLHArea airspace, LLHAreaLayer editor) {
        this(source, airspace, editor, null);
    }

    public LLHArea getAirspace() {
        return this.airspace;
    }

    public LLHAreaControlPoint getControlPoint() {
        return this.controlPoint;
    }
}