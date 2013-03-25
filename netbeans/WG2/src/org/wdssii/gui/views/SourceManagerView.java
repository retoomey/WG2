package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;

public interface SourceManagerView extends CommandListener {

    public static final String ID = "wj.SourceManagerView";

    void update();
}
