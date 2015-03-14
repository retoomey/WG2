package org.wdssii.gui.worldwind;

import java.net.MalformedURLException;
import java.net.URL;
import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.sources.Source;

/**
 *
 * @author Robert Toomey
 */
public class WMSSourceAddParams extends SourceAddParams {

    public WMSSourceAddParams(String aNiceName, URL aSourceURL, boolean connect) {
        super(aNiceName, aSourceURL, connect);
    }

    public WMSSourceAddParams(String aNiceName, String path, boolean connect) throws MalformedURLException {
        super(aNiceName, path, connect);
    }

    @Override
    public Source createSource() {
        return new WMSSource(getNiceName(), getSourceURL());
    }
}