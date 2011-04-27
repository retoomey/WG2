package org.wdssii.gui.volumes;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.render.DrawContext;

public interface LLHAreaDetailLevel extends Comparable<LLHAreaDetailLevel>, AVList {

    boolean meetsCriteria(DrawContext dc, LLHArea airspace);

    @Override
    int compareTo(LLHAreaDetailLevel level);
}
