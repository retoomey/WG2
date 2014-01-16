package org.wdssii.gui.features;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.MapFeature.MapMemento;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;

/**
 * MapGUI handles gui controls for a shapefile map....
 *
 * @author Robert Toomey
 */
public class MapGUI extends FeatureGUI {

    /**
     * The MapFeature we are using
     */
    private MapFeature myFeature;

    /**
     * Creates new form LLHAreaSliceGUI
     */
    public MapGUI(MapFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        MapMemento m = (MapMemento) myFeature.getNewMemento();
	updateToMemento(m);
    }

    private void setupComponents() {
	JScrollPane s = new JScrollPane();
	s.setViewportView(this);
	setRootComponent(s);

        setLayout(new MigLayout(new LC(), null, null));

        add(new IntegerGUI(myFeature, MapMemento.LINE_THICKNESS, "Line Thickness", this,
                1, 15, 1, "Pixels"));
	add(new ColorGUI(myFeature, MapMemento.LINE_COLOR, "Line Color", this));
    }

    /**
     * Load an individual file into the ManualLoadIndex
     */
    public static URL doSingleMapOpenDialog(JComponent center) {

        URL pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                // FIXME: need to get these from the Builders
                return (f.isDirectory() || t.endsWith(".shp"));
            }

            @Override
            public String getDescription() {
                return "ESRI Shapefile";
            }
        });
        chooser.setDialogTitle("Add single map");
        // rcp chooiser.setFilterPath("D:/") ?

        int returnVal = chooser.showOpenDialog(center);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL();
            } catch (MalformedURLException ex) {
                // We assume that chooser knows not to return
                // malformed urls...
            }
        }
        return pickedFile;
    }
}
