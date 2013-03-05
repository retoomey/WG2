package org.wdssii.gui.products;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.DataType;
import org.wdssii.gui.features.FeatureGUI;

/**
 * Create the GUI for a product feature
 *
 * @author Robert Toomey
 */
public class ProductFeatureGUI extends FeatureGUI {

    private static Logger log = LoggerFactory.getLogger(ProductFeatureGUI.class);

    private ProductFeature myProduct;

    public ProductFeatureGUI(ProductFeature p) {
        myProduct = p;
        setupComponents();
    }

    @Override
    public void updateGUI() {
    }

    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        doLayout();
    }

    @Override
    public void deactivateGUI() {
    }

    private void setupComponents() {

        setLayout(new MigLayout(new LC(), null, null));
        JButton export = new JButton("Export...");
        export.setToolTipText("Export data as ESRI file");
        add(export, new CC());
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jExportActionPerformed(e);
            }
        });
        
        JButton Test = new JButton("Test1");
        add(Test, new CC());
        Test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("Test 1");
            }
        });
        
        JButton Test2 = new JButton("Test2");
        add(Test2, new CC());
        Test2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("Test 2");
            }
        });
    }

    public void jExportActionPerformed(ActionEvent e) {
        JFileChooser fileopen = new JFileChooser();
        fileopen.setDialogType(JFileChooser.SAVE_DIALOG);
        fileopen.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                return (f.isDirectory() || t.endsWith(".shp"));
            }

            @Override
            public String getDescription() {
                return "ERSI SHP File Format";
            }
        });
        fileopen.setDialogTitle("Export Quad Polygons and Values");
        int ret = fileopen.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            try {
                // Bim's format....
                URL aURL = file.toURI().toURL();
                //  log.debug("Would try to write to " + aURL.toString());
                if (myProduct != null) {
                    DataType d = myProduct.getLoadedDatatype();
                    if (d != null) {
                        d.exportToESRI(aURL);
                    } else {
                        // warn or something?
                    }
                }
            } catch (MalformedURLException ex) {
            }
        }
    }
}
